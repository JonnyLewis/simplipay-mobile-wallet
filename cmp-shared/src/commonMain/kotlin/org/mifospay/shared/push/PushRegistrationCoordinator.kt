/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.push

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.mifospay.core.datastore.UserPreferencesRepository

/**
 * Drives the push-token lifecycle off the session state:
 *  - authenticated session with a real client → request the OS notification
 *    permission (once per session) and upload the FCM token to the
 *    notification engine whenever it appears or rotates;
 *  - session ends → unregister the previously uploaded token so the device
 *    stops receiving account-bound pushes.
 *
 * Registration retries with capped exponential backoff until it succeeds, so
 * an engine that is unreachable at login still gets the token once it's back.
 * `collectLatest` cancels an in-flight retry loop whenever the session state
 * or token changes, so retries never act on stale state.
 *
 * Started once from the app shell ([org.mifospay.shared.MifosPaySharedApp]).
 */
class PushRegistrationCoordinator(
    private val preferencesRepository: UserPreferencesRepository,
    private val registrar: PushDeviceRegistrar,
) {

    private var registeredToken: String? = null
    private var permissionRequested = false

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                preferencesRepository.userInfo.map { it.authenticated },
                preferencesRepository.clientId,
                PushBridge.token,
            ) { authenticated, clientId, token ->
                Triple(authenticated, clientId, token)
            }
                .distinctUntilChanged()
                .collectLatest { (authenticated, clientId, token) ->
                    if (authenticated && clientId != null && clientId != 0L) {
                        if (!permissionRequested) {
                            // Only latch the flag if a platform requester actually ran,
                            // so a platform that wires the bridge late still prompts.
                            permissionRequested = PushBridge.requestPermission()
                        }
                        if (token != null && token != registeredToken) {
                            registerWithRetry(clientId, token)
                        }
                    } else if (!authenticated) {
                        permissionRequested = false
                        registeredToken?.let { old ->
                            runCatching { registrar.unregister(old) }
                                .onFailure {
                                    Logger.w(
                                        tag = TAG,
                                        messageString = "device unregister failed: ${it.message}",
                                    )
                                }
                            registeredToken = null
                        }
                    }
                }
        }
    }

    private suspend fun registerWithRetry(clientId: Long, token: String) {
        var backoffMs = INITIAL_BACKOFF_MS
        while (true) {
            try {
                registrar.register(clientId, token)
                registeredToken = token
                Logger.d(tag = TAG, messageString = "device token registered for client $clientId")
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(
                    tag = TAG,
                    messageString = "device registration failed (${e.message}); retrying in ${backoffMs}ms",
                )
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    private companion object {
        const val TAG = "PushRegistration"
        const val INITIAL_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 300_000L
    }
}

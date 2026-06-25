/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mifos.authenticator.passcode.PasscodeStorageAdapter
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.AppLockRepository
import org.mifospay.core.data.repository.ClientRepository
import org.mifospay.core.datastore.UserPreferencesRepository
import org.mifospay.core.model.user.RoleInfo
import org.mifospay.core.model.user.UserInfo
import org.mifospay.core.model.user.WALLET_REVOKED_PROBE_MARKER
import org.mifospay.core.model.user.WalletAccessState
import org.mifospay.core.model.user.resolveWalletAccess
import org.mifospay.passcode.PasscodeManager

/**
 * Root ViewModel scoped to [org.mifospay.shared.MifosPayApp].
 *
 * Aggregates the three pieces of state the app shell needs to make session
 * decisions:
 *  - the persisted user-info flow ([UserPreferencesRepository.userInfo]),
 *    surfaced as [userState];
 *  - the passcode storage adapter, used by [isPasscodeNotCreated] for the
 *    "logged in but no passcode yet" branch;
 *  - the app-wide lock flag ([AppLockRepository]) consulted by [isAppLocked]
 *    for the 15-second background-resume re-auth gate.
 *
 * [logOut] is the single fan-out point that clears the user session, the lock
 * flag, and the passcode all at once — every logout path in the app should
 * funnel through here.
 */
class MifosPayViewModel(
    private val userDataRepository: UserPreferencesRepository,
    private val passcodeManager: PasscodeManager,
    private val appLockRepository: AppLockRepository,
    private val passcodeStorageAdapter: PasscodeStorageAdapter,
    private val clientRepository: ClientRepository,
) : ViewModel() {
    /**
     * Reactive session state. Starts as [UserState.UnAuthenticated] and
     * transitions to [UserState.Authenticated] once the underlying user-info
     * flow emits — note that the wrapped `UserInfo.authenticated` field can
     * still be `false` on a logged-out account, so callers must check both
     * the [UserState] variant *and* `userData.authenticated`.
     * `WhileSubscribed(5_000)` keeps the upstream alive across short
     * configuration-change gaps without leaking when the screen is gone.
     */
    val userState: StateFlow<UserState> = userDataRepository.userInfo.map {
        UserState.Authenticated(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = UserState.UnAuthenticated,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    /**
     * Reactive wallet-access gate ([WalletAccessState]) derived from the persisted roles and
     * stored client, kept fresh by [refreshWalletAccess]. `MifosPayApp` switches the whole
     * authenticated shell on this: [WalletAccessState.REVOKED] paints the full-screen "Blocked"
     * overlay, while the per-screen locked/dummy home keys off the client itself.
     */
    val walletAccessState: StateFlow<WalletAccessState> = combine(
        userDataRepository.userInfo,
        userDataRepository.client,
    ) { info, client ->
        resolveWalletAccess(info.roles, client?.id)
    }.stateIn(
        scope = viewModelScope,
        initialValue = WalletAccessState.NOT_ACTIVATED,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    /**
     * `true` iff no non-blank passcode is currently saved by the
     * [PasscodeStorageAdapter]. Used by `MifosPayApp` to detect the
     * "authenticated but no passcode yet" half-state and force a logout.
     */
    fun isPasscodeNotCreated(): Boolean {
        return passcodeStorageAdapter.loadPasscode().isNullOrBlank()
    }

    /**
     * Re-checks the user's wallet access against the backend on every authenticated app-entry,
     * so a backend role change takes effect on the phone WITHOUT a fresh logout+login.
     *
     * Roles are only returned by the authentication response (and `self/userdetails` is 404 on
     * this instance), so a resumed session can't re-pull roles directly. Instead we probe the
     * canonical access signal — `GET /self/clients/{id}` — and reconcile against the stored state:
     *
     *  - **200** → access readable: persist the real client and clear ONLY the client-side
     *    [WALLET_REVOKED_PROBE_MARKER] left by a previous 403 (handles re-grant). A genuine
     *    backend [WALLET_REVOKED_ROLE] is authoritative and is NEVER stripped here, so a user
     *    carrying it stays blocked even if their client happens to be readable (e.g. they also
     *    hold an active KYC role).
     *  - **403** while the user *had* a real client (or is already probe-revoked) → access was
     *    taken away: stamp the [WALLET_REVOKED_PROBE_MARKER] and reset the client, so the shell
     *    shows the "Blocked" screen gracefully instead of letting the home VM 403 with a raw error.
     *  - **403** for a never-activated user (no real client) → still just not activated; leave the
     *    locked/dummy home in place.
     *  - any other error (offline, 5xx) → leave the existing state untouched.
     *
     * Fire-and-forget.
     */
    fun refreshWalletAccess() {
        viewModelScope.launch {
            val info = userDataRepository.userInfo.first()
            if (!info.authenticated || info.clients.isEmpty()) return@launch

            val hadRealClient = (userDataRepository.client.value?.id ?: 0L) != 0L
            val probeRevoked = info.roles.any { it.name.equals(WALLET_REVOKED_PROBE_MARKER, ignoreCase = true) }

            when (val result = clientRepository.getClient(info.clients.first())) {
                is DataState.Success -> {
                    if (probeRevoked) {
                        userDataRepository.updateUserInfo(info.withoutProbeMarker())
                    }
                    userDataRepository.updateClientInfo(result.data)
                }

                is DataState.Error -> {
                    if (result.isForbidden() && (hadRealClient || probeRevoked)) {
                        userDataRepository.updateUserInfo(info.withProbeMarker())
                        userDataRepository.clearClientInfo()
                    }
                }

                is DataState.Loading -> Unit
            }
        }
    }

    private fun DataState.Error<*>.isForbidden(): Boolean =
        exception.message?.contains("403") == true

    private fun UserInfo.withProbeMarker(): UserInfo {
        if (roles.any { it.name.equals(WALLET_REVOKED_PROBE_MARKER, ignoreCase = true) }) return this
        return copy(
            roles = roles + RoleInfo(id = "", name = WALLET_REVOKED_PROBE_MARKER, description = "", disabled = false),
        )
    }

    private fun UserInfo.withoutProbeMarker(): UserInfo =
        copy(roles = roles.filterNot { it.name.equals(WALLET_REVOKED_PROBE_MARKER, ignoreCase = true) })

    /**
     * Single canonical logout. Clears in this order:
     *  1. user-info (flips `authenticated` to false),
     *  2. app-lock flag (so the next session starts unlocked),
     *  3. passcode (via [PasscodeManager.logOut], which deletes the stored
     *     passcode and resets the manager to the creation step).
     *
     * Fire-and-forget: the work runs on [viewModelScope]; callers don't await
     * completion.
     */
    fun logOut() {
        viewModelScope.launch {
            // Explicitly drop the stored auth token first (defence-in-depth with the
            // blanket settings clear in logOut()) so a stale token can never be replayed
            // as a Basic header on the next login attempt.
            userDataRepository.updateToken("")
            userDataRepository.logOut()
            appLockRepository.deleteLock()
            passcodeManager.logOut()
        }
    }

    /**
     * Pass-through to [AppLockRepository.isAppLocked].
     *
     * @return `true` if the app is currently locked, `false` if explicitly
     *         unlocked, or `null` if no lock flag has been written yet
     *         (fresh install / post-logout). The `MifosPayApp` re-auth gate
     *         treats `null` as "no session to gate" — see the gate's
     *         `isAppLocked()?.let { ... }` use site.
     */
    fun isAppLocked(): Boolean? {
        return appLockRepository.isAppLocked()
    }
}

/**
 * Session state surfaced by [MifosPayViewModel.userState]. Two states only —
 * the app shell makes routing decisions off the [Authenticated.userData]
 * `authenticated` flag.
 */
sealed class UserState {
    /** Initial value before user-info has been read from preferences. */
    data object UnAuthenticated : UserState()

    /**
     * User-info has loaded. [userData].`authenticated` distinguishes a
     * persisted session from a logged-out state.
     */
    data class Authenticated(val userData: UserInfo) : UserState()
}

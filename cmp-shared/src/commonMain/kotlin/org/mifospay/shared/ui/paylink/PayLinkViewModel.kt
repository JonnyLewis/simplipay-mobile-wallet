/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.ui.paylink

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.mifospay.core.datastore.UserPreferencesRepository
import org.mifospay.core.ui.utils.BaseViewModel
import org.mifospay.shared.paylink.PayLinkApi
import org.mifospay.shared.paylink.PayLinkDto

/**
 * Drives the Pay Links hub against the SimpliLink service: loads the owner's
 * links and creates new ones. The owner's clientId / wallet account / display
 * name come from preferences — awaited as flows, never read via .value (the
 * DataStore-backed StateFlows start null; see PayViewModel for the pattern).
 */
class PayLinkViewModel(
    private val api: PayLinkApi,
    private val preferencesRepository: UserPreferencesRepository,
) : BaseViewModel<PayLinkState, PayLinkEvent, PayLinkAction>(
    initialState = PayLinkState(),
) {

    init {
        loadLinks()
    }

    override fun handleAction(action: PayLinkAction) {
        when (action) {
            PayLinkAction.Refresh -> loadLinks()
            is PayLinkAction.Create -> create(action.description, action.amountRands)
            PayLinkAction.ClearGenerated -> mutableStateFlow.update { it.copy(generatedUrl = null) }
            PayLinkAction.DismissError -> mutableStateFlow.update { it.copy(error = null) }
        }
    }

    private fun loadLinks() {
        viewModelScope.launch {
            mutableStateFlow.update { it.copy(loading = true, error = null) }
            val clientId = awaitClientId()
            if (clientId == null) {
                mutableStateFlow.update {
                    it.copy(loading = false, error = "Your wallet isn't loaded yet. Open Home once, then try again.")
                }
                return@launch
            }
            runCatching { api.list(clientId) }
                .onSuccess { links ->
                    mutableStateFlow.update { it.copy(loading = false, links = links) }
                }
                .onFailure {
                    mutableStateFlow.update {
                        it.copy(
                            loading = false,
                            error = "Couldn't reach the pay link service. Check your connection and try again.",
                        )
                    }
                }
        }
    }

    private fun create(description: String, amountRands: String) {
        if (description.isBlank() || state.creating) return
        val amountMinor = amountRands.toDoubleOrNull()?.let { (it * 100).toLong() }?.takeIf { it > 0 }
        viewModelScope.launch {
            mutableStateFlow.update { it.copy(creating = true, error = null, generatedUrl = null) }
            val clientId = awaitClientId()
            val accountId = withTimeoutOrNull(WALLET_LOAD_TIMEOUT_MS) {
                preferencesRepository.defaultAccountId.first { it != null }
            }
            if (clientId == null || accountId == null) {
                mutableStateFlow.update {
                    it.copy(creating = false, error = "Your wallet isn't loaded yet. Open Home once, then try again.")
                }
                return@launch
            }
            val payeeName = withTimeoutOrNull(NAME_LOOKUP_TIMEOUT_MS) {
                preferencesRepository.client.mapNotNull { it?.displayName?.takeIf(String::isNotBlank) }.first()
            }
            runCatching { api.create(clientId, accountId, payeeName, description.trim(), amountMinor) }
                .onSuccess { link ->
                    mutableStateFlow.update {
                        it.copy(
                            creating = false,
                            generatedUrl = link.url,
                            links = listOf(link) + it.links,
                        )
                    }
                }
                .onFailure {
                    mutableStateFlow.update {
                        it.copy(
                            creating = false,
                            error = "Couldn't create the pay link — the pay link service is unreachable.",
                        )
                    }
                }
        }
    }

    private suspend fun awaitClientId(): Long? = withTimeoutOrNull(WALLET_LOAD_TIMEOUT_MS) {
        preferencesRepository.clientId.first { it != null && it != 0L }
    }

    private companion object {
        const val WALLET_LOAD_TIMEOUT_MS = 5_000L
        const val NAME_LOOKUP_TIMEOUT_MS = 2_000L
    }
}

data class PayLinkState(
    val links: List<PayLinkDto> = emptyList(),
    val loading: Boolean = false,
    val creating: Boolean = false,
    /** URL of the link just created — drives the QR/share panel. */
    val generatedUrl: String? = null,
    val error: String? = null,
)

sealed interface PayLinkEvent

sealed interface PayLinkAction {
    data object Refresh : PayLinkAction
    data class Create(val description: String, val amountRands: String) : PayLinkAction
    data object ClearGenerated : PayLinkAction
    data object DismissError : PayLinkAction
}

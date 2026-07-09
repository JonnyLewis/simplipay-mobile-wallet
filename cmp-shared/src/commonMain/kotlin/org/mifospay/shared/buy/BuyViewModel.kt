/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.buy

import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.SelfServiceRepository
import org.mifospay.core.datastore.UserPreferencesRepository
import org.mifospay.core.model.account.Account
import org.mifospay.core.model.savingsaccount.Transaction
import org.mifospay.core.ui.utils.BaseViewModel

private const val RECENT_LIMIT = 3

/**
 * Backs the Buy hub: surfaces the user's default ("pay from") account and its
 * most recent transactions so the screen mirrors the design's balance card and
 * "Recent" list. The VAS catalogue itself is static ([VasService]); there is no
 * provider backend wired yet, so this only reads existing wallet data.
 */
class BuyViewModel(
    preferencesRepository: UserPreferencesRepository,
    private val repository: SelfServiceRepository,
) : BaseViewModel<BuyState, Unit, Unit>(
    initialState = BuyState(),
) {
    private val defaultAccountId = preferencesRepository.defaultAccountId.value

    init {
        preferencesRepository.client.value?.let { loadAccount(it.id) }
    }

    private fun loadAccount(clientId: Long) {
        launchIO {
            repository.getActiveAccounts(clientId).collect { result ->
                if (result is DataState.Success) {
                    val account = result.data.firstOrNull { it.id == defaultAccountId }
                        ?: result.data.firstOrNull()
                    mutableStateFlow.update { it.copy(account = account) }
                    account?.let { loadTransactions(it.id) }
                }
            }
        }
    }

    private fun loadTransactions(accountId: Long) {
        launchIO {
            repository.getTransactions(accountId, RECENT_LIMIT).collect { result ->
                if (result is DataState.Success) {
                    mutableStateFlow.update { it.copy(transactions = result.data) }
                }
            }
        }
    }

    override fun handleAction(action: Unit) = Unit
}

@Serializable
data class BuyState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
)

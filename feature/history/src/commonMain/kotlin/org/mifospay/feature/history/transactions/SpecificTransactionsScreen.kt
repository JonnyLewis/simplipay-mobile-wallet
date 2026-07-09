/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.history.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobile_wallet.feature.history.generated.resources.Res
import mobile_wallet.feature.history.generated.resources.feature_history_amount
import mobile_wallet.feature.history.generated.resources.feature_history_error
import mobile_wallet.feature.history.generated.resources.feature_history_error_oops
import mobile_wallet.feature.history.generated.resources.feature_history_note
import mobile_wallet.feature.history.generated.resources.feature_history_transaction_date
import mobile_wallet.feature.history.generated.resources.feature_history_transaction_details
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.common.MoneyFormat
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.model.savingsaccount.Transaction
import org.mifospay.core.ui.ErrorScreenContent
import org.mifospay.core.ui.MifosProgressIndicator
import org.mifospay.core.ui.utils.EventsEffect
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun SpecificTransactionsScreen(
    navigateBack: () -> Unit,
    viewTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpecificTransactionsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            is STEvent.OnNavigateBack -> navigateBack.invoke()
            is STEvent.OnViewTransaction -> viewTransaction.invoke(event.transferId)
        }
    }

    SpecificTransactionsScreenContent(
        state = state.viewState,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

@Composable
internal fun SpecificTransactionsScreenContent(
    state: STState.ViewState,
    onAction: (STAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MifosScaffold(
        modifier = modifier,
        topBarTitle = stringResource(Res.string.feature_history_transaction_details),
        backPress = {
            onAction(STAction.NavigateBack)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is STState.ViewState.Loading -> {
                    MifosProgressIndicator()
                }

                is STState.ViewState.Error -> {
                    ErrorScreenContent(
                        modifier = Modifier.align(Alignment.Center),
                        title = stringResource(Res.string.feature_history_error_oops),
                        subTitle = stringResource(Res.string.feature_history_error),
                    )
                }

                is STState.ViewState.Content -> {
                    TransactionDetails(
                        modifier = Modifier,
                        transaction = state.transaction,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetails(
    transaction: Transaction,
    onAction: (STAction.ViewTransaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        transaction.transfer != null -> {
            TransferTransactionDetails(
                transaction = transaction,
                modifier = modifier,
            )
        }

        else -> {
            RegularTransactionDetails(
                transaction = transaction,
                modifier = modifier,
            )
        }
    }
}

/** Maps internal ledger narratives (e.g. "payout COMMIT") to plain language; null = hide the note row. */
private fun humanizeNote(raw: String?): String? {
    val note = raw?.trim().orEmpty()
    if (note.isEmpty() || note == "-") return null
    val lower = note.lowercase()
    return when {
        lower.contains("payout") || lower.contains("settle") || lower.contains("commit") -> "Payout"
        lower.contains("on-us") || lower.contains("wallet transfer") -> "Wallet transfer"
        lower.contains("refund") || lower.contains("reverse") -> "Refund"
        // Anything still carrying internal jargon (suspense/pset/ledger ids) is hidden rather than shown raw.
        lower.contains("suspense") || lower.contains("pset") || lower.contains("gl ") -> null
        else -> note
    }
}

private fun Transaction.typeLabel(): String = when (transactionType) {
    org.mifospay.core.model.savingsaccount.TransactionType.DEBIT -> "Money out"
    org.mifospay.core.model.savingsaccount.TransactionType.CREDIT -> "Money in"
    else -> "Transaction"
}

@Composable
private fun TransferTransactionDetails(
    transaction: Transaction,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        SectionTitle(
            title = stringResource(Res.string.feature_history_transaction_details),
        )

        DetailRow(
            label = stringResource(Res.string.feature_history_amount),
            value = MoneyFormat.zar(transaction.amount),
        )

        DetailRow(label = "Type", value = transaction.typeLabel())

        DetailRow(
            label = stringResource(Res.string.feature_history_transaction_date),
            value = transaction.date,
        )

        DetailRow(
            label = "Reference",
            value = transaction.transferId?.toString() ?: transaction.transactionId.toString(),
        )

        humanizeNote(transaction.transfer?.transferDescription)?.let { note ->
            DetailRow(
                label = stringResource(Res.string.feature_history_note),
                value = note,
            )
        }
    }
}

@Composable
private fun RegularTransactionDetails(
    transaction: Transaction,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        SectionTitle(
            title = stringResource(Res.string.feature_history_transaction_details),
        )

        DetailRow(
            label = stringResource(Res.string.feature_history_amount),
            value = MoneyFormat.zar(transaction.amount),
        )

        DetailRow(label = "Type", value = transaction.typeLabel())

        DetailRow(
            label = stringResource(Res.string.feature_history_transaction_date),
            value = transaction.date,
        )

        DetailRow(label = "Reference", value = transaction.transactionId.toString())

        humanizeNote(transaction.transfer?.transferDescription)?.let { note ->
            DetailRow(
                label = stringResource(Res.string.feature_history_note),
                value = note,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = KptTheme.typography.titleMedium,
        )

        HorizontalDivider(
            modifier = Modifier.padding(
                top = KptTheme.spacing.sm,
            ),
            color = KptTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = KptTheme.spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label",
            modifier = Modifier.width(140.dp),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = KptTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = KptTheme.colorScheme.onSurface,
        )
    }
}

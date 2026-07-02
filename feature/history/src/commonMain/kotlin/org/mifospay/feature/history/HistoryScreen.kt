/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobile_wallet.feature.history.generated.resources.Res
import mobile_wallet.feature.history.generated.resources.feature_history_empty
import mobile_wallet.feature.history.generated.resources.feature_history_empty_filter
import mobile_wallet.feature.history.generated.resources.feature_history_error_oops
import mobile_wallet.feature.history.generated.resources.feature_history_filter_content_desc
import mobile_wallet.feature.history.generated.resources.feature_history_header_account
import mobile_wallet.feature.history.generated.resources.feature_history_header_all
import mobile_wallet.feature.history.generated.resources.feature_history_header_credit
import mobile_wallet.feature.history.generated.resources.feature_history_header_debit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.common.MoneyFormat
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.model.account.Account
import org.mifospay.core.model.savingsaccount.Currency
import org.mifospay.core.model.savingsaccount.Status
import org.mifospay.core.model.savingsaccount.Transaction
import org.mifospay.core.model.savingsaccount.TransactionType
import org.mifospay.core.ui.EmptyContentScreen
import org.mifospay.core.ui.MifosProgressIndicator
import org.mifospay.core.ui.TransactionFilterBottomSheet
import org.mifospay.core.ui.utils.EventsEffect
import org.mifospay.feature.history.components.TransactionList
import template.core.base.designsystem.theme.KptTheme

@Composable
fun HistoryScreen(
    viewTransferDetail: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            is HistoryEvent.OnTransactionDetail -> {
                state.selectedAccount?.id?.let { accountId ->
                    viewTransferDetail.invoke(accountId, event.transferId)
                }
            }
        }
    }

    HistoryScreenContent(
        modifier = modifier,
        state = state,
        onAction = remember(viewModel) {
            { action -> viewModel.trySendAction(action) }
        },
    )
}

@Composable
internal fun HistoryScreenContent(
    state: HistoryState,
    modifier: Modifier = Modifier,
    onAction: (HistoryAction) -> Unit,
) {
    MifosScaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        when (state.viewState) {
            is HistoryState.ViewState.Loading -> MifosProgressIndicator()

            is HistoryState.ViewState.Error -> {
                EmptyContentScreen(
                    title = stringResource(Res.string.feature_history_error_oops),
                    subTitle = stringResource(state.viewState.message),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    iconTint = KptTheme.colorScheme.error,
                )
            }

            is HistoryState.ViewState.Empty -> {
                EmptyContentScreen(
                    title = stringResource(Res.string.feature_history_error_oops),
                    subTitle = stringResource(Res.string.feature_history_empty),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            is HistoryState.ViewState.Content -> {
                Column(
                    Modifier.fillMaxWidth().padding(paddingValues),
                ) {
                    HistoryScreenHeader(
                        accountNo = state.selectedAccount?.number ?: "",
                        selectedTransactionType = state.selectedTransactionType,
                        onFilterClick = {
                            onAction(HistoryAction.OnFilterClick)
                        },
                    )
                    HistorySummaryRow(
                        transactions = state.transactions,
                    )
                    HistoryFilterChips(
                        selected = state.selectedTransactionType,
                        onSelect = {
                            onAction(HistoryAction.SetTransactionType(it))
                            onAction(HistoryAction.OnApplyFilterClick)
                        },
                    )
                    if (state.viewState.list.isEmpty()) {
                        EmptyContentScreen(
                            title = stringResource(Res.string.feature_history_error_oops),
                            subTitle = stringResource(Res.string.feature_history_empty_filter),
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    } else {
                        TransactionList(
                            transactions = state.viewState.list,
                            onAction = onAction,
                            modifier = modifier,
                        )
                    }
                }
                if (state.showFilter) {
                    TransactionFilterBottomSheet(
                        selectedAccount = state.selectedAccount,
                        accounts = state.accounts,
                        selectedTransactionType = state.selectedTransactionType,
                        onAccountSelected = {
                            onAction(HistoryAction.SetSelectedAccount(it))
                        },
                        onTransactionTypeSelected = {
                            onAction(HistoryAction.SetTransactionType(it))
                        },
                        onClearFilters = {
                            onAction(HistoryAction.ClearFilters)
                        },
                        onApplyFilters = {
                            onAction(HistoryAction.OnApplyFilterClick)
                        },
                        onDismiss = {
                            onAction(HistoryAction.OnFilterClick)
                        },
                    )
                }
            }
        }
    }
}

/** Headless render-harness entry point (themed, side-effect free). */
@Composable
fun HistoryRenderPreview() {
    val currency = Currency(
        code = "ZAR",
        name = "South African Rand",
        decimalPlaces = 2,
        displaySymbol = "R",
        nameCode = "ZAR",
        displayLabel = "South African Rand (R)",
    )
    val status = Status(
        id = 300, code = "status.active", value = "Active",
        submittedAndPendingApproval = false, approved = false, rejected = false,
        withdrawnByApplicant = false, active = true, closed = false,
        prematureClosed = false, transferInProgress = false, transferOnHold = false, matured = false,
    )
    val account = Account(
        name = "Everyday Savings",
        number = "100045567",
        balance = 12480.50,
        id = 1L,
        currency = currency,
        status = status,
    )
    fun txn(id: Long, amount: Double, date: String, type: TransactionType) = Transaction(
        accountId = 1L, amount = amount, date = date, currency = currency,
        transactionType = type, transactionId = id, accountNo = "100045567",
        transferId = null, originalTransactionId = id, paymentDetailId = null, reversed = false,
    )
    val transactions = listOf(
        txn(101, 2500.0, "12 Jun 2026", TransactionType.CREDIT),
        txn(102, 349.99, "11 Jun 2026", TransactionType.DEBIT),
        txn(103, 1200.0, "09 Jun 2026", TransactionType.CREDIT),
        txn(104, 89.50, "08 Jun 2026", TransactionType.DEBIT),
    )
    MifosTheme(darkTheme = false) {
        HistoryScreenContent(
            state = HistoryState(
                clientId = 1L,
                viewState = HistoryState.ViewState.Content(transactions),
                selectedTransactionType = TransactionType.OTHER,
                transactions = transactions,
                accounts = listOf(account),
                selectedAccount = account,
            ),
            onAction = {},
        )
    }
}

@Composable
private fun HistoryScreenHeader(
    accountNo: String,
    selectedTransactionType: TransactionType,
    modifier: Modifier = Modifier,
    onFilterClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tokens = SimpliPayTheme.tokens
        Column(
            Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(Res.string.feature_history_header_account, accountNo),
                style = KptTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = tokens.ink,
            )
            Spacer(Modifier.height(KptTheme.spacing.xs))
            Text(
                text = when (selectedTransactionType) {
                    TransactionType.OTHER -> stringResource(Res.string.feature_history_header_all)
                    TransactionType.DEBIT -> stringResource(Res.string.feature_history_header_debit)
                    TransactionType.CREDIT -> stringResource(Res.string.feature_history_header_credit)
                },
                style = KptTheme.typography.bodyMedium,
                color = tokens.sub,
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .border(
                    width = 1.dp,
                    color = tokens.border,
                    shape = KptTheme.shapes.medium,
                )
                .clip(KptTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier.matchParentSize(),
            ) {
                Icon(
                    imageVector = MifosIcons.Filter,
                    contentDescription = stringResource(Res.string.feature_history_filter_content_desc),
                    tint = tokens.ink,
                )
            }

            if (selectedTransactionType != TransactionType.OTHER) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 8.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tokens.jade),
                )
            }
        }
    }
}

@Composable
private fun HistorySummaryRow(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val totalIn = transactions
        .filter { it.transactionType == TransactionType.CREDIT && !it.reversed }
        .sumOf { it.amount }
    val totalOut = transactions
        .filter { it.transactionType == TransactionType.DEBIT && !it.reversed }
        .sumOf { it.amount }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = stringResource(Res.string.feature_history_header_credit).uppercase(),
            amount = MoneyFormat.zar(totalIn),
            amountColor = tokens.credit,
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = stringResource(Res.string.feature_history_header_debit).uppercase(),
            amount = MoneyFormat.zar(totalOut),
            amountColor = tokens.debit,
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(KptTheme.colorScheme.surface)
            .border(width = 1.dp, color = tokens.border, shape = shape)
            .padding(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelSmall.copy(
                fontFamily = tokens.monoFontFamily,
                letterSpacing = 1.5.sp,
            ),
            color = tokens.cardLabel,
        )
        Text(
            text = amount,
            style = KptTheme.typography.titleMedium.copy(
                fontFamily = tokens.monoFontFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = amountColor,
        )
    }
}

@Composable
private fun HistoryFilterChips(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        FilterChip(
            label = "All",
            active = selected == TransactionType.OTHER,
            onClick = { onSelect(TransactionType.OTHER) },
        )
        FilterChip(
            label = "In",
            active = selected == TransactionType.CREDIT,
            onClick = { onSelect(TransactionType.CREDIT) },
        )
        FilterChip(
            label = "Out",
            active = selected == TransactionType.DEBIT,
            onClick = { onSelect(TransactionType.DEBIT) },
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (active) tokens.jade else KptTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (active) tokens.jade else tokens.border,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) KptTheme.colorScheme.surface else tokens.sub,
        )
    }
}

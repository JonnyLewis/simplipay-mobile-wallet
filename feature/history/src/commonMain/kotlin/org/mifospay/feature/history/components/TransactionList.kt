/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.history.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobile_wallet.core.ui.generated.resources.core_ui_money_in
import mobile_wallet.core.ui.generated.resources.core_ui_money_out
import mobile_wallet.feature.history.generated.resources.Res
import mobile_wallet.feature.history.generated.resources.arrow_outward
import mobile_wallet.feature.history.generated.resources.feature_history_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.mifospay.core.common.CurrencyFormatter
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.model.savingsaccount.Transaction
import org.mifospay.core.model.savingsaccount.TransactionType
import org.mifospay.feature.history.HistoryAction
import template.core.base.designsystem.theme.KptTheme
import mobile_wallet.core.ui.generated.resources.Res as UiRes

@Composable
internal fun TransactionList(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    onAction: (HistoryAction.ViewTransaction) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = lazyListState,
    ) {
        itemsIndexed(
            items = transactions,
            key = { _, it -> it.transactionId },
        ) { i, items ->
            TransactionItem(
                transaction = items,
                modifier = Modifier,
                onClick = {
                    onAction(HistoryAction.ViewTransaction(it))
                },
            )

            if (i != transactions.size - 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KptTheme.spacing.lg),
                )
            }
        }
    }
}

@Composable
internal fun TransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    showLeadingIcon: Boolean = true,
    showDescription: Boolean = false,
    onClick: (Long) -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    Surface(
        modifier = modifier,
        onClick = {
            onClick(transaction.transactionId)
        },
        color = Color.Transparent,
        contentColor = KptTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KptTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
                ) {
                    AnimatedVisibility(showLeadingIcon) {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = KptTheme.spacing.xs),
                            painter = painterResource(
                                resource = when (transaction.transactionType) {
                                    TransactionType.DEBIT -> UiRes.drawable.core_ui_money_out
                                    TransactionType.CREDIT -> UiRes.drawable.core_ui_money_in
                                    else -> UiRes.drawable.core_ui_money_in
                                },
                            ),
                            contentDescription = null,
                        )
                    }

                    Column {
                        Text(
                            text = transaction.transactionType.name,
                            fontWeight = FontWeight(500),
                            style = KptTheme.typography.bodySmall,
                            color = tokens.ink,
                        )
                        Text(
                            text = transaction.date,
                            fontWeight = FontWeight(300),
                            style = KptTheme.typography.bodySmall.copy(
                                fontFamily = tokens.monoFontFamily,
                            ),
                            color = tokens.sub,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val accentColor = when (transaction.transactionType) {
                        TransactionType.CREDIT -> tokens.credit
                        TransactionType.DEBIT -> tokens.debit
                        else -> tokens.ink
                    }
                    Icon(
                        imageVector = vectorResource(mobile_wallet.feature.history.generated.resources.Res.drawable.arrow_outward),
                        modifier = when (transaction.transactionType) {
                            TransactionType.DEBIT -> Modifier.size(16.dp)
                            TransactionType.CREDIT -> Modifier.graphicsLayer(rotationZ = 180f)
                                .size(16.dp)

                            else -> Modifier.graphicsLayer(rotationZ = 180f).size(16.dp)
                        },
                        tint = accentColor,
                        contentDescription = null,
                    )
                    val amount = CurrencyFormatter.format(
                        balance = transaction.amount,
                        currencyCode = transaction.currency.code,
                        maximumFractionDigits = 2,
                    )
                    Text(
                        modifier = Modifier,
                        text = amount,
                        style = TextStyle(
                            fontFamily = tokens.monoFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            textAlign = TextAlign.End,
                        ),
                    )
                }
            }
            if (showDescription && transaction.description.isNotEmpty()) {
                Spacer(Modifier.height(KptTheme.spacing.md))
                Text(
                    text = stringResource(Res.string.feature_history_description),
                    style = KptTheme.typography.bodyMedium,
                )
                Text(
                    text = transaction.description,
                    style = KptTheme.typography.bodySmall,
                )
            }
        }
    }
}

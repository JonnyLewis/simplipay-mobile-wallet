/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.payments.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.common.MoneyFormat
import org.mifospay.core.designsystem.component.MifosButton
import org.mifospay.core.designsystem.component.MifosOutlinedTextField
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.network.model.payments.PaymentRoute
import org.mifospay.core.network.model.payments.PayoutRail
import org.mifospay.core.network.model.payments.SaBank
import template.core.base.designsystem.theme.KptTheme

@Composable
fun PayScreen(
    modifier: Modifier = Modifier,
    startMode: PayMode? = null,
    viewModel: PayViewModel = koinViewModel(),
) {
    // Deep-link entry (Send sheet): open directly on the requested destination type.
    LaunchedEffect(startMode) {
        startMode?.let { viewModel.trySendAction(PayAction.ModeChanged(it)) }
    }
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    PayScreenContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

@Composable
private fun PayScreenContent(
    state: PayState,
    onAction: (PayAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Terminal result takes over the surface.
    state.result?.let { result ->
        PayResultContent(
            result = result,
            onDone = { onAction(PayAction.DismissResult) },
            onSendAsEft = { onAction(PayAction.SendAsEft) },
            modifier = modifier,
        )
        return
    }

    // Review/confirm step before any money leaves.
    if (state.showConfirm) {
        PayConfirmContent(
            state = state,
            onConfirm = { onAction(PayAction.ConfirmSend) },
            onEdit = { onAction(PayAction.EditPayment) },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.lg),
    ) {
        Text(
            text = "Who are you paying?",
            style = KptTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(KptTheme.spacing.md))

        ModeToggle(
            selected = state.mode,
            enabled = !state.isSubmitting,
            onSelect = { onAction(PayAction.ModeChanged(it)) },
        )
        Spacer(Modifier.height(KptTheme.spacing.lg))

        MifosOutlinedTextField(
            value = state.amount,
            label = "Amount (ZAR)",
            onValueChange = { onAction(PayAction.AmountChanged(it)) },
            singleLine = true,
            readOnly = state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Text("R", color = KptTheme.colorScheme.onSurfaceVariant) },
        )
        Spacer(Modifier.height(KptTheme.spacing.md))

        when (state.mode) {
            PayMode.NUMBER -> {
                MifosOutlinedTextField(
                    value = state.phone,
                    label = "Phone number",
                    onValueChange = { onAction(PayAction.PhoneChanged(it)) },
                    singleLine = true,
                    readOnly = state.isSubmitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(MifosIcons.Person, contentDescription = null) },
                )
                Spacer(Modifier.height(KptTheme.spacing.sm))
                Text(
                    text = "On-us if they're a SimpliPay user, otherwise sent instantly via PayShap.",
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            }

            PayMode.BANK -> {
                Text(
                    text = "How fast should it arrive?",
                    style = KptTheme.typography.bodyMedium,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KptTheme.spacing.sm))
                RailSelector(
                    selected = state.bankRail,
                    enabled = !state.isSubmitting,
                    onSelect = { onAction(PayAction.RailChanged(it)) },
                )
                Spacer(Modifier.height(KptTheme.spacing.md))
                MifosOutlinedTextField(
                    value = state.accountHolderName,
                    label = "Account holder name",
                    onValueChange = { onAction(PayAction.HolderNameChanged(it)) },
                    singleLine = true,
                    readOnly = state.isSubmitting,
                )
                Spacer(Modifier.height(KptTheme.spacing.md))
                Text(
                    text = "Recipient's bank",
                    style = KptTheme.typography.bodyMedium,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KptTheme.spacing.sm))
                BankCarousel(
                    banks = state.banks,
                    selectedBranchCode = state.branchCode,
                    enabled = !state.isSubmitting,
                    onSelect = { onAction(PayAction.BankSelected(it)) },
                )
                if (state.branchCode.isNotBlank()) {
                    Spacer(Modifier.height(KptTheme.spacing.sm))
                    Text(
                        text = "Universal branch code ${state.branchCode} (auto-filled)",
                        style = KptTheme.typography.bodySmall,
                        color = KptTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(KptTheme.spacing.md))
                MifosOutlinedTextField(
                    value = state.accountNumber,
                    label = "Account number",
                    onValueChange = { onAction(PayAction.AccountNumberChanged(it)) },
                    singleLine = true,
                    readOnly = state.isSubmitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(KptTheme.spacing.md))
                Text(
                    text = "Account type",
                    style = KptTheme.typography.bodyMedium,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KptTheme.spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
                    ACCOUNT_TYPES.forEach { type ->
                        ChoiceChip(
                            label = type.lowercase().replaceFirstChar { it.uppercase() },
                            selected = state.accountType == type,
                            enabled = !state.isSubmitting,
                            onClick = { onAction(PayAction.AccountTypeChanged(type)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        state.error?.let { err ->
            Spacer(Modifier.height(KptTheme.spacing.md))
            Text(
                text = err,
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(KptTheme.spacing.lg))

        MifosButton(
            onClick = { onAction(PayAction.Submit) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            text = {
                if (state.isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = KptTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(KptTheme.spacing.sm))
                        Text(state.statusText ?: "Processing…")
                    }
                } else {
                    Text(state.payButtonLabel)
                }
            },
        )
    }
}

/**
 * Horizontal bank picker. Selecting a bank auto-populates the universal branch code, so the user
 * never types it. The chosen bank is highlighted (matched by its branch code held in state).
 */
@Composable
private fun BankCarousel(
    banks: List<SaBank>,
    selectedBranchCode: String,
    enabled: Boolean,
    onSelect: (SaBank) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        items(banks) { bank ->
            ChoiceChip(
                label = bank.name,
                selected = bank.universalBranchCode == selectedBranchCode,
                enabled = enabled,
                onClick = { onSelect(bank) },
            )
        }
    }
}

/** Instant (PayShap) vs Standard (EFT) rail choice for a bank-account payout. */
@Composable
private fun RailSelector(
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        RailCard(
            title = "Instant",
            subtitle = "Arrives in seconds via PayShap. Under R50,000.",
            selected = selected == PayoutRail.PAYSHAP,
            enabled = enabled,
            onClick = { onSelect(PayoutRail.PAYSHAP) },
            modifier = Modifier.weight(1f),
        )
        RailCard(
            title = "Standard",
            subtitle = "EFT, typically 1–2 business days. Any amount.",
            selected = selected == PayoutRail.EFT,
            enabled = enabled,
            onClick = { onSelect(PayoutRail.EFT) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RailCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) KptTheme.colorScheme.primary else KptTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(KptTheme.spacing.md),
    ) {
        Text(
            text = title,
            style = KptTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) KptTheme.colorScheme.onPrimary else KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = KptTheme.typography.bodySmall,
            color = if (selected) KptTheme.colorScheme.onPrimary else KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeToggle(
    selected: PayMode,
    enabled: Boolean,
    onSelect: (PayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KptTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleSegment("To number", selected == PayMode.NUMBER, enabled, { onSelect(PayMode.NUMBER) }, Modifier.weight(1f))
        ToggleSegment("To bank account", selected == PayMode.BANK, enabled, { onSelect(PayMode.BANK) }, Modifier.weight(1f))
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) KptTheme.colorScheme.primary else KptTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = if (selected) KptTheme.colorScheme.onPrimary else KptTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) KptTheme.colorScheme.primary else KptTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (selected) KptTheme.colorScheme.onPrimary else KptTheme.colorScheme.onSurface,
        )
    }
}

/** Review-before-send: shows exactly who/how much/how/fee, and only sends on explicit Confirm. */
@Composable
private fun PayConfirmContent(
    state: PayState,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.lg),
    ) {
        Text(
            text = "Review payment",
            style = KptTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(KptTheme.spacing.md))

        Text(
            text = MoneyFormat.zar(state.amount.toDoubleOrNull() ?: 0.0),
            style = KptTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(KptTheme.spacing.lg))

        ConfirmRow(label = "To", value = state.confirmRecipient)
        ConfirmRow(label = "How", value = state.confirmMethod)
        ConfirmRow(label = "Fee", value = state.confirmFee)

        state.error?.let { err ->
            Spacer(Modifier.height(KptTheme.spacing.md))
            Text(text = err, style = KptTheme.typography.bodyMedium, color = KptTheme.colorScheme.error)
        }

        Spacer(Modifier.height(KptTheme.spacing.xl))
        MifosButton(
            onClick = onConfirm,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            text = {
                if (state.isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = KptTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(KptTheme.spacing.sm))
                        Text(state.statusText ?: "Sending…")
                    }
                } else {
                    Text("Confirm & pay")
                }
            },
        )
        Spacer(Modifier.height(KptTheme.spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !state.isSubmitting) { onEdit() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Edit",
                style = KptTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfirmRow(
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
            text = label,
            modifier = Modifier.width(64.dp),
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

@Composable
private fun PayResultContent(
    result: PayResult,
    onDone: () -> Unit,
    onSendAsEft: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, tint, title, subtitle) = when (result) {
        is PayResult.Success -> ResultVisual(
            MifosIcons.CheckCircle,
            KptTheme.colorScheme.primary,
            "Payment sent",
            routeDescription(result.route),
        )
        is PayResult.Pending -> ResultVisual(
            MifosIcons.Info,
            KptTheme.colorScheme.onSurfaceVariant,
            "Still processing",
            "Check payment history for the final status.",
        )
        is PayResult.Failure -> ResultVisual(
            MifosIcons.Error,
            KptTheme.colorScheme.error,
            "Payment failed",
            result.message,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(KptTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(KptTheme.spacing.xl))
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(KptTheme.spacing.lg))
        Text(title, style = KptTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KptTheme.colorScheme.onSurface)
        Spacer(Modifier.height(KptTheme.spacing.sm))
        Text(subtitle, style = KptTheme.typography.bodyMedium, color = KptTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(KptTheme.spacing.xl))
        if (result is PayResult.Failure && result.canSendAsEft) {
            MifosButton(
                onClick = onSendAsEft,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                text = { Text("Send as standard EFT instead") },
            )
            Spacer(Modifier.height(KptTheme.spacing.sm))
        }
        MifosButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            text = { Text("Done") },
        )
    }
}

/** Friendly explanation of the rail the server settled the payment on. */
private fun routeDescription(route: String?): String = when (route) {
    PaymentRoute.ON_US -> "Paid instantly — free between SimpliPay wallets."
    PaymentRoute.PAYSHAP -> "Sent instantly via PayShap."
    PaymentRoute.EFT -> "Sent as a standard EFT — typically arrives in 1–2 business days."
    else -> "Your payment was successful."
}

private data class ResultVisual(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: androidx.compose.ui.graphics.Color,
    val title: String,
    val subtitle: String,
)

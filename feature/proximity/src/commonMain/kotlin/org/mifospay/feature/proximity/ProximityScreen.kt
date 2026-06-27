/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.designsystem.component.MifosGradientBackground
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.component.MifosTopBar
import org.mifospay.core.ui.utils.EventsEffect
import org.mifospay.feature.proximity.component.SlideToConfirm
import org.mifospay.feature.proximity.model.AmountMode
import org.mifospay.feature.proximity.navigation.ProximityEntryMode
import template.core.base.designsystem.theme.KptTheme

@Composable
fun ProximityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQrFallback: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProximityViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val entryMode = state.entryMode

    EventsEffect(viewModel) { event ->
        when (event) {
            ProximityEvent.NavigateBack -> onNavigateBack()
            ProximityEvent.NavigateToQrFallback -> onNavigateToQrFallback()
        }
    }

    MifosGradientBackground {
        MifosScaffold(
            modifier = modifier,
            topBar = {
                MifosTopBar(
                    topBarTitle = when (entryMode) {
                        ProximityEntryMode.Receive -> "Get paid nearby"
                        ProximityEntryMode.Send -> "Nearby"
                    },
                    backPress = { viewModel.trySendAction(ProximityAction.BackClicked) },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(horizontal = KptTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
            ) {
                if (!state.capable) {
                    DeviceNotCapableBanner(
                        bluetoothOff = !state.bluetoothOn,
                        onUseQr = { viewModel.trySendAction(ProximityAction.UseQrInstead) },
                    )
                }

                when (entryMode) {
                    ProximityEntryMode.Receive -> ReceiveContent(
                        amountMode = state.amountMode,
                        amountInput = state.amountInput,
                        enabled = state.capable,
                        onAmountMode = { viewModel.trySendAction(ProximityAction.SetAmountMode(it)) },
                        onAmountChange = { viewModel.trySendAction(ProximityAction.AmountChanged(it)) },
                        onStart = { viewModel.trySendAction(ProximityAction.StartReceiving) },
                    )

                    ProximityEntryMode.Send -> SendContent(enabled = state.capable)
                }
            }
        }
    }
}

@Composable
private fun DeviceNotCapableBanner(
    bluetoothOff: Boolean,
    onUseQr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(KptTheme.spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            Text(
                text = if (bluetoothOff) "Bluetooth is off" else "Device not capable",
                style = KptTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = KptTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = if (bluetoothOff) {
                    "Turn on Bluetooth to pay or get paid nearby."
                } else {
                    "This device can't use proximity payments. You can use a QR code instead."
                },
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onErrorContainer,
            )
            OutlinedButton(onClick = onUseQr) {
                Text("Use QR instead")
            }
        }
    }
}

@Composable
private fun ReceiveContent(
    amountMode: AmountMode,
    amountInput: String,
    enabled: Boolean,
    onAmountMode: (AmountMode) -> Unit,
    onAmountChange: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Text(
            text = "How much do you want to receive?",
            style = KptTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
            FilterChip(
                selected = amountMode == AmountMode.Open,
                onClick = { onAmountMode(AmountMode.Open) },
                label = { Text("Open") },
            )
            FilterChip(
                selected = amountMode == AmountMode.Fixed,
                onClick = { onAmountMode(AmountMode.Fixed) },
                label = { Text("Fixed") },
            )
        }
        if (amountMode == AmountMode.Fixed) {
            OutlinedTextField(
                value = amountInput,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(KptTheme.spacing.sm))
        SlideToConfirm(
            label = "Slide to start receiving",
            onConfirmed = onStart,
            enabled = enabled,
        )
    }
}

@Composable
private fun SendContent(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KptTheme.spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            Text(
                text = if (enabled) "Searching for people nearby…" else "Nearby unavailable",
                style = KptTheme.typography.titleMedium,
            )
            Text(
                text = "Both of you need this screen open and to be close together. " +
                    "Ask them to tap \"Get paid nearby\".",
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

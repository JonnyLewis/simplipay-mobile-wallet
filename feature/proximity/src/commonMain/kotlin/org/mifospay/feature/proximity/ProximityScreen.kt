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
import org.mifospay.feature.proximity.model.NearbyDevice
import org.mifospay.feature.proximity.model.ProximityBand
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
    // A BLE role can only run with capable hardware AND the radio actually on.
    val ready = state.capable && state.bluetoothOn

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
                if (!ready) {
                    DeviceNotCapableBanner(
                        reason = when {
                            !state.capable -> UnavailableReason.NotCapable
                            state.permissionDenied -> UnavailableReason.PermissionDenied
                            else -> UnavailableReason.BluetoothOff
                        },
                        onUseQr = { viewModel.trySendAction(ProximityAction.UseQrInstead) },
                    )
                }

                when (entryMode) {
                    ProximityEntryMode.Receive -> ReceiveContent(
                        amountMode = state.amountMode,
                        amountInput = state.amountInput,
                        enabled = ready,
                        advertising = state.advertising,
                        onAmountMode = { viewModel.trySendAction(ProximityAction.SetAmountMode(it)) },
                        onAmountChange = { viewModel.trySendAction(ProximityAction.AmountChanged(it)) },
                        onStart = { viewModel.trySendAction(ProximityAction.StartReceiving) },
                        onStop = { viewModel.trySendAction(ProximityAction.StopReceiving) },
                    )

                    ProximityEntryMode.Send -> SendContent(
                        enabled = ready,
                        discoveries = state.discoveries,
                    )
                }
            }
        }
    }
}

/** Why proximity can't run right now — each needs different remediation guidance. */
private enum class UnavailableReason { NotCapable, BluetoothOff, PermissionDenied }

@Composable
private fun DeviceNotCapableBanner(
    reason: UnavailableReason,
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
                text = when (reason) {
                    UnavailableReason.NotCapable -> "Device not capable"
                    UnavailableReason.BluetoothOff -> "Bluetooth is off"
                    UnavailableReason.PermissionDenied -> "Bluetooth permission needed"
                },
                style = KptTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = KptTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = when (reason) {
                    UnavailableReason.NotCapable ->
                        "This device can't use proximity payments. You can use a QR code instead."
                    UnavailableReason.BluetoothOff ->
                        "Turn on Bluetooth to pay or get paid nearby."
                    UnavailableReason.PermissionDenied ->
                        "Allow Bluetooth for SimpliPay in Settings to pay or get paid nearby."
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
    advertising: Boolean,
    onAmountMode: (AmountMode) -> Unit,
    onAmountChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        if (advertising) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(KptTheme.spacing.md),
                colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
                ) {
                    Text(
                        text = "📡 You're discoverable",
                        style = KptTheme.typography.titleMedium,
                        color = KptTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Keep this screen open. Nearby SimpliPay users can find you.",
                        style = KptTheme.typography.bodyMedium,
                        color = KptTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("Stop receiving")
            }
            return@Column
        }

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
    discoveries: List<NearbyDevice>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        if (!enabled) {
            Text("Nearby unavailable", style = KptTheme.typography.titleMedium)
            return@Column
        }
        if (discoveries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(KptTheme.spacing.md),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
                ) {
                    Text("📡 Searching for people nearby…", style = KptTheme.typography.titleMedium)
                    Text(
                        text = "Both of you need this screen open and to be close together. " +
                            "Ask them to tap \"Get paid nearby\".",
                        style = KptTheme.typography.bodyMedium,
                        color = KptTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text("Nearby (${discoveries.size})", style = KptTheme.typography.titleMedium)
            discoveries.forEach { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(KptTheme.spacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name ?: "Nearby device",
                                style = KptTheme.typography.titleSmall,
                            )
                            Text(
                                text = device.id.take(8),
                                style = KptTheme.typography.bodySmall,
                                color = KptTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = bandLabel(device.band) + "  ·  ${device.rssi} dBm",
                            style = KptTheme.typography.bodyMedium,
                            color = KptTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun bandLabel(band: ProximityBand): String = when (band) {
    ProximityBand.VeryClose -> "very close"
    ProximityBand.Nearby -> "nearby"
    ProximityBand.InTheRoom -> "in the room"
    ProximityBand.Unknown -> "far"
}

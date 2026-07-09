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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.mifospay.core.designsystem.component.MifosButton
import org.mifospay.core.designsystem.component.MifosGradientBackground
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.component.MifosTopBar
import org.mifospay.core.designsystem.component.ParticleField
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.ui.utils.EventsEffect
import org.mifospay.feature.proximity.model.AmountMode
import org.mifospay.feature.proximity.model.NearbyDevice
import org.mifospay.feature.proximity.model.ProximityBand
import org.mifospay.feature.proximity.navigation.ProximityEntryMode
import template.core.base.designsystem.theme.KptTheme

/**
 * Proximity screen, radar-first: the particle field fills the whole screen,
 * the status message floats in the middle, and the actions anchor to the
 * bottom — like standing inside the radar rather than looking at a card of it.
 */
@Composable
fun ProximityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQrFallback: () -> Unit,
    onNavigateToPay: (phone: String, amount: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProximityViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val entryMode = state.entryMode
    val ready = state.capable && state.bluetoothOn

    EventsEffect(viewModel) { event ->
        when (event) {
            ProximityEvent.NavigateBack -> onNavigateBack()
            ProximityEvent.NavigateToQrFallback -> onNavigateToQrFallback()
            is ProximityEvent.NavigateToPay -> onNavigateToPay(event.phone, event.amount)
        }
    }

    MifosGradientBackground {
        MifosScaffold(
            modifier = modifier,
            topBar = {
                MifosTopBar(
                    topBarTitle = when (entryMode) {
                        ProximityEntryMode.Receive -> "Get paid nearby"
                        ProximityEntryMode.Send -> "Pay someone nearby"
                    },
                    backPress = { viewModel.trySendAction(ProximityAction.BackClicked) },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // The radar field IS the screen.
                ParticleField(modifier = Modifier.fillMaxSize())

                // Top overlays: capability + error banners.
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = KptTheme.spacing.lg)
                        .padding(top = KptTheme.spacing.md),
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
                    state.error?.let { message ->
                        ErrorBanner(
                            message = message,
                            onDismiss = { viewModel.trySendAction(ProximityAction.DismissError) },
                        )
                    }
                }

                CenterStatus(
                    entryMode = entryMode,
                    ready = ready,
                    state = state,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = KptTheme.spacing.xl),
                )

                // Bottom-anchored actions.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = KptTheme.spacing.lg)
                        .padding(bottom = KptTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
                ) {
                    when (entryMode) {
                        ProximityEntryMode.Receive -> ReceiveControls(
                            amountMode = state.amountMode,
                            amountInput = state.amountInput,
                            enabled = ready,
                            advertising = state.advertising,
                            onAmountMode = { viewModel.trySendAction(ProximityAction.SetAmountMode(it)) },
                            onAmountChange = { viewModel.trySendAction(ProximityAction.AmountChanged(it)) },
                            onStart = { viewModel.trySendAction(ProximityAction.StartReceiving) },
                            onStop = { viewModel.trySendAction(ProximityAction.StopReceiving) },
                        )

                        ProximityEntryMode.Send -> SendDeviceList(
                            enabled = ready,
                            discoveries = state.discoveries,
                            connectingId = state.connectingId,
                            onSelect = { viewModel.trySendAction(ProximityAction.DeviceSelected(it)) },
                        )
                    }
                }
            }
        }
    }
}

/** The floating status message at the heart of the radar. */
@Composable
private fun CenterStatus(
    entryMode: ProximityEntryMode,
    ready: Boolean,
    state: ProximityState,
    modifier: Modifier = Modifier,
) {
    val (title, subtitle) = when {
        entryMode == ProximityEntryMode.Receive && state.advertising ->
            "You're discoverable" to if (state.amountMode == AmountMode.Fixed && state.amountInput.isNotBlank()) {
                "Asking for R${state.amountInput} · keep this screen open"
            } else {
                "Nearby SimpliPay users can pay you · keep this screen open"
            }

        entryMode == ProximityEntryMode.Receive ->
            "Get paid nearby" to "Start receiving below to become discoverable to people around you."

        !ready ->
            "Nearby unavailable" to "Turn on Bluetooth to pay someone nearby."

        state.discoveries.isEmpty() ->
            "Looking for people nearby…" to
                "Ask them to open \"Get paid nearby\" and hold their phone close."

        else ->
            "Tap the person you're paying" to "They'll get your payment instantly."
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(KptTheme.spacing.lg))
            // Soft scrim so the message stays legible over the moving particles.
            .background(KptTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = KptTheme.spacing.xl, vertical = KptTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = KptTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(KptTheme.spacing.sm))
        Text(
            text = subtitle,
            style = KptTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Receive-mode actions, anchored to the bottom of the radar. */
@Composable
private fun ReceiveControls(
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
            MifosButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("Stop receiving")
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KptTheme.spacing.md))
                .background(KptTheme.colorScheme.surface.copy(alpha = 0.84f))
                .padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            Text("How much do you want to receive?", style = KptTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
                FilterChip(
                    selected = amountMode == AmountMode.Open,
                    onClick = { onAmountMode(AmountMode.Open) },
                    label = { Text("Any amount") },
                )
                FilterChip(
                    selected = amountMode == AmountMode.Fixed,
                    onClick = { onAmountMode(AmountMode.Fixed) },
                    label = { Text("Set amount") },
                )
            }
            if (amountMode == AmountMode.Fixed) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (ZAR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        MifosButton(onClick = onStart, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Start receiving")
        }
    }
}

/** Send-mode: the discovered receivers, anchored above the bottom edge. */
@Composable
private fun SendDeviceList(
    enabled: Boolean,
    discoveries: List<NearbyDevice>,
    connectingId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled || discoveries.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        discoveries.forEach { device ->
            NearbyRow(
                device = device,
                connecting = connectingId == device.id,
                onClick = { if (connectingId == null) onSelect(device.id) },
            )
        }
    }
}

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
            OutlinedButton(onClick = onUseQr) { Text("Use QR instead") }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onDismiss),
        colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(KptTheme.spacing.md),
    ) {
        Text(
            text = message,
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(KptTheme.spacing.lg),
        )
    }
}

@Composable
private fun NearbyRow(
    device: NearbyDevice,
    connecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(KptTheme.spacing.md),
        colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KptTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MifosIcons.Contact,
                    contentDescription = null,
                    tint = KptTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Nearby request", style = KptTheme.typography.titleSmall)
                Text(
                    text = bandLabel(device.band),
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (connecting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = MifosIcons.ChevronRight,
                    contentDescription = null,
                    tint = KptTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun bandLabel(band: ProximityBand): String = when (band) {
    ProximityBand.VeryClose -> "Very close"
    ProximityBand.Nearby -> "Nearby"
    ProximityBand.InTheRoom -> "In the room"
    ProximityBand.Unknown -> "Far"
}

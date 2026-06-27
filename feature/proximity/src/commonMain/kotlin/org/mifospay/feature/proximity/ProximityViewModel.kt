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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifospay.core.ui.utils.BaseViewModel
import org.mifospay.feature.proximity.model.AmountMode
import org.mifospay.feature.proximity.model.NearbyDevice
import org.mifospay.feature.proximity.navigation.MODE_ARG
import org.mifospay.feature.proximity.navigation.ProximityEntryMode
import org.mifospay.feature.proximity.transport.BleProximityTransport
import org.mifospay.feature.proximity.transport.PeripheralHandshake
import org.mifospay.feature.proximity.transport.RevealedKeys

/**
 * Drives the proximity feature UI.
 *
 * On a capable platform (iOS, via CoreBluetooth) this actually scans for nearby
 * receivers (Send mode) and advertises (Receive mode). On a Noop platform it
 * reports not-capable and the screen shows the "Device not capable" state.
 *
 * Discovery is real; the GATT handshake / token resolve / settlement are later
 * increments (need crypto + backend).
 */
class ProximityViewModel(
    savedStateHandle: SavedStateHandle,
    private val transport: BleProximityTransport,
) : BaseViewModel<ProximityState, ProximityEvent, ProximityAction>(
    initialState = ProximityState(
        entryMode = savedStateHandle.get<String>(MODE_ARG)
            ?.let { runCatching { ProximityEntryMode.valueOf(it) }.getOrNull() }
            ?: ProximityEntryMode.Send,
        capable = transport.capabilities.isCapable,
        bluetoothOn = transport.capabilities.isBluetoothOn,
        supportsPreciseRanging = transport.capabilities.supportsPreciseRanging,
    ),
) {
    private var scanJob: Job? = null
    private var advertiseJob: Job? = null

    init {
        // Sender radar starts scanning as soon as the screen opens.
        if (state.entryMode == ProximityEntryMode.Send && state.capable) {
            startScanning()
        }
    }

    override fun handleAction(action: ProximityAction) {
        when (action) {
            ProximityAction.BackClicked -> sendEvent(ProximityEvent.NavigateBack)

            ProximityAction.UseQrInstead -> sendEvent(ProximityEvent.NavigateToQrFallback)

            is ProximityAction.SetAmountMode ->
                mutableStateFlow.update { it.copy(amountMode = action.mode) }

            is ProximityAction.AmountChanged ->
                mutableStateFlow.update {
                    it.copy(amountInput = action.value.filter { c -> c.isDigit() || c == '.' })
                }

            ProximityAction.StartReceiving -> startAdvertising()

            ProximityAction.StopReceiving -> stopAdvertising()
        }
    }

    private fun startScanning() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            transport.scanForReceivers().collect { discovery ->
                mutableStateFlow.update { st ->
                    val others = st.discoveries.filterNot { it.id == discovery.deviceId }
                    val merged = (others + NearbyDevice(discovery.deviceId, discovery.rssi))
                        .sortedByDescending { it.rssi }
                    st.copy(discoveries = merged)
                }
            }
        }
    }

    private fun startAdvertising() {
        if (!state.capable) return
        mutableStateFlow.update { it.copy(advertising = true) }
        advertiseJob?.cancel()
        advertiseJob = viewModelScope.launch {
            // No GATT handshake yet — collecting keeps the advertiser alive until cancel.
            transport.startAdvertising(DiscoveryOnlyPeripheralHandshake).collect { }
        }
    }

    private fun stopAdvertising() {
        advertiseJob?.cancel()
        advertiseJob = null
        mutableStateFlow.update { it.copy(advertising = false) }
    }
}

/**
 * Placeholder receiver handshake for the discovery-only increment: advertising
 * makes the device discoverable, but no GATT server / commit-reveal is served
 * yet, so these values are unused.
 */
private object DiscoveryOnlyPeripheralHandshake : PeripheralHandshake {
    override fun currentPayload(): ByteArray = ByteArray(20)
    override fun commitment(): ByteArray = ByteArray(32)
    override fun revealOnSenderKey(senderPub: ByteArray): RevealedKeys =
        RevealedKeys(receiverPub = ByteArray(32), shared = ByteArray(32))
}

/** UI state for the proximity feature (a navigable subset of spec §9). */
data class ProximityState(
    val entryMode: ProximityEntryMode,
    val capable: Boolean,
    val bluetoothOn: Boolean,
    val supportsPreciseRanging: Boolean,
    val amountMode: AmountMode = AmountMode.Open,
    val amountInput: String = "",
    val advertising: Boolean = false,
    val discoveries: List<NearbyDevice> = emptyList(),
)

sealed interface ProximityEvent {
    data object NavigateBack : ProximityEvent
    data object NavigateToQrFallback : ProximityEvent
}

sealed interface ProximityAction {
    data object BackClicked : ProximityAction
    data object UseQrInstead : ProximityAction
    data class SetAmountMode(val mode: AmountMode) : ProximityAction
    data class AmountChanged(val value: String) : ProximityAction
    data object StartReceiving : ProximityAction
    data object StopReceiving : ProximityAction
}

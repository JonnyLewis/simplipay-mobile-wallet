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
import org.mifospay.core.datastore.UserPreferencesRepository
import org.mifospay.core.ui.utils.BaseViewModel
import org.mifospay.feature.proximity.model.AmountMode
import org.mifospay.feature.proximity.model.NearbyDevice
import org.mifospay.feature.proximity.navigation.MODE_ARG
import org.mifospay.feature.proximity.navigation.ProximityEntryMode
import org.mifospay.feature.proximity.transport.BleProximityTransport
import org.mifospay.feature.proximity.transport.PeripheralHandshake
import org.mifospay.feature.proximity.transport.ProximityPayLink
import org.mifospay.feature.proximity.transport.RevealedKeys

/**
 * Drives the proximity feature UI (MVP — see `plans/2026-07-02-proximity-mvp-ios-first.md`).
 *
 * Receive mode advertises + serves an [ProximityPayLink] (the user's MSISDN + amount) over GATT.
 * Send mode scans, and on tap connects, reads that pay-link, and hands off to the normal Pay flow
 * (prefilled phone + amount) via [ProximityEvent.NavigateToPay] — so settlement, the mandatory
 * re-auth, polling and result all reuse the existing send path.
 */
class ProximityViewModel(
    savedStateHandle: SavedStateHandle,
    private val transport: BleProximityTransport,
    private val preferencesRepository: UserPreferencesRepository,
) : BaseViewModel<ProximityState, ProximityEvent, ProximityAction>(
    initialState = ProximityState(
        entryMode = savedStateHandle.get<String>(MODE_ARG)
            ?.let { runCatching { ProximityEntryMode.valueOf(it) }.getOrNull() }
            ?: ProximityEntryMode.Send,
        capable = transport.capabilities.isCapable,
        bluetoothOn = transport.capabilities.isBluetoothOn,
        supportsPreciseRanging = transport.capabilities.supportsPreciseRanging,
        permissionDenied = transport.capabilities.permissionDenied,
    ),
) {
    private var scanJob: Job? = null
    private var advertiseJob: Job? = null

    init {
        viewModelScope.launch {
            transport.observeCapabilities().collect { caps ->
                mutableStateFlow.update {
                    it.copy(
                        capable = caps.isCapable,
                        bluetoothOn = caps.isBluetoothOn,
                        supportsPreciseRanging = caps.supportsPreciseRanging,
                        permissionDenied = caps.permissionDenied,
                    )
                }
                reconcileRadios()
            }
        }
    }

    private val ProximityState.ready: Boolean get() = capable && bluetoothOn

    private fun reconcileRadios() {
        val ready = state.ready
        if (state.entryMode == ProximityEntryMode.Send) {
            if (ready && scanJob == null) startScanning() else if (!ready) stopScanning()
        }
        if (state.advertising && !ready) stopAdvertising()
    }

    override fun handleAction(action: ProximityAction) {
        when (action) {
            ProximityAction.BackClicked -> sendEvent(ProximityEvent.NavigateBack)
            ProximityAction.UseQrInstead -> sendEvent(ProximityEvent.NavigateToQrFallback)
            is ProximityAction.SetAmountMode -> mutableStateFlow.update { it.copy(amountMode = action.mode) }
            is ProximityAction.AmountChanged ->
                mutableStateFlow.update { it.copy(amountInput = action.value.filter { c -> c.isDigit() || c == '.' }) }
            ProximityAction.StartReceiving -> startAdvertising()
            ProximityAction.StopReceiving -> stopAdvertising()
            is ProximityAction.DeviceSelected -> connectAndPay(action.deviceId)
            ProximityAction.DismissError -> mutableStateFlow.update { it.copy(error = null) }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
    }

    private fun startScanning() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            transport.scanForReceivers().collect { discovery ->
                mutableStateFlow.update { st ->
                    val others = st.discoveries.filterNot { it.id == discovery.deviceId }
                    val merged = (others + NearbyDevice(discovery.deviceId, discovery.rssi, discovery.name))
                        .sortedByDescending { it.rssi }
                    st.copy(discoveries = merged)
                }
            }
        }
    }

    /** Sender taps a nearby receiver: connect, read their pay-link, hand off to the Pay flow. */
    private fun connectAndPay(deviceId: String) {
        if (state.connectingId != null) return
        mutableStateFlow.update { it.copy(connectingId = deviceId, error = null) }
        viewModelScope.launch {
            val result = runCatching { transport.connectAndHandshake(deviceId) }
            val payLink = result.getOrNull()?.let { ProximityPayLink.decode(it.payload) }
            mutableStateFlow.update { it.copy(connectingId = null) }
            when {
                result.isFailure ->
                    mutableStateFlow.update { it.copy(error = "Couldn't connect. Move closer and try again.") }
                payLink == null ->
                    mutableStateFlow.update { it.copy(error = "That device isn't a valid SimpliPay request.") }
                else -> {
                    stopScanning()
                    val amount = if (payLink.amountMode == AmountMode.Fixed && payLink.amountMinor > 0) {
                        payLink.amountMinor.minorToRands()
                    } else {
                        null
                    }
                    sendEvent(ProximityEvent.NavigateToPay(phone = payLink.phone, amount = amount))
                }
            }
        }
    }

    private fun startAdvertising() {
        if (!state.ready) return
        val phone = preferencesRepository.client.value?.mobileNo
        if (phone.isNullOrBlank()) {
            mutableStateFlow.update { it.copy(error = "Your wallet isn't loaded yet. Open Home once, then try again.") }
            return
        }
        val amountMinor = if (state.amountMode == AmountMode.Fixed) state.amountInput.toMinorUnits() else 0L
        if (state.amountMode == AmountMode.Fixed && amountMinor <= 0L) {
            mutableStateFlow.update { it.copy(error = "Enter the amount you want to receive.") }
            return
        }
        val payLink = ProximityPayLink(phone = phone, amountMode = state.amountMode, amountMinor = amountMinor)
        mutableStateFlow.update { it.copy(advertising = true) }
        advertiseJob?.cancel()
        advertiseJob = viewModelScope.launch {
            transport.startAdvertising(PayLinkPeripheral(payLink)).collect { }
        }
    }

    private fun stopAdvertising() {
        advertiseJob?.cancel()
        advertiseJob = null
        mutableStateFlow.update { it.copy(advertising = false) }
    }
}

/** Serves the MVP [ProximityPayLink] on each GATT read. No ECDH in the MVP (Phase 4). */
private class PayLinkPeripheral(private val payLink: ProximityPayLink) : PeripheralHandshake {
    private val bytes = payLink.encode()
    override fun currentPayload(): ByteArray = bytes
    override fun commitment(): ByteArray = ByteArray(0)
    override fun revealOnSenderKey(senderPub: ByteArray): RevealedKeys =
        RevealedKeys(receiverPub = ByteArray(0), shared = ByteArray(0))
}

private fun String.toMinorUnits(): Long {
    val d = toDoubleOrNull() ?: return 0L
    return (d * 100).toLong()
}

private fun Long.minorToRands(): String {
    val whole = this / 100
    val cents = this % 100
    return if (cents == 0L) whole.toString() else "$whole." + cents.toString().padStart(2, '0')
}

/** UI state for the proximity feature. */
data class ProximityState(
    val entryMode: ProximityEntryMode,
    val capable: Boolean,
    val bluetoothOn: Boolean,
    val supportsPreciseRanging: Boolean,
    val permissionDenied: Boolean = false,
    val amountMode: AmountMode = AmountMode.Open,
    val amountInput: String = "",
    val advertising: Boolean = false,
    val discoveries: List<NearbyDevice> = emptyList(),
    /** deviceId currently being connected to (Send), else null. */
    val connectingId: String? = null,
    val error: String? = null,
)

sealed interface ProximityEvent {
    data object NavigateBack : ProximityEvent
    data object NavigateToQrFallback : ProximityEvent

    /** Hand off to the normal Pay flow, prefilled from the discovered receiver's pay-link. */
    data class NavigateToPay(val phone: String, val amount: String?) : ProximityEvent
}

sealed interface ProximityAction {
    data object BackClicked : ProximityAction
    data object UseQrInstead : ProximityAction
    data class SetAmountMode(val mode: AmountMode) : ProximityAction
    data class AmountChanged(val value: String) : ProximityAction
    data object StartReceiving : ProximityAction
    data object StopReceiving : ProximityAction
    data class DeviceSelected(val deviceId: String) : ProximityAction
    data object DismissError : ProximityAction
}

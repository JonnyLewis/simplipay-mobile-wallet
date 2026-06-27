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
import kotlinx.coroutines.flow.update
import org.mifospay.core.ui.utils.BaseViewModel
import org.mifospay.feature.proximity.model.AmountMode
import org.mifospay.feature.proximity.navigation.MODE_ARG
import org.mifospay.feature.proximity.navigation.ProximityEntryMode
import org.mifospay.feature.proximity.transport.BleProximityTransport

/**
 * Drives the proximity feature UI. In this increment the injected
 * [BleProximityTransport] is the capability-less Noop, so [ProximityState.capable]
 * is false and the screen renders the "Device not capable" state (spec §8.1).
 * The real Android/iOS transports (plan T4) will flip capabilities on without
 * any change here.
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
    override fun handleAction(action: ProximityAction) {
        when (action) {
            ProximityAction.BackClicked -> sendEvent(ProximityEvent.NavigateBack)

            ProximityAction.UseQrInstead -> sendEvent(ProximityEvent.NavigateToQrFallback)

            is ProximityAction.SetAmountMode ->
                mutableStateFlow.update { it.copy(amountMode = action.mode) }

            is ProximityAction.AmountChanged ->
                mutableStateFlow.update { it.copy(amountInput = action.value.filter { c -> c.isDigit() || c == '.' }) }

            ProximityAction.StartReceiving -> {
                // Real advertising lands with the platform transport (plan T4).
                if (state.capable) {
                    mutableStateFlow.update { it.copy(advertising = true) }
                }
            }

            ProximityAction.StopReceiving ->
                mutableStateFlow.update { it.copy(advertising = false) }
        }
    }
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

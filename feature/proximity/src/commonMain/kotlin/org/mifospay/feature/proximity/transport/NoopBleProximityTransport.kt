/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Capability-less transport. Reports [BleCapabilities.NONE] so the feature
 * entry renders grayed-out with a "Device not capable" banner (spec §8.1) and
 * falls back to QR.
 *
 * This is the binding on Desktop / Web (no usable BLE), and is also the
 * temporary binding on Android / iOS until the real `BluetoothLe*` /
 * `CoreBluetooth` transports land (see the implementation plan, task T4).
 */
class NoopBleProximityTransport : BleProximityTransport {

    override val capabilities: BleCapabilities = BleCapabilities.NONE

    override suspend fun startAdvertising(
        peripheral: PeripheralHandshake,
    ): Flow<PeripheralHandshakeResult> = emptyFlow()

    override suspend fun stopAdvertising() = Unit

    override fun scanForReceivers(): Flow<BleDiscovery> = emptyFlow()

    override suspend fun stopScan() = Unit

    override suspend fun connectAndHandshake(deviceId: String): GattHandshakeResult =
        throw UnsupportedOperationException("BLE not available on this device")

    override suspend fun disconnect(deviceId: String) = Unit
}

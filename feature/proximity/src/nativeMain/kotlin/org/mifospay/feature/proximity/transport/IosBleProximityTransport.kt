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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSNumber
import platform.darwin.NSObject

/**
 * iOS CoreBluetooth implementation of [BleProximityTransport].
 *
 * THIS INCREMENT delivers the testable discovery layer:
 *  - [startAdvertising] makes the device discoverable (advertises the fixed
 *    service UUID) so a nearby scanner sees it.
 *  - [scanForReceivers] discovers nearby advertisers and streams them (id +
 *    RSSI) to the radar.
 *
 * The GATT commit/reveal handshake + token read ([connectAndHandshake]) and the
 * crypto/backend resolve are later increments — they require the crypto provider
 * (plan D1) which isn't in the tree yet. Discovery alone is enough to verify BLE
 * works between two iPhones (and iPhone↔Android).
 *
 * Managers + delegates are held as strong properties for their lifetime — a
 * locally-scoped CoreBluetooth delegate is released and its callbacks silently
 * never fire (the #1 CoreBluetooth-from-Kotlin/Native footgun).
 */
@OptIn(ExperimentalForeignApi::class)
class IosBleProximityTransport : BleProximityTransport {

    private val serviceUuid: CBUUID = CBUUID.UUIDWithString(PROXIMITY_SERVICE_UUID)

    private var centralManager: CBCentralManager? = null
    private var centralDelegate: ScanDelegate? = null
    private var peripheralManager: CBPeripheralManager? = null
    private var peripheralDelegate: AdvertiseDelegate? = null

    // Persistent probe manager whose only job is to learn the real radio /
    // permission state. iOS reports it asynchronously via didUpdateState, so we
    // start optimistic-but-not-ready (isBluetoothOn=false) and correct within
    // milliseconds. Held strongly — a released CB delegate silently stops firing.
    private val capabilitiesState = MutableStateFlow(
        BleCapabilities(
            canAdvertise = true,
            canScan = true,
            isBluetoothOn = false,
            supportsPreciseRanging = false,
        ),
    )
    private val probeDelegate = CapabilityProbeDelegate { state ->
        capabilitiesState.value = capabilitiesFor(state)
    }
    private val probeManager: CBCentralManager = CBCentralManager(probeDelegate, null)

    override val capabilities: BleCapabilities get() = capabilitiesState.value

    override fun observeCapabilities(): StateFlow<BleCapabilities> = capabilitiesState.asStateFlow()

    override suspend fun startAdvertising(
        peripheral: PeripheralHandshake,
    ): Flow<PeripheralHandshakeResult> = callbackFlow {
        val delegate = AdvertiseDelegate(
            onPoweredOn = { manager ->
                manager.startAdvertising(
                    mapOf(
                        CBAdvertisementDataServiceUUIDsKey to listOf(serviceUuid),
                        // Foreground-only local name so it's easy to spot in a BLE
                        // scanner (e.g. LightBlue) as "SimpliPay" during testing.
                        // iOS drops this in the background by design.
                        CBAdvertisementDataLocalNameKey to ADVERTISED_NAME,
                    ),
                )
            },
        )
        val manager = CBPeripheralManager(delegate, null)
        peripheralManager = manager
        peripheralDelegate = delegate
        // No PeripheralHandshakeResult is emitted yet (GATT handshake is a later
        // increment); the flow stays open to keep advertising alive until cancel.
        awaitClose {
            manager.stopAdvertising()
            peripheralManager = null
            peripheralDelegate = null
        }
    }

    override suspend fun stopAdvertising() {
        peripheralManager?.stopAdvertising()
        peripheralManager = null
        peripheralDelegate = null
    }

    override fun scanForReceivers(): Flow<BleDiscovery> = callbackFlow {
        val delegate = ScanDelegate(
            onPoweredOn = { manager ->
                manager.scanForPeripheralsWithServices(listOf(serviceUuid), null)
            },
            onDiscover = { peripheral, rssi, advertisedName ->
                trySend(
                    BleDiscovery(
                        deviceId = peripheral.identifier.UUIDString,
                        rssi = rssi,
                        name = advertisedName,
                    ),
                )
            },
        )
        val manager = CBCentralManager(delegate, null)
        centralManager = manager
        centralDelegate = delegate
        awaitClose {
            manager.stopScan()
            centralManager = null
            centralDelegate = null
        }
    }

    override suspend fun stopScan() {
        centralManager?.stopScan()
        centralManager = null
        centralDelegate = null
    }

    override suspend fun connectAndHandshake(deviceId: String): GattHandshakeResult =
        throw NotImplementedError("GATT commit/reveal handshake is a later increment (needs crypto + backend)")

    override suspend fun disconnect(deviceId: String) = Unit
}

/** Fixed 128-bit discovery service UUID (spec §4.3). */
private const val PROXIMITY_SERVICE_UUID = "9F1B0001-7C3A-4D2E-9A1F-2B6C8D0E5A77"

/** Foreground-only advertised local name (testing aid). */
private const val ADVERTISED_NAME = "SimpliPay"

/**
 * Maps a CoreBluetooth manager state to honest capabilities. `Unsupported`
 * (e.g. the iOS simulator, which has no BLE radio) means the device genuinely
 * can't do proximity → not capable. Every other non-powered-on state
 * (off / unauthorized / resetting / unknown) keeps the role capabilities but
 * reports the radio as not ready, so the UI gates the slide and never claims
 * "You're discoverable" while nothing is actually being broadcast.
 */
@OptIn(ExperimentalForeignApi::class)
private fun capabilitiesFor(state: CBManagerState): BleCapabilities = when (state) {
    CBManagerStatePoweredOn -> BleCapabilities(
        canAdvertise = true,
        canScan = true,
        isBluetoothOn = true,
        supportsPreciseRanging = false,
    )
    CBManagerStateUnsupported -> BleCapabilities.NONE
    CBManagerStateUnauthorized -> BleCapabilities(
        canAdvertise = true,
        canScan = true,
        isBluetoothOn = false,
        supportsPreciseRanging = false,
        permissionDenied = true,
    )
    else -> BleCapabilities(
        canAdvertise = true,
        canScan = true,
        isBluetoothOn = false,
        supportsPreciseRanging = false,
    )
}

/** Strong-held central delegate that reports radio/permission state transitions. */
@OptIn(ExperimentalForeignApi::class)
private class CapabilityProbeDelegate(
    val onState: (CBManagerState) -> Unit,
) : NSObject(), CBCentralManagerDelegateProtocol {

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        onState(central.state)
    }
}

private class ScanDelegate(
    val onPoweredOn: (CBCentralManager) -> Unit,
    val onDiscover: (CBPeripheral, Int, String?) -> Unit,
) : NSObject(), CBCentralManagerDelegateProtocol {

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state == CBManagerStatePoweredOn) onPoweredOn(central)
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber,
    ) {
        val name = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        onDiscover(didDiscoverPeripheral, RSSI.intValue, name)
    }
}

private class AdvertiseDelegate(
    val onPoweredOn: (CBPeripheralManager) -> Unit,
) : NSObject(), CBPeripheralManagerDelegateProtocol {

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        if (peripheral.state == CBManagerStatePoweredOn) onPoweredOn(peripheral)
    }
}

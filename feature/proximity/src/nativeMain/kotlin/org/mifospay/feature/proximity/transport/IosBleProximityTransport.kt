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
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreBluetooth.CBATTErrorInvalidOffset
import platform.CoreBluetooth.CBATTErrorSuccess
import platform.CoreBluetooth.CBATTRequest
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBAttributePermissionsReadable
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicPropertyRead
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBMutableCharacteristic
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMakeRange
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.Foundation.subdataWithRange
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS CoreBluetooth implementation of [BleProximityTransport].
 *
 * - [startAdvertising] advertises the fixed service UUID AND stands up a GATT
 *   server with a single readable characteristic that returns the receiver's
 *   [PeripheralHandshake.currentPayload] (the MVP pay-link, [ProximityPayLink]).
 * - [scanForReceivers] discovers nearby advertisers (id + RSSI) and retains the
 *   `CBPeripheral`s so [connectAndHandshake] can connect and read the payload.
 * - [connectAndHandshake] connects to a discovered peripheral, discovers the
 *   service/characteristic and reads the payload bytes.
 *
 * The MVP does NOT run the spec's commit/reveal ECDH — [GattHandshakeResult]'s
 * crypto fields come back empty; only [GattHandshakeResult.payload] is populated.
 *
 * Managers + delegates are held as strong properties for their lifetime — a
 * locally-scoped CoreBluetooth delegate is released and its callbacks silently
 * never fire (the #1 CoreBluetooth-from-Kotlin/Native footgun).
 */
@OptIn(ExperimentalForeignApi::class)
class IosBleProximityTransport : BleProximityTransport {

    private val serviceUuid: CBUUID = CBUUID.UUIDWithString(PROXIMITY_SERVICE_UUID)
    private val payloadCharUuid: CBUUID = CBUUID.UUIDWithString(PROXIMITY_PAYLOAD_CHAR_UUID)

    private var centralManager: CBCentralManager? = null
    private var centralDelegate: ScanDelegate? = null
    private var peripheralManager: CBPeripheralManager? = null
    private var peripheralDelegate: AdvertiseDelegate? = null

    // Discovered peripherals retained by scan id so connectAndHandshake can use them.
    private val discovered = mutableMapOf<String, CBPeripheral>()

    // Strong-held during an in-flight connect+read so callbacks keep firing.
    private var readDelegate: ReadDelegate? = null

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
            serviceUuid = serviceUuid,
            payloadCharUuid = payloadCharUuid,
            payloadProvider = { peripheral.currentPayload() },
            onReadyToAdvertise = { manager ->
                manager.startAdvertising(
                    mapOf(
                        CBAdvertisementDataServiceUUIDsKey to listOf(serviceUuid),
                        // Foreground-only local name so it's easy to spot in a BLE
                        // scanner (e.g. LightBlue). iOS drops this in the background.
                        CBAdvertisementDataLocalNameKey to ADVERTISED_NAME,
                    ),
                )
            },
        )
        val manager = CBPeripheralManager(delegate, null)
        peripheralManager = manager
        peripheralDelegate = delegate
        // No PeripheralHandshakeResult is emitted (the MVP settles via the Pay flow,
        // not a GATT-side callback); the flow stays open to keep advertising alive.
        awaitClose {
            manager.stopAdvertising()
            manager.removeAllServices()
            peripheralManager = null
            peripheralDelegate = null
        }
    }

    override suspend fun stopAdvertising() {
        peripheralManager?.stopAdvertising()
        peripheralManager?.removeAllServices()
        peripheralManager = null
        peripheralDelegate = null
    }

    override fun scanForReceivers(): Flow<BleDiscovery> = callbackFlow {
        val delegate = ScanDelegate(
            onPoweredOn = { manager ->
                manager.scanForPeripheralsWithServices(listOf(serviceUuid), null)
            },
            onDiscover = { peripheral, rssi, advertisedName ->
                discovered[peripheral.identifier.UUIDString] = peripheral
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

    override suspend fun connectAndHandshake(deviceId: String): GattHandshakeResult {
        val central = centralManager
            ?: throw IllegalStateException("Not scanning; cannot connect")
        val delegate = centralDelegate
            ?: throw IllegalStateException("Scan delegate gone; cannot connect")
        val peripheral = discovered[deviceId]
            ?: throw IllegalStateException("Device $deviceId not in range")

        val payload = suspendCancellableCoroutine { cont ->
            val reader = ReadDelegate(
                serviceUuid = serviceUuid,
                payloadCharUuid = payloadCharUuid,
                onRead = { bytes ->
                    if (cont.isActive) cont.resume(bytes)
                },
                onError = { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                },
            )
            readDelegate = reader
            peripheral.delegate = reader
            delegate.onConnect = { p -> p.discoverServices(listOf(serviceUuid)) }
            delegate.onConnectFail = { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
            central.connectPeripheral(peripheral, null)
            cont.invokeOnCancellation {
                central.cancelPeripheralConnection(peripheral)
            }
        }

        central.cancelPeripheralConnection(peripheral)
        readDelegate = null
        return GattHandshakeResult(
            payload = payload,
            ecdhShared = ByteArray(0),
            receiverPub = ByteArray(0),
            windowId = 0,
        )
    }

    override suspend fun disconnect(deviceId: String) {
        discovered[deviceId]?.let { centralManager?.cancelPeripheralConnection(it) }
    }
}

/** Fixed 128-bit discovery service UUID (spec §4.3). */
private const val PROXIMITY_SERVICE_UUID = "9F1B0001-7C3A-4D2E-9A1F-2B6C8D0E5A77"

/** The single readable characteristic that serves the MVP pay-link payload. */
private const val PROXIMITY_PAYLOAD_CHAR_UUID = "9F1B0002-7C3A-4D2E-9A1F-2B6C8D0E5A77"

/** Foreground-only advertised local name (testing aid). */
private const val ADVERTISED_NAME = "SimpliPay"

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

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { NSData.create(bytes = it.addressOf(0), length = size.convert()) }
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
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

/** Scan + connect callbacks for the sender central. */
@OptIn(ExperimentalForeignApi::class)
private class ScanDelegate(
    val onPoweredOn: (CBCentralManager) -> Unit,
    val onDiscover: (CBPeripheral, Int, String?) -> Unit,
) : NSObject(), CBCentralManagerDelegateProtocol {

    var onConnect: ((CBPeripheral) -> Unit)? = null
    var onConnectFail: ((Throwable) -> Unit)? = null

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

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        onConnect?.invoke(didConnectPeripheral)
    }

    override fun centralManager(
        central: CBCentralManager,
        didFailToConnectPeripheral: CBPeripheral,
        error: NSError?,
    ) {
        onConnectFail?.invoke(IllegalStateException("Connect failed: ${error?.localizedDescription}"))
    }
}

/** Peripheral delegate that discovers the payload characteristic and reads it. */
@OptIn(ExperimentalForeignApi::class)
private class ReadDelegate(
    val serviceUuid: CBUUID,
    val payloadCharUuid: CBUUID,
    val onRead: (ByteArray) -> Unit,
    val onError: (Throwable) -> Unit,
) : NSObject(), CBPeripheralDelegateProtocol {

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        if (didDiscoverServices != null) {
            onError(IllegalStateException("Service discovery failed"))
            return
        }
        val service = (peripheral.services as? List<*>)?.filterIsInstance<CBService>()
            ?.firstOrNull { it.UUID == serviceUuid }
        if (service == null) {
            onError(IllegalStateException("Service not found"))
            return
        }
        peripheral.discoverCharacteristics(listOf(payloadCharUuid), service)
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?,
    ) {
        if (error != null) {
            onError(IllegalStateException("Characteristic discovery failed"))
            return
        }
        val characteristic = (didDiscoverCharacteristicsForService.characteristics as? List<*>)
            ?.filterIsInstance<CBCharacteristic>()
            ?.firstOrNull { it.UUID == payloadCharUuid }
        if (characteristic == null) {
            onError(IllegalStateException("Payload characteristic not found"))
            return
        }
        peripheral.readValueForCharacteristic(characteristic)
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?,
    ) {
        if (error != null) {
            onError(IllegalStateException("Read failed"))
            return
        }
        val data = didUpdateValueForCharacteristic.value
        if (data == null) {
            onError(IllegalStateException("Empty payload"))
            return
        }
        onRead(data.toByteArray())
    }
}

/** Peripheral-manager delegate: stands up the GATT server and answers reads. */
@OptIn(ExperimentalForeignApi::class)
private class AdvertiseDelegate(
    val serviceUuid: CBUUID,
    val payloadCharUuid: CBUUID,
    val payloadProvider: () -> ByteArray,
    val onReadyToAdvertise: (CBPeripheralManager) -> Unit,
) : NSObject(), CBPeripheralManagerDelegateProtocol {

    private var serviceAdded = false

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        if (peripheral.state == CBManagerStatePoweredOn && !serviceAdded) {
            serviceAdded = true
            // value = null → dynamic value, answered in didReceiveReadRequest.
            val characteristic = CBMutableCharacteristic(
                type = payloadCharUuid,
                properties = CBCharacteristicPropertyRead,
                value = null,
                permissions = CBAttributePermissionsReadable,
            )
            val service = CBMutableService(type = serviceUuid, primary = true)
            service.setCharacteristics(listOf(characteristic))
            peripheral.addService(service)
        }
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didAddService: CBService,
        error: NSError?,
    ) {
        if (error == null) onReadyToAdvertise(peripheral)
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didReceiveReadRequest: CBATTRequest,
    ) {
        val payload = payloadProvider().toNSData()
        val offset = didReceiveReadRequest.offset
        if (offset > payload.length) {
            peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorInvalidOffset)
            return
        }
        didReceiveReadRequest.value = if (offset == 0uL) {
            payload
        } else {
            payload.subdataWithRange(NSMakeRange(offset, payload.length - offset))
        }
        peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorSuccess)
    }
}

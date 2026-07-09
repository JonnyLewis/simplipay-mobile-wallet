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

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Fixed 128-bit discovery service UUID (spec §4.3) — must match [IosBleProximityTransport]. */
private val SERVICE_UUID: UUID = UUID.fromString("9F1B0001-7C3A-4D2E-9A1F-2B6C8D0E5A77")

/** The single readable characteristic that serves the MVP pay-link payload. */
private val PAYLOAD_CHAR_UUID: UUID = UUID.fromString("9F1B0002-7C3A-4D2E-9A1F-2B6C8D0E5A77")

/**
 * Android `BluetoothLe*` + GATT implementation of [BleProximityTransport], the
 * parity counterpart to [IosBleProximityTransport] (spec §4.2, plan T4).
 *
 * MVP scope, identical to iOS:
 * - [startAdvertising] stands up a GATT server with one readable characteristic
 *   that returns [PeripheralHandshake.currentPayload], then advertises the fixed
 *   service UUID (device name in the scan response as a testing aid).
 * - [scanForReceivers] scans filtered on the service UUID and reports id + RSSI.
 * - [connectAndHandshake] connects, discovers the service/characteristic and
 *   reads the payload. The spec's commit/reveal ECDH is not run here either — the
 *   crypto fields of [GattHandshakeResult] come back empty.
 *
 * Permissions are surfaced via [capabilities]/[observeCapabilities]; the UI gates
 * on [BleCapabilities.permissionDenied] (spec §8.1). BLE calls are only made once
 * the corresponding permission is held, so the `MissingPermission` lint is
 * suppressed at the guarded call sites.
 */
@SuppressLint("MissingPermission")
class AndroidBleProximityTransport(
    private val context: Context,
) : BleProximityTransport {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    // Advertising (receiver) state — held for their lifetime so callbacks fire.
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    // Scanning (sender) state.
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private val discovered = mutableMapOf<String, BluetoothDevice>()

    // In-flight connect+read.
    private var activeGatt: BluetoothGatt? = null

    override val capabilities: BleCapabilities get() = probeCapabilities()

    override fun observeCapabilities(): Flow<BleCapabilities> = callbackFlow {
        trySend(probeCapabilities())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(probeCapabilities())
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }

    private fun probeCapabilities(): BleCapabilities {
        val a = adapter ?: return BleCapabilities.NONE
        // Advertising stands up a GATT server, which on API 31+ also needs
        // BLUETOOTH_CONNECT (openGattServer + sendResponse) — and the sender's
        // connectAndHandshake needs it too — so fold it into the gate.
        val advPermitted = hasPermissions(advertisePermissions() + connectPermissions())
        val scanPermitted = hasPermissions(scanPermissions())
        return BleCapabilities(
            // Peripheral (advertise) support is not universal; gate on it + the permission.
            canAdvertise = a.isMultipleAdvertisementSupported && advPermitted,
            canScan = scanPermitted,
            isBluetoothOn = a.isEnabled,
            supportsPreciseRanging = false,
            permissionDenied = !advPermitted || !scanPermitted,
        )
    }

    // ---- RECEIVER role ------------------------------------------------------

    override suspend fun startAdvertising(
        peripheral: PeripheralHandshake,
    ): Flow<PeripheralHandshakeResult> = callbackFlow {
        val producerScope = this
        val a = adapter
        if (a == null || !a.isEnabled) {
            close(IllegalStateException("Bluetooth is off"))
            return@callbackFlow
        }
        if (!hasPermissions(advertisePermissions()) || !hasPermissions(connectPermissions())) {
            close(SecurityException("BLE advertise/connect permission not granted"))
            return@callbackFlow
        }
        val leAdvertiser = a.bluetoothLeAdvertiser
        if (leAdvertiser == null) {
            close(IllegalStateException("Device cannot advertise"))
            return@callbackFlow
        }

        val advCallback = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                close(IllegalStateException("Advertise failed: $errorCode"))
            }
        }

        // GATT server: answer reads of the payload characteristic with the
        // receiver's current window payload (value = dynamic, like iOS value = nil).
        val serverCallback = object : BluetoothGattServerCallback() {
            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                // The flow may have been cancelled (and cleaned up) while addService
                // was in flight — don't start an orphan advertisement in that case.
                if (!producerScope.isActive) return
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    close(IllegalStateException("addService failed: $status"))
                    return
                }
                // Start advertising only once the service is live, so an eager
                // sender that connects immediately finds the characteristic.
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .setConnectable(true)
                    .build()
                val data = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()
                // Local name (testing aid) rides the scan response so the 31-byte
                // advertisement isn't overrun by name + UUID together.
                val scanResponse = AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .build()
                leAdvertiser.startAdvertising(settings, data, scanResponse, advCallback)
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic,
            ) {
                val server = gattServer ?: return
                if (characteristic.uuid != PAYLOAD_CHAR_UUID) {
                    server.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    return
                }
                val full = peripheral.currentPayload()
                if (offset > full.size) {
                    server.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
                    return
                }
                val value = full.copyOfRange(offset, full.size)
                server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        val server = bluetoothManager?.openGattServer(context, serverCallback)
        if (server == null) {
            close(IllegalStateException("Cannot open GATT server"))
            return@callbackFlow
        }
        gattServer = server
        advertiser = leAdvertiser
        advertiseCallback = advCallback

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                PAYLOAD_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
            ),
        )
        if (!server.addService(service)) {
            // No onServiceAdded will arrive, so advertising would never start.
            close(IllegalStateException("addService rejected"))
            return@callbackFlow
        } // → onServiceAdded starts advertising

        // No PeripheralHandshakeResult is emitted (the MVP settles via the Pay
        // flow); the flow stays open to keep advertising alive.
        awaitClose {
            runCatching { leAdvertiser.stopAdvertising(advCallback) }
            runCatching { server.clearServices() }
            runCatching { server.close() }
            advertiser = null
            advertiseCallback = null
            gattServer = null
        }
    }

    override suspend fun stopAdvertising() {
        advertiser?.let { adv -> advertiseCallback?.let { runCatching { adv.stopAdvertising(it) } } }
        runCatching { gattServer?.clearServices() }
        runCatching { gattServer?.close() }
        advertiser = null
        advertiseCallback = null
        gattServer = null
    }

    // ---- SENDER role --------------------------------------------------------

    override fun scanForReceivers(): Flow<BleDiscovery> = callbackFlow {
        val a = adapter
        if (a == null || !a.isEnabled) {
            close(IllegalStateException("Bluetooth is off"))
            return@callbackFlow
        }
        if (!hasPermissions(scanPermissions())) {
            close(SecurityException("BLE scan permission not granted"))
            return@callbackFlow
        }
        val leScanner = a.bluetoothLeScanner
        if (leScanner == null) {
            close(IllegalStateException("Device cannot scan"))
            return@callbackFlow
        }
        // Fresh scan — drop devices retained from a previous scan (stale / rotated
        // private addresses) so we don't try to connect to something out of range.
        discovered.clear()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                discovered[device.address] = device
                trySend(
                    BleDiscovery(
                        deviceId = device.address,
                        rssi = result.rssi,
                        name = result.scanRecord?.deviceName,
                    ),
                )
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("Scan failed: $errorCode"))
            }
        }

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build(),
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        leScanner.startScan(filters, settings, callback)
        scanner = leScanner
        scanCallback = callback

        awaitClose {
            runCatching { callback.let { leScanner.stopScan(it) } }
            scanner = null
            scanCallback = null
        }
    }

    override suspend fun stopScan() {
        scanCallback?.let { cb -> runCatching { scanner?.stopScan(cb) } }
        scanner = null
        scanCallback = null
    }

    override suspend fun connectAndHandshake(deviceId: String): GattHandshakeResult {
        if (!hasPermissions(connectPermissions())) {
            throw SecurityException("BLE connect permission not granted")
        }
        val device = discovered[deviceId]
            ?: throw IllegalStateException("Device $deviceId not in range")

        var gatt: BluetoothGatt? = null
        try {
            val payload = suspendCancellableCoroutine { cont ->
                // BLE callbacks can race (e.g. a read completing as the link drops);
                // resume the continuation at most once.
                val resumed = java.util.concurrent.atomic.AtomicBoolean(false)
                fun succeed(bytes: ByteArray) {
                    if (resumed.compareAndSet(false, true)) cont.resume(bytes)
                }
                fun fail(e: Throwable) {
                    if (resumed.compareAndSet(false, true)) cont.resumeWithException(e)
                }

                val callback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED ->
                                if (!g.discoverServices()) fail(IllegalStateException("discoverServices rejected"))
                            BluetoothProfile.STATE_DISCONNECTED ->
                                fail(IllegalStateException("Disconnected before read"))
                        }
                    }

                    override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            fail(IllegalStateException("Service discovery failed: $status"))
                            return
                        }
                        val characteristic = g.getService(SERVICE_UUID)?.getCharacteristic(PAYLOAD_CHAR_UUID)
                        if (characteristic == null) {
                            fail(IllegalStateException("Payload characteristic not found"))
                            return
                        }
                        if (!g.readCharacteristic(characteristic)) {
                            fail(IllegalStateException("readCharacteristic rejected"))
                        }
                    }

                    // The 3-arg overload fires on every API level (the API-33 variant
                    // is only dispatched when overridden); read via characteristic.value.
                    @Suppress("DEPRECATION")
                    override fun onCharacteristicRead(
                        g: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int,
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            succeed(characteristic.value ?: ByteArray(0))
                        } else {
                            fail(IllegalStateException("Read failed: $status"))
                        }
                    }
                }

                gatt = device.connectGatt(context, false, callback)
                activeGatt = gatt
                cont.invokeOnCancellation {
                    runCatching { gatt?.disconnect() }
                    runCatching { gatt?.close() }
                }
            }

            // MVP: crypto fields empty, only the payload is populated (parity with iOS).
            return GattHandshakeResult(
                payload = payload,
                ecdhShared = ByteArray(0),
                receiverPub = ByteArray(0),
                windowId = 0,
            )
        } finally {
            // Always release the client GATT, on success or any failure/cancel.
            runCatching { gatt?.disconnect() }
            runCatching { gatt?.close() }
            if (activeGatt === gatt) activeGatt = null
        }
    }

    override suspend fun disconnect(deviceId: String) {
        runCatching { activeGatt?.disconnect() }
        runCatching { activeGatt?.close() }
        activeGatt = null
    }

    // ---- permissions --------------------------------------------------------

    private fun hasPermissions(permissions: List<String>): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun advertisePermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            emptyList() // legacy BLUETOOTH_ADMIN is an install-time permission
        }

    private fun scanPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun connectPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyList() // legacy BLUETOOTH is an install-time permission
        }
}

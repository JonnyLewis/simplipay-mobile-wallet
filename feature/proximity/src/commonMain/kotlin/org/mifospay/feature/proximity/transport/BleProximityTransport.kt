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
import kotlinx.coroutines.flow.flowOf

/**
 * The single platform seam for BLE proximity (spec §4.2). A plain interface,
 * bound per platform via Koin — NOT an `expect class` (which can't receive the
 * Android `Context` its actual needs). Mirrors the repo's CameraView / IdOcr
 * seam convention.
 *
 * Discovery rule (spec §4.3): the advertisement carries only the fixed service
 * UUID; the 20-byte payload + ECDH keys are always obtained over GATT.
 *
 * NOTE: real Android (`BluetoothLe*`) and iOS (`CoreBluetooth`) implementations
 * land in a later increment; v1 of the module ships [NoopBleProximityTransport]
 * so the UI is navigable and capability-gated on every platform.
 */
interface BleProximityTransport {

    val capabilities: BleCapabilities

    /**
     * Live capability/radio state. The radio (and BLE permission) state isn't
     * known synchronously on every platform — iOS only learns it asynchronously
     * via `CBManagerDidUpdateState` — so callers should observe this rather than
     * trust the one-shot [capabilities] snapshot, which may start optimistic.
     *
     * Default: a single static emission of [capabilities], for platforms whose
     * capabilities never change (Noop on Desktop/Web/Android).
     */
    fun observeCapabilities(): Flow<BleCapabilities> = flowOf(capabilities)

    // ---- RECEIVER role ------------------------------------------------------

    /**
     * Advertise the discovery service UUID and run the GATT server.
     * [peripheral] supplies the rotating payload per read (iOS `value = nil`),
     * the commitment, and reveals the receiver key after the sender writes its
     * key. Emits a [PeripheralHandshakeResult] per completed sender handshake.
     */
    suspend fun startAdvertising(peripheral: PeripheralHandshake): Flow<PeripheralHandshakeResult>

    suspend fun stopAdvertising()

    // ---- SENDER role --------------------------------------------------------

    /** Cold flow of discoveries; starts scanning on first collect, stops on cancel. */
    fun scanForReceivers(): Flow<BleDiscovery>

    suspend fun stopScan()

    /**
     * Connect → commit/reveal ECDH (spec §5.7) → atomic windowed read of the
     * payload characteristic. Returns the token + shared secret + receiver
     * pubkey + the window the read happened under (caller aborts on disagreement).
     */
    suspend fun connectAndHandshake(deviceId: String): GattHandshakeResult

    suspend fun disconnect(deviceId: String)
}

/** Runtime-probed device capabilities (spec §4.2, §8.1). */
data class BleCapabilities(
    val canAdvertise: Boolean,
    val canScan: Boolean,
    val isBluetoothOn: Boolean,
    val supportsPreciseRanging: Boolean,
    /**
     * The OS BLE permission has been explicitly denied. Distinct from
     * [isBluetoothOn]=false (radio off): toggling Bluetooth won't help — the
     * user must grant the permission in Settings, so the UI says so.
     */
    val permissionDenied: Boolean = false,
) {
    /** Neither role available → feature is grayed out as "Device not capable". */
    val isCapable: Boolean get() = canAdvertise || canScan

    companion object {
        val NONE = BleCapabilities(
            canAdvertise = false,
            canScan = false,
            isBluetoothOn = false,
            supportsPreciseRanging = false,
        )
    }
}

/** A single scan hit. Identity for the radar is keyed on the resolved token, not [deviceId] (spec §4.3). */
data class BleDiscovery(
    val deviceId: String,
    val rssi: Int,
    // Advertised local name if present (a testing aid — real display name comes
    // from the server resolve, not the air, spec §5.4).
    val name: String? = null,
)

/** Result of a sender-side connect + commit/reveal handshake + payload read. */
class GattHandshakeResult(
    val payload: ByteArray,
    val ecdhShared: ByteArray,
    val receiverPub: ByteArray,
    val windowId: Long,
)

/** Receiver-side per-connection inputs used by the GATT server while advertising. */
interface PeripheralHandshake {
    /** The current 20-byte payload for this window (re-read each GATT read). */
    fun currentPayload(): ByteArray

    /** Commitment `C_r` published on the COMMIT characteristic (session-fixed). */
    fun commitment(): ByteArray

    /** Given a sender pubkey, reveal the receiver pubkey + the derived shared secret. */
    fun revealOnSenderKey(senderPub: ByteArray): RevealedKeys
}

class RevealedKeys(
    val receiverPub: ByteArray,
    val shared: ByteArray,
)

/** Emitted by the receiver transport when a sender completes the handshake. */
class PeripheralHandshakeResult(
    val shared: ByteArray,
    val token: ByteArray,
    val windowId: Long,
)

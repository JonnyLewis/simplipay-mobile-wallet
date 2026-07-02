/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity.transport

import org.mifospay.feature.proximity.model.AmountMode

/**
 * MVP over-the-GATT payload (see `plans/2026-07-02-proximity-mvp-ios-first.md`).
 *
 * Instead of the full spec's server-signed rotating token, the receiver serves a
 * compact record identifying how to pay them on the wallet's existing on-us /
 * PayShap rail: their MSISDN plus the requested amount. The sender reads this
 * over a GATT connection (never in the broadcast) and hands it to the normal Pay
 * flow. Phase 4 replaces this with the signed-token model — the transport seam
 * and [PeripheralHandshake.currentPayload]/[GattHandshakeResult.payload] byte
 * carriers are unchanged, so only the encoding here is throwaway.
 *
 * Wire format (little-endian), kept well under the BLE default MTU:
 * ```
 * [0]      version = 1
 * [1]      amountMode: 0 = Open, 1 = Fixed
 * [2..9]   amount in minor units (cents), Long; 0 when Open
 * [10]     phone length (bytes)
 * [11..]   phone, UTF-8
 * ```
 */
data class ProximityPayLink(
    val phone: String,
    val amountMode: AmountMode,
    /** Requested amount in minor units (cents); null/0 when [amountMode] is Open. */
    val amountMinor: Long,
) {
    fun encode(): ByteArray {
        val phoneBytes = phone.encodeToByteArray()
        require(phoneBytes.size <= MAX_PHONE_BYTES) { "phone too long for BLE payload" }
        val out = ByteArray(HEADER_SIZE + phoneBytes.size)
        out[0] = VERSION
        out[1] = if (amountMode == AmountMode.Fixed) 1 else 0
        val amount = if (amountMode == AmountMode.Fixed) amountMinor else 0L
        for (i in 0 until 8) {
            out[2 + i] = ((amount shr (8 * i)) and 0xFF).toByte()
        }
        out[10] = phoneBytes.size.toByte()
        phoneBytes.copyInto(out, HEADER_SIZE)
        return out
    }

    companion object {
        private const val VERSION: Byte = 1
        private const val HEADER_SIZE = 11
        private const val MAX_PHONE_BYTES = 32

        /** Returns null on any malformed/short/unknown-version buffer (never throws on bad air data). */
        fun decode(bytes: ByteArray): ProximityPayLink? {
            if (bytes.size < HEADER_SIZE) return null
            if (bytes[0] != VERSION) return null
            val mode = if (bytes[1].toInt() == 1) AmountMode.Fixed else AmountMode.Open
            var amount = 0L
            for (i in 0 until 8) {
                amount = amount or ((bytes[2 + i].toLong() and 0xFF) shl (8 * i))
            }
            val phoneLen = bytes[10].toInt() and 0xFF
            if (bytes.size < HEADER_SIZE + phoneLen || phoneLen == 0) return null
            val phone = bytes.copyOfRange(HEADER_SIZE, HEADER_SIZE + phoneLen).decodeToString()
            return ProximityPayLink(phone = phone, amountMode = mode, amountMinor = amount)
        }
    }
}

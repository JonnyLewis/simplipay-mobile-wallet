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

import org.mifospay.feature.proximity.model.DeviceClass

/**
 * The exact 20-byte on-air payload (spec §5.1). Deliberately carries NO
 * PII / pay-link / account — only an opaque rotating token + classifier.
 *
 * | offset | len | field         |
 * |--------|-----|---------------|
 * | 0      | 1   | version       |
 * | 1      | 1   | device_class  |
 * | 2      | 1   | flags         |
 * | 3      | 16  | ephemeral_token |
 * | 19     | 1   | crc8 (SMBUS)  |
 */
data class ProximityPayload(
    val version: Int,
    val deviceClass: DeviceClass,
    val flags: Int,
    val token: ByteArray,
) {
    init {
        require(token.size == TOKEN_LEN) { "token must be $TOKEN_LEN bytes, was ${token.size}" }
    }

    /** Encode to the wire 20-byte form, appending the CRC-8/SMBUS of bytes 0..18. */
    fun encode(): ByteArray {
        val out = ByteArray(SIZE)
        out[0] = version.toByte()
        out[1] = (deviceClass.wire and 0x07).toByte()
        out[2] = flags.toByte()
        token.copyInto(out, destinationOffset = 3)
        out[19] = crc8Smbus(out, 0, 19)
        return out
    }

    // ByteArray needs explicit equals/hashCode in a data class.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProximityPayload) return false
        return version == other.version &&
            deviceClass == other.deviceClass &&
            flags == other.flags &&
            token.contentEquals(other.token)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + deviceClass.hashCode()
        result = 31 * result + flags
        result = 31 * result + token.contentHashCode()
        return result
    }

    companion object {
        const val SIZE = 20
        const val TOKEN_LEN = 16
        const val VERSION_1 = 0x01

        // flags bits (spec §5.1)
        const val FLAG_DONATION_WINDOWED = 0x01
        const val FLAG_POS_ATTESTED = 0x02
        const val FLAG_ECDH_REQUIRED = 0x04

        /**
         * Decode and verify a 20-byte payload. Returns null on wrong size,
         * bad CRC, unknown version, or unknown device class (drop silently —
         * spec §5.4 / §12).
         */
        fun decode(bytes: ByteArray): ProximityPayload? {
            if (bytes.size != SIZE) return null
            if (crc8Smbus(bytes, 0, 19) != bytes[19]) return null
            val version = bytes[0].toInt() and 0xFF
            if (version != VERSION_1) return null
            val deviceClass = DeviceClass.fromWire(bytes[1].toInt() and 0xFF) ?: return null
            val flags = bytes[2].toInt() and 0xFF
            val token = bytes.copyOfRange(3, 19)
            return ProximityPayload(version, deviceClass, flags, token)
        }

        /**
         * CRC-8/SMBUS: poly 0x07, init 0x00, no in/out reflection, xorout 0x00
         * (spec §5.1 / §6.1). Computed over [from, until).
         */
        fun crc8Smbus(data: ByteArray, from: Int, until: Int): Byte {
            var crc = 0x00
            for (i in from until until) {
                crc = crc xor (data[i].toInt() and 0xFF)
                repeat(8) {
                    crc = if (crc and 0x80 != 0) {
                        (crc shl 1) xor 0x07
                    } else {
                        crc shl 1
                    }
                    crc = crc and 0xFF
                }
            }
            return crc.toByte()
        }
    }
}

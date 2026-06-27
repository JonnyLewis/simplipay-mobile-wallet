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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProximityPayloadTest {

    private fun token(seed: Int) = ByteArray(16) { (it + seed).toByte() }

    @Test
    fun encode_isExactly20Bytes() {
        val payload = ProximityPayload(
            version = ProximityPayload.VERSION_1,
            deviceClass = DeviceClass.P2P,
            flags = ProximityPayload.FLAG_ECDH_REQUIRED,
            token = token(1),
        )
        assertEquals(ProximityPayload.SIZE, payload.encode().size)
    }

    @Test
    fun encode_then_decode_roundTrips() {
        val original = ProximityPayload(
            version = ProximityPayload.VERSION_1,
            deviceClass = DeviceClass.DONATION,
            flags = ProximityPayload.FLAG_DONATION_WINDOWED or ProximityPayload.FLAG_ECDH_REQUIRED,
            token = token(7),
        )
        val decoded = ProximityPayload.decode(original.encode())
        assertEquals(original, decoded)
    }

    @Test
    fun decode_rejects_corruptedCrc() {
        val bytes = ProximityPayload(
            version = ProximityPayload.VERSION_1,
            deviceClass = DeviceClass.POS,
            flags = 0,
            token = token(3),
        ).encode()
        bytes[5] = (bytes[5] + 1).toByte() // flip a token byte, CRC no longer matches
        assertNull(ProximityPayload.decode(bytes))
    }

    @Test
    fun decode_rejects_wrongSize() {
        assertNull(ProximityPayload.decode(ByteArray(19)))
        assertNull(ProximityPayload.decode(ByteArray(21)))
    }

    @Test
    fun decode_rejects_unknownVersion() {
        val bytes = ProximityPayload(
            version = ProximityPayload.VERSION_1,
            deviceClass = DeviceClass.P2P,
            flags = 0,
            token = token(2),
        ).encode()
        bytes[0] = 0x99.toByte()
        bytes[19] = ProximityPayload.crc8Smbus(bytes, 0, 19) // fix CRC so only version is wrong
        assertNull(ProximityPayload.decode(bytes))
    }

    @Test
    fun deviceClass_onlyLow3BitsAreSignificant() {
        // high bits set, low 3 bits = P2P → still decodes as P2P
        assertEquals(DeviceClass.P2P, DeviceClass.fromWire(0xF9))
        assertTrue(DeviceClass.fromWire(0x00) == null)
    }
}

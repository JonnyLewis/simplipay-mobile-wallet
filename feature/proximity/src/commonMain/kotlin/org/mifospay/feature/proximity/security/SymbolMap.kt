/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity.security

import org.mifospay.feature.proximity.model.Handle
import org.mifospay.feature.proximity.model.HandleColor
import org.mifospay.feature.proximity.model.HandleSymbol

/**
 * Maps an HKDF output to the human-comparable colour + emoji [Handle] (spec §5.7, §14.2).
 *
 * Deterministic and pure, so both devices derive the identical handle from the
 * same key material and a human compares them.
 *
 * NOTE on entropy: the spec targets ~20 bits via a curated set sized so that the
 * visible pair carries the entropy. The colour×symbol space here (8 × 16 = 128 ≈
 * 7 bits) is a STARTING set; expanding the curated, colour-blind-safe symbol set
 * to hit the full 20-bit target is tracked as a follow-up (spec §14.2). The
 * derivation below already consumes a full 20-bit slice so the mapping is
 * forward-compatible when the sets grow.
 */
object SymbolMap {

    private val colors = HandleColor.entries
    private val symbols = HandleSymbol.entries

    /** Take the first 20 bits of [material] and fold them into a colour + symbol. */
    fun toHandle(material: ByteArray): Handle {
        require(material.size >= 3) { "need at least 3 bytes of key material" }
        // first 20 bits, big-endian
        val bits20 = ((material[0].toInt() and 0xFF) shl 12) or
            ((material[1].toInt() and 0xFF) shl 4) or
            ((material[2].toInt() and 0xFF) ushr 4)
        val color = colors[(bits20 ushr 10) % colors.size]
        val symbol = symbols[bits20 % symbols.size]
        return Handle(color, symbol)
    }
}

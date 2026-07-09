/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.network.model.payments

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZarAmountTest {

    @Test
    fun minorUnits_formatToRandsString() {
        assertEquals("50.00", ZarAmount.fromMinorUnits(5_000).toApiString())
        assertEquals("0.05", ZarAmount.fromMinorUnits(5).toApiString())
        assertEquals("0.00", ZarAmount.fromMinorUnits(0).toApiString())
        assertEquals("1234.56", ZarAmount.fromMinorUnits(123_456).toApiString())
    }

    @Test
    fun fromRands_parsesVariousForms() {
        assertEquals(5_000, ZarAmount.fromRands("50").minorUnits)
        assertEquals(5_000, ZarAmount.fromRands("50.00").minorUnits)
        assertEquals(5_050, ZarAmount.fromRands("50.5").minorUnits)
        assertEquals(5_005, ZarAmount.fromRands("50.05").minorUnits)
        assertEquals(0, ZarAmount.fromRands("0").minorUnits)
        // trailing precision beyond 2 dp is truncated
        assertEquals(5_012, ZarAmount.fromRands("50.129").minorUnits)
    }

    @Test
    fun toDouble_forPayin() {
        assertEquals(300.0, ZarAmount.fromMinorUnits(30_000).toDouble())
    }

    @Test
    fun negativeAmounts_rejected() {
        assertFailsWith<IllegalArgumentException> { ZarAmount.fromMinorUnits(-1) }
        assertFailsWith<IllegalArgumentException> { ZarAmount.fromRands("-5.00") }
    }

    @Test
    fun malformedStrings_rejected() {
        assertFailsWith<IllegalArgumentException> { ZarAmount.fromRands("") }
        assertFailsWith<IllegalArgumentException> { ZarAmount.fromRands("abc") }
        assertFailsWith<IllegalArgumentException> { ZarAmount.fromRands("1.2.3") }
    }
}

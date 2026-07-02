/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.common

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The single, deterministic money formatter for the wallet (ZAR). Locale-independent so the format is
 * identical on every platform (the per-platform [CurrencyFormatter] produced "R 30,00" on iOS vs "R100"
 * elsewhere — that drift is what this replaces). Format: `R1,234.56` (comma thousands, dot decimals),
 * negatives as `-R1,234.56`. Use this everywhere money is shown.
 */
object MoneyFormat {

    /** e.g. 1234.5 -> "R1,234.50", -30.0 -> "-R30.00", null -> "R0.00". */
    fun zar(amount: Double?): String {
        val value = amount ?: 0.0
        val negative = value < 0
        val cents = abs(value).times(100).roundToLong()
        val whole = cents / 100
        val fraction = (cents % 100).toString().padStart(2, '0')
        val grouped = groupThousands(whole)
        val sign = if (negative) "-" else ""
        return "${sign}R$grouped.$fraction"
    }

    /** Signed for ledger lists: credits "+R10.00", debits "-R10.00". */
    fun zarSigned(amount: Double?, isCredit: Boolean): String {
        val magnitude = zar(abs(amount ?: 0.0))
        return if (isCredit) "+$magnitude" else "-$magnitude"
    }

    private fun groupThousands(whole: Long): String {
        val digits = whole.toString()
        val sb = StringBuilder()
        val firstGroup = digits.length % 3
        for (i in digits.indices) {
            if (i != 0 && (i - firstGroup) % 3 == 0) sb.append(',')
            sb.append(digits[i])
        }
        return sb.toString()
    }
}

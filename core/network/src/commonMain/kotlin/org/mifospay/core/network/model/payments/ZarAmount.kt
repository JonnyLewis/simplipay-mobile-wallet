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

/**
 * A ZAR money value held as **minor units** (cents) to avoid floating-point drift, with
 * conversion to the `"50.00"` rands string the Payments API expects on
 * `POST /channel/transfer`.
 *
 * The app internally represents amounts in minor units ("paise"); [fromMinorUnits] bridges
 * that to the API. [fromRands] parses a user-facing rands string.
 */
class ZarAmount private constructor(
    /** Value in cents. Always >= 0. */
    val minorUnits: Long,
) {
    /** The API string form, e.g. `"50.00"`. */
    fun toApiString(): String {
        val rands = minorUnits / 100
        val cents = minorUnits % 100
        val centsPadded = if (cents < 10) "0$cents" else "$cents"
        return "$rands.$centsPadded"
    }

    /** As a Double (for `/payin`, whose amount is a number, not a string). */
    fun toDouble(): Double = minorUnits / 100.0

    override fun equals(other: Any?): Boolean =
        this === other || (other is ZarAmount && other.minorUnits == minorUnits)

    override fun hashCode(): Int = minorUnits.hashCode()

    override fun toString(): String = "ZarAmount(${toApiString()})"

    companion object {
        fun fromMinorUnits(minorUnits: Long): ZarAmount {
            require(minorUnits >= 0) { "Amount must be non-negative, was $minorUnits" }
            return ZarAmount(minorUnits)
        }

        /**
         * Parses a rands string such as `"50"`, `"50.5"` or `"50.00"`. Anything beyond two
         * decimal places is truncated. Throws [IllegalArgumentException] on malformed input.
         */
        fun fromRands(rands: String): ZarAmount {
            val trimmed = rands.trim()
            require(trimmed.isNotEmpty()) { "Amount is empty" }
            val negative = trimmed.startsWith("-")
            require(!negative) { "Amount must be non-negative, was $rands" }

            val parts = trimmed.removePrefix("+").split(".")
            require(parts.size <= 2) { "Malformed amount: $rands" }

            val whole = parts[0].ifEmpty { "0" }
            require(whole.all { it.isDigit() }) { "Malformed amount: $rands" }

            val fraction = parts.getOrNull(1).orEmpty()
            require(fraction.all { it.isDigit() }) { "Malformed amount: $rands" }
            val centsStr = fraction.padEnd(2, '0').take(2).ifEmpty { "0" }

            return ZarAmount(whole.toLong() * 100 + centsStr.toLong())
        }
    }
}

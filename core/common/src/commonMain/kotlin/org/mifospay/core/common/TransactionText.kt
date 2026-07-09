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

/**
 * Maps internal ledger narratives to customer-facing text. The connector writes notes like
 * "payout COMMIT"/"payout SETTLE"/"on-us payment"; users must never see the internal phase words.
 * Single source so History and Buy render descriptions consistently.
 */
object TransactionText {

    /** Plain-language label, or null when the note is pure internal jargon that should be hidden. */
    fun humanize(raw: String?): String? {
        val note = raw?.trim().orEmpty()
        if (note.isEmpty() || note == "-") return null
        val lower = note.lowercase()
        return when {
            "payout" in lower || "settle" in lower || "commit" in lower -> "Payout"
            "on-us" in lower || "wallet transfer" in lower -> "Wallet transfer"
            "refund" in lower || "reverse" in lower -> "Refund"
            "airtime" in lower -> "Airtime"
            "data" in lower -> "Data"
            "electric" in lower || "power" in lower -> "Electricity"
            // Still-internal terms (suspense/pset/gl ids) are hidden rather than shown raw.
            "suspense" in lower || "pset" in lower || "gl " in lower -> null
            else -> note
        }
    }
}

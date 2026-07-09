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
 * Maps the backend savings-product code to a customer-friendly wallet name. The ledger product is named
 * "SIMPLIWALLET-SAWL" (and legacy "SA SimpliWallet"); users should never see the internal code. Single
 * source so the Home card, Finance list, and any picker stay consistent.
 */
object WalletNaming {

    private const val FRIENDLY = "SimpliPay Wallet"

    fun friendly(rawName: String?): String {
        if (rawName.isNullOrBlank()) return FRIENDLY
        val n = rawName.trim()
        val looksInternal = n.startsWith("SIMPLIWALLET", ignoreCase = true) ||
            n.contains("SAWL", ignoreCase = true) ||
            n.equals("SA SimpliWallet", ignoreCase = true)
        return if (looksInternal) FRIENDLY else n
    }
}

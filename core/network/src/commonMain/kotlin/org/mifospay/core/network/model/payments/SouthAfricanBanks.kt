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
 * A South African bank the user can pay out to, with its **universal branch code** (single code
 * used for all branches). The user picks a bank and the branch code is auto-populated — the codes
 * here MUST match the connector's PayShap participation allowlist (simplipay.rails.payshap.banks)
 * so an Instant (PayShap) payout to a listed bank is actually routable.
 */
data class SaBank(
    val name: String,
    val universalBranchCode: String,
)

/** The canonical list shown in the bank picker (alphabetical). */
object SouthAfricanBanks {
    val ALL: List<SaBank> = listOf(
        SaBank("Absa", "632005"),
        SaBank("African Bank", "430000"),
        SaBank("Bidvest Bank", "462005"),
        SaBank("Capitec", "470010"),
        SaBank("Discovery Bank", "679000"),
        SaBank("FNB", "250655"),
        SaBank("Investec", "580105"),
        SaBank("Nedbank", "198765"),
        SaBank("Sasfin Bank", "460005"),
        SaBank("Standard Bank", "051001"),
        SaBank("TymeBank", "678910"),
    )

    /** Look up a bank by its universal branch code (to restore selection from state). */
    fun byBranchCode(code: String?): SaBank? = ALL.firstOrNull { it.universalBranchCode == code }
}

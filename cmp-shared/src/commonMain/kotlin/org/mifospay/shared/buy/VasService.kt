/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.buy

import androidx.compose.ui.graphics.vector.ImageVector
import org.mifospay.core.designsystem.icon.MifosIcons

/**
 * The catalogue of Value-Added Services offered on the Buy hub. Order and tile
 * labels mirror the "Buy · VAS" frame of the SimpliPay design (Airtime, Data,
 * Electric, Water, DStv/TV, Vouchers, Betting, Bills).
 *
 * @property tileLabel short label shown on the grid tile.
 * @property label full display name used as the purchase-screen title.
 * @property icon the tile/header glyph.
 * @property identifierLabel the account-identifier field label on the purchase
 *           form (e.g. "Phone number", "Meter number").
 * @property identifierPlaceholder example value shown as the field placeholder.
 */
enum class VasService(
    val tileLabel: String,
    val label: String,
    val icon: ImageVector,
    val identifierLabel: String,
    val identifierPlaceholder: String,
) {
    AIRTIME("Airtime", "Airtime", MifosIcons.Airtime, "Phone number", "082 000 0000"),
    DATA("Data", "Data", MifosIcons.Data, "Phone number", "082 000 0000"),
    ELECTRICITY("Electric", "Electricity", MifosIcons.Electricity, "Meter number", "0123 4567 8901"),
    WATER("Water", "Water", MifosIcons.Water, "Account number", "ACC-000000"),
    TV("DStv/TV", "DStv / TV", MifosIcons.TvCable, "Smartcard number", "1234 5678 90"),
    VOUCHERS("Vouchers", "Vouchers", MifosIcons.GiftCard, "Recipient email", "name@example.com"),
    BETTING("Betting", "Betting", MifosIcons.Betting, "Betting ID", "ID-000000"),
    BILLS("Bills", "Bills", MifosIcons.Receipt, "Account number", "ACC-000000"),
    ;

    companion object {
        fun fromName(name: String?): VasService =
            entries.firstOrNull { it.name == name } ?: AIRTIME
    }
}

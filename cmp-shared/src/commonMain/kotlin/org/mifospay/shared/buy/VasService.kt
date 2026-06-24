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
 * The catalogue of Value-Added Services offered on the Buy hub.
 *
 * @property label display name shown on the grid tile and the purchase screen.
 * @property icon the tile/header glyph.
 * @property identifierLabel the account-identifier field label on the purchase
 *           form (e.g. "Phone number", "Meter number").
 * @property identifierPlaceholder example value shown as the field placeholder.
 */
enum class VasService(
    val label: String,
    val icon: ImageVector,
    val identifierLabel: String,
    val identifierPlaceholder: String,
) {
    AIRTIME("Airtime", MifosIcons.Airtime, "Phone number", "082 000 0000"),
    DATA("Data", MifosIcons.Data, "Phone number", "082 000 0000"),
    ELECTRICITY("Electricity", MifosIcons.Electricity, "Meter number", "0123 4567 8901"),
    TV("TV / Cable", MifosIcons.TvCable, "Smartcard number", "1234 5678 90"),
    WATER("Water", MifosIcons.Water, "Account number", "ACC-000000"),
    INTERNET("Internet", MifosIcons.Internet, "Account number", "ACC-000000"),
    BETTING("Betting", MifosIcons.Betting, "Betting ID", "ID-000000"),
    GIFT_CARD("Gift Card", MifosIcons.GiftCard, "Recipient email", "name@example.com"),
    ;

    companion object {
        fun fromName(name: String?): VasService =
            entries.firstOrNull { it.name == name } ?: AIRTIME
    }
}

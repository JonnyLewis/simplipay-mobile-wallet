/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.mifospay.shared.ui.paylink.PayLinkScreen

@Serializable
data object PayLinkRoute

fun NavController.navigateToPayLink() {
    this.navigate(PayLinkRoute)
}

fun NavGraphBuilder.payLinkScreen(onBackClick: () -> Unit) {
    composable<PayLinkRoute> {
        PayLinkScreen(onBackClick = onBackClick)
    }
}

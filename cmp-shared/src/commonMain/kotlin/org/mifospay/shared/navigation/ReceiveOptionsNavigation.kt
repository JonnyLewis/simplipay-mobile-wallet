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
import androidx.navigation.compose.dialog
import kotlinx.serialization.Serializable
import org.mifospay.shared.ui.components.ReceiveOptionsBottomSheet

@Serializable
data object ReceiveOptionsRoute

fun NavController.navigateToReceiveOptions() {
    this.navigate(ReceiveOptionsRoute)
}

fun NavGraphBuilder.receiveOptionsDialog(
    onReceiveByBarcode: () -> Unit,
    onReceiveByPayLink: () -> Unit,
    onReceiveByMobile: () -> Unit,
    onProximityPayment: () -> Unit,
    onDismiss: () -> Unit,
) {
    dialog<ReceiveOptionsRoute> {
        ReceiveOptionsBottomSheet(
            onReceiveByBarcode = onReceiveByBarcode,
            onReceiveByPayLink = onReceiveByPayLink,
            onReceiveByMobile = onReceiveByMobile,
            onProximityPayment = onProximityPayment,
            onDismiss = onDismiss,
        )
    }
}

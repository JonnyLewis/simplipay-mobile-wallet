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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.feature.payments.RequestScreen

/**
 * Standalone "Receive" screen — the SAME [RequestScreen] shown in the Payments → Request tab, reached from
 * the home "Receive Money" quick action. One receive experience (Payment QR + mobile number) instead of the
 * old divergent bottom sheet. Keeps its own back bar; the app chrome adds the bottom nav (see isReceiveRoute).
 */
@Serializable
data object ReceiveRoute

fun NavController.navigateToReceive() {
    this.navigate(ReceiveRoute)
}

fun NavGraphBuilder.receiveScreen(onBackClick: () -> Unit, onShowQr: () -> Unit) {
    composable<ReceiveRoute> {
        MifosScaffold(
            modifier = Modifier.fillMaxSize(),
            backPress = onBackClick,
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            RequestScreen(
                modifier = Modifier.padding(padding),
                showQr = onShowQr,
            )
        }
    }
}

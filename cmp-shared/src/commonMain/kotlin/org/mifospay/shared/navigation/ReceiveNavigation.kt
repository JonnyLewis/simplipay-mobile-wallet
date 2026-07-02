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
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.feature.payments.ReceiveDetailScreen
import org.mifospay.feature.payments.ReceiveMethod
import org.mifospay.feature.payments.RequestScreen

/**
 * Standalone "Receive" screen — the SAME [RequestScreen] shown in the Payments → Request tab, reached from
 * the home "Receive Money" quick action. It lists receive methods (Payment QR + mobile number + EFT); each
 * opens its own detail screen ([ReceiveDetailRoute]) instead of dumping every method's metadata inline.
 * Keeps its own back bar; the app chrome adds the bottom nav (see isReceiveRoute).
 */
@Serializable
data object ReceiveRoute

/** Per-method receive detail screen (mobile number / EFT bank-transfer details). */
@Serializable
data class ReceiveDetailRoute(val method: String)

fun NavController.navigateToReceive() {
    this.navigate(ReceiveRoute)
}

fun NavController.navigateToReceiveDetail(method: ReceiveMethod) {
    this.navigate(ReceiveDetailRoute(method.name))
}

fun NavGraphBuilder.receiveScreen(
    onBackClick: () -> Unit,
    onShowQr: () -> Unit,
    onShowDetail: (ReceiveMethod) -> Unit,
) {
    composable<ReceiveRoute> {
        MifosScaffold(
            modifier = Modifier.fillMaxSize(),
            backPress = onBackClick,
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            RequestScreen(
                modifier = Modifier.padding(padding),
                showQr = onShowQr,
                onShowDetail = onShowDetail,
            )
        }
    }
}

fun NavGraphBuilder.receiveDetailScreen(onBackClick: () -> Unit) {
    composable<ReceiveDetailRoute> { entry ->
        val method = runCatching { ReceiveMethod.valueOf(entry.toRoute<ReceiveDetailRoute>().method) }
            .getOrDefault(ReceiveMethod.EFT)
        ReceiveDetailScreen(
            method = method,
            onBack = onBackClick,
        )
    }
}

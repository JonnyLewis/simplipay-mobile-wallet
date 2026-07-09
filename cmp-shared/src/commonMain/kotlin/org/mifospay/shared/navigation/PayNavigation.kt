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
import org.mifospay.feature.payments.pay.PayMode
import org.mifospay.feature.payments.pay.PayScreen

/**
 * Standalone Send-money payment screen (the same PayScreen embedded in the Payments tab),
 * deep-linked from the home Send sheet onto a specific destination type:
 * [PayMode.NUMBER] (on-us / PayShap by phone) or [PayMode.BANK] (Instant PayShap / Standard EFT).
 */
@Serializable
data class PayRoute(
    val mode: String,
    // Optional prefill (used by the proximity flow: pay the receiver we discovered over BLE).
    val phone: String? = null,
    val amount: String? = null,
)

fun NavController.navigateToPay(mode: PayMode) {
    this.navigate(PayRoute(mode.name))
}

/** Open Pay prefilled — e.g. proximity Send hands off the discovered receiver's phone + amount. */
fun NavController.navigateToPayPrefilled(mode: PayMode, phone: String?, amount: String?) {
    this.navigate(PayRoute(mode.name, phone = phone, amount = amount))
}

fun NavGraphBuilder.payScreen(
    onBackClick: () -> Unit,
    navigateForPasscodeVerification: (verificationKey: String) -> Unit,
) {
    composable<PayRoute> { entry ->
        val route = entry.toRoute<PayRoute>()
        val mode = runCatching { PayMode.valueOf(route.mode) }.getOrDefault(PayMode.NUMBER)
        MifosScaffold(
            modifier = Modifier.fillMaxSize(),
            backPress = onBackClick,
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            PayScreen(
                modifier = Modifier.padding(padding),
                startMode = mode,
                startPhone = route.phone,
                startAmount = route.amount,
                navigateForPasscodeVerification = navigateForPasscodeVerification,
                // The passcode gate writes its result onto this (PayRoute) destination's handle.
                entryStateHandle = entry.savedStateHandle,
            )
        }
    }
}

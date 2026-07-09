/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.home.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import org.koin.compose.koinInject
import org.mifospay.core.datastore.UserPreferencesRepository
import org.mifospay.feature.home.HomeScreen
import org.mifospay.feature.home.LockedHomeScreen

const val HOME_ROUTE = "home_route"

fun NavController.navigateToHome(navOptions: NavOptions? = null) = navigate(HOME_ROUTE, navOptions)

fun NavGraphBuilder.homeScreen(
    onNavigateBack: () -> Unit,
    onRequest: (String) -> Unit,
    onPay: () -> Unit,
    onTopUp: () -> Unit,
    onBuy: () -> Unit,
    navigateToTransactionDetail: (Long, Long) -> Unit,
    navigateToAccountDetail: (Long) -> Unit,
    navigateToHistory: () -> Unit,
) {
    composable(route = HOME_ROUTE) {
        // A wallet-no-access user has no stored client (login skips the client fetch for them),
        // so the real HomeViewModel — which requires a client and would fetch /clients/{id}/... —
        // must not be built. Note the stored client is never null: it defaults to an empty
        // Client with id == 0, so "no real client" means a null OR zero-id client. Show the
        // locked/dummy home until they're KYC-activated.
        val preferencesRepository = koinInject<UserPreferencesRepository>()
        val client by preferencesRepository.client.collectAsStateWithLifecycle()
        if (client == null || client?.id == 0L) {
            LockedHomeScreen(
                // TODO: submit OTP to the verification backend
                onVerify = { _ -> },
            )
        } else {
            HomeScreen(
                onRequest = onRequest,
                onPay = onPay,
                onTopUp = onTopUp,
                onBuy = onBuy,
                onNavigateBack = onNavigateBack,
                navigateToTransactionDetail = navigateToTransactionDetail,
                navigateToAccountDetail = navigateToAccountDetail,
                navigateToHistory = navigateToHistory,
            )
        }
    }
}

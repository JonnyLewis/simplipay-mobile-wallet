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

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read

const val BUY_ROUTE = "buy_route"
private const val BUY_SERVICE_ARG = "service"
private const val BUY_SERVICE_ROUTE = "buy_service_route"

fun NavController.navigateToBuy(navOptions: NavOptions? = null) = navigate(BUY_ROUTE, navOptions)

private fun NavController.navigateToBuyService(service: VasService) =
    navigate("$BUY_SERVICE_ROUTE/${service.name}")

/** Registers the Buy hub + the per-service purchase form under the given controller. */
fun NavGraphBuilder.buyGraph(navController: NavController) {
    composable(route = BUY_ROUTE) {
        BuyScreen(
            onBack = navController::popBackStack,
            onServiceClick = navController::navigateToBuyService,
        )
    }
    composable(
        route = "$BUY_SERVICE_ROUTE/{$BUY_SERVICE_ARG}",
        arguments = listOf(navArgument(BUY_SERVICE_ARG) { type = NavType.StringType }),
    ) { entry ->
        val service = VasService.fromName(
            entry.arguments?.read { getStringOrNull(BUY_SERVICE_ARG) },
        )
        BuyServiceScreen(
            service = service,
            onBack = navController::popBackStack,
        )
    }
}

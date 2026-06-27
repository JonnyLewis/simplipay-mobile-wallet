/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.mifospay.core.ui.composableWithSlideTransitions
import org.mifospay.feature.proximity.ProximityScreen

/** Which door the user came through (spec §8.1) — Send-money or Get-paid. */
enum class ProximityEntryMode { Receive, Send }

private const val PROXIMITY_ROUTE_BASE = "proximity_route"
internal const val MODE_ARG = "mode"
const val PROXIMITY_ROUTE = "$PROXIMITY_ROUTE_BASE/{$MODE_ARG}"

fun NavController.navigateToProximity(
    mode: ProximityEntryMode,
    navOptions: NavOptions? = null,
) {
    navigate(route = "$PROXIMITY_ROUTE_BASE/${mode.name}", navOptions = navOptions)
}

fun NavGraphBuilder.proximityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQrFallback: () -> Unit,
) {
    composableWithSlideTransitions(
        route = PROXIMITY_ROUTE,
        arguments = listOf(navArgument(MODE_ARG) { type = NavType.StringType }),
    ) {
        ProximityScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToQrFallback = onNavigateToQrFallback,
        )
    }
}

/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import org.mifos.feature.passcode.navigateToRootMifosPasscodeScreen
import org.mifospay.core.data.util.Constants.WALLET_ACCOUNT_SAVINGS_PRODUCT_ID
import org.mifospay.feature.auth.navigation.loginScreen
import org.mifospay.feature.auth.navigation.mobileVerificationScreen
import org.mifospay.feature.auth.navigation.navigateToLogin
import org.mifospay.feature.auth.navigation.navigateToSignup
import org.mifospay.feature.auth.navigation.signupScreen
import org.mifospay.feature.auth.socialSignup.signupMethodScreen
import org.mifospay.shared.onboarding.LandingScreen

internal fun NavGraphBuilder.loginNavGraph(
    navController: NavController,
    onShowInstanceSelector: () -> Unit,
) {
    navigation(
        route = MifosNavGraph.LOGIN_GRAPH,
        startDestination = MifosNavGraph.LANDING_ROUTE,
    ) {
        composable(
            route = MifosNavGraph.LANDING_ROUTE,
            // Fade in fast from the splash (matches the splash's 140ms exit) instead of the
            // 700ms default, shrinking the window where splash + landing animate together.
            enterTransition = { fadeIn(tween(140)) },
        ) {
            LandingScreen(
                // Skip the merchant/customer chooser — new accounts are customers by default.
                onCreateAccount = {
                    navController.navigateToSignup(savingsProductId = WALLET_ACCOUNT_SAVINGS_PRODUCT_ID)
                },
                onLogin = navController::navigateToLogin,
            )
        }

        loginScreen(
            onNavigateBack = navController::popBackStack,
            navigateToMifosPasscodeScreen = navController::navigateToRootMifosPasscodeScreen,
            onNavigateToSignupScreen = {
                navController.navigateToSignup(savingsProductId = WALLET_ACCOUNT_SAVINGS_PRODUCT_ID)
            },
            onShowInstanceSelector = onShowInstanceSelector,
        )

        signupMethodScreen(
            onNavigateBack = navController::popBackStack,
            onNavigateToSignUp = {
                navController.navigateToSignup(savingsProductId = it)
            },
        )

        signupScreen(
            onNavigateBack = navController::popBackStack,
            onNavigateToLogin = navController::navigateToLogin,
        )

        mobileVerificationScreen(
            onNavigateBack = navController::popBackStack,
            onOtpVerificationSuccess = { fullNumber ->
                navController.navigateToSignup(mobileNumber = fullNumber)
            },
        )
    }
}

/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.authenticator.biometrics.BiometricStorageAdapter
import org.mifos.authenticator.biometrics.PlatformAuthenticatorCompositionProvider
import org.mifos.feature.passcode.ROOT_MIFOS_PASSCODE_ROUTE
import org.mifos.feature.passcode.navigateToReAuthMifosPasscodeScreen
import org.mifospay.core.common.GlobalAuthManager
import org.mifospay.core.data.util.NetworkMonitor
import org.mifospay.core.data.util.TimeZoneMonitor
import org.mifospay.core.designsystem.component.MifosDialogBox
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.model.user.WalletAccessState
import org.mifospay.passcode.PasscodeManager
import org.mifospay.passcode.PasscodeStep
import org.mifospay.shared.UserState.Authenticated
import org.mifospay.shared.navigation.MifosNavGraph.LOGIN_GRAPH
import org.mifospay.shared.navigation.RootNavGraph
import org.mifospay.shared.ui.BlockedScreen
import template.core.base.designsystem.theme.KptTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Top-level entry point composable for the Mifos Pay app, called from each
 * platform's `setContent` (Android `MainActivity`, desktop `main.kt`, iOS
 * view-controller, web `Application.kt`).
 *
 * Wraps the entire UI tree in [PlatformAuthenticatorCompositionProvider] so the
 * `platformAuthenticationProvider` and `platformAvailableAuthenticationOption`
 * composition locals from the biometrics library resolve everywhere downstream.
 * Without this wrapper, any descendant that reads those locals (passcode
 * screen, settings biometrics toggle, intra-bank transfer auth gate) crashes at
 * compose time.
 *
 * @param modifier Modifier forwarded to the root [RootNavGraph].
 * @param networkMonitor Connectivity observer; defaults to the Koin singleton.
 * @param timeZoneMonitor Time-zone change observer; defaults to the Koin
 *        singleton.
 */
@Composable
fun MifosPaySharedApp(
    modifier: Modifier = Modifier,
    handleAppLocale: (locale: String?) -> Unit,
    networkMonitor: NetworkMonitor = koinInject(),
    timeZoneMonitor: TimeZoneMonitor = koinInject(),
) {
    val biometricStorageAdapter: BiometricStorageAdapter = koinInject()
    PlatformAuthenticatorCompositionProvider(
        biometricStorageAdapter = biometricStorageAdapter,
    ) {
        MifosPayApp(modifier, networkMonitor, timeZoneMonitor, handleAppLocale)
    }
}

/**
 * Internal app-shell composable hosting the [RootNavGraph].
 *
 * Owns three cross-cutting concerns that need a stable composition root:
 *  - **Background re-auth gate** — observes `Lifecycle.Event.ON_STOP` /
 *    `ON_RESUME`; on resume, pushes the re-auth passcode screen iff **all
 *    four** conditions hold: backgrounded for > [lockTimeOut] (15 s), the
 *    lock flag is non-null and `false` (= an established session that was
 *    actively unlocked, not a fresh install), and
 *    `passcodeManager.state.value.passcodeStep == Enter` (the passcode
 *    surface is in unlock mode, not a creation/change flow). The `null`
 *    branch of [AppLockRepository.isAppLocked] is treated as "no session
 *    yet, don't gate."
 *  - **Unauthorized 401 dialog** — observes [GlobalAuthManager.isUnauthorized]
 *    (set by `KtorInterceptor` on any HTTP 401); on `true`, shows the
 *    "Unauthorized User" dialog whose `onConfirm` logs out, navigates to
 *    [LOGIN_GRAPH], and resets [GlobalAuthManager].
 *  - **Start-destination resolution** — derives `navDestination` from
 *    [MifosPayViewModel.userState]: [UserState.UnAuthenticated] →
 *    [LOGIN_GRAPH]; [UserState.Authenticated] with `userData.authenticated`
 *    true → [ROOT_MIFOS_PASSCODE_ROUTE], else [LOGIN_GRAPH]. This only
 *    seeds the NavHost on first composition; subsequent state changes don't
 *    re-target the start destination.
 *
 * @param passcodeManager Library singleton injected from Koin. Read directly
 *        (rather than going through the VM) so the lifecycle observer can
 *        snapshot `state.value.passcodeStep` synchronously inside the
 *        `ON_RESUME` callback without a Flow subscription.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun MifosPayApp(
    modifier: Modifier = Modifier,
    networkMonitor: NetworkMonitor,
    timeZoneMonitor: TimeZoneMonitor,
    handleAppLocale: (locale: String?) -> Unit,
    passcodeManager: PasscodeManager = koinInject(),
    viewModel: MifosPayViewModel = koinViewModel(),
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val walletAccess by viewModel.walletAccessState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    val showErrorDialog = remember { mutableStateOf<Boolean>(false) }
    val isUnauthorized by GlobalAuthManager.isUnauthorized.collectAsStateWithLifecycle()

    val lockTimeOut = 15.seconds

    // Monotonic mark of the most recent ON_STOP. `null` means "no stop
    // recorded yet" or "already consumed on a previous ON_RESUME" — either
    // way, ON_RESUME must skip the re-auth gate. Monotonic clock is used
    // instead of wall-clock (Clock.System.now()) to defeat clock-rewind:
    // a user with passcode access could otherwise background the app, roll
    // the device clock back via Settings → Date & time, and resume far
    // past the 15-second window without triggering re-auth.
    val onStopMark = remember { mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null) }

    LaunchedEffect(isUnauthorized) {
        if (isUnauthorized && userState !is Authenticated) {
            showErrorDialog.value = true
        }
    }

    LaunchedEffect(Unit) {
        val state = userState
        if (
            state is Authenticated &&
            state.userData.authenticated &&
            viewModel.isPasscodeNotCreated()
        ) {
            viewModel.logOut()
        }
    }

    // On every authenticated entry (fresh login OR resume from a stored session),
    // re-probe wallet access so a backend role upgrade (no-access → KYC1/KYC2)
    // unlocks the full wallet without the user having to log out and back in.
    LaunchedEffect(userState) {
        (userState as? Authenticated)?.let { authed ->
            if (authed.userData.authenticated) viewModel.refreshWalletAccess()
        }
    }

    if (showErrorDialog.value) {
        MifosDialogBox(
            title = "Unauthorized User",
            showDialogState = showErrorDialog.value,
            confirmButtonText = "Ok",
            onConfirm = {
                showErrorDialog.value = false
                viewModel.logOut()
                navController.navigate(LOGIN_GRAPH) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
                GlobalAuthManager.reset()
            },
            onDismiss = {},
            message = "Please login again to continue",
        )
    }

    val navDestination = when (userState) {
        is UserState.UnAuthenticated -> LOGIN_GRAPH
        is Authenticated -> if (
            (userState as Authenticated).userData.authenticated &&
            !viewModel.isPasscodeNotCreated()
        ) {
            ROOT_MIFOS_PASSCODE_ROUTE
        } else {
            LOGIN_GRAPH
        }
    }

    val lifeCycleObserver = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifeCycleObserver) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    onStopMark.value?.let { mark ->
                        val inactiveTime = mark.elapsedNow()
                        Logger.a { "inactiveTime: ${inactiveTime.inWholeSeconds}s" }
                        viewModel.isAppLocked()?.let {
                            if (inactiveTime > lockTimeOut && !it) {
                                if (passcodeManager.state.value.passcodeStep == PasscodeStep.Enter) {
                                    navController.navigateToReAuthMifosPasscodeScreen()
                                }
                            }
                        }
                    }
                    onStopMark.value = null
                    // Warm-resume case (process still alive, so the LaunchedEffect above
                    // won't re-fire): re-probe wallet access so an upgrade applied while
                    // the app was backgrounded unlocks the wallet on return.
                    viewModel.refreshWalletAccess()
                }
                Lifecycle.Event.ON_STOP -> {
                    onStopMark.value = TimeSource.Monotonic.markNow()
                }
                else -> {}
            }
        }
        lifeCycleObserver.addObserver(observer)
        onDispose { lifeCycleObserver.removeObserver(observer) }
    }

    MifosTheme {
        // Paint the Platinum-Ivory screen background across the whole window —
        // behind every screen — so the status-bar and home-indicator safe areas
        // are ivory too (individual scaffolds inset their own background via
        // navigationBarsPadding, which otherwise left white "forehead/chin" bands).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KptTheme.colorScheme.background),
        ) {
            RootNavGraph(
                networkMonitor = networkMonitor,
                timeZoneMonitor = timeZoneMonitor,
                navHostController = navController,
                startDestination = navDestination,
                modifier = modifier,
                handleAppLocale = handleAppLocale,
                onClickLogout = {
                    viewModel.logOut()
                    navController.navigate(LOGIN_GRAPH) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
            )

            // Full-screen block for a revoked user — drawn on top of the whole shell (covering
            // the bottom nav) once they're authenticated. Resolved reactively, so a mid-session
            // revocation detected by refreshWalletAccess() drops it over the wallet immediately.
            val authed = (userState as? Authenticated)?.userData?.authenticated == true
            if (authed && walletAccess == WalletAccessState.REVOKED) {
                BlockedScreen(
                    onLogout = {
                        viewModel.logOut()
                        navController.navigate(LOGIN_GRAPH) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }
    }
}

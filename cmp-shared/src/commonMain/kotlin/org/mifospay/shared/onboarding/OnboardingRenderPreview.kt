/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.onboarding

import androidx.compose.runtime.Composable
import org.mifospay.core.designsystem.theme.MifosTheme

/**
 * Themed, side-effect-free entry points for the onboarding screens, used by the
 * headless `:cmp-desktop:renderShots` harness to render the splash and landing to
 * PNG (the splash's auto-forward is suppressed with a `Long.MAX_VALUE` hold).
 */
@Composable
fun SplashRenderPreview() {
    MifosTheme(darkTheme = false) {
        SplashScreen(onTimeout = {}, holdMillis = Long.MAX_VALUE)
    }
}

@Composable
fun LandingRenderPreview() {
    MifosTheme(darkTheme = false) {
        LandingScreen(onCreateAccount = {}, onLogin = {})
    }
}

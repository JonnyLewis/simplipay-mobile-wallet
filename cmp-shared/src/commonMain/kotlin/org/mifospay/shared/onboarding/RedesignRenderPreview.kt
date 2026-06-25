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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import org.mifospay.core.designsystem.icon.MifosIcons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.mifospay.core.designsystem.component.Rosette
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.ui.MifosProgressIndicator
import org.mifospay.feature.auth.login.LoginRenderPreview
import org.mifospay.feature.history.HistoryRenderPreview
import org.mifospay.feature.home.HomeRenderPreview
import org.mifospay.feature.profile.ProfileRenderPreview

/**
 * Thin re-exports so the headless `:cmp-desktop:renderShots` harness can render
 * the re-skinned feature screens (which live in feature modules cmp-desktop does
 * not depend on directly) to PNG. Each delegate is already wrapped in
 * `MifosTheme` and driven with representative mock state by the feature module.
 */
@Composable
fun LoginShot() = LoginRenderPreview()

@Composable
fun HomeShot() = HomeRenderPreview()

@Composable
fun HistoryShot() = HistoryRenderPreview()

@Composable
fun ProfileShot() = ProfileRenderPreview()

@Composable
fun LoadingShot() = MifosTheme(darkTheme = false) { MifosProgressIndicator() }

@Composable
fun BuyShot() = MifosTheme(darkTheme = false) {
    org.mifospay.shared.buy.BuyScreenContent(
        accountName = "Savings",
        accountMask = "•• 4567",
        balanceText = "R 12,480.50",
        recent = listOf(
            org.mifospay.shared.buy.BuyRecentItem("MTN Airtime", "12 Jun", "−R 50.00", credit = false, icon = MifosIcons.Airtime),
            org.mifospay.shared.buy.BuyRecentItem("City Power", "9 Jun", "−R 200.00", credit = false, icon = MifosIcons.Electricity),
            org.mifospay.shared.buy.BuyRecentItem("Vodacom 5GB", "5 Jun", "−R 99.00", credit = false, icon = MifosIcons.Data),
        ),
        onBack = {},
        onServiceClick = {},
    )
}

@Composable
fun BuyServiceShot() = MifosTheme(darkTheme = false) {
    org.mifospay.shared.buy.BuyServiceScreen(
        service = org.mifospay.shared.buy.VasService.ELECTRICITY,
        onBack = {},
    )
}

@Composable
fun KycVerifyShot() = MifosTheme(darkTheme = false) {
    Box(modifier = Modifier.fillMaxSize().background(SimpliPayTheme.tokens.ivoryStops[0])) {
        org.mifospay.feature.auth.signup.kyc.KycVerifyPreview()
    }
}

@Composable
fun KycReviewShot() = MifosTheme(darkTheme = false) {
    Box(modifier = Modifier.fillMaxSize().background(SimpliPayTheme.tokens.ivoryStops[0])) {
        org.mifospay.feature.auth.signup.kyc.KycReviewPreview()
    }
}

@Composable
fun LogoPainterShot() = MifosTheme(darkTheme = false) {
    Box(
        modifier = Modifier.fillMaxSize().background(SimpliPayTheme.tokens.ivoryStops[0]),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = org.mifospay.core.designsystem.component.rememberWalletWordmarkPainter(),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
        )
    }
}

/**
 * The app-icon artwork: the signature SimpliPay [Rosette] over a soft jade glow on
 * an ivory field. Rendered square (1024×1024) by the harness and written to the
 * iOS `AppIcon.appiconset`.
 */
@Composable
fun AppIconShot() = MifosTheme(darkTheme = false) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.ivoryStops[0]),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(235.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to tokens.jade.copy(alpha = 0.20f),
                        0.7f to Color.Transparent,
                    ),
                ),
        )
        Rosette(radiusX = 98.dp, radiusY = 32.dp, count = 35, opacity = 0.95f)
    }
}

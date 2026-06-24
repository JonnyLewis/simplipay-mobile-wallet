/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.KptMaterialTheme
import template.core.base.designsystem.theme.KptThemeProviderImpl
import template.core.base.designsystem.toKptTypography

@Composable
fun MifosTheme(
    // The "Platinum Ivory" redesign is LIGHT-ONLY. We deliberately ignore the
    // system dark-mode setting: the dark palette has not been reworked and the
    // redesigned screens + SimpliPayTokens assume light (ivory) surfaces, so a
    // device in Dark Mode would render dark-ink content on dark backgrounds —
    // i.e. a black screen. Forcing light keeps every iPhone consistent until a
    // proper dark retheme ships.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Color scheme — always light for now.
    val selectedColorScheme = lightKptColorScheme
    val typography = getTypography().toKptTypography()
    val theme = KptThemeProviderImpl(
        colors = selectedColorScheme,
        typography = typography,
        // optionally shapes, spacing, elevation if you want to override defaults
    )

    // Flat Platinum-Ivory background (no gradient) so the screen bg fills
    // edge-to-edge, including the status-bar and home-indicator safe areas.
    val gradientColors = GradientColors(
        top = backgroundLight,
        bottom = backgroundLight,
        container = backgroundLight,
    )
    // Background theme
    val defaultBackgroundTheme = BackgroundTheme(
        color = Color.Transparent,
        tonalElevation = 2.dp,
    )
    val backgroundTheme = when {
        else -> defaultBackgroundTheme
    }
    val tintTheme = when {
        else -> TintTheme()
    }

    // SimpliPay non-Material tokens (ivory gradient, money colours, mono font, …)
    val simpliPayTokens = lightSimpliPayTokens()

    // Composition locals
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
        LocalSimpliPayTokens provides simpliPayTokens,
    ) {
        KptMaterialTheme(
            theme = theme,
            content = content,
        )
    }
}

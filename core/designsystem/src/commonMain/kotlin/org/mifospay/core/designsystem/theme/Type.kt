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

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import mobile_wallet.core.designsystem.generated.resources.Res
import mobile_wallet.core.designsystem.generated.resources.geist_mono_bold
import mobile_wallet.core.designsystem.generated.resources.geist_mono_medium
import mobile_wallet.core.designsystem.generated.resources.geist_mono_regular
import mobile_wallet.core.designsystem.generated.resources.geist_mono_semibold
import mobile_wallet.core.designsystem.generated.resources.plus_jakarta_sans_bold
import mobile_wallet.core.designsystem.generated.resources.plus_jakarta_sans_extrabold
import mobile_wallet.core.designsystem.generated.resources.plus_jakarta_sans_medium
import mobile_wallet.core.designsystem.generated.resources.plus_jakarta_sans_regular
import mobile_wallet.core.designsystem.generated.resources.plus_jakarta_sans_semibold
import org.jetbrains.compose.resources.Font

/**
 * SimpliPay "Platinum Ivory" display/body family — **Plus Jakarta Sans**.
 *
 * Used for everything except numeric/monetary values and eyebrow labels, which use
 * [monoFontFamily] (Geist Mono). Headings in the design are weight 800 with tight
 * negative tracking; see [getTypography].
 */
@Composable
internal fun displayFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal),
        Font(Res.font.plus_jakarta_sans_medium, FontWeight.Medium),
        Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
        Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold),
        Font(Res.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
    )
}

/**
 * SimpliPay monospace family — **Geist Mono**.
 *
 * The design renders **all** money (`R 12,480.50`), card numbers (`•••• 4567`), IDs,
 * numeric dates/times and eyebrow/section labels in this family. Reach for it via
 * [SimpliPayTokens.monoFontFamily] in feature code rather than re-declaring it.
 */
@Composable
internal fun monoFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.geist_mono_regular, FontWeight.Normal),
        Font(Res.font.geist_mono_medium, FontWeight.Medium),
        Font(Res.font.geist_mono_semibold, FontWeight.SemiBold),
        Font(Res.font.geist_mono_bold, FontWeight.Bold),
    )
}

// Set of Material typography styles to start with
@Composable
internal fun getTypography(): Typography {
    val display = displayFontFamily()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 57.sp,
            lineHeight = 60.sp,
            letterSpacing = (-1.7).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 45.sp,
            lineHeight = 48.sp,
            letterSpacing = (-1.35).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            letterSpacing = (-1.0).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.9).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.8).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.6).sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Bottom,
                trim = LineHeightStyle.Trim.None,
            ),
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.5).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.3).sp,
        ),
        titleSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        ),
        // Default text style
        bodyLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        ),
        bodyMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
        ),
        // Used for Button
        labelLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.15).sp,
        ),
        // Used for Navigation items
        labelMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.LastLineBottom,
            ),
        ),
        // Used for Tag
        labelSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.LastLineBottom,
            ),
        ),
    )
}

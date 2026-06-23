/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * SimpliPay "Platinum Ivory" design values that have **no Material `ColorScheme`
 * slot** — the warm ivory-gradient card fills, the credit/debit money colours, the
 * on-ivory ink/label pair, the verified gold, the hairline rosette stroke, and the
 * Geist Mono family used for every numeric value.
 *
 * Resolve in feature code with [SimpliPayTheme.tokens]; provided by [MifosTheme]
 * via [LocalSimpliPayTokens]. Material-mapped colours (jade → `primary`, ivory bg →
 * `background`/`surface`, ink → `onSurface`, …) still come from `KptTheme`/
 * `MaterialTheme.colorScheme`; this holds only what those can't express.
 */
@Immutable
data class SimpliPayTokens(
    val jade: Color,
    val jadeTint: Color,
    val credit: Color,
    val creditTint: Color,
    val debit: Color,
    val ink: Color,
    val sub: Color,
    val faint: Color,
    val border: Color,
    val line: Color,
    val cardInk: Color,
    val cardLabel: Color,
    val ivoryBorder: Color,
    val verifiedGold: Color,
    val rosetteStroke: Color,
    /** 155° ivory gradient: `#FBFAF6 → #F0EEE6 (60%) → #E6E3D8`. */
    val ivoryGradient: Brush,
    /** Ordered stops of [ivoryGradient], for components that build their own angled brush. */
    val ivoryStops: List<Color>,
    /** Geist Mono — apply to money, card numbers, IDs, numeric dates and eyebrow labels. */
    val monoFontFamily: FontFamily,
)

val LocalSimpliPayTokens = staticCompositionLocalOf<SimpliPayTokens> {
    error("SimpliPayTokens not provided. Wrap content in MifosTheme.")
}

/** Convenience accessor mirroring `KptTheme` / `MaterialTheme`. */
object SimpliPayTheme {
    val tokens: SimpliPayTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSimpliPayTokens.current
}

private val Ivory1 = Color(0xFFFBFAF6)
private val Ivory2 = Color(0xFFF0EEE6)
private val Ivory3 = Color(0xFFE6E3D8)

/**
 * Builds the light-theme token set. Must be called from a composable because the
 * Geist Mono [FontFamily] is loaded from compose resources.
 */
@Composable
internal fun lightSimpliPayTokens(): SimpliPayTokens = SimpliPayTokens(
    jade = Color(0xFF0F8A7B),
    jadeTint = Color(0x1A0F8A7B), // rgba(15,138,123,0.10)
    credit = Color(0xFF0E9466),
    creditTint = Color(0x1A0E9466), // rgba(14,148,102,0.10)
    debit = Color(0xFF4A5560),
    ink = Color(0xFF101418),
    sub = Color(0xFF6F7782),
    faint = Color(0xFFA39E93),
    border = Color(0xFFDDD8CE),
    line = Color(0xFFECE8DF),
    cardInk = Color(0xFF16140E),
    cardLabel = Color(0xFF7A7464),
    ivoryBorder = Color(0xFFE0DCCE),
    verifiedGold = Color(0xFFC8B27A),
    rosetteStroke = Color(0xFF9C9684),
    ivoryGradient = Brush.linearGradient(
        0.0f to Ivory1,
        0.6f to Ivory2,
        1.0f to Ivory3,
    ),
    ivoryStops = listOf(Ivory1, Ivory2, Ivory3),
    monoFontFamily = monoFontFamily(),
)

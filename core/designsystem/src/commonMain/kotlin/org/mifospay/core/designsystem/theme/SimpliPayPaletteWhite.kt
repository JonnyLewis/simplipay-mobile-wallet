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

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import template.core.base.designsystem.toKptColorScheme

/**
 * SimpliPay **"Blueprint"** palette — the strong-white, bold alternative to the
 * "Platinum Ivory" default. Light-only, same discipline (one dominant + one
 * accent, 60/30/10, AA-gated), inverted to an uncompromising white ground.
 *
 * ```
 *   Dominant   #1B34D8  ultramarine   — every control, tab, active state, CTA
 *   Accent     #E8A21C  marigold      — money action ONLY (filled events, dark ink on top)
 *   Ink        #0E1524  near-black navy
 *   Ground     #F7F8FC  strong white  (cards #FFFFFF, hairlines carry separation)
 *   Wallet card ultramarine flood, white guilloché — the single bold event
 * ```
 *
 * Green (received) and red (declines) are left on the app's existing semantic
 * values — only the dominant + accent pair changes.
 *
 * -------------------------------------------------------------------------
 * HOW TO SWITCH THE APP TO BLUEPRINT — three edits in [MifosTheme] (`Theme.kt`):
 *
 *   1. line ~32   val selectedColorScheme = lightBlueprintColorScheme
 *   2. lines ~43  GradientColors(top/bottom/container = backgroundBlueprintLight)
 *   3. line ~60   val simpliPayTokens = lightBlueprintSimpliPayTokens()
 *
 * Revert = point those three references back at `lightKptColorScheme`,
 * `backgroundLight`, and `lightSimpliPayTokens()`. The ivory theme is never
 * deleted, so this is a safe A/B swap.
 * -------------------------------------------------------------------------
 */

// ── Material-slot colours (Blueprint light) ──────────────────────────────

// Dominant — ultramarine
private val primaryBlueprint = Color(0xFF1B34D8)
private val onPrimaryBlueprint = Color(0xFFFFFFFF)
private val primaryContainerBlueprint = Color(0xFFDEE3FB) // ultramarine tint, opaque
private val onPrimaryContainerBlueprint = Color(0xFF0E1C7A)

// Secondary — credit-green accent (semantic, kept stable)
private val secondaryBlueprint = Color(0xFF0E9466)
private val onSecondaryBlueprint = Color(0xFFFFFFFF)
private val secondaryContainerBlueprint = Color(0xFFD4ECDF)
private val onSecondaryContainerBlueprint = Color(0xFF06311F)

// Tertiary — marigold accent (the money "event")
private val tertiaryBlueprint = Color(0xFFE8A21C)
private val onTertiaryBlueprint = Color(0xFF201200)
private val tertiaryContainerBlueprint = Color(0xFFFBE9C2)
private val onTertiaryContainerBlueprint = Color(0xFF3A2A04)

// Error — declines only
private val errorBlueprint = Color(0xFFCE6A5E)
private val onErrorBlueprint = Color(0xFFFFFFFF)
private val errorContainerBlueprint = Color(0xFFF6DCD7)
private val onErrorContainerBlueprint = Color(0xFF5A1810)

// Strong-white surfaces
private val backgroundBlueprint = Color(0xFFF7F8FC) // screen bg — reads white, lets cards separate
private val onBackgroundBlueprint = Color(0xFF0E1524) // ink
private val surfaceBlueprint = Color(0xFFFFFFFF) // cards / fields
private val onSurfaceBlueprint = Color(0xFF0E1524) // ink
private val surfaceVariantBlueprint = Color(0xFFE9EBF3) // subtle cool fill / line
private val onSurfaceVariantBlueprint = Color(0xFF5A6274) // sub
private val outlineBlueprint = Color(0xFF9AA1AD) // faint
private val outlineVariantBlueprint = Color(0xFFD9DBE4) // border
private val scrimBlueprint = Color(0xFF000000)
private val inverseSurfaceBlueprint = Color(0xFF0E1524)
private val inverseOnSurfaceBlueprint = Color(0xFFEEF0FD)
private val inversePrimaryBlueprint = Color(0xFFA9B6F5)
private val surfaceDimBlueprint = Color(0xFFE7E9F3)
private val surfaceBrightBlueprint = Color(0xFFFFFFFF)
private val surfaceContainerLowestBlueprint = Color(0xFFFFFFFF)
private val surfaceContainerLowBlueprint = Color(0xFFFBFBFE)
private val surfaceContainerBlueprint = Color(0xFFF4F5FB)
private val surfaceContainerHighBlueprint = Color(0xFFEEEFF7)
private val surfaceContainerHighestBlueprint = Color(0xFFE7E9F3)

/** Flat screen background for [GradientColors] when Blueprint is active. */
val backgroundBlueprintLight = backgroundBlueprint

private val LightBlueprintColorScheme = lightColorScheme(
    primary = primaryBlueprint,
    onPrimary = onPrimaryBlueprint,
    primaryContainer = primaryContainerBlueprint,
    onPrimaryContainer = onPrimaryContainerBlueprint,
    secondary = secondaryBlueprint,
    onSecondary = onSecondaryBlueprint,
    secondaryContainer = secondaryContainerBlueprint,
    onSecondaryContainer = onSecondaryContainerBlueprint,
    tertiary = tertiaryBlueprint,
    onTertiary = onTertiaryBlueprint,
    tertiaryContainer = tertiaryContainerBlueprint,
    onTertiaryContainer = onTertiaryContainerBlueprint,
    error = errorBlueprint,
    onError = onErrorBlueprint,
    errorContainer = errorContainerBlueprint,
    onErrorContainer = onErrorContainerBlueprint,
    background = backgroundBlueprint,
    onBackground = onBackgroundBlueprint,
    surface = surfaceBlueprint,
    onSurface = onSurfaceBlueprint,
    surfaceVariant = surfaceVariantBlueprint,
    onSurfaceVariant = onSurfaceVariantBlueprint,
    outline = outlineBlueprint,
    outlineVariant = outlineVariantBlueprint,
    scrim = scrimBlueprint,
    inverseSurface = inverseSurfaceBlueprint,
    inverseOnSurface = inverseOnSurfaceBlueprint,
    inversePrimary = inversePrimaryBlueprint,
    surfaceDim = surfaceDimBlueprint,
    surfaceBright = surfaceBrightBlueprint,
    surfaceContainerLowest = surfaceContainerLowestBlueprint,
    surfaceContainerLow = surfaceContainerLowBlueprint,
    surfaceContainer = surfaceContainerBlueprint,
    surfaceContainerHigh = surfaceContainerHighBlueprint,
    surfaceContainerHighest = surfaceContainerHighestBlueprint,
)

/** Drop-in replacement for `lightKptColorScheme` in [MifosTheme]. */
val lightBlueprintColorScheme = LightBlueprintColorScheme.toKptColorScheme()

// ── Non-Material tokens (Blueprint) ──────────────────────────────────────
// The wallet card flips from an ivory gradient to an ULTRAMARINE FLOOD — the
// single bold event on the white ground — so the card-scoped tokens now carry
// on-brand (white) ink and a translucent-white guilloché.

// 155° ultramarine flood: #2440E4 → #1B34D8 (60%) → #10205F
private val Brand1 = Color(0xFF2440E4)
private val Brand2 = Color(0xFF1B34D8)
private val Brand3 = Color(0xFF10205F)

/**
 * Builds the Blueprint token set. Drop-in replacement for `lightSimpliPayTokens()`
 * in [MifosTheme]. Must be called from a composable (Geist Mono is loaded from
 * compose resources).
 */
@Composable
internal fun lightBlueprintSimpliPayTokens(): SimpliPayTokens = SimpliPayTokens(
    // dominant
    jade = Color(0xFF1B34D8),
    // ultramarine tint, 10%
    jadeTint = Color(0x1A1B34D8),
    // money semantics (kept stable)
    credit = Color(0xFF0E9466),
    creditTint = Color(0x1A0E9466),
    debit = Color(0xFF4A5560),
    // ink pair on white
    ink = Color(0xFF0E1524),
    sub = Color(0xFF5A6274),
    faint = Color(0xFF9AA1AD),
    border = Color(0xFFD9DBE4),
    line = Color(0xFFE9EBF3),
    // ↓ card-scoped: card is now an ultramarine flood
    // balance / numbers on the flood
    cardInk = Color(0xFFFFFFFF),
    // muted eyebrow label on the flood (70% white)
    cardLabel = Color(0xB3FFFFFF),
    // thin light top-border on the flood card
    ivoryBorder = Color(0x40FFFFFF),
    // accent — marigold (the money event)
    verifiedGold = Color(0xFFE8A21C),
    // guilloché rosette on the flood card — translucent white
    rosetteStroke = Color(0x59FFFFFF),
    // NOTE: field is named `ivoryGradient`/`ivoryStops` in the data class, but
    // under Blueprint it holds the ULTRAMARINE flood used behind the balance.
    ivoryGradient = Brush.linearGradient(
        0.0f to Brand1,
        0.6f to Brand2,
        1.0f to Brand3,
    ),
    ivoryStops = listOf(Brand1, Brand2, Brand3),
    monoFontFamily = monoFontFamily(),
)

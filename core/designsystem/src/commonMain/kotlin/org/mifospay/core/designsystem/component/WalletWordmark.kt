/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import org.mifospay.core.designsystem.theme.SimpliPayTheme

private fun wordmarkText(accentColor: Color) = buildAnnotatedString {
    append("simplipay")
    withStyle(SpanStyle(color = accentColor)) { append(".") }
}

/**
 * The `simplipay.` wordmark — always lowercase, ExtraBold, tight `-0.03em`
 * tracking, with the trailing period in jade. Used on splash, landing and login.
 *
 * @param fontSize design sizes: 26sp (login), 34sp (splash), 48sp (landing).
 */
@Composable
fun WalletWordmark(
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    inkColor: Color = SimpliPayTheme.tokens.ink,
    accentColor: Color = SimpliPayTheme.tokens.jade,
) {
    Text(
        modifier = modifier,
        text = wordmarkText(accentColor),
        style = TextStyle(
            fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = (fontSize.value * -0.03f).sp,
            color = inkColor,
        ),
    )
}

/**
 * The `simplipay.` wordmark with the brand letter-wave: each letter bubbles up,
 * settles back with a small overshoot, staggered left to right, then rests —
 * matching `simplipay-assets/web/wordmark-wave.html` (2.4 s cycle, 90 ms stagger,
 * cubic-bezier(0.36, 0, 0.24, 1)). The wave plays **exactly once per composition**
 * (i.e. once each time the home screen is loaded) instead of looping. Same styling
 * contract as [WalletWordmark]; the trailing period rides the wave last, in jade.
 */
@Composable
fun AnimatedWalletWordmark(
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    inkColor: Color = SimpliPayTheme.tokens.ink,
    accentColor: Color = SimpliPayTheme.tokens.jade,
) {
    val style = TextStyle(
        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        letterSpacing = (fontSize.value * -0.03f).sp,
    )
    // Wave amplitude scales with the wordmark size (≈ -0.33em up, +0.08em settle).
    val rise = fontSize.value / 3f
    val settle = fontSize.value / 12f

    // One-shot: a single clock (ms) runs from 0 until the last (staggered) letter
    // has settled back, then stops — so the wave plays once. Re-entering the home
    // screen re-composes this and restarts the clock, replaying it once per load
    // rather than looping forever.
    val totalMs = (WORDMARK.length - 1) * WAVE_STAGGER_MS + WAVE_SETTLE_END_MS
    val clockMs = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clockMs.snapTo(0f)
        clockMs.animateTo(
            targetValue = totalMs.toFloat(),
            animationSpec = tween(durationMillis = totalMs, easing = LinearEasing),
        )
    }

    Row(modifier = modifier) {
        WORDMARK.forEachIndexed { index, letter ->
            Text(
                text = letter.toString(),
                style = style,
                color = if (letter == '.') accentColor else inkColor,
                // Read the clock in the layout phase (not composition) and map it to
                // this letter's staggered slice of the wave.
                modifier = Modifier.offset {
                    val local = clockMs.value - index * WAVE_STAGGER_MS
                    IntOffset(0, waveLetterOffset(local, rise, settle).dp.roundToPx())
                },
            )
        }
    }
}

/**
 * Vertical offset (dp) of one wordmark letter at [localMs] into its own wave slice:
 * up by [rise], back past baseline by [settle], then to rest — the keyframe shape of
 * the original loop, evaluated as a one-shot. Returns 0 before the slice starts and
 * after it has settled.
 */
private fun waveLetterOffset(localMs: Float, rise: Float, settle: Float): Float {
    if (localMs <= 0f) return 0f
    val p1 = WAVE_CYCLE_MS * 0.12f
    val p2 = WAVE_CYCLE_MS * 0.26f
    val p3 = WAVE_CYCLE_MS * 0.38f
    return when {
        localMs < p1 -> lerp(0f, -rise, WaveEasing.transform(localMs / p1))
        localMs < p2 -> lerp(-rise, settle, WaveEasing.transform((localMs - p1) / (p2 - p1)))
        localMs < p3 -> lerp(settle, 0f, WaveEasing.transform((localMs - p2) / (p3 - p2)))
        else -> 0f
    }
}

private const val WORDMARK = "simplipay."
private const val WAVE_CYCLE_MS = 2_400
private const val WAVE_STAGGER_MS = 90
private const val WAVE_SETTLE_END_MS = WAVE_CYCLE_MS * 38 / 100
private val WaveEasing = CubicBezierEasing(0.36f, 0f, 0.24f, 1f)

/**
 * The `simplipay.` wordmark as a scalable [Painter] — the same brand logo as
 * [WalletWordmark] but usable where a `Painter` is required (e.g. the passcode
 * library's logo slot, or an `Image`). The text is measured once and **fit-scaled
 * preserving aspect ratio** into whatever bounds it's drawn at, so it never
 * distorts regardless of the host's box shape.
 *
 * @param fontSize the base size the wordmark is measured at; the painter scales
 *        crisply from this. `48sp` is a good logo base.
 */
@Composable
fun rememberWalletWordmarkPainter(
    fontSize: TextUnit = 48.sp,
    inkColor: Color = SimpliPayTheme.tokens.ink,
    accentColor: Color = SimpliPayTheme.tokens.jade,
): Painter {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(
        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        letterSpacing = (fontSize.value * -0.03f).sp,
        color = inkColor,
    )
    val layout = remember(measurer, style, accentColor) {
        measurer.measure(text = wordmarkText(accentColor), style = style)
    }
    return remember(layout) { WordmarkPainter(layout) }
}

private class WordmarkPainter(
    private val layout: TextLayoutResult,
) : Painter() {
    override val intrinsicSize: Size =
        Size(layout.size.width.toFloat(), layout.size.height.toFloat())

    override fun DrawScope.onDraw() {
        val lw = layout.size.width.toFloat()
        val lh = layout.size.height.toFloat()
        if (lw <= 0f || lh <= 0f) return
        val s = minOf(size.width / lw, size.height / lh)
        val dx = (size.width - lw * s) / 2f
        val dy = (size.height - lh * s) / 2f
        translate(dx, dy) {
            scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
                drawText(layout)
            }
        }
    }
}

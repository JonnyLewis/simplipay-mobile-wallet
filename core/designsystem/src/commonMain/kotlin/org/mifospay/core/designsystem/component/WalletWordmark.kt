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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * settles back with a small overshoot, staggered left to right, then the word
 * rests before looping — matching `simplipay-assets/web/wordmark-wave.html`
 * (2.4 s cycle, 90 ms stagger, cubic-bezier(0.36, 0, 0.24, 1)). Same styling
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
    val transition = rememberInfiniteTransition(label = "wordmarkWave")
    Row(modifier = modifier) {
        WORDMARK.forEachIndexed { index, letter ->
            val offsetDp by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = WAVE_CYCLE_MS
                        0f at 0 using WaveEasing
                        -rise at (WAVE_CYCLE_MS * 12 / 100) using WaveEasing
                        settle at (WAVE_CYCLE_MS * 26 / 100) using WaveEasing
                        0f at (WAVE_CYCLE_MS * 38 / 100)
                        // rest at baseline for the remainder of the cycle
                    },
                    initialStartOffset = StartOffset(index * WAVE_STAGGER_MS),
                ),
                label = "wordmarkLetter$index",
            )
            Text(
                text = letter.toString(),
                style = style,
                color = if (letter == '.') accentColor else inkColor,
                // Lambda offset: the per-frame value is read in layout, not composition.
                modifier = Modifier.offset { IntOffset(0, offsetDp.dp.roundToPx()) },
            )
        }
    }
}

private const val WORDMARK = "simplipay."
private const val WAVE_CYCLE_MS = 2_400
private const val WAVE_STAGGER_MS = 90
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

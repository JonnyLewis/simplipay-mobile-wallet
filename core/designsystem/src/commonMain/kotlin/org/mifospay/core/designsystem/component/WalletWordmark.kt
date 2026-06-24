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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.TextUnit
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

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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mifospay.core.designsystem.theme.SimpliPayTheme

/**
 * SimpliPay "rosette" — the guilloché / spirograph fan that sits behind balance,
 * Buy and KYC ID cards and forms the hero of the splash screen.
 *
 * It is [count] hairline **ellipses** (note: `rx != ry`), all sharing one centre,
 * each rotated by an equal `360° / count` slice. Stroke is a true `0.5px`
 * (do not round to 1dp — that loses the fineness) in warm grey
 * [SimpliPayTokens.rosetteStroke]. Per-ellipse [opacity] accumulates where the
 * fans overlap, exactly like the source SVG.
 *
 * Direct port of `rosette(rx, ry, n, op)` from `SimpliPay All Screens.dc.html`:
 * the drawing box is `radiusX * 2.3` square with the centre at `radiusX * 1.15`.
 *
 * Replicate the exact parameters per usage:
 * - Splash hero (spinning 90s): `radiusX=120, radiusY=39, count=35, opacity=0.85`
 * - Balance card top-right: `86, 30, 40, 0.30`; bottom-left: `70, 22, 34, 0.22`
 * - Buy "Pay from" card: `60, 20, 34, 0.26`
 * - KYC ID card: `70, 23, 34, 0.26`
 *
 * @param rotation extra rotation applied to the whole fan — drive with an infinite
 *        transition for the splash (`90s` linear, 0°→360°); leave `0f` on cards.
 */
@Composable
fun Rosette(
    radiusX: Dp,
    radiusY: Dp,
    count: Int,
    opacity: Float,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    color: Color = SimpliPayTheme.tokens.rosetteStroke,
) {
    val box = radiusX * 2.3f
    Canvas(modifier = modifier.size(box)) {
        val rx = radiusX.toPx()
        val ry = radiusY.toPx()
        val center = Offset(rx * 1.15f, rx * 1.15f)
        val strokePx = 0.5.dp.toPx()
        val topLeft = Offset(center.x - rx, center.y - ry)
        val size = Size(rx * 2f, ry * 2f)
        val step = 360f / count
        for (i in 0 until count) {
            rotate(degrees = i * step + rotation, pivot = center) {
                drawOval(
                    color = color,
                    topLeft = topLeft,
                    size = size,
                    alpha = opacity,
                    style = Stroke(width = strokePx),
                )
            }
        }
    }
}

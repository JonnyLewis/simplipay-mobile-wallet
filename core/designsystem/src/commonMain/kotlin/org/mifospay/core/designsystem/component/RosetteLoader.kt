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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mifospay.core.designsystem.theme.SimpliPayTheme

/**
 * The **splash hero**: the big spinning [Rosette] over a soft jade radial glow.
 *
 * Defaults are the splash values: `rosette(166, 54, 35, 0.85)` spinning `81s`
 * linear over a `431dp` radial jade glow (`#0F8A7B` @ 0.10 → transparent). This
 * is intentionally large and is used **only** on the splash screen — for the
 * app's loading indicator use the compact [RosetteLoadingIndicator] instead.
 */
@Composable
fun RosetteLoader(
    modifier: Modifier = Modifier,
    radiusX: Dp = 166.dp,
    radiusY: Dp = 54.dp,
    count: Int = 35,
    opacity: Float = 0.85f,
    glowSize: Dp = 431.dp,
    spinDurationMillis: Int = 81_000,
) {
    val tokens = SimpliPayTheme.tokens
    val angle by rosetteSpin(spinDurationMillis)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(glowSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to tokens.jade.copy(alpha = 0.10f),
                        0.7f to Color.Transparent,
                    ),
                ),
        )
        Rosette(
            radiusX = radiusX,
            radiusY = radiusY,
            count = count,
            opacity = opacity,
            // Spin via a layer transform on the cached fan (rotationZ in the deferred
            // graphicsLayer block) rather than re-tessellating the ovals every frame.
            modifier = Modifier.graphicsLayer { rotationZ = angle },
        )
    }
}

/**
 * The app's **loading indicator**: just the spinning [Rosette], small and
 * glow-less, meant to hover over the UI (typically inside a translucent overlay)
 * while content loads. This is the "extracted" rosette — none of the splash's
 * glow/wordmark/dots — so it reads as a compact spinner.
 *
 * Defaults to a ~`129dp` box (`radiusX = 56`) turning once every [spinDurationMillis].
 */
@Composable
fun RosetteLoadingIndicator(
    modifier: Modifier = Modifier,
    radiusX: Dp = 56.dp,
    radiusY: Dp = 18.dp,
    count: Int = 35,
    opacity: Float = 0.85f,
    spinDurationMillis: Int = 9_000,
) {
    val angle by rosetteSpin(spinDurationMillis)
    Rosette(
        radiusX = radiusX,
        radiusY = radiusY,
        count = count,
        opacity = opacity,
        // Spin the cached fan via a layer transform instead of redrawing it each frame.
        modifier = modifier.graphicsLayer { rotationZ = angle },
    )
}

/** Shared 0°→360° linear infinite spin used by both rosette loaders. */
@Composable
private fun rosetteSpin(durationMillis: Int) =
    rememberInfiniteTransition(label = "rosette-spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
        ),
        label = "rosette-angle",
    )

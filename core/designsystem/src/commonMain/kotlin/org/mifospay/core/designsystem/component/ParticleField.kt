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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import kotlin.math.roundToInt
import kotlin.random.Random

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
)

/**
 * SimpliPay constellation particle field — the slow-drifting jade "network" behind
 * the landing screen (and only there in the source design).
 *
 * Port of `setupField` / `animateField` from `SimpliPay All Screens.dc.html`, tuned
 * over the original design: **20% more particles** (density `1.35 → 1.62`), a
 * **faster drift** (velocity `±0.144 → ±0.3024` px/frame), and a **30%-darker**
 * jade (lerped 30% toward black) for stronger contrast on the ivory field:
 * - particle count = `round(width * height / 16000 * density)` from the measured size
 * - velocity per axis ∈ `[-0.3024, +0.3024]` px/frame
 * - particles bounce off the edges (velocity inverts; no wrap-around)
 * - dots drawn at [dotAlpha] (`0.30`); a line is drawn between any two within
 *   [linkDistancePx] (`120`), its alpha `(1 - d²/max²) * 0.28` — quadratic fade
 *
 * The field is purely decorative; place it behind content and do not let it take
 * pointer input. The animation loop is tied to composition, so it stops when the
 * composable leaves the tree (mirroring the prototype's `cancelAnimationFrame`).
 */
@Composable
fun ParticleField(
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.ui.graphics.lerp(SimpliPayTheme.tokens.jade, Color.Black, 0.3f),
    density: Float = 1.62f,
    linkDistancePx: Float = 120f,
    dotAlpha: Float = 0.30f,
    maxLinkAlpha: Float = 0.28f,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val particles = remember { mutableStateListOf<Particle>() }

    // A frame counter the Canvas reads so it redraws every animation frame.
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(size) {
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        if (w <= 0f || h <= 0f) return@LaunchedEffect

        // Seed once for this size: count = round(W*H/16000 * density).
        val n = (w * h / 16000f * density).roundToInt().coerceAtLeast(1)
        particles.clear()
        repeat(n) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    vx = Random.nextFloat() * 0.6048f - 0.3024f,
                    vy = Random.nextFloat() * 0.6048f - 0.3024f,
                    radius = Random.nextFloat() * 1.3f + 0.5f,
                ),
            )
        }

        while (isActive) {
            withFrameNanos {
                for (p in particles) {
                    if (p.x > w || p.x < 0f) p.vx = -p.vx
                    if (p.y > h || p.y < 0f) p.vy = -p.vy
                    p.x += p.vx
                    p.y += p.vy
                }
                frame++
            }
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged { size = it },
    ) {
        // Touch `frame` so the draw re-runs each animation tick.
        @Suppress("UNUSED_EXPRESSION")
        frame

        val max2 = linkDistancePx * linkDistancePx

        // Dots.
        for (p in particles) {
            drawCircle(
                color = color,
                radius = p.radius,
                center = Offset(p.x, p.y),
                alpha = dotAlpha,
            )
        }
        // Links between nearby particles (quadratic distance fade).
        for (a in particles.indices) {
            val pa = particles[a]
            for (b in a + 1 until particles.size) {
                val pb = particles[b]
                val dx = pa.x - pb.x
                val dy = pa.y - pb.y
                val d2 = dx * dx + dy * dy
                if (d2 < max2) {
                    val alpha = (1f - d2 / max2) * maxLinkAlpha
                    drawLine(
                        color = color,
                        start = Offset(pa.x, pa.y),
                        end = Offset(pb.x, pb.y),
                        strokeWidth = 1f,
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

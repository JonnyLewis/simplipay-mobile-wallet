/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.mifospay.core.designsystem.component.Rosette
import org.mifospay.core.designsystem.component.WalletWordmark
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import kotlin.math.roundToInt

/**
 * SimpliPay splash / loading screen (design frame 01).
 *
 * The hero is the [Rosette] `(120, 39, 35, 0.85)` spinning `90s` linear over a soft
 * `312dp` radial jade glow, with the `simplipay.` wordmark and three staggered
 * loading dots ([LdDots]) below. After [holdMillis] it forwards to [onTimeout]
 * (the session-resolved destination).
 */
@Composable
internal fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 1500,
) {
    val tokens = SimpliPayTheme.tokens

    LaunchedEffect(Unit) {
        delay(holdMillis)
        onTimeout()
    }

    val transition = rememberInfiniteTransition(label = "splash")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90_000, easing = LinearEasing),
        ),
        label = "rosette-spin",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Soft jade glow + spinning rosette, vertically centred.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(312.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            0.0f to tokens.jade.copy(alpha = 0.10f),
                            0.7f to Color.Transparent,
                        ),
                    ),
            )
            Rosette(
                radiusX = 120.dp,
                radiusY = 39.dp,
                count = 35,
                opacity = 0.85f,
                rotation = angle,
            )
        }

        // Wordmark + loading dots pinned toward the bottom.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Spacer(Modifier.weight(1f))
            WalletWordmark(fontSize = 34.sp)
            Spacer(Modifier.size(14.dp))
            LdDots()
        }
    }
}

/** Three jade dots bobbing up (`ldDot`), staggered 0 / 200 / 400 ms. */
@Composable
private fun LdDots() {
    val jade = SimpliPayTheme.tokens.jade
    val transition = rememberInfiniteTransition(label = "ld-dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(0 to 1f, 200 to 0.5f, 400 to 0.3f).forEach { (delayMs, baseAlpha) ->
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = androidx.compose.animation.core.EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delayMs),
                ),
                label = "dot-$delayMs",
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(jade.copy(alpha = baseAlpha)),
            )
        }
    }
}

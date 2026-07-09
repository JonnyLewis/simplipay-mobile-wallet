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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.mifospay.core.designsystem.component.RosetteLoader
import org.mifospay.core.designsystem.component.WalletWordmark
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import kotlin.math.roundToInt

/**
 * SimpliPay splash / loading screen (design frame 01).
 *
 * The hero is the large spinning [RosetteLoader] over its soft radial jade glow,
 * with the `simplipay.` wordmark and
 * three staggered loading dots ([LdDots]) below. After [holdMillis] (5s) it
 * forwards to [onTimeout] (the session-resolved destination).
 *
 * [onTimeout] is captured via [rememberUpdatedState] so the post-delay forward
 * uses the LATEST destination — the session resolves asynchronously after this
 * screen first composes, and we must not fire a stale first-frame target.
 */
@Composable
internal fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 5000,
) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) {
        delay(holdMillis)
        currentOnTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Soft jade glow + spinning rosette, vertically centred — the same
        // RosetteLoader the app uses as its loading indicator.
        RosetteLoader()

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

/**
 * Three jade dots that bob up and **brighten in a left-to-right wave**, staggered
 * 0 / 200 / 400 ms — so the jade highlight visibly travels across the dots as they
 * move. Each dot's vertical offset and colour intensity are driven by the same
 * staggered `phase` (0→1), keeping the lift and the colour shift in sync.
 */
@Composable
private fun LdDots() {
    val jade = SimpliPayTheme.tokens.jade
    val transition = rememberInfiniteTransition(label = "ld-dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(3) { index ->
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = androidx.compose.animation.core.EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200),
                ),
                label = "dot-$index",
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (-4f * phase).roundToInt()) }
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(jade.copy(alpha = 0.3f + 0.7f * phase)),
            )
        }
    }
}

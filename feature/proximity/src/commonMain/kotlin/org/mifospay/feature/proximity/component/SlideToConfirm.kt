/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mifospay.core.designsystem.icon.MifosIcons
import template.core.base.designsystem.theme.KptTheme
import kotlin.math.roundToInt

/**
 * Drag-the-thumb-left-to-right confirm control (spec §8.7). Crossing ~85% of
 * the track fires [onConfirmed]; releasing earlier springs back. Reusable —
 * the final placement is the Pay confirm sheet, but it works anywhere a
 * deliberate one-way confirm is wanted.
 *
 * Real-device niceties from the spec (haptics, reduce-motion, an explicit
 * button a11y fallback) are TODO; this is the gesture core.
 */
@Composable
fun SlideToConfirm(
    label: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val density = LocalDensity.current
    val thumbSize = 56.dp
    val trackHeight = 64.dp

    var trackWidthPx by remember { mutableStateOf(0) }
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var confirmed by remember { mutableStateOf(false) }

    val maxOffset = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)

    LaunchedEffect(enabled) {
        if (!enabled) offsetX.snapTo(0f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(
                if (enabled) {
                    KptTheme.colorScheme.primaryContainer
                } else {
                    KptTheme.colorScheme.surfaceVariant
                },
            )
            .onSizeChanged { trackWidthPx = it.width },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (confirmed) "✓" else label,
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onPrimaryContainer,
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(4.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(
                    if (enabled) KptTheme.colorScheme.primary else KptTheme.colorScheme.outline,
                )
                .pointerInput(enabled, maxOffset) {
                    if (!enabled || maxOffset <= 0f) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Compute progress live here — the pointerInput block only
                            // re-runs on key change, so closing over the composition-time
                            // `progress` val would read a stale 0f and never confirm.
                            val endProgress = offsetX.value / maxOffset
                            if (endProgress >= CONFIRM_THRESHOLD) {
                                confirmed = true
                                scope.launch { offsetX.animateTo(maxOffset) }
                                onConfirmed()
                            } else {
                                scope.launch { offsetX.animateTo(0f) }
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, maxOffset))
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MifosIcons.ArrowForward,
                contentDescription = null,
                tint = KptTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private const val CONFIRM_THRESHOLD = 0.85f

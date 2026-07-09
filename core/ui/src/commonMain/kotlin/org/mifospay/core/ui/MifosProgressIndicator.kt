/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifospay.core.designsystem.component.RosetteLoadingIndicator
import org.mifospay.core.designsystem.theme.MifosTheme

/**
 * Gates a loading spinner behind a short delay so loads that finish quickly never flash
 * a spinner (which would otherwise also animate concurrently with a screen transition and
 * read as jitter). Returns `true` once [delayMillis] has elapsed.
 */
@Composable
private fun rememberDelayedVisible(delayMillis: Long = 180L): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visible = true
    }
    return visible
}

@Composable
fun MifosProgressIndicator(
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (rememberDelayedVisible()) {
            RosetteLoadingIndicator()
        }
    }
}

@Composable
fun MifosProgressIndicatorMini(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (rememberDelayedVisible()) {
            RosetteLoadingIndicator(radiusX = 44.dp, radiusY = 14.dp)
        }
    }
}

@Composable
fun MifosProgressIndicatorOverlay(
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
            .clickable(
                enabled = false,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { },
        contentAlignment = Alignment.Center,
    ) {
        if (rememberDelayedVisible()) {
            RosetteLoadingIndicator()
        }
    }
}

@Preview
@Composable
private fun Loading_Preview() {
    MifosTheme {
        MifosProgressIndicator()
    }
}

@Preview
@Composable
private fun Overlay_Loading_Preview() {
    MifosTheme {
        MifosProgressIndicatorOverlay()
    }
}

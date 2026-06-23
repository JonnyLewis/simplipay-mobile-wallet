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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mifospay.core.designsystem.component.ParticleField
import org.mifospay.core.designsystem.component.WalletWordmark
import org.mifospay.core.designsystem.theme.SimpliPayTheme

/**
 * SimpliPay landing screen (design frame 02): the [ParticleField] constellation
 * behind a large `simplipay.` wordmark, a two-line tagline, and the two entry
 * CTAs. Shown to unauthenticated users as the start of the login flow.
 */
@Composable
internal fun LandingScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Decorative constellation, behind everything, non-interactive.
        ParticleField(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))

            Column(modifier = Modifier.padding(start = 30.dp, end = 30.dp, bottom = 56.dp)) {
                WalletWordmark(fontSize = 48.sp)
                Text(
                    modifier = Modifier.padding(top = 15.dp),
                    text = "Money, simplified.\nSend, buy and track in one place.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = tokens.sub,
                )
            }

            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PrimaryCta(label = "Create account", onClick = onCreateAccount)
                SecondaryCta(label = "I already have an account", onClick = onLogin)
            }
        }
    }
}

@Composable
private fun PrimaryCta(label: String, onClick: () -> Unit) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = tokens.jade,
                spotColor = tokens.jade,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.jade)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFBFAF6),
        )
    }
}

@Composable
private fun SecondaryCta(label: String, onClick: () -> Unit) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tokens.ink,
            textAlign = TextAlign.Center,
        )
    }
}

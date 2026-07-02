/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.designsystem.theme.SimpliPayTheme

/**
 * Top-Up Wallet hub: lets the user choose how to add money to their wallet.
 * Each option is a tappable card; the actual funding flows are not wired to a
 * backend yet, so the callbacks are stubs supplied by navigation.
 */
@Composable
internal fun TopUpWalletScreen(
    onEft: () -> Unit,
    onCash: () -> Unit,
    onCard: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    MifosScaffold(
        modifier = modifier.fillMaxSize(),
        backPress = onBackClick,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Header — back affordance + big title + subtitle.
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, tokens.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MifosIcons.ArrowBack2,
                    contentDescription = "Back",
                    tint = tokens.sub,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Top-Up Wallet",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = tokens.ink,
            )
            Text(
                text = "Choose how you'd like to add money",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.sub,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(20.dp))

            // Funding rails aren't wired to a backend yet — shown as clearly non-tappable "coming soon"
            // rather than actionable-looking rows that silently no-op.
            TopUpOptionCard(
                icon = MifosIcons.Bank,
                title = "Top up with EFT",
                subtitle = "Transfer from your bank account",
                onClick = onEft,
                comingSoon = true,
            )
            Spacer(Modifier.height(12.dp))
            TopUpOptionCard(
                icon = MifosIcons.AttachMoney,
                title = "Top up with cash",
                subtitle = "Deposit cash at a partner outlet",
                onClick = onCash,
                comingSoon = true,
            )
            Spacer(Modifier.height(12.dp))
            TopUpOptionCard(
                icon = MifosIcons.CreditCard,
                title = "Top up with card",
                subtitle = "Use a debit or credit card",
                onClick = onCard,
                comingSoon = true,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopUpOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    comingSoon: Boolean = false,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    val contentAlpha = if (comingSoon) 0.5f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = tokens.border, shape = shape)
            .clickable(enabled = !comingSoon, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.jadeTint.copy(alpha = contentAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.jade.copy(alpha = contentAlpha),
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.ink.copy(alpha = contentAlpha),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.sub.copy(alpha = contentAlpha),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (comingSoon) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tokens.jadeTint)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Coming soon",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.jade,
                )
            }
        } else {
            Icon(
                imageVector = MifosIcons.ChevronRight,
                contentDescription = null,
                tint = tokens.sub,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview
@Composable
private fun TopUpWalletScreenPreview() {
    MifosTheme(darkTheme = false) {
        TopUpWalletScreen(
            onEft = {},
            onCash = {},
            onCard = {},
            onBackClick = {},
        )
    }
}

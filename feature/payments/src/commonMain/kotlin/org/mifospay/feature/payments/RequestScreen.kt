/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import mobile_wallet.feature.payments.generated.resources.Res
import mobile_wallet.feature.payments.generated.resources.feature_payments_receive
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.MifosTheme
import template.core.base.designsystem.theme.KptTheme

/** The ways someone can pay money into this wallet; each opens its own detail screen. */
enum class ReceiveMethod { MOBILE, EFT }

@Composable
fun RequestScreen(
    showQr: () -> Unit,
    onShowDetail: (ReceiveMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            modifier = Modifier.padding(top = KptTheme.spacing.sm, bottom = KptTheme.spacing.sm),
            text = stringResource(Res.string.feature_payments_receive),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.primary,
        )

        ReceiveOptionRow(
            icon = MifosIcons.QrCode,
            title = "Payment QR",
            subtitle = "Show a QR code to get paid",
            onClick = showQr,
        )
        ReceiveOptionRow(
            icon = MifosIcons.Contact,
            title = "Mobile number",
            subtitle = "Get paid instantly via PayShap",
            onClick = { onShowDetail(ReceiveMethod.MOBILE) },
        )
        ReceiveOptionRow(
            icon = MifosIcons.Bank,
            title = "Bank transfer (EFT)",
            subtitle = "Share your account details",
            onClick = { onShowDetail(ReceiveMethod.EFT) },
        )
    }
}

@Composable
private fun ReceiveOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = KptTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(KptTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KptTheme.colorScheme.onPrimaryContainer,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(text = title, style = KptTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = MifosIcons.ChevronRight,
            contentDescription = null,
            tint = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun RequestScreenPreview() {
    MifosTheme {
        RequestScreen(
            showQr = {},
            onShowDetail = {},
            modifier = Modifier,
        )
    }
}

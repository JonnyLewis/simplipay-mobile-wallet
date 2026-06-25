/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifospay.core.designsystem.component.MifosBottomSheet
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import template.core.base.designsystem.theme.KptTheme

/**
 * "Send money" chooser shown when the user taps Send on home. Offers the four
 * ways to send: to a mobile number, to a bank account, by scanning a barcode/QR,
 * and (coming soon) a proximity/tap payment.
 */
@Composable
fun TransferOptionsBottomSheet(
    onSendToMobile: () -> Unit,
    onSendToBank: () -> Unit,
    onSendToBarcode: () -> Unit,
    onProximityPayment: () -> Unit,
    onAutoPay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    MifosBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = KptTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = "Send money",
                    modifier = Modifier.padding(
                        horizontal = KptTheme.spacing.lg,
                        vertical = KptTheme.spacing.sm,
                    ),
                    style = KptTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = tokens.ink,
                )

                TransferOptionRow(
                    icon = MifosIcons.SendToMobile,
                    title = "Send to mobile number",
                    subtitle = "Pay a contact by their phone number",
                    onClick = onSendToMobile,
                )
                TransferOptionRow(
                    icon = MifosIcons.SendToBank,
                    title = "Send to bank account",
                    subtitle = "Transfer money to a bank account",
                    onClick = onSendToBank,
                )
                TransferOptionRow(
                    icon = MifosIcons.Scan,
                    title = "Send to barcode",
                    subtitle = "Scan a QR or barcode to pay",
                    onClick = onSendToBarcode,
                )
                TransferOptionRow(
                    icon = MifosIcons.Proximity,
                    title = "Proximity payment",
                    subtitle = "Coming soon",
                    onClick = onProximityPayment,
                )
                TransferOptionRow(
                    icon = MifosIcons.Payment,
                    title = "AutoPay",
                    subtitle = "Schedule and manage recurring payments",
                    onClick = onAutoPay,
                )
            }
        },
    )
}

@Composable
private fun TransferOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = KptTheme.spacing.lg, vertical = KptTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tokens.jadeTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.jade,
                modifier = Modifier.size(22.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = KptTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = tokens.ink,
            )
            Text(
                text = subtitle,
                style = KptTheme.typography.bodySmall,
                color = tokens.sub,
            )
        }
    }
}

@Preview
@Composable
fun TransferOptionsBottomSheetPreview() {
    MifosTheme {
        TransferOptionsBottomSheet(
            onSendToMobile = {},
            onSendToBank = {},
            onSendToBarcode = {},
            onProximityPayment = {},
            onAutoPay = {},
            onDismiss = {},
        )
    }
}

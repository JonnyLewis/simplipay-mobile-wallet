/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobile_wallet.feature.profile.generated.resources.Res
import mobile_wallet.feature.profile.generated.resources.feature_profile_email
import mobile_wallet.feature.profile.generated.resources.feature_profile_mobile
import mobile_wallet.feature.profile.generated.resources.feature_profile_username
import mobile_wallet.feature.profile.generated.resources.feature_profile_vpa
import org.jetbrains.compose.resources.stringResource
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.model.client.Client
import template.core.base.designsystem.theme.KptTheme

@Composable
fun ProfileDetailsCard(
    client: Client,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(KptTheme.colorScheme.surface)
            .border(width = 1.dp, color = tokens.border, shape = cardShape)
            .padding(horizontal = KptTheme.spacing.lg),
    ) {
        ProfileItem(
            label = stringResource(Res.string.feature_profile_username),
            value = client.displayName,
        )
        ProfileItem(
            label = stringResource(Res.string.feature_profile_email),
            value = client.emailAddress,
            valueFontFamily = tokens.monoFontFamily,
        )
        ProfileItem(
            label = stringResource(Res.string.feature_profile_vpa),
            value = client.externalId,
            valueFontFamily = tokens.monoFontFamily,
        )
        ProfileItem(
            label = stringResource(Res.string.feature_profile_mobile),
            value = client.mobileNo,
            valueFontFamily = tokens.monoFontFamily,
            showDivider = false,
        )
    }
}

@Composable
fun ProfileItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueFontFamily: FontFamily? = null,
    showDivider: Boolean = true,
) {
    val tokens = SimpliPayTheme.tokens
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.height(KptTheme.spacing.md))
        Text(
            text = label.uppercase(),
            color = tokens.cardLabel,
            style = KptTheme.typography.labelSmall.copy(
                fontFamily = tokens.monoFontFamily,
                letterSpacing = 1.5.sp,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = tokens.ink,
            style = KptTheme.typography.bodyLarge.copy(
                fontFamily = valueFontFamily ?: KptTheme.typography.bodyLarge.fontFamily,
                fontWeight = FontWeight(500),
            ),
        )
        Spacer(modifier = Modifier.height(KptTheme.spacing.md))
        if (showDivider) {
            HorizontalDivider(color = tokens.line)
        }
    }
}

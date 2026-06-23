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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.mifospay.core.designsystem.theme.SimpliPayTheme

/**
 * The `simplipay.` wordmark — always lowercase, ExtraBold, tight `-0.03em`
 * tracking, with the trailing period in jade. Used on splash, landing and login.
 *
 * @param fontSize design sizes: 26sp (login), 34sp (splash), 48sp (landing).
 */
@Composable
fun WalletWordmark(
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    inkColor: androidx.compose.ui.graphics.Color = SimpliPayTheme.tokens.ink,
    accentColor: androidx.compose.ui.graphics.Color = SimpliPayTheme.tokens.jade,
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            append("simplipay")
            withStyle(SpanStyle(color = accentColor)) { append(".") }
        },
        style = TextStyle(
            fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = (fontSize.value * -0.03f).sp,
            color = inkColor,
        ),
    )
}

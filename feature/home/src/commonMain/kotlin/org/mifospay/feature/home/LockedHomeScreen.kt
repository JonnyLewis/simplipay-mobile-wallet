/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.SimpliPayTheme

private const val OTP_LENGTH = 6
private const val RESEND_SECONDS = 60

/**
 * Home screen shown to a **wallet-no-access** user — a dummy/sample home behind a translucent
 * lock veil explaining the account isn't activated yet, with a CTA that brings up an OTP entry
 * sheet. Deliberately VM-free: a no-access user has no stored client, so the real [HomeViewModel]
 * (which requires one) must not be constructed.
 *
 * @param onVerify invoked when an OTP is submitted — wire to the real verification flow when it
 * exists. For now the sheet collects the code and dismisses.
 */
@Composable
internal fun LockedHomeScreen(
    onVerify: (otp: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOtpSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Dummy, non-interactive home backdrop (slightly blurred under the veil).
        DummyHomeBackdrop(modifier = Modifier.blur(2.dp))

        // Translucent veil that also swallows touches so the dummy content can't be used.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
                .clickable(
                    enabled = false,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            contentAlignment = Alignment.Center,
        ) {
            LockCard(onVerify = { showOtpSheet = true })
        }

        if (showOtpSheet) {
            OtpEntrySheet(
                onDismiss = { showOtpSheet = false },
                onSubmit = { code ->
                    onVerify(code)
                    showOtpSheet = false
                },
            )
        }
    }
}

@Composable
private fun LockCard(onVerify: () -> Unit) {
    val tokens = SimpliPayTheme.tokens
    Column(
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.border, RoundedCornerShape(22.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(tokens.jade.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MifosIcons.Security,
                contentDescription = null,
                tint = tokens.jade,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Account not activated yet",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = tokens.ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Verify your number to activate your wallet. Until then your balance and " +
                "transactions are just a preview.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.sub,
        )
        Spacer(Modifier.height(22.dp))
        JadeButton(text = "Verify now", onClick = onVerify)
    }
}

/**
 * Bottom-anchored OTP entry sheet over a dimmed scrim. Rendered in a full-screen [Dialog] so it
 * also covers the app's bottom navigation bar. Collects a [OTP_LENGTH]-digit code via a hidden
 * [BasicTextField] rendered as individual cells, with a numeric keyboard, plus a
 * [RESEND_SECONDS]-second resend countdown.
 */
@Composable
private fun OtpEntrySheet(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    var otp by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableStateOf(RESEND_SECONDS) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Tick down once a second until the resend becomes available again.
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Inner card; absorbs taps so a tap inside doesn't dismiss the sheet.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        enabled = false,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {}
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(tokens.border),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Enter OTP code",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = tokens.ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "We sent a $OTP_LENGTH-digit code to your number. Enter it below to " +
                        "verify and activate your wallet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.sub,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))

                BasicTextField(
                    value = otp,
                    onValueChange = { input ->
                        if (input.length <= OTP_LENGTH && input.all(Char::isDigit)) otp = input
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.focusRequester(focusRequester),
                    decorationBox = { OtpCells(otp = otp) },
                )

                Spacer(Modifier.height(28.dp))
                JadeButton(
                    text = "Verify",
                    enabled = otp.length == OTP_LENGTH,
                    onClick = { onSubmit(otp) },
                )
                Spacer(Modifier.height(16.dp))
                ResendRow(
                    secondsLeft = secondsLeft,
                    onResend = {
                        otp = ""
                        secondsLeft = RESEND_SECONDS
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Shows the resend countdown while [secondsLeft] > 0, then an active "Resend code" action. */
@Composable
private fun ResendRow(
    secondsLeft: Int,
    onResend: () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    if (secondsLeft > 0) {
        Text(
            text = "Resend code in 0:${secondsLeft.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelLarge,
            color = tokens.sub,
        )
    } else {
        Text(
            text = "Resend code",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = tokens.jade,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onResend)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun OtpCells(otp: String) {
    val tokens = SimpliPayTheme.tokens
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        repeat(OTP_LENGTH) { index ->
            val char = otp.getOrNull(index)?.toString() ?: ""
            val isActive = index == otp.length
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) tokens.jade else tokens.border,
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = tokens.monoFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = tokens.ink,
                )
            }
        }
    }
}

@Composable
private fun JadeButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) tokens.jade else tokens.jade.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFFBFAF6),
        )
    }
}

/** A static, sample home layout used only as the backdrop behind the lock veil. */
@Composable
private fun DummyHomeBackdrop(modifier: Modifier = Modifier) {
    val tokens = SimpliPayTheme.tokens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "simplipay.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = tokens.ink,
        )
        // Balance card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(brush = tokens.ivoryGradient)
                .border(1.dp, tokens.ivoryBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text("Available balance", style = MaterialTheme.typography.labelMedium, color = tokens.cardLabel)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "R 12,480.50",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = tokens.monoFontFamily,
                    fontWeight = FontWeight.Bold,
                ),
                color = tokens.cardInk,
            )
        }
        // Quick actions
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Send", "Request", "Buy").forEach { label ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, tokens.border, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(tokens.jade.copy(alpha = 0.12f)))
                    Spacer(Modifier.height(8.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, color = tokens.ink)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text("Recent", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = tokens.ink)
        listOf("MTN Airtime" to "−R 50.00", "Salary credit" to "+R 12,000.00", "City Power" to "−R 200.00").forEach { (title, amount) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, tokens.border, RoundedCornerShape(14.dp)))
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = tokens.ink, modifier = Modifier.weight(1f))
                Text(amount, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = tokens.monoFontFamily), color = tokens.sub)
            }
        }
    }
}

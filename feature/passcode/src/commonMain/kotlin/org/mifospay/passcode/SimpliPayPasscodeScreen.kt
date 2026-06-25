/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.passcode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mifospay.core.designsystem.theme.SimpliPayTheme

/**
 * SimpliPay-styled passcode keypad matching the `pinS` design frame: avatar + greeting,
 * jade dots, raised ivory mono keys, Face ID as a keypad cell on unlock, and "Forgot PIN?".
 * Drives the (vendored) [PasscodeManager] which owns create/confirm/verify + persistence.
 *
 * Mirrors the upstream `PasscodeScreen` API so [org.mifos.feature.passcode.MifosPasscode]
 * can swap to it with no behavioural change.
 */
@Composable
fun SimpliPayPasscodeScreen(
    passcodeManager: PasscodeManager,
    onResult: (PasscodeResult) -> Unit,
    modifier: Modifier = Modifier,
    userInitial: String? = null,
    isExternalAuthEnabled: Boolean = false,
    externalAuthButton: (@Composable (Modifier) -> Unit)? = null,
) {
    val tokens = SimpliPayTheme.tokens
    val state by passcodeManager.state.collectAsStateWithLifecycle()

    DisposableEffect(onResult) {
        passcodeManager.setResultCallback(onResult)
        onDispose { passcodeManager.setResultCallback(null) }
    }

    // New passcodes are 6 digits to match the design (no 4/6 switch).
    LaunchedEffect(Unit) {
        if (passcodeManager.state.value.loadedPasscode == null) {
            passcodeManager.updatePasscodeLength(PasscodeLength.SIX_DIGIT)
        }
    }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.shakeAnimationTrigger) {
        if (state.shakeAnimationTrigger > 0) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 320
                    (-14f) at 60
                    14f at 120
                    (-9f) at 180
                    9f at 240
                    0f at 320
                },
            )
        }
    }

    val title = when (state.passcodeStep) {
        PasscodeStep.Create -> "Create your PIN"
        PasscodeStep.Confirm -> "Confirm your PIN"
        PasscodeStep.ChangeVerify -> "Enter your current PIN"
        else -> if (userInitial != null) "Hi" else "Enter your PIN"
    }
    val subtitle = when (state.passcodeStep) {
        PasscodeStep.Create -> "Choose a ${state.passcodeLength.length}-digit PIN"
        PasscodeStep.Confirm -> "Re-enter your PIN to confirm"
        PasscodeStep.ChangeVerify -> "Verify it's you before changing your PIN"
        else -> "Enter your PIN to unlock"
    }
    val showForgot = state.passcodeStep == PasscodeStep.Enter ||
        state.passcodeStep == PasscodeStep.ChangeVerify
    val showFaceId = state.passcodeStep == PasscodeStep.Enter &&
        isExternalAuthEnabled && externalAuthButton != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        // Avatar
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(tokens.jade.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (userInitial?.take(1)?.uppercase()).orEmpty(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = tokens.jade,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = tokens.ink,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.sub,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(34.dp))
        // Dots
        Row(
            modifier = Modifier.graphicsLayer { translationX = shake.value },
            horizontalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            repeat(state.passcodeLength.length) { index ->
                val filled = index < state.filledDots
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (filled) tokens.jade else Color.Transparent)
                        .border(
                            width = 1.6.dp,
                            color = if (filled) tokens.jade else tokens.faint,
                            shape = CircleShape,
                        ),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Keypad
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    row.forEach { digit ->
                        PinKey(
                            label = digit,
                            tokens = tokens,
                            modifier = Modifier.weight(1f),
                            onClick = { passcodeManager.enterKey(digit) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                // Bottom-left: Face ID on unlock, else empty.
                Box(modifier = Modifier.weight(1f).height(60.dp), contentAlignment = Alignment.Center) {
                    if (showFaceId) externalAuthButton?.invoke(Modifier)
                }
                PinKey(
                    label = "0",
                    tokens = tokens,
                    modifier = Modifier.weight(1f),
                    onClick = { passcodeManager.enterKey("0") },
                )
                // Bottom-right: delete.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clickable { passcodeManager.deleteKey() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⌫",
                        style = MaterialTheme.typography.headlineSmall,
                        color = tokens.sub,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        if (showForgot) {
            Text(
                text = "Forgot PIN?",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = tokens.jade,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { passcodeManager.forgetPasscode() }
                    .padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun PinKey(
    label: String,
    tokens: org.mifospay.core.designsystem.theme.SimpliPayTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(60.dp)
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .clip(shape)
            .background(tokens.ivoryStops[1])
            .border(1.dp, tokens.ivoryBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = tokens.monoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 25.sp,
            ),
            color = tokens.cardInk,
        )
    }
}

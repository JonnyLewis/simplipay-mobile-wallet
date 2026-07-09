/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.buy

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.theme.SimpliPayTheme

private val QUICK_AMOUNTS = listOf(20, 50, 100, 200, 500)

/**
 * Purchase form for a single [VasService]: an account-identifier field, an
 * amount field (with quick-amount chips) and a jade "Continue" CTA. The flow is
 * UI-complete; on submit it shows an in-app confirmation (there is no VAS
 * provider backend wired yet).
 */
@Composable
internal fun BuyServiceScreen(
    service: VasService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var identifier by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val isValid = identifier.isNotBlank() && (amount.toIntOrNull() ?: 0) > 0

    MifosScaffold(
        modifier = modifier.fillMaxSize(),
        topBarTitle = service.label,
        backPress = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            FieldLabel(service.identifierLabel)
            IvoryField(
                value = identifier,
                onValueChange = { identifier = it },
                placeholder = service.identifierPlaceholder,
                keyboardType = KeyboardType.Text,
            )

            FieldLabel("Amount")
            IvoryField(
                value = amount,
                onValueChange = { new -> amount = new.filter { it.isDigit() }.take(7) },
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                leading = "R",
                mono = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QUICK_AMOUNTS.forEach { value ->
                    AmountChip(
                        modifier = Modifier.weight(1f),
                        label = "R$value",
                        selected = amount == value.toString(),
                        onClick = { amount = value.toString() },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            JadeCta(
                label = "Continue",
                enabled = isValid,
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "${service.label} of R$amount for $identifier submitted.",
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        color = SimpliPayTheme.tokens.ink,
    )
}

@Composable
private fun IvoryField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    leading: String? = null,
    mono: Boolean = false,
) {
    val tokens = SimpliPayTheme.tokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = tokens.faint) },
        leadingIcon = leading?.let { { Text(it, color = tokens.sub, fontWeight = FontWeight.SemiBold) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = if (mono) {
            androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontFamily = tokens.monoFontFamily)
        } else {
            androidx.compose.material3.MaterialTheme.typography.bodyLarge
        },
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            focusedBorderColor = tokens.jade,
            unfocusedBorderColor = tokens.border,
            cursorColor = tokens.jade,
            focusedTextColor = tokens.ink,
            unfocusedTextColor = tokens.ink,
        ),
    )
}

@Composable
private fun AmountChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) tokens.jade else androidx.compose.material3.MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = if (selected) tokens.jade else tokens.border, shape = shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                fontFamily = tokens.monoFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (selected) Color(0xFFFBFAF6) else tokens.sub,
        )
    }
}

@Composable
private fun JadeCta(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .then(
                if (enabled) {
                    Modifier.shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = tokens.jade, spotColor = tokens.jade)
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.jade)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            color = Color(0xFFFBFAF6),
        )
    }
}

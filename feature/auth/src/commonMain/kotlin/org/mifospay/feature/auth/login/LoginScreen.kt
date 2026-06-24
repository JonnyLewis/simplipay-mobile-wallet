/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.auth.login

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mobile_wallet.feature.auth.generated.resources.Res
import mobile_wallet.feature.auth.generated.resources.feature_auth_connected_to
import mobile_wallet.feature.auth.generated.resources.feature_auth_login
import mobile_wallet.feature.auth.generated.resources.feature_auth_password
import mobile_wallet.feature.auth.generated.resources.feature_auth_sign_up
import mobile_wallet.feature.auth.generated.resources.feature_auth_username
import mobile_wallet.feature.auth.generated.resources.feature_auth_welcome_back
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.designsystem.component.BasicDialogState
import org.mifospay.core.designsystem.component.MifosBasicDialog
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.component.WalletWordmark
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.ui.MifosProgressIndicatorOverlay
import org.mifospay.core.ui.utils.EventsEffect
import template.core.base.platform.PlatformBuildConfig
import template.core.base.ui.detectMultiTapGesture

@Composable
internal fun LoginScreen(
    onNavigateBack: () -> Unit,
    navigateToMifosPasscodeScreen: () -> Unit,
    navigateToSignupScreen: () -> Unit,
    onShowInstanceSelector: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            is LoginEvent.NavigateBack -> onNavigateBack.invoke()
            is LoginEvent.NavigateToSignup -> navigateToSignupScreen.invoke()
            is LoginEvent.NavigateToMifosPasscodeScreen -> navigateToMifosPasscodeScreen()
            is LoginEvent.ShowToast -> {
                scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LoginDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(LoginAction.ErrorDialogDismiss) }
        },
    )

    LoginScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onAction = remember(viewModel) {
            { viewModel.trySendAction(it) }
        },
        onShowInstanceSelector = onShowInstanceSelector,
    )

    if (state.dialogState is LoginState.DialogState.Loading) {
        MifosProgressIndicatorOverlay()
    }
}

@Composable
private fun LoginScreen(
    state: LoginState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onAction: (LoginAction) -> Unit,
    onShowInstanceSelector: () -> Unit,
) {
    MifosScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LoginScreenContent(
            state = state,
            onAction = onAction,
            onShowInstanceSelector = onShowInstanceSelector,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}

@Composable
private fun LoginDialogs(
    dialogState: LoginState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is LoginState.DialogState.Error -> MifosBasicDialog(
            visibilityState = BasicDialogState.Shown(
                message = dialogState.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        is LoginState.DialogState.Loading -> Unit

        null -> Unit
    }
}

@Composable
private fun LoginScreenContent(
    state: LoginState,
    modifier: Modifier = Modifier,
    onAction: (LoginAction) -> Unit,
    onShowInstanceSelector: () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens

    Column(
        modifier = modifier
            .fillMaxSize()
            .detectMultiTapGesture(onGestureDetected = onShowInstanceSelector)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        WalletWordmark(fontSize = 26.sp)

        Text(
            modifier = Modifier.padding(top = 36.dp),
            text = stringResource(Res.string.feature_auth_welcome_back),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp,
            ),
            color = tokens.ink,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "Sign in to continue to your wallet.",
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.sub,
        )

        Spacer(Modifier.height(28.dp))

        IvoryTextField(
            value = state.username,
            onValueChange = { onAction(LoginAction.UsernameChanged(it)) },
            label = stringResource(Res.string.feature_auth_username),
            leadingIcon = MifosIcons.Person,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        Spacer(Modifier.height(14.dp))

        IvoryTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = stringResource(Res.string.feature_auth_password),
            leadingIcon = MifosIcons.OutlinedLock,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = if (state.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { onAction(LoginAction.TogglePasswordVisibility) }) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) {
                            MifosIcons.OutlinedVisibilityOff
                        } else {
                            MifosIcons.OutlinedVisibility
                        },
                        contentDescription = null,
                        tint = tokens.faint,
                    )
                }
            },
        )

        Spacer(Modifier.height(28.dp))

        val isLoginButtonEnabled = state.username.isNotEmpty() && state.password.isNotEmpty()
        JadeButton(
            label = stringResource(Res.string.feature_auth_login),
            enabled = isLoginButtonEnabled,
            onClick = { onAction(LoginAction.LoginClicked) },
        )

        SignupRow(
            onSignupClick = { onAction(LoginAction.SignupClicked) },
        )

        Spacer(modifier = Modifier.weight(1f))

        // Server Instance Info at bottom - only visible in debug builds
        if (PlatformBuildConfig.isDebug) {
            ServerInstanceInfo(
                endpoint = state.selectedInstanceEndpoint ?: "",
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun IvoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val tokens = SimpliPayTheme.tokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = tokens.faint)
        },
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = tokens.jade,
            unfocusedBorderColor = tokens.border,
            cursorColor = tokens.jade,
            focusedTextColor = tokens.ink,
            unfocusedTextColor = tokens.ink,
            focusedLabelColor = tokens.jade,
            unfocusedLabelColor = tokens.sub,
        ),
    )
}

@Composable
private fun JadeButton(
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
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = tokens.jade,
                        spotColor = tokens.jade,
                    )
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
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFBFAF6),
        )
    }
}

@Composable
private fun SignupRow(
    onSignupClick: () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Don’t have an account yet? ",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.sub,
        )
        Text(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onSignupClick()
            },
            text = stringResource(Res.string.feature_auth_sign_up),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = tokens.jade,
        )
    }
}

@Composable
private fun ServerInstanceInfo(
    endpoint: String,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    if (endpoint.isNotEmpty()) {
        Text(
            text = stringResource(Res.string.feature_auth_connected_to, endpoint),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = tokens.monoFontFamily),
            color = tokens.faint,
            modifier = modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    MifosTheme {
        LoginScreen(
            state = LoginState(dialogState = null),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onShowInstanceSelector = {},
        )
    }
}

/** Headless render-harness entry point (themed, side-effect free). */
@Composable
fun LoginRenderPreview() {
    MifosTheme(darkTheme = false) {
        LoginScreen(
            state = LoginState(
                username = "jonny.wallet",
                password = "secret123",
                dialogState = null,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onShowInstanceSelector = {},
        )
    }
}

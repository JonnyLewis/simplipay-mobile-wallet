/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.auth.signup.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mobile_wallet.feature.auth.generated.resources.Res
import mobile_wallet.feature.auth.generated.resources.feature_auth_address_line_1
import mobile_wallet.feature.auth.generated.resources.feature_auth_address_line_2
import mobile_wallet.feature.auth.generated.resources.feature_auth_confirm_password
import mobile_wallet.feature.auth.generated.resources.feature_auth_country
import mobile_wallet.feature.auth.generated.resources.feature_auth_email
import mobile_wallet.feature.auth.generated.resources.feature_auth_first_name
import mobile_wallet.feature.auth.generated.resources.feature_auth_last_name
import mobile_wallet.feature.auth.generated.resources.feature_auth_mobile_no
import mobile_wallet.feature.auth.generated.resources.feature_auth_password
import mobile_wallet.feature.auth.generated.resources.feature_auth_pin_code
import mobile_wallet.feature.auth.generated.resources.feature_auth_state
import mobile_wallet.feature.auth.generated.resources.feature_auth_username
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.designsystem.component.BasicDialogState
import org.mifospay.core.designsystem.component.LoadingDialogState
import org.mifospay.core.designsystem.component.MifosBasicDialog
import org.mifospay.core.designsystem.component.MifosLoadingDialog
import org.mifospay.core.designsystem.component.MifosOutlinedTextField
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.component.MifosTopAppBar
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.ui.CombinedPasswordErrorCard
import org.mifospay.core.ui.DropdownBoxItem
import org.mifospay.core.ui.ExposedDropdownBox
import org.mifospay.core.ui.MifosPasswordField
import org.mifospay.core.ui.utils.EventsEffect
import org.mifospay.feature.auth.signup.SignUpAction
import org.mifospay.feature.auth.signup.SignUpEvent
import org.mifospay.feature.auth.signup.SignUpState
import org.mifospay.feature.auth.signup.SignupViewModel
import org.mifospay.feature.mpay.qr.scan.CodeType
import org.mifospay.feature.mpay.qr.scan.QrScannerWithPermissions

/** Steps of the KYC-style signup wizard (facial verification intentionally omitted). */
private enum class KycStep(val title: String) {
    VERIFY("Verify your identity"),
    SCANNING("Scan your ID"),
    DETAILS("Your details"),
    REVIEW("Review & confirm"),
}

/**
 * Redesigned signup as a KYC wizard: choose to **scan an ID** or **enter
 * manually**, fill/confirm details, then submit. This is a UI restructure only —
 * it drives the same [SignupViewModel] and hits the same registration API
 * (`createUser` → `createClient` → `assignClientToUser`) via [SignUpAction.SubmitClick].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KycSignupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignupViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(KycStep.VERIFY) }

    EventsEffect(viewModel) { event ->
        when (event) {
            is SignUpEvent.NavigateBack -> onNavigateBack()
            is SignUpEvent.NavigateToLogin -> onNavigateToLogin(event.username)
            is SignUpEvent.ShowToast -> scope.launch { snackbarHostState.showSnackbar(event.message) }
        }
    }

    KycDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) { { viewModel.trySendAction(SignUpAction.ErrorDialogDismiss) } },
    )

    val onAction: (SignUpAction) -> Unit = remember(viewModel) { { viewModel.trySendAction(it) } }
    val goBack: () -> Unit = {
        when (step) {
            KycStep.VERIFY -> onAction(SignUpAction.CloseClick)
            KycStep.SCANNING -> step = KycStep.VERIFY
            KycStep.DETAILS -> step = KycStep.VERIFY
            KycStep.REVIEW -> step = KycStep.DETAILS
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    MifosScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MifosTopAppBar(
                title = step.title,
                scrollBehavior = scrollBehavior,
                navigationIcon = MifosIcons.Back,
                navigationIconContentDescription = "Back",
                onNavigationIconClick = goBack,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (step) {
                KycStep.VERIFY -> KycVerifyStep(
                    onScan = { step = KycStep.SCANNING },
                    onEnter = { step = KycStep.DETAILS },
                )
                KycStep.SCANNING -> KycScanningStep(onContinue = { step = KycStep.DETAILS })
                KycStep.DETAILS -> KycDetailsStep(
                    state = state,
                    onAction = onAction,
                    onContinue = { step = KycStep.REVIEW },
                )
                KycStep.REVIEW -> KycReviewStep(
                    state = state,
                    onEdit = { step = KycStep.DETAILS },
                    onConfirm = { onAction(SignUpAction.SubmitClick) },
                )
            }
        }
    }
}

/* ----------------------------- Step 1: Verify ----------------------------- */

@Composable
private fun KycVerifyStep(
    onScan: () -> Unit,
    onEnter: () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Confirm who you are to open your wallet.",
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.sub,
        )
        Spacer(Modifier.height(8.dp))
        ChoiceCard(
            icon = MifosIcons.Camera,
            title = "Scan an ID card",
            subtitle = "We read your details from your ID",
            onClick = onScan,
        )
        ChoiceCard(
            icon = MifosIcons.Edit,
            title = "Enter manually",
            subtitle = "Type your details yourself",
            onClick = onEnter,
        )
    }
}

@Composable
private fun ChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = tokens.border, shape = shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .background(tokens.jadeTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tokens.jade, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = tokens.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tokens.sub)
        }
        Icon(MifosIcons.ChevronRight, contentDescription = null, tint = tokens.faint)
    }
}

/* ----------------------------- Step 2: Scanning ----------------------------- */

@Composable
private fun KycScanningStep(onContinue: () -> Unit) {
    val tokens = SimpliPayTheme.tokens
    val clipboard = LocalClipboardManager.current
    // Phase 0: capture the raw PDF417 payload from the back of the SA smart ID so we
    // can reverse-engineer the field layout. A later phase parses this into the
    // SignUpState fields; for now we surface the raw string for inspection.
    var rawPayload by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        if (rawPayload == null) {
            Text(
                text = "Scan the barcode on the back of your ID",
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.ink,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Line up the wide striped PDF417 barcode inside the frame.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.sub,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1C1A16))
                    .border(width = 1.dp, color = tokens.ivoryBorder, shape = RoundedCornerShape(18.dp)),
            ) {
                QrScannerWithPermissions(
                    types = listOf(CodeType.PDF417),
                    modifier = Modifier.fillMaxSize(),
                    onScanned = { code ->
                        rawPayload = code
                        true // stop scanning once we have a payload
                    },
                )
            }
        } else {
            Text(
                text = "ID barcode captured ✓",
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.ink,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Raw decoded payload (development capture):",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.sub,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = tokens.border, shape = RoundedCornerShape(14.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = rawPayload.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = tokens.monoFontFamily),
                    color = tokens.ink,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Copy payload",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.jade,
                    modifier = Modifier
                        .clickable { clipboard.setText(AnnotatedString(rawPayload.orEmpty())) }
                        .padding(vertical = 4.dp),
                )
                Text(
                    text = "Scan again",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.jade,
                    modifier = Modifier
                        .clickable { rawPayload = null }
                        .padding(vertical = 4.dp),
                )
            }
            JadeButton(label = "Continue", enabled = true, onClick = onContinue)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Step 3: Details ----------------------------- */

@Composable
private fun KycDetailsStep(
    state: SignUpState,
    onAction: (SignUpAction) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        MifosOutlinedTextField(
            value = state.firstNameInput,
            label = stringResource(Res.string.feature_auth_first_name),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            onValueChange = { onAction(SignUpAction.FirstNameInputChange(it)) },
        )
        MifosOutlinedTextField(
            value = state.lastNameInput,
            label = stringResource(Res.string.feature_auth_last_name),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            onValueChange = { onAction(SignUpAction.LastNameInputChange(it)) },
        )
        MifosOutlinedTextField(
            value = state.userNameInput,
            label = stringResource(Res.string.feature_auth_username),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            onValueChange = { onAction(SignUpAction.UserNameInputChange(it)) },
        )
        MifosOutlinedTextField(
            value = state.emailInput,
            label = stringResource(Res.string.feature_auth_email),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            onValueChange = { onAction(SignUpAction.EmailInputChange(it)) },
        )
        MifosOutlinedTextField(
            value = state.mobileNumberInput,
            label = stringResource(Res.string.feature_auth_mobile_no),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            onValueChange = { onAction(SignUpAction.MobileNumberInputChange(it)) },
        )
        Column {
            var showPassword by rememberSaveable { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            MifosPasswordField(
                value = state.passwordInput,
                label = stringResource(Res.string.feature_auth_password),
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { onAction(SignUpAction.PasswordInputChange(it)) },
                showPassword = showPassword,
                showPasswordChange = { showPassword = !showPassword },
                interactionSource = interactionSource,
            )
            Spacer(Modifier.height(4.dp))
            CombinedPasswordErrorCard(
                modifier = Modifier.fillMaxWidth(),
                errors = state.passwordFeedback,
                passwordStrengthState = state.passwordStrengthState,
                currentCharacterCount = state.passwordInput.length,
                isPasswordFieldFocused = isFocused,
            )
        }
        run {
            var showConfirm by rememberSaveable { mutableStateOf(false) }
            MifosPasswordField(
                value = state.confirmPasswordInput,
                label = stringResource(Res.string.feature_auth_confirm_password),
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { onAction(SignUpAction.ConfirmPasswordInputChange(it)) },
                showPassword = showConfirm,
                showPasswordChange = { showConfirm = !showConfirm },
            )
        }
        MifosOutlinedTextField(
            value = state.addressLine1Input,
            label = stringResource(Res.string.feature_auth_address_line_1),
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onAction(SignUpAction.AddressLine1InputChange(it)) },
        )
        MifosOutlinedTextField(
            value = state.addressLine2Input,
            label = stringResource(Res.string.feature_auth_address_line_2),
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onAction(SignUpAction.AddressLine2InputChange(it)) },
        )
        MifosOutlinedTextField(
            value = state.pinCodeInput,
            label = stringResource(Res.string.feature_auth_pin_code),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChange = { onAction(SignUpAction.PinCodeInputChange(it)) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            var countryExpanded by remember { mutableStateOf(false) }
            var stateExpanded by remember { mutableStateOf(false) }
            ExposedDropdownBox(
                expanded = countryExpanded,
                label = stringResource(Res.string.feature_auth_country),
                value = state.countryInput,
                onExpandChange = { countryExpanded = it },
                modifier = Modifier.weight(1.5f),
            ) {
                state.countriesWithStates.keys.forEach { country ->
                    DropdownBoxItem(
                        text = country,
                        onClick = {
                            onAction(SignUpAction.CountryInputChange(country))
                            countryExpanded = false
                        },
                    )
                }
            }
            if (state.statesForSelectedCountry.isNullOrEmpty()) {
                MifosOutlinedTextField(
                    value = state.stateInput,
                    label = stringResource(Res.string.feature_auth_state),
                    onValueChange = { onAction(SignUpAction.StateInputChange(it)) },
                    modifier = Modifier.weight(1.5f),
                )
            } else {
                ExposedDropdownBox(
                    expanded = stateExpanded,
                    label = stringResource(Res.string.feature_auth_state),
                    value = state.stateInput,
                    onExpandChange = { stateExpanded = it },
                    modifier = Modifier.weight(1.5f),
                ) {
                    state.statesForSelectedCountry.forEach { stateName ->
                        DropdownBoxItem(
                            text = stateName,
                            onClick = {
                                onAction(SignUpAction.StateInputChange(stateName))
                                stateExpanded = false
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        JadeButton(label = "Continue", enabled = true, onClick = onContinue)
        Spacer(Modifier.height(20.dp))
    }
}

/* ----------------------------- Step 4: Review ----------------------------- */

@Composable
private fun KycReviewStep(
    state: SignUpState,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Confirm everything matches your documents.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.sub,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(width = 1.dp, color = tokens.border, shape = RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            SummaryRow("Full name", "${state.firstNameInput} ${state.lastNameInput}".trim())
            SummaryRow("Username", state.userNameInput)
            SummaryRow("Email", state.emailInput)
            SummaryRow("Mobile number", state.mobileNumberInput, mono = true)
            SummaryRow("Address", listOfNotNull(state.addressLine1Input, state.addressLine2Input).filter { it.isNotBlank() }.joinToString(", "))
            SummaryRow("Postal code", state.pinCodeInput, mono = true)
            SummaryRow("Country / State", listOf(state.countryInput, state.stateInput).filter { it.isNotBlank() }.joinToString(" · "), last = true)
        }
        Text(
            text = "Edit details",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.jade,
            modifier = Modifier.clickable(onClick = onEdit).padding(vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        JadeButton(label = "Confirm & create account", enabled = true, onClick = onConfirm)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, mono: Boolean = false, last: Boolean = false) {
    val tokens = SimpliPayTheme.tokens
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = tokens.monoFontFamily),
            color = tokens.cardLabel,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value.ifBlank { "—" },
            style = if (mono) {
                MaterialTheme.typography.bodyLarge.copy(fontFamily = tokens.monoFontFamily)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = tokens.ink,
        )
    }
    if (!last) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.line))
    }
}

/* ----------------------------- Shared bits ----------------------------- */

@Composable
private fun JadeButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.jade)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color(0xFFFBFAF6))
    }
}

/** Headless render-harness entry points (themed by the caller). */
@Composable
fun KycVerifyPreview() = KycVerifyStep(onScan = {}, onEnter = {})

@Composable
fun KycReviewPreview() = KycReviewStep(
    state = SignUpState(
        firstNameInput = "Jonny",
        lastNameInput = "Wallet",
        userNameInput = "jonny.wallet",
        emailInput = "jonny.wallet@example.com",
        mobileNumberInput = "0820000143",
        addressLine1Input = "12 Long Street",
        pinCodeInput = "8001",
        countryInput = "South Africa",
        stateInput = "Western Cape",
    ),
    onEdit = {},
    onConfirm = {},
)

@Composable
private fun KycDialogs(
    dialogState: SignUpState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        SignUpState.DialogState.Loading -> MifosLoadingDialog(visibilityState = LoadingDialogState.Shown)
        is SignUpState.DialogState.Error.StringMessage -> MifosBasicDialog(
            visibilityState = BasicDialogState.Shown(dialogState.message),
            onDismissRequest = onDismissRequest,
        )
        is SignUpState.DialogState.Error.ResourceMessage -> MifosBasicDialog(
            visibilityState = BasicDialogState.Shown(stringResource(dialogState.message)),
            onDismissRequest = onDismissRequest,
        )
        else -> Unit
    }
}

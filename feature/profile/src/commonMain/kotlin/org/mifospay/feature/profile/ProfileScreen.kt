/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobile_wallet.feature.profile.generated.resources.Res
import mobile_wallet.feature.profile.generated.resources.feature_profile
import mobile_wallet.feature.profile.generated.resources.feature_profile_personal_qr_code
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.designsystem.component.BasicDialogState
import org.mifospay.core.designsystem.component.LoadingDialogState
import org.mifospay.core.designsystem.component.MifosBasicDialog
import org.mifospay.core.designsystem.component.MifosButton
import org.mifospay.core.designsystem.component.MifosLoadingDialog
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.model.client.Client
import org.mifospay.core.ui.ErrorScreenContent
import org.mifospay.core.ui.MifosProgressIndicatorOverlay
import org.mifospay.core.ui.utils.EventsEffect
import org.mifospay.feature.profile.components.ProfileDetailsCard
import org.mifospay.feature.profile.components.ProfileImage
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun ProfileScreen(
    navigateBack: () -> Unit,
    onEditProfile: () -> Unit,
    onLinkBackAccount: () -> Unit,
    showQrCode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val clientState by viewModel.clientState.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            ProfileEvent.OnEditProfile -> onEditProfile.invoke()
            ProfileEvent.OnLinkBankAccount -> onLinkBackAccount.invoke()
            ProfileEvent.ShowQRCode -> showQrCode.invoke()
            is ProfileEvent.OnNavigateBack -> navigateBack.invoke()
        }
    }

    ProfileDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(ProfileAction.DismissErrorDialog) }
        },
    )

    ProfileScreenContent(
        state = state,
        clientState = clientState,
        onAction = remember(viewModel) {
            { action -> viewModel.trySendAction(action) }
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
internal fun ProfileScreenContent(
    state: ProfileState,
    clientState: ProfileState.ViewState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MifosScaffold(
        modifier = modifier,
        topBarTitle = stringResource(Res.string.feature_profile),
        backPress = {
            onAction(ProfileAction.NavigateBack)
        },
        containerColor = KptTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            when (clientState) {
                is ProfileState.ViewState.Loading -> MifosProgressIndicatorOverlay()

                is ProfileState.ViewState.Error -> {
                    ErrorScreenContent(
                        onClickRetry = { },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ProfileState.ViewState.Success -> {
                    ProfileScreenContent(
                        state = clientState,
                        clientImage = state.clientImage,
                        onAction = onAction,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileScreenContent(
    state: ProfileState.ViewState.Success,
    clientImage: String?,
    modifier: Modifier = Modifier,
    onAction: (ProfileAction) -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KptTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        ProfileImage(bitmap = clientImage)

        Text(
            text = state.client.displayName,
            style = KptTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = tokens.ink,
        )

        // Gold brand badge — shown for active wallet members (KYC tiers deferred,
        // so this maps to the client's active status rather than a KYC level).
        if (state.client.active) {
            VerifiedBadge()
        }

        ProfileDetailsCard(
            client = state.client,
            modifier = Modifier.fillMaxWidth(),
        )

        MifosButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            text = {
                Text(
                    text = stringResource(Res.string.feature_profile_personal_qr_code),
                )
            },
            onClick = {
                onAction(ProfileAction.ShowPersonalQRCode)
            },
            leadingIcon = {
                Icon(
                    imageVector = MifosIcons.QrCode,
                    contentDescription = stringResource(Res.string.feature_profile_personal_qr_code),
                )
            },
        )

//        TODO uncomment this after migrating to self api to link savings account
//        MifosButton(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(55.dp),
//            text = {
//                Text(
//                    text = stringResource(Res.string.feature_profile_link_bank_account),
//                )
//            },
//            onClick = {
//                onAction(ProfileAction.NavigateToLinkBankAccount)
//            },
//            leadingIcon = {
//                Icon(imageVector = MifosIcons.AttachMoney, contentDescription = "")
//            },
//        )

        Spacer(modifier = Modifier.height(1.dp))
    }
}

/** Headless render-harness entry point (themed, side-effect free). */
@Composable
fun ProfileRenderPreview() {
    val client = Client(
        id = 1L,
        accountNo = "100045567",
        externalId = "jonny@simplipay",
        active = true,
        activationDate = emptyList(),
        firstname = "Jonny",
        lastname = "Wallet",
        displayName = "Jonny Wallet",
        mobileNo = "+27 82 555 0143",
        emailAddress = "jonny.wallet@example.com",
        dateOfBirth = emptyList(),
        isStaff = false,
        officeId = 1L,
        officeName = "Head Office",
        savingsProductName = "Everyday Savings",
    )
    MifosTheme(darkTheme = false) {
        ProfileScreenContent(
            state = ProfileState(clientId = 1L),
            clientState = ProfileState.ViewState.Success(client),
            onAction = {},
        )
    }
}

@Composable
private fun VerifiedBadge(
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tokens.verifiedGold.copy(alpha = 0.18f))
            .border(
                width = 1.dp,
                color = tokens.verifiedGold,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = MifosIcons.Check,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tokens.verifiedGold,
        )
        Text(
            text = "VERIFIED",
            style = KptTheme.typography.labelSmall.copy(
                fontFamily = tokens.monoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = tokens.verifiedGold,
        )
    }
}

@Composable
private fun ProfileDialogs(
    dialogState: ProfileState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is ProfileState.DialogState.Error -> MifosBasicDialog(
            visibilityState = BasicDialogState.Shown(
                message = stringResource(dialogState.message),
            ),
            onDismissRequest = onDismissRequest,
        )

        is ProfileState.DialogState.Loading -> MifosLoadingDialog(
            visibilityState = LoadingDialogState.Shown,
        )

        null -> Unit
    }
}

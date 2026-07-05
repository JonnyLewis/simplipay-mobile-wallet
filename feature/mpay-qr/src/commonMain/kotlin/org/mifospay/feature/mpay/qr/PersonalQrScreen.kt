/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.mpay.qr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobile_wallet.feature.mpay_qr.generated.resources.Res
import mobile_wallet.feature.mpay_qr.generated.resources.feature_mpay_qr_personal_qr
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.ui.MifosProgressIndicator
import org.mifospay.feature.mpay.qr.components.QrCodeCard
import org.mifospay.feature.mpay.qr.components.QrType
import template.core.base.designsystem.theme.KptTheme

/**
 * The profile's "Personal QR": just the user's wallet QR — no account picker,
 * amount, share row or ID section (that's the Receive flow's [MpayQrScreen]).
 */
@Composable
fun PersonalQrScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MpayQrViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    MifosScaffold(
        modifier = modifier,
        topBarTitle = stringResource(Res.string.feature_mpay_qr_personal_qr),
        backPress = navigateBack,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(KptTheme.spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            when (val viewState = state.viewState) {
                is MpayQrState.ViewState.Loading -> MifosProgressIndicator()

                is MpayQrState.ViewState.Error -> Text(
                    text = viewState.message,
                    style = KptTheme.typography.bodyMedium,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                is MpayQrState.ViewState.Content -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
                ) {
                    QrCodeCard(
                        data = viewState.intraBankData,
                        options = viewState.options,
                        qrType = QrType.INTRA_BANK,
                    )
                    Text(
                        text = state.client.displayName,
                        style = KptTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "••${
                            (state.selectedAccount?.number ?: state.defaultAccount.accountNo).takeLast(4)
                        }",
                        style = KptTheme.typography.bodySmall,
                        color = KptTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

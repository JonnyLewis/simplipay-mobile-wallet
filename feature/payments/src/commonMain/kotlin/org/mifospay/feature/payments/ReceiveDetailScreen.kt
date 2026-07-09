/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.icon.MifosIcons
import template.core.base.designsystem.theme.KptTheme

/**
 * Detail screen for a single receive method (reached from the Receive list). Shows the method's
 * metadata as label/value rows and a single "copy all" action in the top-right of the app bar,
 * so the user isn't shown every method's details at once.
 */
@Composable
fun ReceiveDetailScreen(
    method: ReceiveMethod,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = koinViewModel(),
) {
    val clipboard = LocalClipboardManager.current
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val title: String
    val rows: List<Pair<String, String>>
    val copyAll: String
    when (method) {
        ReceiveMethod.MOBILE -> {
            title = "Mobile number"
            rows = listOf("Mobile number" to state.mobileNo)
            copyAll = state.mobileNo
        }
        ReceiveMethod.EFT -> {
            title = "Bank transfer (EFT)"
            rows = listOf(
                "Account name" to state.accountName,
                "Bank" to state.bankName,
                "Account number" to state.accountNumber,
                "Branch code" to state.branchCode,
            )
            copyAll = state.eftDetailsText
        }
    }

    MifosScaffold(
        modifier = modifier.fillMaxSize(),
        backPress = onBack,
        topBarTitle = title,
        containerColor = KptTheme.colorScheme.background,
        actions = {
            IconButton(onClick = { clipboard.setText(AnnotatedString(copyAll)) }) {
                Icon(
                    imageVector = MifosIcons.Copy,
                    contentDescription = "Copy details",
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.lg),
        ) {
            rows.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs)) {
                    Text(
                        text = label,
                        style = KptTheme.typography.bodySmall,
                        color = KptTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = KptTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

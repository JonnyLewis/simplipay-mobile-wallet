/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.ui.MifosScrollableTabRow
import org.mifospay.core.ui.utility.TabContent

@Composable
fun PaymentsRoute(
    tabContents: List<TabContent>,
    onPayLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaymentScreenContent(
        tabContents = tabContents,
        onPayLinkClick = onPayLinkClick,
        modifier = modifier,
    )
}

@Composable
private fun PaymentScreenContent(
    tabContents: List<TabContent>,
    onPayLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { tabContents.size })

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(
        pagerState.currentPage,
    ) {
        keyboardController?.hide()
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        PayLinksEntry(onClick = onPayLinkClick)
        MifosScrollableTabRow(
            tabContents = tabContents,
            pagerState = pagerState,
        )
    }
}

@Composable
private fun PayLinksEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.jadeTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MifosIcons.PayLink,
                contentDescription = null,
                tint = tokens.jade,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pay Links",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = tokens.ink,
            )
            Text(
                text = "Create a shareable link or QR to get paid",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.sub,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = MifosIcons.ChevronRight,
            contentDescription = null,
            tint = tokens.sub,
            modifier = Modifier.size(20.dp),
        )
    }
}

// TODO History, SI, Invoices are not using self api
//  https://venus.mifos.community/fineract-provider/api/v1/standinginstructions?clientId=2
//  https://venus.mifos.community/fineract-provider/api/v1/datatables/invoice/2
//
enum class PaymentsScreenContents {
    SEND,
    REQUEST,
    HISTORY,
    SI,
    INVOICES,
    AUTOPAY,
}

@Preview
@Composable
private fun PaymentsScreenPreview() {
    PaymentScreenContent(
        tabContents = emptyList(),
        onPayLinkClick = {},
    )
}

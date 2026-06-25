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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.common.CurrencyFormatter
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.component.Rosette
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.model.savingsaccount.TransactionType

/** Tile icon-chip background and glyph colour, taken from the design tokens. */
private val TileChip = Color(0xFFEFEDE6)
private val TileGlyph = Color(0xFF3F4B57)

/** Shared height so the "Pay from" balance card lines up with the VAS service tiles. */
private val VasTileHeight = 94.dp

/** A single row in the Buy hub's "Recent" list (decoupled from the data model). */
internal data class BuyRecentItem(
    val title: String,
    val date: String,
    val amountText: String,
    val credit: Boolean,
    val icon: ImageVector,
)

/**
 * Picks a VAS service glyph for a recent-activity row from its description, so the list
 * reads as service purchases (airtime/electricity/data…) like the design — falling back
 * to a receipt glyph when the description doesn't name a known service.
 */
private fun iconForDescription(description: String): ImageVector {
    val d = description.lowercase()
    return when {
        "airtime" in d -> MifosIcons.Airtime
        "data" in d -> MifosIcons.Data
        "electric" in d || "power" in d -> MifosIcons.Electricity
        "water" in d -> MifosIcons.Water
        "dstv" in d || "tv" in d -> MifosIcons.TvCable
        "voucher" in d -> MifosIcons.GiftCard
        "bet" in d -> MifosIcons.Betting
        else -> MifosIcons.Receipt
    }
}

/**
 * Buy / Value-Added Services hub (design frame 14): a "pay from" balance card, a
 * 4-column grid of top-up and bill-payment services and a list of recent
 * activity. Tapping a tile opens [BuyServiceScreen] for that service.
 */
@Composable
internal fun BuyScreen(
    onBack: () -> Unit,
    onServiceClick: (VasService) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BuyViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val account = state.account

    val recent = state.transactions.take(3).map { txn ->
        val credit = txn.transactionType == TransactionType.CREDIT
        val sign = if (credit) "+" else "−"
        val symbol = txn.currency.displaySymbol
        BuyRecentItem(
            title = txn.description.ifBlank { if (credit) "Money in" else "Money out" },
            date = txn.date,
            amountText = "$sign$symbol ${CurrencyFormatter.format(txn.amount, 2)}",
            credit = credit,
            icon = iconForDescription(txn.description),
        )
    }

    BuyScreenContent(
        accountName = account?.name,
        accountMask = account?.number?.let { "•• ${it.takeLast(4)}" },
        balanceText = account?.let { "${it.currency.displaySymbol} ${CurrencyFormatter.format(it.balance, 2)}" },
        recent = recent,
        onBack = onBack,
        onServiceClick = onServiceClick,
        modifier = modifier,
    )
}

@Composable
internal fun BuyScreenContent(
    accountName: String?,
    accountMask: String?,
    balanceText: String?,
    recent: List<BuyRecentItem>,
    onBack: () -> Unit,
    onServiceClick: (VasService) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    MifosScaffold(
        modifier = modifier.fillMaxSize(),
        backPress = onBack,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Header — back affordance + big title + subtitle.
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, tokens.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MifosIcons.ArrowBack2,
                    contentDescription = "Back",
                    tint = tokens.sub,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Buy",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = tokens.ink,
            )
            Text(
                text = "Airtime, data, electricity & bills",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.sub,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(16.dp))

            if (balanceText != null) {
                PayFromCard(
                    accountLabel = listOfNotNull(accountName, accountMask).joinToString(" · "),
                    balanceText = balanceText,
                )
                Spacer(Modifier.height(18.dp))
            }

            ServiceGrid(onServiceClick = onServiceClick)

            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = tokens.ink,
                )
                Spacer(Modifier.height(4.dp))
                recent.forEachIndexed { index, item ->
                    RecentRow(item = item, showDivider = index != recent.lastIndex)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PayFromCard(
    accountLabel: String,
    balanceText: String,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(VasTileHeight)
            .clip(shape)
            .background(brush = tokens.ivoryGradient)
            .border(1.dp, tokens.ivoryBorder, shape),
    ) {
        // Overlay rosette — matchParentSize keeps it from driving the card height;
        // the parent clip() trims it to the top-right corner.
        Box(modifier = Modifier.matchParentSize()) {
            Rosette(
                radiusX = 60.dp,
                radiusY = 20.dp,
                count = 34,
                opacity = 0.26f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 46.dp, y = (-40).dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Pay from",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.cardLabel,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = accountLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = tokens.cardInk,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = balanceText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = tokens.monoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tokens.jade,
            )
        }
    }
}

@Composable
private fun ServiceGrid(
    onServiceClick: (VasService) -> Unit,
    modifier: Modifier = Modifier,
) {
    val services = VasService.entries
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        services.chunked(4).forEach { rowServices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowServices.forEach { service ->
                    ServiceTile(
                        service = service,
                        onClick = { onServiceClick(service) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceTile(
    service: VasService,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .height(VasTileHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = tokens.border, shape = shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TileChip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = service.icon,
                contentDescription = null,
                tint = TileGlyph,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = service.tileLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            fontWeight = FontWeight.SemiBold,
            color = tokens.ink,
        )
    }
}

@Composable
private fun RecentRow(
    item: BuyRecentItem,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TileChip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = TileGlyph,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = tokens.ink,
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.sub,
                )
            }
            Text(
                text = item.amountText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = tokens.monoFontFamily),
                color = if (item.credit) tokens.credit else tokens.debit,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.line),
            )
        }
    }
}

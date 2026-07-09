/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.ui.paylink

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.common.MoneyFormat
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.shared.paylink.PayLinkDto

/** Lifecycle status of a pay link, mirroring Stripe Payment Links' status badges. */
private enum class PayLinkStatus { AVAILABLE, PAID, EXPIRED, CANCELLED }

/** A single pay link as shown in the list. */
private data class PayLink(
    val id: String,
    val description: String,
    val amountText: String,
    val shortUrl: String,
    val url: String,
    val status: PayLinkStatus,
)

/** Maps a SimpliLink DTO to the row model (ACTIVE reads as the Available badge). */
private fun PayLinkDto.toRow(): PayLink = PayLink(
    id = slug,
    description = description,
    amountText = amountMinor?.let { MoneyFormat.zar(it / 100.0) } ?: "Any amount",
    shortUrl = url.removePrefix("https://").removePrefix("http://"),
    url = url,
    status = when (status) {
        "PAID" -> PayLinkStatus.PAID
        "EXPIRED" -> PayLinkStatus.EXPIRED
        "CANCELLED" -> PayLinkStatus.CANCELLED
        else -> PayLinkStatus.AVAILABLE
    },
)

/**
 * Pay Links hub, modeled on Stripe Payment Links: the owner's links from the
 * SimpliLink service, each with description, amount, short URL and a status
 * badge, plus a "New pay link" panel. Generate posts to the service; the
 * returned canonical URL is shown alongside a QR with copy / share actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayLinkScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayLinkViewModel = koinViewModel(),
) {
    val tokens = SimpliPayTheme.tokens
    val clipboard = LocalClipboardManager.current
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    // Create-panel local state (field contents); the generated URL comes from the service.
    var showCreate by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val generatedUrl = state.generatedUrl

    MifosScaffold(
        modifier = modifier.fillMaxSize(),
        backPress = onBackClick,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { viewModel.trySendAction(PayLinkAction.Refresh) },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                // Header — back affordance + title + subtitle (BuyScreen pattern).
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, tokens.border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onBackClick),
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
                    text = "Pay Links",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = tokens.ink,
                )
                Text(
                    text = "Share a link or QR to get paid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.sub,
                    modifier = Modifier.padding(top = 2.dp),
                )

                Spacer(Modifier.height(16.dp))

                // Primary "New pay link" action — toggles the in-screen create panel.
                NewPayLinkButton(
                    onClick = {
                        showCreate = !showCreate
                        if (!showCreate) {
                            description = ""
                            amount = ""
                            viewModel.trySendAction(PayLinkAction.ClearGenerated)
                        }
                    },
                )

                if (showCreate) {
                    Spacer(Modifier.height(14.dp))
                    CreatePayLinkPanel(
                        description = description,
                        onDescriptionChange = {
                            description = it
                            // Editing invalidates a previously generated link.
                            viewModel.trySendAction(PayLinkAction.ClearGenerated)
                        },
                        amount = amount,
                        onAmountChange = {
                            amount = it
                            viewModel.trySendAction(PayLinkAction.ClearGenerated)
                        },
                        generatedUrl = generatedUrl,
                        onGenerate = {
                            viewModel.trySendAction(PayLinkAction.Create(description, amount))
                        },
                        onCopy = { url ->
                            clipboard.setText(AnnotatedString(url))
                            // TODO: surface a confirmation toast once a host hook exists.
                        },
                        onShareUrl = { _ ->
                            // TODO: wire to template.core.base.ui.ShareUtils.shareText(url)
                            //  once the backend pay-link service is available.
                        },
                        onShareBarcode = { _ ->
                            // TODO: render the QR painter to an ImageBitmap and call
                            //  ShareUtils.shareImage(...) once the service is available.
                        },
                    )
                }

                state.error?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB42318),
                    )
                    Spacer(Modifier.height(10.dp))
                    RetryButton(onClick = { viewModel.trySendAction(PayLinkAction.Refresh) })
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "Your links",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = tokens.ink,
                )
                Spacer(Modifier.height(10.dp))

                when {
                    state.loading -> Text(
                        text = "Loading your links…",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.sub,
                    )
                    state.links.isEmpty() -> Text(
                        text = "No pay links yet — create one above to get paid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.sub,
                    )
                    else -> state.links.map { it.toRow() }.forEach { link ->
                        PayLinkRow(
                            link = link,
                            onCopy = { clipboard.setText(AnnotatedString(link.url)) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RetryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.jade, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Retry",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = tokens.jade,
        )
    }
}

@Composable
private fun NewPayLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.jade)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MifosIcons.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "New pay link",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

@Composable
private fun CreatePayLinkPanel(
    description: String,
    onDescriptionChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    generatedUrl: String?,
    onGenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onShareUrl: (String) -> Unit,
    onShareBarcode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.border, shape)
            .padding(16.dp),
    ) {
        Text(
            text = "Create a pay link",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = tokens.ink,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            placeholder = { Text("What is this payment for?") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = payLinkFieldColors(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Amount (optional)") },
            placeholder = { Text("R 0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            colors = payLinkFieldColors(),
        )

        Spacer(Modifier.height(14.dp))

        val canGenerate = description.isNotBlank()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (canGenerate) tokens.jade else tokens.line)
                .clickable(enabled = canGenerate, onClick = onGenerate)
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Generate pay link",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (canGenerate) Color.White else tokens.sub,
            )
        }

        if (generatedUrl != null) {
            Spacer(Modifier.height(16.dp))
            GeneratedLinkPanel(
                url = generatedUrl,
                onCopy = onCopy,
                onShareUrl = onShareUrl,
                onShareBarcode = onShareBarcode,
            )
        }
    }
}

@Composable
private fun GeneratedLinkPanel(
    url: String,
    onCopy: (String) -> Unit,
    onShareUrl: (String) -> Unit,
    onShareBarcode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.jadeTint)
            .border(1.dp, tokens.jade, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Pay link ready",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = tokens.jade,
        )
        Spacer(Modifier.height(12.dp))

        // QR rendering the generated URL — qrose, mirroring feature/mpay-qr.
        val painter = rememberQrCodePainter(data = url)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painter,
                contentDescription = "Pay link QR code",
                modifier = Modifier.size(180.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = tokens.monoFontFamily),
            color = tokens.ink,
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GeneratedAction(
                icon = MifosIcons.Copy,
                label = "Copy link",
                onClick = { onCopy(url) },
                modifier = Modifier.weight(1f),
            )
            GeneratedAction(
                icon = MifosIcons.Share,
                label = "Share URL",
                onClick = { onShareUrl(url) },
                modifier = Modifier.weight(1f),
            )
            GeneratedAction(
                icon = MifosIcons.QrCode2,
                label = "Barcode",
                onClick = { onShareBarcode(url) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GeneratedAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.jade,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.SemiBold,
            color = tokens.ink,
        )
    }
}

@Composable
private fun PayLinkRow(
    link: PayLink,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.border, shape)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = link.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = tokens.ink,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.size(8.dp))
                StatusChip(status = link.status)
            }
            Text(
                text = link.shortUrl,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = tokens.monoFontFamily),
                color = tokens.sub,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = link.amountText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = tokens.monoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tokens.ink,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.line)
                .clickable(onClick = onCopy),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MifosIcons.Copy,
                contentDescription = "Copy link",
                tint = tokens.sub,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun StatusChip(
    status: PayLinkStatus,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    // Status → colour: Available = jade, Paid = teal/blue, Expired = grey, Cancelled = red.
    val (label, fg, bg) = when (status) {
        PayLinkStatus.AVAILABLE -> Triple("Available", tokens.jade, tokens.jadeTint)
        PayLinkStatus.PAID -> Triple("Paid", Color(0xFF1469A6), Color(0x1A1469A6))
        PayLinkStatus.EXPIRED -> Triple("Expired", tokens.sub, tokens.line)
        PayLinkStatus.CANCELLED -> Triple("Cancelled", Color(0xFFB42318), Color(0x1AB42318))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@Composable
private fun payLinkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SimpliPayTheme.tokens.jade,
    unfocusedBorderColor = SimpliPayTheme.tokens.border,
    focusedLabelColor = SimpliPayTheme.tokens.jade,
    unfocusedLabelColor = SimpliPayTheme.tokens.sub,
    cursorColor = SimpliPayTheme.tokens.jade,
)

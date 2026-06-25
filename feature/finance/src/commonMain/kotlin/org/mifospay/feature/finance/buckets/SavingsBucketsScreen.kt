/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.finance.buckets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import template.core.base.designsystem.theme.KptTheme

/* -------------------------------------------------------------------------- *
 *  Domain model (local / mock only — no ViewModel, no backend)
 * -------------------------------------------------------------------------- */

/** How a bucket releases / restricts the money it holds. */
private enum class LockType { Flexible, Locked }

/** Cadence for the optional recurring auto-contribution. */
private enum class Frequency(val label: String) { Weekly("Weekly"), Monthly("Monthly") }

/**
 * A visual theme for a bucket: a small jade-friendly accent colour paired with an
 * emoji glyph. Kept as a tiny curated palette so cards stay on-brand.
 */
private data class BucketTheme(val emoji: String, val accent: Color)

// Curated, on-brand palette: jade (safety), sky (holiday), berry (December),
// gold (home), slate (car), violet (education), rose (wedding), green (grow).
private val BucketThemes = listOf(
    BucketTheme("🛟", Color(0xFF0F8A7B)),
    BucketTheme("🏖️", Color(0xFF2B9CD8)),
    BucketTheme("🎄", Color(0xFFC0504D)),
    BucketTheme("🏠", Color(0xFFC8B27A)),
    BucketTheme("🚗", Color(0xFF4A5560)),
    BucketTheme("🎓", Color(0xFF7E57C2)),
    BucketTheme("💍", Color(0xFFD98AAE)),
    BucketTheme("🌱", Color(0xFF0E9466)),
)

/**
 * A single savings goal. All amounts are in ZAR (R). [limit] is interpreted as a
 * **max-balance cap** — the bucket stops accepting contributions once it is reached
 * (0.0 == no cap). [recurringAmount] of 0.0 means auto-contribution is off.
 */
private data class Bucket(
    val id: Long,
    val name: String,
    val theme: BucketTheme,
    val saved: Double,
    val target: Double,
    val payoutDate: String,
    val recurringAmount: Double,
    val frequency: Frequency,
    val maxBalanceCap: Double,
    val lockType: LockType,
    val roundUps: Boolean,
)

/* -------------------------------------------------------------------------- *
 *  Money / formatting helpers (simple, local)
 * -------------------------------------------------------------------------- */

/** Formats a Rand amount as `R 1,234.56` with thousands separators. */
private fun rand(amount: Double): String {
    val rounded = ((amount * 100).toLong())
    val whole = rounded / 100
    val cents = (rounded % 100).toInt()
    val digits = whole.toString()
    val sb = StringBuilder()
    val first = digits.length % 3
    for (i in digits.indices) {
        if (i != 0 && (i - first) % 3 == 0) sb.append(',')
        sb.append(digits[i])
    }
    val centsText = if (cents < 10) "0$cents" else "$cents"
    return "R $sb.$centsText"
}

/** Whole-rand variant for compact captions, e.g. `R 12,000`. */
private fun randWhole(amount: Double): String = rand(amount).substringBefore('.')

/* -------------------------------------------------------------------------- *
 *  Seed data
 * -------------------------------------------------------------------------- */

private fun seedBuckets(): List<Bucket> = listOf(
    Bucket(
        id = 1,
        name = "Emergency fund",
        theme = BucketThemes[0],
        saved = 8400.0,
        target = 20000.0,
        payoutDate = "Open-ended",
        recurringAmount = 750.0,
        frequency = Frequency.Monthly,
        maxBalanceCap = 0.0,
        lockType = LockType.Flexible,
        roundUps = true,
    ),
    Bucket(
        id = 2,
        name = "December holiday",
        theme = BucketThemes[1],
        saved = 5200.0,
        target = 12000.0,
        payoutDate = "01 Dec 2026",
        recurringAmount = 500.0,
        frequency = Frequency.Monthly,
        maxBalanceCap = 12000.0,
        lockType = LockType.Locked,
        roundUps = false,
    ),
    Bucket(
        id = 3,
        name = "New laptop",
        theme = BucketThemes[7],
        saved = 18900.0,
        target = 22000.0,
        payoutDate = "15 Sep 2026",
        recurringAmount = 300.0,
        frequency = Frequency.Weekly,
        maxBalanceCap = 0.0,
        lockType = LockType.Flexible,
        roundUps = true,
    ),
    Bucket(
        id = 4,
        name = "Wedding",
        theme = BucketThemes[6],
        saved = 31000.0,
        target = 80000.0,
        payoutDate = "20 Mar 2027",
        recurringAmount = 0.0,
        frequency = Frequency.Monthly,
        maxBalanceCap = 0.0,
        lockType = LockType.Locked,
        roundUps = false,
    ),
)

/* -------------------------------------------------------------------------- *
 *  Public entry point — mounted as a Finance tab by cmp-shared.
 * -------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsBucketsScreen(modifier: Modifier = Modifier) {
    val tokens = SimpliPayTheme.tokens

    val buckets = remember { mutableStateListOf<Bucket>().apply { addAll(seedBuckets()) } }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalSaved = buckets.sumOf { it.saved }
    val totalTarget = buckets.sumOf { it.target }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = KptTheme.spacing.md,
            end = KptTheme.spacing.md,
            top = KptTheme.spacing.md,
            bottom = KptTheme.spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        item {
            BucketsHeader(
                totalSaved = totalSaved,
                totalTarget = totalTarget,
                bucketCount = buckets.size,
                onCreate = { showSheet = true },
            )
        }

        items(buckets, key = { it.id }) { bucket ->
            BucketCard(
                bucket = bucket,
                onAddMoney = { /* stub — backend wired later */ },
                onWithdraw = { /* stub — backend wired later */ },
            )
        }

        item {
            Text(
                text = "Buckets are sub-savings goals. Money set aside here keeps " +
                    "growing toward each target — locked buckets stay untouched " +
                    "until they mature.",
                style = KptTheme.typography.bodySmall,
                color = tokens.sub,
                modifier = Modifier.padding(
                    horizontal = KptTheme.spacing.xs,
                    vertical = KptTheme.spacing.sm,
                ),
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = KptTheme.colorScheme.surface,
            dragHandle = null,
        ) {
            CreateBucketForm(
                onDismiss = { showSheet = false },
                onCreate = { newBucket ->
                    buckets.add(0, newBucket)
                    showSheet = false
                },
                nextId = (buckets.maxOfOrNull { it.id } ?: 0L) + 1,
            )
        }
    }
}

/* -------------------------------------------------------------------------- *
 *  Header + summary + create button
 * -------------------------------------------------------------------------- */

@Composable
private fun BucketsHeader(
    totalSaved: Double,
    totalTarget: Double,
    bucketCount: Int,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val cardShape = RoundedCornerShape(20.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(brush = tokens.ivoryGradient)
                .border(1.dp, tokens.ivoryBorder, cardShape)
                .padding(KptTheme.spacing.lg),
        ) {
            Column {
                Text(
                    text = "TOTAL IN BUCKETS",
                    style = KptTheme.typography.labelSmall.copy(
                        fontFamily = tokens.monoFontFamily,
                        letterSpacing = 2.sp,
                    ),
                    color = tokens.cardLabel,
                )
                Spacer(Modifier.height(KptTheme.spacing.xs))
                Text(
                    text = rand(totalSaved),
                    style = KptTheme.typography.headlineMedium.copy(
                        fontFamily = tokens.monoFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = tokens.cardInk,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "across $bucketCount goals · ${randWhole(totalTarget)} target",
                    style = KptTheme.typography.bodySmall.copy(
                        fontFamily = tokens.monoFontFamily,
                    ),
                    color = tokens.cardLabel,
                )
            }
        }

        Spacer(Modifier.height(KptTheme.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Your buckets",
                style = KptTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = tokens.ink,
            )
            CreateBucketButton(onClick = onCreate)
        }
    }
}

@Composable
private fun CreateBucketButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tokens.jade)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "+",
            style = KptTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFFBFAF6),
        )
        Text(
            text = "New bucket",
            style = KptTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFFBFAF6),
        )
    }
}

/* -------------------------------------------------------------------------- *
 *  Bucket card
 * -------------------------------------------------------------------------- */

@Composable
private fun BucketCard(
    bucket: Bucket,
    onAddMoney: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val cardShape = RoundedCornerShape(18.dp)
    val progress = if (bucket.target > 0) (bucket.saved / bucket.target).coerceIn(0.0, 1.0) else 0.0
    val percent = (progress * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(KptTheme.colorScheme.surface)
            .border(1.dp, tokens.border, cardShape)
            .padding(KptTheme.spacing.md),
    ) {
        // Title row: emoji badge + name + lock badge.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bucket.theme.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = bucket.theme.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(KptTheme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bucket.name,
                    style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = bucket.payoutDate,
                    style = KptTheme.typography.bodySmall.copy(fontFamily = tokens.monoFontFamily),
                    color = tokens.sub,
                )
            }
            LockBadge(lockType = bucket.lockType)
        }

        Spacer(Modifier.height(KptTheme.spacing.md))

        // Progress bar.
        ProgressBar(progress = progress.toFloat(), accent = bucket.theme.accent)

        Spacer(Modifier.height(KptTheme.spacing.sm))

        // Saved / target + percent.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = rand(bucket.saved),
                    style = KptTheme.typography.bodyMedium.copy(
                        fontFamily = tokens.monoFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = tokens.ink,
                )
                Text(
                    text = " / ${rand(bucket.target)}",
                    style = KptTheme.typography.bodySmall.copy(fontFamily = tokens.monoFontFamily),
                    color = tokens.sub,
                )
            }
            Text(
                text = "$percent%",
                style = KptTheme.typography.labelMedium.copy(
                    fontFamily = tokens.monoFontFamily,
                    fontWeight = FontWeight.Bold,
                ),
                color = bucket.theme.accent,
            )
        }

        // Optional captions: recurring + round-ups.
        val captions = buildList {
            if (bucket.recurringAmount > 0) {
                add("Auto ${randWhole(bucket.recurringAmount)}/${bucket.frequency.label.lowercase()}")
            }
            if (bucket.maxBalanceCap > 0) add("Cap ${randWhole(bucket.maxBalanceCap)}")
            if (bucket.roundUps) add("Round-ups on")
        }
        if (captions.isNotEmpty()) {
            Spacer(Modifier.height(KptTheme.spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                captions.forEach { MetaChip(text = it) }
            }
        }

        Spacer(Modifier.height(KptTheme.spacing.md))

        // Quick actions.
        Row(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
            ActionPill(
                text = "Add money",
                filled = true,
                onClick = onAddMoney,
                modifier = Modifier.weight(1f),
            )
            ActionPill(
                text = if (bucket.lockType == LockType.Locked) "Locked" else "Withdraw",
                filled = false,
                enabled = bucket.lockType != LockType.Locked,
                onClick = onWithdraw,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(tokens.line),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(accent),
        )
    }
}

@Composable
private fun LockBadge(
    lockType: LockType,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val locked = lockType == LockType.Locked
    val bg = if (locked) tokens.jadeTint else tokens.line
    val fg = if (locked) tokens.jade else tokens.sub
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (locked) "🔒 Locked" else "Flexible",
            style = KptTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}

@Composable
private fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(KptTheme.colorScheme.surface)
            .border(1.dp, tokens.border, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = KptTheme.typography.labelSmall.copy(fontFamily = tokens.monoFontFamily),
            color = tokens.sub,
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(50)
    val base = if (filled) {
        Modifier.background(if (enabled) tokens.jade else tokens.faint)
    } else {
        Modifier
            .background(KptTheme.colorScheme.surface)
            .border(1.dp, tokens.border, shape)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .then(base)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = KptTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = when {
                filled -> Color(0xFFFBFAF6)
                !enabled -> tokens.faint
                else -> tokens.ink
            },
        )
    }
}

/* -------------------------------------------------------------------------- *
 *  Create flow (ModalBottomSheet content)
 * -------------------------------------------------------------------------- */

@Composable
private fun CreateBucketForm(
    onDismiss: () -> Unit,
    onCreate: (Bucket) -> Unit,
    nextId: Long,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens

    var name by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("") }
    var openingText by remember { mutableStateOf("") }
    var recurringText by remember { mutableStateOf("") }
    var capText by remember { mutableStateOf("") }
    var themeIndex by remember { mutableStateOf(0) }
    var frequency by remember { mutableStateOf(Frequency.Monthly) }
    var durationIndex by remember { mutableStateOf(1) } // default 6 months
    var lockType by remember { mutableStateOf(LockType.Flexible) }
    var roundUps by remember { mutableStateOf(false) }

    val durations = listOf("3 months", "6 months", "12 months", "Open-ended")

    val goal = goalText.toDoubleOrNull() ?: 0.0
    val isValid = name.isNotBlank() && goal > 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(
                start = KptTheme.spacing.md,
                end = KptTheme.spacing.md,
                top = KptTheme.spacing.md,
                bottom = KptTheme.spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        // Grab handle + title.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(tokens.border),
        )
        Text(
            text = "New bucket",
            style = KptTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = tokens.ink,
        )

        // Name.
        FieldLabel("Name")
        BucketTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "e.g. Emergency fund",
        )

        // Emoji / colour theme.
        FieldLabel("Theme")
        ThemePicker(
            selectedIndex = themeIndex,
            onSelect = { themeIndex = it },
        )

        // Goal + opening deposit.
        FieldLabel("Goal amount (R)")
        BucketTextField(
            value = goalText,
            onValueChange = { goalText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "0.00",
            numeric = true,
        )

        FieldLabel("Opening deposit (R) · optional")
        BucketTextField(
            value = openingText,
            onValueChange = { openingText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "0.00",
            numeric = true,
        )

        // Payout duration.
        FieldLabel("Pays out in")
        ChipRow(
            options = durations,
            selectedIndex = durationIndex,
            onSelect = { durationIndex = it },
        )

        // Recurring contribution.
        FieldLabel("Auto-contribution (R) · optional")
        BucketTextField(
            value = recurringText,
            onValueChange = { recurringText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "0.00",
            numeric = true,
        )
        SegmentedSelector(
            options = Frequency.entries.map { it.label },
            selectedIndex = Frequency.entries.indexOf(frequency),
            onSelect = { frequency = Frequency.entries[it] },
        )

        // Max-balance cap (the "limit").
        FieldLabel("Max balance cap (R) · optional")
        BucketTextField(
            value = capText,
            onValueChange = { capText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "No cap",
            numeric = true,
        )

        // Lock type.
        FieldLabel("Access")
        SegmentedSelector(
            options = listOf("Flexible", "Locked"),
            selectedIndex = if (lockType == LockType.Flexible) 0 else 1,
            onSelect = { lockType = if (it == 0) LockType.Flexible else LockType.Locked },
        )
        Text(
            text = if (lockType == LockType.Locked) {
                "Locked: no withdrawals until the goal or payout date is reached."
            } else {
                "Flexible: withdraw any time."
            },
            style = KptTheme.typography.bodySmall,
            color = tokens.sub,
        )

        // Round-ups toggle.
        ToggleRow(
            title = "Round-ups into this bucket",
            subtitle = "Spare change from card spending is swept in.",
            checked = roundUps,
            onCheckedChange = { roundUps = it },
        )

        Spacer(Modifier.height(KptTheme.spacing.xs))

        // Actions.
        Row(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
            ActionPill(
                text = "Cancel",
                filled = false,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            ActionPill(
                text = "Create bucket",
                filled = true,
                enabled = isValid,
                onClick = {
                    val opening = openingText.toDoubleOrNull() ?: 0.0
                    onCreate(
                        Bucket(
                            id = nextId,
                            name = name.trim(),
                            theme = BucketThemes[themeIndex],
                            saved = opening,
                            target = goal,
                            payoutDate = durations[durationIndex],
                            recurringAmount = recurringText.toDoubleOrNull() ?: 0.0,
                            frequency = frequency,
                            maxBalanceCap = capText.toDoubleOrNull() ?: 0.0,
                            lockType = lockType,
                            roundUps = roundUps,
                        ),
                    )
                },
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    val tokens = SimpliPayTheme.tokens
    Text(
        text = text.uppercase(),
        style = KptTheme.typography.labelSmall.copy(
            fontFamily = tokens.monoFontFamily,
            letterSpacing = 1.5.sp,
        ),
        color = tokens.sub,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BucketTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
) {
    val tokens = SimpliPayTheme.tokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                style = KptTheme.typography.bodyMedium,
                color = tokens.faint,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.jade,
            unfocusedBorderColor = tokens.border,
            focusedTextColor = tokens.ink,
            unfocusedTextColor = tokens.ink,
            cursorColor = tokens.jade,
            focusedContainerColor = KptTheme.colorScheme.surface,
            unfocusedContainerColor = KptTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun ThemePicker(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        BucketThemes.forEachIndexed { index, theme ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectAaa()
                    .clip(CircleShape)
                    .background(theme.accent.copy(alpha = if (selected) 0.20f else 0.10f))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) theme.accent else tokens.border,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = theme.emoji, fontSize = 18.sp)
            }
        }
    }
}

/** Square aspect helper for the theme swatches (keeps circular chips uniform). */
private fun Modifier.aspectAaa(): Modifier = this.height(44.dp)

@Composable
private fun ChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) tokens.jadeTint else KptTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = if (selected) tokens.jade else tokens.border,
                        shape = RoundedCornerShape(50),
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = KptTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (selected) tokens.jade else tokens.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.line)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) KptTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = KptTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (selected) tokens.ink else tokens.sub,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, tokens.border, RoundedCornerShape(12.dp))
            .padding(KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = KptTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.ink,
            )
            Text(
                text = subtitle,
                style = KptTheme.typography.bodySmall,
                color = tokens.sub,
            )
        }
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFBFAF6),
                checkedTrackColor = tokens.jade,
                uncheckedThumbColor = KptTheme.colorScheme.surface,
                uncheckedTrackColor = tokens.border,
                uncheckedBorderColor = tokens.border,
            ),
        )
    }
}

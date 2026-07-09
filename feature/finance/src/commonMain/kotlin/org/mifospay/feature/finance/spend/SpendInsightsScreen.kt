/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.finance.spend

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import template.core.base.designsystem.theme.KptTheme
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Mock data model (UI-only — no ViewModel, no backend, no navigation).
// ---------------------------------------------------------------------------

/** The three selectable reporting periods shown in the segmented control. */
private enum class SpendPeriod(val label: String) {
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
}

/** A spend category slice — drives both the donut arc and its legend row. */
private data class SpendCategory(
    val name: String,
    val amount: Double,
    val color: Color,
)

/** One bar in the spend-trend chart (a day or a week, depending on period). */
private data class TrendBar(
    val label: String,
    val amount: Double,
)

/** A row in the "Top merchants" list. */
private data class MerchantRow(
    val name: String,
    val category: String,
    val amount: Double,
)

/** Everything the dashboard renders for a single [SpendPeriod]. */
private data class SpendSnapshot(
    val totalSpent: Double,
    val previousTotal: Double,
    val budget: Double,
    val categories: List<SpendCategory>,
    val trend: List<TrendBar>,
    val moneyIn: Double,
    val moneyOut: Double,
    val merchants: List<MerchantRow>,
    val insight: String,
)

// ---------------------------------------------------------------------------
// Category palette — derived from the jade token plus believable accents.
// ---------------------------------------------------------------------------

private object SpendPalette {
    val groceries = Color(0xFF0F8A7B) // jade
    val airtime = Color(0xFF3FA9F5)
    val electricity = Color(0xFFF5A623)
    val transport = Color(0xFF7B61FF)
    val eatingOut = Color(0xFFE0567A)
    val transfers = Color(0xFF2BB673)
    val shopping = Color(0xFFC8B27A) // verified gold accent
    val other = Color(0xFF8A93A0)
}

private fun categoriesFor(scale: Double): List<SpendCategory> = listOf(
    SpendCategory("Groceries", 3120.0 * scale, SpendPalette.groceries),
    SpendCategory("Airtime & Data", 540.0 * scale, SpendPalette.airtime),
    SpendCategory("Electricity & Bills", 2380.0 * scale, SpendPalette.electricity),
    SpendCategory("Transport", 1650.0 * scale, SpendPalette.transport),
    SpendCategory("Eating out", 1890.0 * scale, SpendPalette.eatingOut),
    SpendCategory("Transfers", 1450.0 * scale, SpendPalette.transfers),
    SpendCategory("Shopping", 980.0 * scale, SpendPalette.shopping),
    SpendCategory("Other", 470.0 * scale, SpendPalette.other),
)

/** Builds the mock snapshot for the selected period. */
private fun snapshotFor(period: SpendPeriod): SpendSnapshot = when (period) {
    SpendPeriod.THIS_WEEK -> SpendSnapshot(
        totalSpent = 3120.75,
        previousTotal = 3540.00,
        budget = 4500.0,
        categories = categoriesFor(0.25),
        trend = listOf(
            TrendBar("Mon", 410.0),
            TrendBar("Tue", 280.0),
            TrendBar("Wed", 620.0),
            TrendBar("Thu", 190.0),
            TrendBar("Fri", 880.0),
            TrendBar("Sat", 540.0),
            TrendBar("Sun", 200.75),
        ),
        moneyIn = 4200.0,
        moneyOut = 3120.75,
        merchants = listOf(
            MerchantRow("Woolworths", "Groceries", 612.40),
            MerchantRow("Uber", "Transport", 248.00),
            MerchantRow("Nando's", "Eating out", 196.50),
            MerchantRow("Eskom Prepaid", "Electricity & Bills", 350.00),
        ),
        insight = "You're tracking 12% under last week — nice work staying on budget.",
    )

    SpendPeriod.THIS_MONTH -> SpendSnapshot(
        totalSpent = 12480.50,
        previousTotal = 10580.00,
        budget = 18000.0,
        categories = categoriesFor(1.0),
        trend = listOf(
            TrendBar("W1", 2480.0),
            TrendBar("W2", 3620.0),
            TrendBar("W3", 2980.0),
            TrendBar("W4", 3400.50),
        ),
        moneyIn = 21500.0,
        moneyOut = 12480.50,
        merchants = listOf(
            MerchantRow("Checkers", "Groceries", 1840.25),
            MerchantRow("Eskom Prepaid", "Electricity & Bills", 1200.00),
            MerchantRow("Uber Eats", "Eating out", 968.70),
            MerchantRow("Takealot", "Shopping", 749.00),
            MerchantRow("Gautrain", "Transport", 520.00),
        ),
        insight = "You spent 18% more on Eating out than last month.",
    )

    SpendPeriod.LAST_MONTH -> SpendSnapshot(
        totalSpent = 10580.00,
        previousTotal = 11240.00,
        budget = 18000.0,
        categories = categoriesFor(0.85),
        trend = listOf(
            TrendBar("W1", 2980.0),
            TrendBar("W2", 2240.0),
            TrendBar("W3", 3120.0),
            TrendBar("W4", 2240.0),
        ),
        moneyIn = 20800.0,
        moneyOut = 10580.00,
        merchants = listOf(
            MerchantRow("Pick n Pay", "Groceries", 1620.00),
            MerchantRow("City of Joburg", "Electricity & Bills", 1380.00),
            MerchantRow("Bolt", "Transport", 612.50),
            MerchantRow("Mr Price", "Shopping", 489.99),
            MerchantRow("Steers", "Eating out", 248.00),
        ),
        insight = "Groceries were your biggest category — 15% of everything you spent.",
    )
}

// ---------------------------------------------------------------------------
// Money formatting — "R 1,234.56" with grouped thousands.
// ---------------------------------------------------------------------------

private fun formatRand(value: Double, withDecimals: Boolean = true): String {
    val rounded = if (withDecimals) {
        ((abs(value) * 100).roundToInt())
    } else {
        (abs(value).roundToInt() * 100)
    }
    val whole = rounded / 100
    val cents = rounded % 100
    val wholeStr = whole.toString()
    val grouped = buildString {
        val digits = wholeStr.reversed()
        digits.forEachIndexed { i, c ->
            if (i != 0 && i % 3 == 0) append(',')
            append(c)
        }
    }.reversed()
    val sign = if (value < 0) "-" else ""
    return if (withDecimals) {
        val centsStr = cents.toString().padStart(2, '0')
        "${sign}R $grouped.$centsStr"
    } else {
        "${sign}R $grouped"
    }
}

private fun percentOf(part: Double, whole: Double): Int =
    if (whole <= 0.0) 0 else ((part / whole) * 100).roundToInt()

// ---------------------------------------------------------------------------
// Public entry point — mounted as a tab inside the Finance screen.
// ---------------------------------------------------------------------------

/**
 * Spend Insights — a self-contained, scrollable spending-analytics dashboard.
 *
 * UI-only: it holds its own [SpendPeriod] selection in local state and renders
 * mock data via [snapshotFor]. No ViewModel, navigation or backend; the charts
 * (donut + bars) are drawn directly with Compose [Canvas].
 */
@Composable
fun SpendInsightsScreen(modifier: Modifier = Modifier) {
    var period by remember { mutableStateOf(SpendPeriod.THIS_MONTH) }
    val snapshot = remember(period) { snapshotFor(period) }
    val tokens = SimpliPayTheme.tokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        PeriodSelector(
            selected = period,
            onSelect = { period = it },
        )

        HeroSummaryCard(snapshot = snapshot, period = period)

        CategoryBreakdownCard(snapshot = snapshot)

        SpendTrendCard(snapshot = snapshot, period = period)

        IncomeVsExpensesCard(snapshot = snapshot)

        TopMerchantsCard(snapshot = snapshot)

        InsightCallout(text = snapshot.insight)

        Spacer(Modifier.height(KptTheme.spacing.sm))
    }
}

// ---------------------------------------------------------------------------
// 1. Period selector — segmented chip row with local remember state.
// ---------------------------------------------------------------------------

@Composable
private fun PeriodSelector(
    selected: SpendPeriod,
    onSelect: (SpendPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(tokens.line)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SpendPeriod.entries.forEach { item ->
            val active = item == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) KptTheme.colorScheme.surface else Color.Transparent)
                    .then(
                        if (active) {
                            Modifier.border(
                                width = 1.dp,
                                color = tokens.border,
                                shape = RoundedCornerShape(50),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.label,
                    style = KptTheme.typography.labelMedium.copy(
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (active) tokens.ink else tokens.sub,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared card chrome — matches the rounded ivory house style.
// ---------------------------------------------------------------------------

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KptTheme.colorScheme.surface)
            .border(width = 1.dp, color = tokens.border, shape = shape)
            .padding(KptTheme.spacing.md),
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = SimpliPayTheme.tokens.ink,
    )
}

// ---------------------------------------------------------------------------
// 2. Hero summary card — total, change vs previous, budget progress.
// ---------------------------------------------------------------------------

@Composable
private fun HeroSummaryCard(
    snapshot: SpendSnapshot,
    period: SpendPeriod,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(22.dp)
    val changePct = percentOf(
        snapshot.totalSpent - snapshot.previousTotal,
        snapshot.previousTotal,
    )
    val spentMore = snapshot.totalSpent > snapshot.previousTotal
    // Spending more than the previous period is "bad" (red); less is "good" (green).
    val changeColor = if (spentMore) tokens.debit else tokens.credit
    val arrow = if (spentMore) "↑" else "↓"
    val priorLabel = when (period) {
        SpendPeriod.THIS_WEEK -> "last week"
        else -> "last month"
    }
    val budgetFraction = (snapshot.totalSpent / snapshot.budget).coerceIn(0.0, 1.0).toFloat()
    val overBudget = snapshot.totalSpent > snapshot.budget

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush = tokens.ivoryGradient)
            .border(width = 1.dp, color = tokens.ivoryBorder, shape = shape)
            .padding(KptTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
            Text(
                text = "TOTAL SPENT • ${period.label.uppercase()}",
                style = KptTheme.typography.labelSmall.copy(
                    fontFamily = tokens.monoFontFamily,
                    letterSpacing = 1.5.sp,
                ),
                color = tokens.cardLabel,
            )
            Text(
                text = formatRand(snapshot.totalSpent),
                style = KptTheme.typography.headlineLarge.copy(
                    fontFamily = tokens.monoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = tokens.cardInk,
            )
            Text(
                text = "$arrow ${abs(changePct)}% vs $priorLabel",
                style = KptTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = changeColor,
            )

            Spacer(Modifier.height(KptTheme.spacing.xs))

            // Budget progress bar.
            BudgetBar(
                fraction = budgetFraction,
                fill = if (overBudget) tokens.debit else tokens.jade,
                track = Color(0x14000000),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${formatRand(snapshot.totalSpent, withDecimals = false)} of " +
                        formatRand(snapshot.budget, withDecimals = false),
                    style = KptTheme.typography.bodySmall.copy(fontFamily = tokens.monoFontFamily),
                    color = tokens.cardLabel,
                )
                Text(
                    text = if (overBudget) "Over budget" else "${(budgetFraction * 100).roundToInt()}% used",
                    style = KptTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (overBudget) tokens.debit else tokens.cardLabel,
                )
            }
        }
    }
}

@Composable
private fun BudgetBar(
    fraction: Float,
    fill: Color,
    track: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val radius = size.height / 2f
        // Track.
        drawRoundRect(
            color = track,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
        // Fill.
        val fillWidth = (size.width * fraction).coerceAtLeast(if (fraction > 0f) size.height else 0f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = fill,
                size = Size(fillWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Spend-by-category — Canvas donut + legend list.
// ---------------------------------------------------------------------------

@Composable
private fun CategoryBreakdownCard(
    snapshot: SpendSnapshot,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val total = snapshot.categories.sumOf { it.amount }

    InsightCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md)) {
            SectionTitle("Where your money went")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentAlignment = Alignment.Center,
            ) {
                DonutChart(
                    categories = snapshot.categories,
                    trackColor = tokens.line,
                    modifier = Modifier.size(180.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total",
                        style = KptTheme.typography.labelSmall,
                        color = tokens.sub,
                    )
                    Text(
                        text = formatRand(total, withDecimals = false),
                        style = KptTheme.typography.titleMedium.copy(
                            fontFamily = tokens.monoFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = tokens.ink,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
                snapshot.categories.forEach { cat ->
                    CategoryLegendRow(category = cat, total = total)
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    categories: List<SpendCategory>,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val total = categories.sumOf { it.amount }.toFloat()
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.16f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        val gapDeg = 2.5f

        // Faint background ring so empty slices read cleanly.
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt),
        )

        if (total <= 0f) return@Canvas

        var startAngle = -90f
        categories.forEach { cat ->
            val fullSweep = (cat.amount.toFloat() / total) * 360f
            val sweep = (fullSweep - gapDeg).coerceAtLeast(0f)
            drawArc(
                color = cat.color,
                startAngle = startAngle + gapDeg / 2f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            startAngle += fullSweep
        }
    }
}

@Composable
private fun CategoryLegendRow(
    category: SpendCategory,
    total: Double,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val pct = percentOf(category.amount, total)
    val fraction = if (total > 0) (category.amount / total).toFloat() else 0f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(category.color),
        )
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.name,
                    style = KptTheme.typography.bodyMedium,
                    color = tokens.ink,
                )
                Text(
                    text = formatRand(category.amount, withDecimals = false),
                    style = KptTheme.typography.bodyMedium.copy(
                        fontFamily = tokens.monoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = tokens.ink,
                )
            }
            Spacer(Modifier.height(5.dp))
            // Thin per-category bar.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            ) {
                val r = size.height / 2f
                drawRoundRect(
                    color = tokens.line,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                )
                val w = (size.width * fraction).coerceAtLeast(if (fraction > 0f) size.height else 0f)
                if (w > 0f) {
                    drawRoundRect(
                        color = category.color,
                        size = Size(w, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                    )
                }
            }
        }
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Text(
            text = "$pct%",
            style = KptTheme.typography.labelMedium.copy(fontFamily = tokens.monoFontFamily),
            color = tokens.sub,
            modifier = Modifier.width(34.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 4. Spend trend — Canvas bar chart, tallest bar highlighted.
// ---------------------------------------------------------------------------

@Composable
private fun SpendTrendCard(
    snapshot: SpendSnapshot,
    period: SpendPeriod,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val subtitle = when (period) {
        SpendPeriod.THIS_WEEK -> "Daily spend"
        else -> "Weekly spend"
    }

    InsightCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("Spend trend")
                Text(
                    text = subtitle,
                    style = KptTheme.typography.labelMedium,
                    color = tokens.sub,
                )
            }

            BarChart(
                bars = snapshot.trend,
                barColor = tokens.jadeTint,
                highlightColor = tokens.jade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                snapshot.trend.forEach { bar ->
                    Text(
                        text = bar.label,
                        style = KptTheme.typography.labelSmall.copy(
                            fontFamily = tokens.monoFontFamily,
                        ),
                        color = tokens.sub,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun BarChart(
    bars: List<TrendBar>,
    barColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier,
) {
    if (bars.isEmpty()) return
    val maxAmount = bars.maxOf { it.amount }.toFloat().coerceAtLeast(1f)
    val maxIndex = bars.indexOfFirst { it.amount == bars.maxOf { b -> b.amount } }

    Canvas(modifier = modifier) {
        val count = bars.size
        val slot = size.width / count
        val barWidth = slot * 0.5f
        val gap = (slot - barWidth) / 2f
        val radius = barWidth / 2.5f

        bars.forEachIndexed { i, bar ->
            val fraction = bar.amount.toFloat() / maxAmount
            val barHeight = (size.height * fraction).coerceAtLeast(barWidth.coerceAtMost(size.height))
            val left = i * slot + gap
            val top = size.height - barHeight
            drawRoundRect(
                color = if (i == maxIndex) highlightColor else barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 5. Income vs expenses — compact two-row summary.
// ---------------------------------------------------------------------------

@Composable
private fun IncomeVsExpensesCard(
    snapshot: SpendSnapshot,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    InsightCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md)) {
            SectionTitle("Money in vs out")
            MoneyFlowRow(
                label = "Money in",
                amount = snapshot.moneyIn,
                dotColor = tokens.credit,
                amountColor = tokens.credit,
                sign = "+",
            )
            MoneyFlowRow(
                label = "Money out",
                amount = snapshot.moneyOut,
                dotColor = tokens.debit,
                amountColor = tokens.ink,
                sign = "−",
            )
        }
    }
}

@Composable
private fun MoneyFlowRow(
    label: String,
    amount: Double,
    dotColor: Color,
    amountColor: Color,
    sign: String,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Text(
            text = label,
            style = KptTheme.typography.bodyMedium,
            color = tokens.ink,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$sign${formatRand(amount)}",
            style = KptTheme.typography.bodyLarge.copy(
                fontFamily = tokens.monoFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = amountColor,
        )
    }
}

// ---------------------------------------------------------------------------
// 6. Top merchants / largest transactions.
// ---------------------------------------------------------------------------

@Composable
private fun TopMerchantsCard(
    snapshot: SpendSnapshot,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    InsightCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
            SectionTitle("Top merchants")
            Spacer(Modifier.height(KptTheme.spacing.xs))
            snapshot.merchants.forEachIndexed { i, m ->
                MerchantListRow(merchant = m, color = categoryColor(m.category))
                if (i != snapshot.merchants.lastIndex) {
                    Spacer(Modifier.height(KptTheme.spacing.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(tokens.line),
                    )
                    Spacer(Modifier.height(KptTheme.spacing.sm))
                }
            }
        }
    }
}

/** Maps a merchant's category label back to its palette swatch for the avatar tint. */
private fun categoryColor(category: String): Color = when (category) {
    "Groceries" -> SpendPalette.groceries
    "Airtime & Data" -> SpendPalette.airtime
    "Electricity & Bills" -> SpendPalette.electricity
    "Transport" -> SpendPalette.transport
    "Eating out" -> SpendPalette.eatingOut
    "Transfers" -> SpendPalette.transfers
    "Shopping" -> SpendPalette.shopping
    else -> SpendPalette.other
}

@Composable
private fun MerchantListRow(
    merchant: MerchantRow,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = merchant.name.take(1).uppercase(),
                style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
        }
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant.name,
                style = KptTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.ink,
            )
            Text(
                text = merchant.category,
                style = KptTheme.typography.bodySmall,
                color = tokens.sub,
            )
        }
        Text(
            text = "−${formatRand(merchant.amount)}",
            style = KptTheme.typography.bodyMedium.copy(
                fontFamily = tokens.monoFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tokens.ink,
        )
    }
}

// ---------------------------------------------------------------------------
// 7. Insight callout — friendly auto-insight sentence.
// ---------------------------------------------------------------------------

@Composable
private fun InsightCallout(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.jadeTint)
            .border(width = 1.dp, color = tokens.jade.copy(alpha = 0.25f), shape = shape)
            .padding(KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tokens.jade),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✨",
                style = KptTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Column {
            Text(
                text = "Insight",
                style = KptTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = tokens.jade,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = text,
                style = KptTheme.typography.bodyMedium,
                color = tokens.ink,
            )
        }
    }
}

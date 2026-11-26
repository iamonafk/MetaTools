package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MappingDiagnosticsParser
import com.example.data.MappingIntelligenceAnalyzer
import com.example.model.CatalogFlagItem
import com.example.model.CategoryAnalyticsItem
import com.example.model.ComparisonReport
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UpdatedOrange
import com.example.ui.theme.WarningYellow
import java.util.Locale

@Composable
fun MappingAnalyticsSection(
    report: ComparisonReport,
    onSelectCategoryInCatalog: (String) -> Unit,
    onOpenFlagDetail: (CatalogFlagItem) -> Unit,
    onCopyCategoryFlags: (String) -> Unit = {},
    onExportCategory: (String) -> Unit = {},
    onFilterByNamespace: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var categoryQuery by remember { mutableStateOf("") }

    val nonEmptyCategories = remember(report.categoryAnalytics, categoryQuery) {
        val active = report.categoryAnalytics.filter { it.totalUniqueFlags > 0 }
        val q = categoryQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) active else active.filter { it.categoryName.lowercase(Locale.ROOT).contains(q) }
    }

    val mostChangedCategories = remember(report.categoryAnalytics) {
        report.categoryAnalytics
            .filter { (it.newFlagsCount + it.removedFlagsCount + it.updatedFlagsCount) > 0 }
            .sortedByDescending { it.newFlagsCount + it.removedFlagsCount + it.updatedFlagsCount }
            .take(5)
    }

    val highestSubflagFlags = remember(report.catalogFlags) {
        report.catalogFlags
            .filter { it.totalDistinctSubFlags > 0 }
            .sortedByDescending { it.totalDistinctSubFlags }
            .take(6)
    }

    val deltaUniversal = report.summary.totalNewFlags - report.summary.totalOldFlags
    val deltaSubs = report.summary.totalNewSubFlags - report.summary.totalOldSubFlags
    val subToUniversalRatio = if (report.summary.totalNewFlags > 0) {
        String.format(Locale.US, "%.2f", report.summary.totalNewSubFlags.toFloat() / report.summary.totalNewFlags.toFloat())
    } else {
        "0.00"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Volume, Growth % & Ratio Comparison
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "VOLUME, GROWTH & STRUCTURE RATIO",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetricBox(
                    title = "Flag Growth",
                    value = MappingIntelligenceAnalyzer.formatGrowthPercent(report.summary.mappingGrowthPercent),
                    subtitle = "${if (deltaUniversal >= 0) "+" else ""}$deltaUniversal flags",
                    color = if (deltaUniversal >= 0) AccentGreen else DangerRed,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    title = "Subflag Growth",
                    value = MappingIntelligenceAnalyzer.formatGrowthPercent(report.summary.subflagGrowthPercent),
                    subtitle = "${if (deltaSubs >= 0) "+" else ""}$deltaSubs subs",
                    color = if (deltaSubs >= 0) AccentGreen else DangerRed,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    title = "Sub / Flag Ratio",
                    value = subToUniversalRatio,
                    subtitle = "${report.summary.threadsP92FlagsCount} P92/Threads",
                    color = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            if (report.summary.oldByteSize > 0L || report.summary.newByteSize > 0L) {
                Text(
                    text = "Size: ${MappingDiagnosticsParser.formatBytes(report.summary.oldByteSize)} → ${MappingDiagnosticsParser.formatBytes(report.summary.newByteSize)} • Active Categories: ${report.categoryAnalytics.count { it.totalUniqueFlags > 0 }}/65",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Namespace & Prefix Analysis (including Threads / P92 Detection)
        if (report.namespaceStats.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NAMESPACE & THREADS / P92 ANALYSIS",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap prefix to filter",
                        color = AccentGreen,
                        fontSize = 10.sp
                    )
                }

                report.namespaceStats.take(12).forEach { ns ->
                    val growthStr = MappingIntelligenceAnalyzer.formatGrowthPercent(ns.growthPercent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmoledBackground)
                            .clickable { onFilterByNamespace(ns.prefix) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = ns.prefix,
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (ns.isThreadsP92) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(InfoBlue.copy(alpha = 0.16f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "THREADS / P92",
                                        color = InfoBlue,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${ns.oldCount} → ${ns.newCount} ($growthStr)",
                            color = if (ns.growthPercent >= 0f) AccentGreen else DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 3. Most Changed Categories
        if (mostChangedCategories.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MOST CHANGED CATEGORIES",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                mostChangedCategories.forEach { cat ->
                    val changes = cat.newFlagsCount + cat.removedFlagsCount + cat.updatedFlagsCount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmoledBackground)
                            .clickable { onSelectCategoryInCatalog(cat.categoryName) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.categoryName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "+${cat.newFlagsCount} new • -${cat.removedFlagsCount} rem • ~${cat.updatedFlagsCount} upd",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "$changes changes",
                            color = UpdatedOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. Universal Flags with Highest Subflag Count
        if (highestSubflagFlags.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "FLAGS WITH HIGHEST SUBFLAG COUNT",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                highestSubflagFlags.forEach { flag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmoledBackground)
                            .clickable { onOpenFlagDetail(flag) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = flag.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Text(
                                text = flag.category,
                                color = InfoBlue,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${flag.totalDistinctSubFlags} subs",
                            color = AccentGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 5. Category Analytics Breakdown (with Copy All & Export Category)
        Text(
            text = "CATEGORY ANALYTICS BREAKDOWN (${nonEmptyCategories.size} ACTIVE)",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        nonEmptyCategories.sortedByDescending { it.totalUniqueFlags }.forEach { cat ->
            CategoryAnalyticsRowCard(
                item = cat,
                onClick = { onSelectCategoryInCatalog(cat.categoryName) },
                onCopyCategory = { onCopyCategoryFlags(cat.categoryName) },
                onExportCategory = { onExportCategory(cat.categoryName) }
            )
        }
    }
}

@Composable
private fun AnalyticsMetricBox(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text(text = title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = subtitle, color = TextTertiary, fontSize = 9.5.sp, maxLines = 1)
    }
}

@Composable
private fun CategoryAnalyticsRowCard(
    item: CategoryAnalyticsItem,
    onClick: () -> Unit,
    onCopyCategory: () -> Unit,
    onExportCategory: () -> Unit
) {
    val total = item.totalUniqueFlags.coerceAtLeast(1).toFloat()
    val newFrac = item.newFlagsCount / total
    val updFrac = item.updatedFlagsCount / total
    val remFrac = item.removedFlagsCount / total
    val uncFrac = (1f - newFrac - updFrac - remFrac).coerceAtLeast(0f)
    val flagGrowthStr = MappingIntelligenceAnalyzer.formatGrowthPercent(item.flagGrowthPercent)
    val subGrowthStr = MappingIntelligenceAnalyzer.formatGrowthPercent(item.subflagGrowthPercent)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.categoryName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${item.totalUniqueFlags} flags ($flagGrowthStr)",
                color = if (item.flagGrowthPercent >= 0f) AccentGreen else DangerRed,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Stacked Distribution Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AmoledBackground)
        ) {
            if (newFrac > 0f) {
                Box(modifier = Modifier.weight(newFrac).height(6.dp).background(AccentGreen))
            }
            if (updFrac > 0f) {
                Box(modifier = Modifier.weight(updFrac).height(6.dp).background(UpdatedOrange))
            }
            if (remFrac > 0f) {
                Box(modifier = Modifier.weight(remFrac).height(6.dp).background(DangerRed))
            }
            if (uncFrac > 0f) {
                Box(modifier = Modifier.weight(uncFrac).height(6.dp).background(TextTertiary.copy(alpha = 0.45f)))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Old ${item.oldCount} → New ${item.newCount} • ${item.totalSubFlags} subs ($subGrowthStr)",
                color = TextTertiary,
                fontSize = 10.sp
            )
            Text(
                text = "+${item.newFlagsCount}  -${item.removedFlagsCount}  ~${item.updatedFlagsCount}",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Quick Category Actions: Copy All Flags / Export Category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmoledBackground)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                    .clickable(onClick = onCopyCategory)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy All Flags", color = AccentGreen, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmoledBackground)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                    .clickable(onClick = onExportCategory)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Category", color = InfoBlue, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


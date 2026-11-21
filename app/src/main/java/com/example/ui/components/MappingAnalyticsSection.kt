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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MappingDiagnosticsParser
import com.example.model.CatalogFlagItem
import com.example.model.CategoryAnalyticsItem
import com.example.model.ComparisonReport
import com.example.ui.theme.AccentGreen
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

@Composable
fun MappingAnalyticsSection(
    report: ComparisonReport,
    onSelectCategoryInCatalog: (String) -> Unit,
    onOpenFlagDetail: (CatalogFlagItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val nonEmptyCategories = remember(report.categoryAnalytics) {
        report.categoryAnalytics.filter { it.totalUniqueFlags > 0 }
    }

    val mostChangedCategories = remember(nonEmptyCategories) {
        nonEmptyCategories
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. File Size & Completeness Comparison Card
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
                text = "MAPPING HEALTH & VOLUME COMPARISON",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetricBox(
                    title = "Old Health",
                    value = "${report.summary.oldHealthScore}%",
                    subtitle = "${report.summary.totalOldFlags} flags",
                    color = if (report.summary.oldHealthScore >= 85) AccentGreen else WarningYellow,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    title = "New Health",
                    value = "${report.summary.newHealthScore}%",
                    subtitle = "${report.summary.totalNewFlags} flags",
                    color = if (report.summary.newHealthScore >= 85) AccentGreen else WarningYellow,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    title = "Net Flag Delta",
                    value = if (deltaUniversal >= 0) "+$deltaUniversal" else "$deltaUniversal",
                    subtitle = "${if (deltaSubs >= 0) "+" else ""}$deltaSubs subs",
                    color = if (deltaUniversal >= 0) AccentGreen else DangerRed,
                    modifier = Modifier.weight(1f)
                )
            }

            if (report.summary.oldByteSize > 0L || report.summary.newByteSize > 0L) {
                Text(
                    text = "File Size: ${MappingDiagnosticsParser.formatBytes(report.summary.oldByteSize)} → ${MappingDiagnosticsParser.formatBytes(report.summary.newByteSize)} • Active Categories: ${nonEmptyCategories.size}/65",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Most Changed Categories
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

        // 3. Universal Flags with Highest Subflag Count
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

        // 4. Per-Category Analytics Breakdown (65 feature categories)
        Text(
            text = "CATEGORY ANALYTICS BREAKDOWN (${nonEmptyCategories.size} ACTIVE)",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        nonEmptyCategories.sortedByDescending { it.totalUniqueFlags }.forEach { cat ->
            CategoryAnalyticsRowCard(
                item = cat,
                onClick = { onSelectCategoryInCatalog(cat.categoryName) }
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
        Text(text = value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(text = title, color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
        Text(text = subtitle, color = TextTertiary, fontSize = 9.5.sp)
    }
}

@Composable
private fun CategoryAnalyticsRowCard(
    item: CategoryAnalyticsItem,
    onClick: () -> Unit
) {
    val total = item.totalUniqueFlags.coerceAtLeast(1).toFloat()
    val newFrac = item.newFlagsCount / total
    val updFrac = item.updatedFlagsCount / total
    val remFrac = item.removedFlagsCount / total
    val uncFrac = (1f - newFrac - updFrac - remFrac).coerceAtLeast(0f)

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
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${item.totalUniqueFlags} flags (${item.unchangedPercent}% unchanged)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
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
                text = "Old ${item.oldCount} → New ${item.newCount} • ${item.totalSubFlags} subflags",
                color = TextTertiary,
                fontSize = 10.5.sp
            )
            Text(
                text = "+${item.newFlagsCount}  -${item.removedFlagsCount}  ~${item.updatedFlagsCount}",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

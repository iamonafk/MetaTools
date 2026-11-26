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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MappingDiagnosticsParser
import com.example.data.MappingIntelligenceAnalyzer
import com.example.model.CompareFilterTab
import com.example.model.CompareQuickFilter
import com.example.model.CompareResultSection
import com.example.model.ComparisonSummary
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
fun ComparisonDashboard(
    summary: ComparisonSummary,
    selectedFilter: CompareFilterTab,
    onFilterSelected: (CompareFilterTab) -> Unit,
    onQuickFilterSelected: (CompareQuickFilter) -> Unit = {},
    onOpenHealthTab: () -> Unit = {},
    onNavigateSection: (CompareResultSection) -> Unit = {},
    onRefreshReindex: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalUniqueOld = summary.totalOldFlags.coerceAtLeast(1)
    val unchangedPct = ((summary.unchangedFlagsCount * 100f) / totalUniqueOld).toInt().coerceIn(0, 100)
    val flagGrowthStr = MappingIntelligenceAnalyzer.formatGrowthPercent(summary.mappingGrowthPercent)
    val subGrowthStr = MappingIntelligenceAnalyzer.formatGrowthPercent(summary.subflagGrowthPercent)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPARISON DASHBOARD",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Health: ${summary.oldHealthScore}%→${summary.newHealthScore}%",
                    color = if (summary.newHealthScore >= 85) AccentGreen else WarningYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = onRefreshReindex,
                    modifier = Modifier.testTag("button_refresh_reindex")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh and re-index",
                        tint = AccentGreen
                    )
                }
            }
        }

        // Old vs New Totals Strip + Growth %
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OLD MAPPING TOTALS",
                        color = TextTertiary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.totalOldFlags} universal • ${summary.totalOldSubFlags} subs",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (summary.oldByteSize > 0L) {
                        Text(
                            text = MappingDiagnosticsParser.formatBytes(summary.oldByteSize),
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "NEW MAPPING TOTALS",
                        color = TextTertiary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.totalNewFlags} universal • ${summary.totalNewSubFlags} subs",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (summary.newByteSize > 0L) {
                        Text(
                            text = MappingDiagnosticsParser.formatBytes(summary.newByteSize),
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mapping Growth: $flagGrowthStr",
                    color = if (summary.mappingGrowthPercent >= 0f) AccentGreen else DangerRed,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Subflag Growth: $subGrowthStr",
                    color = if (summary.subflagGrowthPercent >= 0f) AccentGreen else DangerRed,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2x2 Primary Universal & Change Dashboard Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardCard(
                label = "New Universal Flags",
                subLabel = "+${summary.newSubFlagsCount} new subflags",
                count = summary.newUniversalFlagsCount,
                accentColor = AccentGreen,
                isSelected = selectedFilter == CompareFilterTab.NEW,
                onClick = {
                    onNavigateSection(CompareResultSection.NEW_FLAGS)
                    onFilterSelected(CompareFilterTab.NEW)
                },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_new_flags"
            )

            DashboardCard(
                label = "Removed Universal",
                subLabel = "-${summary.removedSubFlagsCount} removed subflags",
                count = summary.removedUniversalFlagsCount,
                accentColor = DangerRed,
                isSelected = selectedFilter == CompareFilterTab.REMOVED,
                onClick = {
                    onNavigateSection(CompareResultSection.REMOVED_FLAGS)
                    onFilterSelected(CompareFilterTab.REMOVED)
                },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_removed_flags"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardCard(
                label = "Updated Universal",
                subLabel = "~${summary.updatedSubFlagsCount} subflag codes changed",
                count = summary.updatedUniversalFlagsCount,
                accentColor = UpdatedOrange,
                isSelected = selectedFilter == CompareFilterTab.UPDATED,
                onClick = {
                    onNavigateSection(CompareResultSection.UPDATED_FLAGS)
                    onFilterSelected(CompareFilterTab.UPDATED)
                },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_updated_flags"
            )

            DashboardCard(
                label = "Unchanged Flags",
                subLabel = "$unchangedPct% stability vs Old",
                count = summary.unchangedFlagsCount,
                accentColor = TextSecondary,
                isSelected = selectedFilter == CompareFilterTab.UNCHANGED,
                onClick = {
                    onNavigateSection(CompareResultSection.UNCHANGED_FLAGS)
                    onFilterSelected(CompareFilterTab.UNCHANGED)
                },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_unchanged_flags"
            )
        }

        // Subflags & Intelligence Counters Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniStatPill(
                label = "New Sub",
                value = "${summary.newSubFlagsCount}",
                color = AccentGreen,
                onClick = { onNavigateSection(CompareResultSection.NEW_SUBFLAGS) },
                modifier = Modifier.weight(1f)
            )
            MiniStatPill(
                label = "Rem Sub",
                value = "${summary.removedSubFlagsCount}",
                color = DangerRed,
                onClick = { onNavigateSection(CompareResultSection.REMOVED_SUBFLAGS) },
                modifier = Modifier.weight(1f)
            )
            MiniStatPill(
                label = "Upd Sub",
                value = "${summary.updatedSubFlagsCount}",
                color = UpdatedOrange,
                onClick = { onNavigateSection(CompareResultSection.UPDATED_SUBFLAGS) },
                modifier = Modifier.weight(1f)
            )
            MiniStatPill(
                label = "P92/Threads",
                value = "${summary.threadsP92FlagsCount}",
                color = InfoBlue,
                onClick = { onNavigateSection(CompareResultSection.MAPPING_HEALTH) },
                modifier = Modifier.weight(1f)
            )
        }

        // Diagnostics & Integrity Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniStatPill(
                label = "Duplicates",
                value = "${summary.duplicateEntriesCount}",
                color = if (summary.duplicateEntriesCount > 0) WarningYellow else TextSecondary,
                onClick = { onNavigateSection(CompareResultSection.DUPLICATES) },
                modifier = Modifier.weight(1f)
            )
            MiniStatPill(
                label = "Invalid",
                value = "${summary.invalidEntriesCount}",
                color = if (summary.invalidEntriesCount > 0) DangerRed else TextSecondary,
                onClick = { onNavigateSection(CompareResultSection.INVALID_ENTRIES) },
                modifier = Modifier.weight(1f)
            )
            MiniStatPill(
                label = "Other Cat",
                value = "${summary.unknownCategoryCount}",
                color = InfoBlue,
                onClick = { onNavigateSection(CompareResultSection.UNKNOWN_CATEGORIES) },
                modifier = Modifier.weight(1f)
            )
            MiniStatPill(
                label = "Re-parent",
                value = "${summary.parentChildChangesCount}",
                color = if (summary.parentChildChangesCount > 0) UpdatedOrange else TextSecondary,
                onClick = { onNavigateSection(CompareResultSection.UPDATED_SUBFLAGS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DashboardCard(
    label: String,
    subLabel: String,
    count: Int,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val borderColor = if (isSelected) accentColor else AmoledBorder
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.14f) else AmoledCard

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(testTag)
    ) {
        Column {
            Text(
                text = "$count",
                color = accentColor,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subLabel,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MiniStatPill(
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

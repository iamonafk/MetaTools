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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompareFilterTab
import com.example.model.ComparisonSummary
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningYellow

@Composable
fun ComparisonDashboard(
    summary: ComparisonSummary,
    selectedFilter: CompareFilterTab,
    onFilterSelected: (CompareFilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "COMPARISON SUMMARY",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 2x2 Grid of Dashboard Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // New Flags (Green)
            DashboardCard(
                label = "Total New Flags",
                count = summary.newFlagsCount,
                accentColor = AccentGreen,
                isSelected = selectedFilter == CompareFilterTab.NEW,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == CompareFilterTab.NEW) CompareFilterTab.ALL else CompareFilterTab.NEW
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_new_flags"
            )

            // Removed Flags (Red)
            DashboardCard(
                label = "Total Removed Flags",
                count = summary.removedFlagsCount,
                accentColor = DangerRed,
                isSelected = selectedFilter == CompareFilterTab.REMOVED,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == CompareFilterTab.REMOVED) CompareFilterTab.ALL else CompareFilterTab.REMOVED
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_removed_flags"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Updated Flags (Orange / Yellow)
            DashboardCard(
                label = "Total Updated Flags",
                count = summary.updatedFlagsCount,
                accentColor = WarningYellow,
                isSelected = selectedFilter == CompareFilterTab.UPDATED,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == CompareFilterTab.UPDATED) CompareFilterTab.ALL else CompareFilterTab.UPDATED
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_updated_flags"
            )

            // Unchanged Flags (Gray)
            DashboardCard(
                label = "Unchanged Flags",
                count = summary.unchangedFlagsCount,
                accentColor = TextSecondary,
                isSelected = false,
                onClick = { onFilterSelected(CompareFilterTab.ALL) },
                modifier = Modifier.weight(1f),
                testTag = "dashboard_unchanged_flags"
            )
        }
    }
}

@Composable
private fun DashboardCard(
    label: String,
    count: Int,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val borderColor = if (isSelected) accentColor else AmoledBorder
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.12f) else AmoledCard

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
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

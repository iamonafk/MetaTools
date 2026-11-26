package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MappingCompareEngine
import com.example.model.MultiFileComparisonReport
import com.example.model.SessionDiffReport
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

private enum class MultiMatrixFilter(val label: String) {
    ALL("All Flags"),
    ONLY_ONE("Only In 1 File"),
    COMMON_ALL("Shared In All"),
    CODE_DIVERGENT("Code Changed")
}

@Composable
fun MultiFileMatrixSection(
    multiReport: MultiFileComparisonReport,
    searchQuery: String,
    onCopy: (String, String) -> Unit,
    onExportMatrix: () -> Unit,
    modifier: Modifier = Modifier
) {
    var matrixFilter by remember { mutableStateOf(MultiMatrixFilter.ALL) }
    var visibleLimit by remember(matrixFilter, searchQuery) { mutableIntStateOf(60) }

    val totalSources = multiReport.sourceNames.size
    val filteredEntries = remember(multiReport, matrixFilter, searchQuery) {
        multiReport.entries.filter { entry ->
            val matchesMode = when (matrixFilter) {
                MultiMatrixFilter.ALL -> true
                MultiMatrixFilter.ONLY_ONE -> entry.presentInSourceLabels.size == 1
                MultiMatrixFilter.COMMON_ALL -> entry.presentInSourceLabels.size == totalSources
                MultiMatrixFilter.CODE_DIVERGENT -> entry.distinctCodesCount > 1
            }
            if (!matchesMode) return@filter false

            if (searchQuery.isBlank()) {
                true
            } else {
                MappingCompareEngine.matchesQuery(
                    searchQuery,
                    entry.flagName,
                    entry.category,
                    entry.namespacePrefix
                ) || entry.codesBySource.values.any { code ->
                    code != null && MappingCompareEngine.matchesQuery(searchQuery, code)
                } || entry.subFlagNames.any { sf ->
                    MappingCompareEngine.matchesQuery(searchQuery, sf)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_file_matrix_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Matrix Overview Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AmoledCard)
                .border(1.dp, AccentGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MULTI-FILE MATRIX (${totalSources} MAPPINGS)",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGreenContainer)
                        .clickable(onClick = onExportMatrix)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Matrix", color = AccentGreen, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MatrixStatBox("Unique", "${multiReport.uniqueFlagsTotal}", TextPrimary, Modifier.weight(1f))
                MatrixStatBox("In All", "${multiReport.commonInAllCount}", AccentGreen, Modifier.weight(1f))
                MatrixStatBox("1-File", "${multiReport.onlyInOneFileCount}", InfoBlue, Modifier.weight(1f))
                MatrixStatBox("Code Diff", "${multiReport.codeDivergentCount}", UpdatedOrange, Modifier.weight(1f))
            }

            if (multiReport.pairwiseDeltas.isNotEmpty()) {
                HorizontalDivider(color = AmoledBorder)
                Text("PAIRWISE DELTAS:", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                multiReport.pairwiseDeltas.forEach { delta ->
                    Text(
                        text = "• $delta",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MultiMatrixFilter.entries.forEach { mf ->
                val selected = matrixFilter == mf
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) AccentGreenContainer else AmoledCard)
                        .border(1.dp, if (selected) AccentGreen else AmoledBorder, RoundedCornerShape(6.dp))
                        .clickable { matrixFilter = mf }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mf.label,
                        color = if (selected) AccentGreen else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Text(
            text = "Showing ${minOf(visibleLimit, filteredEntries.size)} of ${filteredEntries.size} cross-file entries",
            color = TextTertiary,
            fontSize = 11.sp
        )

        filteredEntries.take(visibleLimit).forEach { entry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmoledCard)
                    .border(
                        1.dp,
                        if (entry.distinctCodesCount > 1) UpdatedOrange.copy(alpha = 0.5f) else AmoledBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCopy(entry.flagName, "Flag Name") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.flagName,
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy flag name",
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${entry.presentInSourceLabels.size}/$totalSources files",
                        color = if (entry.presentInSourceLabels.size == totalSources) AccentGreen else InfoBlue,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${entry.category} • ${entry.namespacePrefix}",
                    color = TextTertiary,
                    fontSize = 10.sp
                )

                HorizontalDivider(color = AmoledBorder)

                multiReport.sourceNames.forEach { srcName ->
                    val code = entry.codesBySource[srcName]
                    val subs = entry.subflagCountsBySource[srcName] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = srcName,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (code != null) "$code ($subs subs)" else "Missing",
                            color = if (code != null) AccentGreen else DangerRed.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (filteredEntries.size > visibleLimit) {
            OutlinedButton(
                onClick = { visibleLimit += 80 },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "Load More (${filteredEntries.size - visibleLimit} remaining)",
                    color = AccentGreen,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SessionDiffSummaryCard(
    sessionDiff: SessionDiffReport,
    onInspectFlagByName: (String) -> Unit,
    onCopy: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledCard)
            .border(1.dp, InfoBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SAVED SESSION COMPARISON DIFF",
            color = InfoBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "A: ${sessionDiff.sessionALabel}\nB: ${sessionDiff.sessionBLabel}",
            color = TextPrimary,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MatrixStatBox(
                "Δ New",
                "${if (sessionDiff.deltaNewCount >= 0) "+" else ""}${sessionDiff.deltaNewCount}",
                AccentGreen,
                Modifier.weight(1f)
            )
            MatrixStatBox(
                "Δ Removed",
                "${if (sessionDiff.deltaRemovedCount >= 0) "+" else ""}${sessionDiff.deltaRemovedCount}",
                DangerRed,
                Modifier.weight(1f)
            )
            MatrixStatBox(
                "Δ Updated",
                "${if (sessionDiff.deltaUpdatedCount >= 0) "+" else ""}${sessionDiff.deltaUpdatedCount}",
                UpdatedOrange,
                Modifier.weight(1f)
            )
            MatrixStatBox(
                "Shifts",
                "${sessionDiff.statusTransitions.size}",
                WarningYellow,
                Modifier.weight(1f)
            )
        }

        if (sessionDiff.newlyAppearedInB.isNotEmpty()) {
            Text(
                text = "NEWLY APPEARED IN SESSION B (${sessionDiff.newlyAppearedInB.size}):",
                color = AccentGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            sessionDiff.newlyAppearedInB.take(15).forEach { flag ->
                Text(
                    text = "+ $flag",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInspectFlagByName(flag) }
                        .padding(vertical = 2.dp)
                )
            }
        }

        if (sessionDiff.statusTransitions.isNotEmpty()) {
            Text(
                text = "STATUS TRANSITIONS A → B (${sessionDiff.statusTransitions.size}):",
                color = WarningYellow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            sessionDiff.statusTransitions.take(15).forEach { tr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopy(tr.flagName, "Transition Flag") }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr.flagName,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${tr.statusInA} → ${tr.statusInB}",
                        color = UpdatedOrange,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = label, color = TextTertiary, fontSize = 9.5.sp, maxLines = 1)
    }
}

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.CompareHistoryRecord
import com.example.model.ComparisonReport
import com.example.model.DiagnosticIssue
import com.example.model.MappingParseDiagnostics
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MappingHealthDiagnosticsSection(
    report: ComparisonReport,
    onInspectFlagByName: (String) -> Unit,
    onCopy: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiagnosticsSourceCard(
            title = "OLD MAPPING HEALTH & COMPLETENESS",
            diagnostics = report.oldDiagnostics,
            onInspectFlagByName = onInspectFlagByName,
            onCopy = onCopy
        )

        DiagnosticsSourceCard(
            title = "NEW MAPPING HEALTH & COMPLETENESS",
            diagnostics = report.newDiagnostics,
            onInspectFlagByName = onInspectFlagByName,
            onCopy = onCopy
        )

        // Duplicate Cleanup Suggestions Card
        if (report.cleanupSuggestions.isNotEmpty()) {
            DuplicateCleanupSuggestionsCard(
                report = report,
                onCopy = onCopy
            )
        }

        // Potential Rename Candidates (Uncertain - clearly separated)
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
                text = "POTENTIAL RENAME CANDIDATES (${report.potentialRenames.size})",
                color = WarningYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Uncertain candidates matched by identical code between removed & new entries (not auto-confirmed).",
                color = TextSecondary,
                fontSize = 11.sp
            )
            if (report.potentialRenames.isEmpty()) {
                Text(
                    text = "No same-code rename candidates detected.",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            } else {
                report.potentialRenames.take(40).forEach { cand ->
                    PotentialRenameCandidateCard(
                        candidate = cand,
                        onInspectOld = {
                            onInspectFlagByName(cand.parentFlagName ?: cand.oldName)
                        },
                        onInspectNew = {
                            onInspectFlagByName(cand.parentFlagName ?: cand.newName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DuplicateCleanupSuggestionsCard(
    report: ComparisonReport,
    onCopy: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledCard)
            .border(1.dp, WarningYellow.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "DUPLICATE CLEANUP SUGGESTIONS (${report.cleanupSuggestions.size})",
            color = WarningYellow,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Review before editing mapping JSON. Original files are never altered automatically.",
            color = TextSecondary,
            fontSize = 11.sp
        )
        report.cleanupSuggestions.take(40).forEach { sug ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledBackground)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    .clickable { onCopy(sug.rawEntry, "Duplicate Raw Entry") }
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "[${sug.sourceSide}] ${sug.suggestionType}",
                        color = WarningYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${sug.flagCode}",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = sug.recommendation,
                    color = TextPrimary,
                    fontSize = 11.5.sp
                )
                Text(
                    text = sug.rawEntry,
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun DuplicatesDiagnosticsView(
    report: ComparisonReport,
    onInspectFlagByName: (String) -> Unit,
    onCopy: (String, String) -> Unit
) {
    val dupFlags = remember(report.catalogFlags) {
        report.catalogFlags.filter { it.isDuplicateInOld || it.isDuplicateInNew }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DuplicateCleanupSuggestionsCard(report = report, onCopy = onCopy)

        val oldDups = report.oldDiagnostics?.let { it.duplicateUniversalEntries + it.duplicateSubFlagEntries }.orEmpty()
        val newDups = report.newDiagnostics?.let { it.duplicateUniversalEntries + it.duplicateSubFlagEntries }.orEmpty()

        IssueGroupBlock(
            label = "New Mapping Duplicate Entries",
            issues = newDups,
            accentColor = WarningYellow,
            onInspectFlagByName = onInspectFlagByName,
            onCopy = onCopy,
            initiallyExpanded = true
        )
        IssueGroupBlock(
            label = "Old Mapping Duplicate Entries",
            issues = oldDups,
            accentColor = WarningYellow,
            onInspectFlagByName = onInspectFlagByName,
            onCopy = onCopy,
            initiallyExpanded = true
        )
        Text(
            text = "DUPLICATE CATALOG FLAGS (${dupFlags.size})",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        dupFlags.take(80).forEach { item ->
            CatalogUniversalCard(
                item = item,
                showExpandSubFlags = false,
                onOpenDetail = { onInspectFlagByName(item.name) },
                onCopy = onCopy
            )
        }
    }
}

@Composable
fun InvalidEntriesDiagnosticsView(
    report: ComparisonReport,
    onInspectFlagByName: (String) -> Unit,
    onCopy: (String, String) -> Unit
) {
    val newInvalid = report.newDiagnostics?.let {
        it.malformedEntries + it.missingNameEntries + it.emptySubFlagEntries + it.subFlagConsistencyIssues
    }.orEmpty()
    val oldInvalid = report.oldDiagnostics?.let {
        it.malformedEntries + it.missingNameEntries + it.emptySubFlagEntries + it.subFlagConsistencyIssues
    }.orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IssueGroupBlock(
            label = "New Mapping Invalid / Empty Subflag / Conflict Entries",
            issues = newInvalid,
            accentColor = DangerRed,
            onInspectFlagByName = onInspectFlagByName,
            onCopy = onCopy,
            initiallyExpanded = true
        )
        IssueGroupBlock(
            label = "Old Mapping Invalid / Empty Subflag / Conflict Entries",
            issues = oldInvalid,
            accentColor = DangerRed,
            onInspectFlagByName = onInspectFlagByName,
            onCopy = onCopy,
            initiallyExpanded = true
        )
    }
}

@Composable
fun UnknownCategoriesDiagnosticsView(
    report: ComparisonReport,
    onInspectFlagByName: (String) -> Unit,
    onCopy: (String, String) -> Unit
) {
    val unknownFlags = remember(report.catalogFlags) {
        report.catalogFlags.filter { it.isUnknownCategory }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "FLAGS MAPPED TO 'OTHER' CATEGORY (${unknownFlags.size})",
            color = InfoBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        if (unknownFlags.isEmpty()) {
            Text(
                text = "Every detected universal flag matched a known feature category.",
                color = AccentGreen,
                fontSize = 12.sp
            )
        } else {
            unknownFlags.take(100).forEach { item ->
                CatalogUniversalCard(
                    item = item,
                    showExpandSubFlags = false,
                    onOpenDetail = { onInspectFlagByName(item.name) },
                    onCopy = onCopy
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsSourceCard(
    title: String,
    diagnostics: MappingParseDiagnostics?,
    onInspectFlagByName: (String) -> Unit,
    onCopy: (String, String) -> Unit
) {
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
                text = title,
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (diagnostics != null) {
                Text(
                    text = "Health ${diagnostics.healthScore}% • Comp ${diagnostics.completenessPercent}%",
                    color = if (diagnostics.healthScore >= 85) AccentGreen else WarningYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (diagnostics == null) {
            Text(
                text = "Diagnostics unavailable for restored session.",
                color = TextTertiary,
                fontSize = 12.sp
            )
        } else {
            Text(
                text = "${diagnostics.sourceName} • ${diagnostics.validEntryCount}/${diagnostics.rawEntryCount} valid • ${diagnostics.totalSubFlagsCount} subflags",
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            IssueGroupBlock(
                label = "Invalid / Malformed / Missing Name",
                issues = diagnostics.malformedEntries + diagnostics.missingNameEntries,
                accentColor = DangerRed,
                onInspectFlagByName = onInspectFlagByName,
                onCopy = onCopy
            )

            IssueGroupBlock(
                label = "Empty Subflag Entries",
                issues = diagnostics.emptySubFlagEntries,
                accentColor = UpdatedOrange,
                onInspectFlagByName = onInspectFlagByName,
                onCopy = onCopy
            )

            IssueGroupBlock(
                label = "Subflag Code Consistency Conflicts",
                issues = diagnostics.subFlagConsistencyIssues,
                accentColor = WarningYellow,
                onInspectFlagByName = onInspectFlagByName,
                onCopy = onCopy
            )

            IssueGroupBlock(
                label = "Duplicate Universal & Subflag Entries",
                issues = diagnostics.duplicateUniversalEntries + diagnostics.duplicateSubFlagEntries,
                accentColor = WarningYellow,
                onInspectFlagByName = onInspectFlagByName,
                onCopy = onCopy
            )

            IssueGroupBlock(
                label = "Unknown / 'Other' Category Entries",
                issues = diagnostics.unknownCategoryEntries,
                accentColor = InfoBlue,
                onInspectFlagByName = onInspectFlagByName,
                onCopy = onCopy
            )
        }
    }
}

@Composable
private fun IssueGroupBlock(
    label: String,
    issues: List<DiagnosticIssue>,
    accentColor: Color,
    onInspectFlagByName: (String) -> Unit,
    onCopy: (String, String) -> Unit,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = issues.isNotEmpty()) { expanded = !expanded }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${issues.size}",
                color = if (issues.isNotEmpty()) accentColor else AccentGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (expanded && issues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            issues.take(50).forEach { issue ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmoledCard)
                        .clickable {
                            if (issue.flagName.isNotEmpty()) {
                                onInspectFlagByName(issue.flagName)
                            } else {
                                onCopy(issue.rawEntry, "Raw Entry")
                            }
                        }
                        .padding(6.dp)
                ) {
                    Text(
                        text = "#${issue.entryIndex}: ${issue.reason}",
                        color = accentColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = issue.rawEntry,
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }
            }
            if (issues.size > 50) {
                Text(
                    text = "+ ${issues.size - 50} more (Export report for full list)",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MappingHistorySection(
    history: List<CompareHistoryRecord>,
    onLoadRecord: (CompareHistoryRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var historyQuery by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    val filtered = remember(history, historyQuery) {
        val q = historyQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) history else history.filter {
            it.oldSourceName.lowercase(Locale.ROOT).contains(q) ||
                it.newSourceName.lowercase(Locale.ROOT).contains(q)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPARISON HISTORY (${history.size})",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = DangerRed, fontSize = 11.sp)
                }
            }
        }

        if (history.isNotEmpty()) {
            OutlinedTextField(
                value = historyQuery,
                onValueChange = { historyQuery = it },
                placeholder = { Text("Search previous comparisons...", color = TextTertiary, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmoledCard,
                    unfocusedContainerColor = AmoledCard,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = AmoledBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (history.isEmpty()) "No saved comparisons yet" else "No matching comparison sessions",
                        color = TextSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }
        } else {
            filtered.forEach { rec ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                        .clickable { onLoadRecord(rec) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(rec.timestamp)),
                            color = TextTertiary,
                            fontSize = 10.5.sp
                        )
                        IconButton(
                            onClick = { onDeleteRecord(rec.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete history entry",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "${rec.oldSourceName}  →  ${rec.newSourceName}",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+${rec.newCount} new  •  -${rec.removedCount} rem  •  ~${rec.updatedCount} upd",
                            color = UpdatedOrange,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Health ${rec.healthScore}%",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onLoadRecord(rec) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "Reopen Comparison",
                            color = AccentGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

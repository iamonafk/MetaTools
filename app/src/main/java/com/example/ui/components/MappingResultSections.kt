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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompareSearchIndex
import com.example.data.MappingCompareEngine
import com.example.data.MappingIntelligenceAnalyzer
import com.example.model.CatalogDisplayMode
import com.example.model.CatalogFlagItem
import com.example.model.CatalogScopeFilter
import com.example.model.CatalogSortOption
import com.example.model.CompareFilterTab
import com.example.model.CompareHistoryRecord
import com.example.model.CompareQuickFilter
import com.example.model.CompareResultSection
import com.example.model.ComparisonReport
import com.example.model.ExportFileFormat
import com.example.model.ExportReportType
import com.example.model.FlagChangeStatus
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

@Composable
fun MappingResultSectionsHub(
    report: ComparisonReport,
    multiFileReport: MultiFileComparisonReport?,
    sessionDiffReport: SessionDiffReport?,
    activeSection: CompareResultSection,
    onSelectSection: (CompareResultSection) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCommitSearchHistory: (String) -> Unit,
    onClearSearch: () -> Unit,
    compareSearchHistory: List<String>,
    onClearSearchHistory: () -> Unit,
    quickFilter: CompareQuickFilter,
    onQuickFilterSelected: (CompareQuickFilter) -> Unit,
    catalogCategory: String,
    selectedCatalogCategories: Set<String>,
    onSelectCatalogCategory: (String) -> Unit,
    onOpenMultiCategoryDialog: () -> Unit,
    catalogSort: CatalogSortOption,
    onSelectCatalogSort: (CatalogSortOption) -> Unit,
    catalogScope: CatalogScopeFilter,
    onSelectCatalogScope: (CatalogScopeFilter) -> Unit,
    namespaceFilter: String,
    onSelectNamespaceFilter: (String) -> Unit,
    exactSubflagCount: Int?,
    onSelectExactSubflagCount: (Int?) -> Unit,
    onlyInOneMapping: Boolean,
    onToggleOnlyInOneMapping: (Boolean) -> Unit,
    favoritesOnly: Boolean,
    onToggleFavoritesOnly: (Boolean) -> Unit,
    favoriteFlagNames: Set<String>,
    onToggleFavoriteFlag: (String) -> Unit,
    catalogDisplayMode: CatalogDisplayMode,
    onSelectCatalogDisplayMode: (CatalogDisplayMode) -> Unit,
    expandAllCategories: Boolean,
    onToggleExpandAllCategories: () -> Unit,
    selectedResultKeys: Set<String>,
    onToggleResultSelection: (String) -> Unit,
    onClearResultSelection: () -> Unit,
    onBulkCopySelected: () -> Unit,
    onOpenExportDialog: (ExportReportType, String?) -> Unit,
    onDirectSaveReport: (ExportReportType, ExportFileFormat, String?, Boolean) -> Unit,
    onDirectCopyReport: (ExportReportType, ExportFileFormat, String?) -> Unit,
    onDirectShareReport: (ExportReportType, ExportFileFormat, String?) -> Unit,
    onCopyAllInCategory: (String) -> Unit,
    onOpenFlagDetail: (CatalogFlagItem) -> Unit,
    onOpenFlagDetailByName: (String) -> Unit,
    onCopy: (String, String) -> Unit,
    onShare: (String, String) -> Unit,
    onRefreshReindex: () -> Unit,
    searchIndex: CompareSearchIndex?,
    compareHistory: List<CompareHistoryRecord>,
    onLoadHistoryRecord: (CompareHistoryRecord) -> Unit,
    onDeleteHistoryRecord: (String) -> Unit,
    onClearAllHistory: () -> Unit
) {
    var prefixMenuExpanded by remember { mutableStateOf(false) }

    val updatedUniversalTotal = remember(report) {
        report.catalogFlags.count { it.status == FlagChangeStatus.UPDATED }
    }
    val duplicatesCount = remember(report) {
        report.catalogFlags.count { it.isDuplicateInOld || it.isDuplicateInNew }
    }
    val invalidCount = remember(report) {
        (report.oldDiagnostics?.let { it.invalidCount + it.subFlagConsistencyIssues.size } ?: 0) +
            (report.newDiagnostics?.let { it.invalidCount + it.subFlagConsistencyIssues.size } ?: 0)
    }
    val unknownCatsCount = remember(report) {
        report.catalogFlags.count { it.isUnknownCategory }
    }
    val activeCategoriesCount = remember(report) {
        report.categoryAnalytics.count { it.totalUniqueFlags > 0 }
    }

    fun sectionBadgeCount(sec: CompareResultSection): Int? = when (sec) {
        CompareResultSection.OVERVIEW -> null
        CompareResultSection.COMPLETE_CATALOG -> report.catalogFlags.size
        CompareResultSection.NEW_FLAGS -> report.newFlags.size
        CompareResultSection.REMOVED_FLAGS -> report.removedFlags.size
        CompareResultSection.UPDATED_FLAGS -> updatedUniversalTotal
        CompareResultSection.UNCHANGED_FLAGS -> report.unchangedFlags.size
        CompareResultSection.NEW_SUBFLAGS -> report.newSubFlags.size
        CompareResultSection.REMOVED_SUBFLAGS -> report.removedSubFlags.size
        CompareResultSection.UPDATED_SUBFLAGS -> report.updatedSubFlags.size + report.parentChildChanges.size
        CompareResultSection.CATEGORIES -> activeCategoriesCount
        CompareResultSection.DUPLICATES -> duplicatesCount
        CompareResultSection.INVALID_ENTRIES -> invalidCount
        CompareResultSection.UNKNOWN_CATEGORIES -> unknownCatsCount
        CompareResultSection.MAPPING_HEALTH -> report.summary.newHealthScore
        CompareResultSection.EXPORT_REPORTS -> ExportReportType.entries.size
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Dedicated 15-Section Horizontal Strip with Counts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("compare_15_sections_strip"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompareResultSection.entries.forEach { sec ->
                val isSelected = activeSection == sec
                val count = sectionBadgeCount(sec)
                val accent = when (sec) {
                    CompareResultSection.NEW_FLAGS, CompareResultSection.NEW_SUBFLAGS -> AccentGreen
                    CompareResultSection.REMOVED_FLAGS, CompareResultSection.REMOVED_SUBFLAGS, CompareResultSection.INVALID_ENTRIES -> DangerRed
                    CompareResultSection.UPDATED_FLAGS, CompareResultSection.UPDATED_SUBFLAGS -> UpdatedOrange
                    CompareResultSection.DUPLICATES -> WarningYellow
                    CompareResultSection.UNKNOWN_CATEGORIES -> InfoBlue
                    else -> AccentGreen
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.16f) else AmoledCard)
                        .border(1.dp, if (isSelected) accent else AmoledBorder, RoundedCornerShape(8.dp))
                        .clickable { onSelectSection(sec) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("result_section_${sec.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = sec.label,
                        color = if (isSelected) accent else TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (count != null) {
                        Text(
                            text = if (sec == CompareResultSection.MAPPING_HEALTH) "$count%" else "$count",
                            color = if (isSelected) accent else TextTertiary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 2. Global Search Box + Recent Searches + Advanced Filters
        if (activeSection != CompareResultSection.EXPORT_REPORTS) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Search universal, subflag, code, prefix, or multi-keyword...",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onCommitSearchHistory(searchQuery) }) {
                                Icon(Icons.Default.History, contentDescription = "Save search", tint = AccentGreen, modifier = Modifier.size(17.dp))
                            }
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onCommitSearchHistory(searchQuery) }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmoledCard,
                    unfocusedContainerColor = AmoledCard,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = AmoledBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compare_search_input")
            )

            // Clickable Search History Pills
            if (compareSearchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent:", color = TextTertiary, fontSize = 10.sp)
                    compareSearchHistory.take(8).forEach { hist ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmoledCard)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                                .clickable { onSearchQueryChanged(hist) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = hist,
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    IconButton(
                        onClick = onClearSearchHistory,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear search history",
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Advanced Filters Bar (Favorites, 1-File Only, Exact Subflag Count, Namespace Prefix)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterToggleChip(
                    label = "★ Favorites (${favoriteFlagNames.size})",
                    selected = favoritesOnly,
                    accent = WarningYellow,
                    onClick = { onToggleFavoritesOnly(!favoritesOnly) }
                )

                FilterToggleChip(
                    label = "Only 1 Mapping",
                    selected = onlyInOneMapping,
                    accent = InfoBlue,
                    onClick = { onToggleOnlyInOneMapping(!onlyInOneMapping) }
                )

                val exactSubLabel = if (exactSubflagCount == null) "Exact Subs: Any" else "Exact Subs: $exactSubflagCount"
                FilterToggleChip(
                    label = exactSubLabel,
                    selected = exactSubflagCount != null,
                    accent = AccentGreen,
                    onClick = {
                        val next = when (exactSubflagCount) {
                            null -> 0
                            0 -> 1
                            1 -> 2
                            2 -> 3
                            3 -> 5
                            else -> null
                        }
                        onSelectExactSubflagCount(next)
                    }
                )

                Box {
                    FilterToggleChip(
                        label = "Prefix: $namespaceFilter",
                        selected = namespaceFilter != "All",
                        accent = AccentGreen,
                        onClick = { prefixMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = prefixMenuExpanded,
                        onDismissRequest = { prefixMenuExpanded = false },
                        modifier = Modifier
                            .background(AmoledCard)
                            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Prefixes", color = if (namespaceFilter == "All") AccentGreen else TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                onSelectNamespaceFilter("All")
                                prefixMenuExpanded = false
                            }
                        )
                        report.namespaceStats.take(15).forEach { ns ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${ns.prefix} (${ns.newCount})${if (ns.isThreadsP92) " [P92]" else ""}",
                                        color = if (namespaceFilter == ns.prefix) AccentGreen else TextPrimary,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    onSelectNamespaceFilter(ns.prefix)
                                    prefixMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Multi-Select Bulk Action Bar
        if (selectedResultKeys.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentGreenContainer)
                    .border(1.dp, AccentGreen, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("bulk_selection_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedResultKeys.size} selected",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Copy",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmoledBackground)
                            .clickable(onClick = onBulkCopySelected)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Export",
                        color = AccentGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmoledBackground)
                            .clickable { onOpenExportDialog(ExportReportType.COMPLETE_CATALOG, null) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Clear",
                        color = DangerRed,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmoledBackground)
                            .clickable(onClick = onClearResultSelection)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 4. Dedicated Section Body
        when (activeSection) {
            CompareResultSection.OVERVIEW -> {
                ComparisonDashboard(
                    summary = report.summary,
                    selectedFilter = CompareFilterTab.ALL,
                    onFilterSelected = {},
                    onQuickFilterSelected = onQuickFilterSelected,
                    onOpenHealthTab = { onSelectSection(CompareResultSection.MAPPING_HEALTH) },
                    onNavigateSection = onSelectSection,
                    onRefreshReindex = onRefreshReindex,
                    modifier = Modifier.testTag("comparison_dashboard")
                )

                if (multiFileReport != null) {
                    MultiFileMatrixSection(
                        multiReport = multiFileReport,
                        searchQuery = searchQuery,
                        onCopy = onCopy,
                        onExportMatrix = { onOpenExportDialog(ExportReportType.MULTI_FILE_MATRIX_REPORT, null) }
                    )
                }

                if (sessionDiffReport != null) {
                    SessionDiffSummaryCard(
                        sessionDiff = sessionDiffReport,
                        onInspectFlagByName = onOpenFlagDetailByName,
                        onCopy = onCopy
                    )
                }

                if (report.parentChildChanges.isNotEmpty()) {
                    Text(
                        text = "PARENT-CHILD RELATIONSHIP MOVEMENTS (${report.parentChildChanges.size})",
                        color = UpdatedOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    report.parentChildChanges.take(8).forEach { pch ->
                        ParentChildChangeCard(
                            item = pch,
                            onCopySubFlagName = { onCopy(pch.subFlagName, "Subflag Name") },
                            onInspectOldParent = { onOpenFlagDetailByName(pch.oldParentFlagName) },
                            onInspectNewParent = { onOpenFlagDetailByName(pch.newParentFlagName) },
                            onShare = { onShare("${pch.subFlagName}: ${pch.oldParentFlagName} -> ${pch.newParentFlagName}", "Share Parent Move") }
                        )
                    }
                }

                MappingHistorySection(
                    history = compareHistory,
                    onLoadRecord = onLoadHistoryRecord,
                    onDeleteRecord = onDeleteHistoryRecord,
                    onClearAll = onClearAllHistory
                )
            }

            CompareResultSection.COMPLETE_CATALOG -> {
                MappingCatalogSection(
                    report = report,
                    searchQuery = searchQuery,
                    quickFilter = quickFilter,
                    selectedCategory = catalogCategory,
                    selectedCategories = selectedCatalogCategories,
                    sortOption = catalogSort,
                    scopeFilter = catalogScope,
                    onCategorySelected = onSelectCatalogCategory,
                    onSortSelected = onSelectCatalogSort,
                    onScopeSelected = onSelectCatalogScope,
                    onOpenDetail = onOpenFlagDetail,
                    onCopy = onCopy,
                    namespaceFilter = namespaceFilter,
                    exactSubflagCount = exactSubflagCount,
                    onlyInOneMapping = onlyInOneMapping,
                    favoritesOnly = favoritesOnly,
                    favoriteFlagNames = favoriteFlagNames,
                    onToggleFavorite = onToggleFavoriteFlag,
                    displayMode = catalogDisplayMode,
                    onDisplayModeChanged = onSelectCatalogDisplayMode,
                    expandAllSubflags = expandAllCategories,
                    onToggleExpandAll = onToggleExpandAllCategories,
                    selectedKeys = selectedResultKeys,
                    onToggleSelection = onToggleResultSelection,
                    onOpenMultiCategoryDialog = onOpenMultiCategoryDialog,
                    searchIndex = searchIndex
                )
            }

            CompareResultSection.NEW_FLAGS -> {
                PaginatedSectionList(
                    items = report.newFlags.filter {
                        passesFlagFilters(it.flag.name, it.flag.category, it.flag.subFlags.size, true, favoritesOnly, favoriteFlagNames, namespaceFilter, exactSubflagCount, onlyInOneMapping, searchQuery)
                    },
                    emptyText = "No new universal flags match current filters"
                ) { item ->
                    NewUniversalFlagCard(
                        item = item,
                        onCopyName = { onCopy(item.flag.name, "Universal Flag Name") },
                        onCopyCode = { onCopy(item.flag.code, "Universal Flag Code") },
                        onShare = { onShare("${item.flag.name} (${item.flag.code})", "Share New Flag") },
                        onInspect = { onOpenFlagDetailByName(item.flag.name) }
                    )
                }
            }

            CompareResultSection.REMOVED_FLAGS -> {
                PaginatedSectionList(
                    items = report.removedFlags.filter {
                        passesFlagFilters(it.flag.name, it.flag.category, it.flag.subFlags.size, true, favoritesOnly, favoriteFlagNames, namespaceFilter, exactSubflagCount, onlyInOneMapping, searchQuery)
                    },
                    emptyText = "No removed universal flags match current filters"
                ) { item ->
                    RemovedUniversalFlagCard(
                        item = item,
                        onCopyName = { onCopy(item.flag.name, "Removed Flag Name") },
                        onCopyCode = { onCopy(item.flag.code, "Removed Flag Code") },
                        onShare = { onShare("${item.flag.name} (${item.flag.code})", "Share Removed Flag") },
                        onInspect = { onOpenFlagDetailByName(item.flag.name) }
                    )
                }
            }

            CompareResultSection.UPDATED_FLAGS -> {
                val updatedCatalogFlags = remember(report.catalogFlags, searchQuery, favoritesOnly, favoriteFlagNames, namespaceFilter, exactSubflagCount, onlyInOneMapping) {
                    report.catalogFlags.filter {
                        it.status == FlagChangeStatus.UPDATED &&
                            passesFlagFilters(it.name, it.category, it.totalDistinctSubFlags, it.isOnlyInOneMapping, favoritesOnly, favoriteFlagNames, namespaceFilter, exactSubflagCount, onlyInOneMapping, searchQuery)
                    }
                }
                val codeChanges = remember(report.updatedFlagCodes, searchQuery, favoritesOnly, favoriteFlagNames, namespaceFilter) {
                    report.updatedFlagCodes.filter {
                        passesFlagFilters(it.flagName, it.category, 0, false, favoritesOnly, favoriteFlagNames, namespaceFilter, null, false, searchQuery)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    codeChanges.take(30).forEach { item ->
                        UpdatedUniversalFlagCodeCard(
                            item = item,
                            onCopyFlagName = { onCopy(item.flagName, "Universal Flag Name") },
                            onCopyOldCode = { onCopy(item.oldCode, "Old Code") },
                            onCopyNewCode = { onCopy(item.newCode, "New Code") },
                            onShare = { onShare("${item.flagName}: ${item.oldCode} -> ${item.newCode}", "Share Code Change") },
                            onInspect = { onOpenFlagDetailByName(item.flagName) }
                        )
                    }
                    PaginatedSectionList(
                        items = updatedCatalogFlags,
                        emptyText = "No updated universal flags match current filters"
                    ) { item ->
                        CatalogUniversalCard(
                            item = item,
                            showExpandSubFlags = true,
                            forceExpanded = expandAllCategories,
                            isCompact = catalogDisplayMode == CatalogDisplayMode.COMPACT,
                            isFavorite = favoriteFlagNames.contains(item.name),
                            isSelected = selectedResultKeys.contains(item.name),
                            onToggleFavorite = { onToggleFavoriteFlag(item.name) },
                            onToggleSelection = { onToggleResultSelection(item.name) },
                            onOpenDetail = { onOpenFlagDetail(item) },
                            onCopy = onCopy
                        )
                    }
                }
            }

            CompareResultSection.UNCHANGED_FLAGS -> {
                PaginatedSectionList(
                    items = report.unchangedFlags.filter {
                        passesFlagFilters(it.flag.name, it.flag.category, it.flag.subFlags.size, false, favoritesOnly, favoriteFlagNames, namespaceFilter, exactSubflagCount, onlyInOneMapping, searchQuery)
                    },
                    emptyText = "No unchanged flags match current filters"
                ) { item ->
                    UnchangedUniversalFlagCard(
                        item = item,
                        onCopyName = { onCopy(item.flag.name, "Universal Flag Name") },
                        onCopyCode = { onCopy(item.flag.code, "Universal Flag Code") },
                        onInspect = { onOpenFlagDetailByName(item.flag.name) }
                    )
                }
            }

            CompareResultSection.NEW_SUBFLAGS -> {
                PaginatedSectionList(
                    items = report.newSubFlags.filter {
                        passesFlagFilters(it.subFlag.name, it.category, 1, true, favoritesOnly, favoriteFlagNames, namespaceFilter, null, false, searchQuery, it.parentFlagName, it.subFlag.code)
                    },
                    emptyText = "No new subflags match current filters"
                ) { item ->
                    NewSubFlagCard(
                        item = item,
                        onCopySubFlagName = { onCopy(item.subFlag.name, "Subflag Name") },
                        onCopySubFlagCode = { onCopy(item.subFlag.code, "Subflag Code") },
                        onShare = { onShare("${item.parentFlagName} -> ${item.subFlag.name} (${item.subFlag.code})", "Share New Subflag") },
                        onInspect = { onOpenFlagDetailByName(item.parentFlagName) }
                    )
                }
            }

            CompareResultSection.REMOVED_SUBFLAGS -> {
                PaginatedSectionList(
                    items = report.removedSubFlags.filter {
                        passesFlagFilters(it.subFlag.name, it.category, 1, true, favoritesOnly, favoriteFlagNames, namespaceFilter, null, false, searchQuery, it.parentFlagName, it.subFlag.code)
                    },
                    emptyText = "No removed subflags match current filters"
                ) { item ->
                    RemovedSubFlagCard(
                        item = item,
                        onCopySubFlagName = { onCopy(item.subFlag.name, "Removed Subflag Name") },
                        onCopySubFlagCode = { onCopy(item.subFlag.code, "Removed Subflag Code") },
                        onShare = { onShare("${item.parentFlagName} -> ${item.subFlag.name} (${item.subFlag.code})", "Share Removed Subflag") },
                        onInspect = { onOpenFlagDetailByName(item.parentFlagName) }
                    )
                }
            }

            CompareResultSection.UPDATED_SUBFLAGS -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val filteredParentMoves = report.parentChildChanges.filter {
                        MappingCompareEngine.matchesQuery(searchQuery, it.subFlagName, it.oldParentFlagName, it.newParentFlagName, it.category)
                    }
                    if (filteredParentMoves.isNotEmpty()) {
                        Text(
                            text = "PARENT-CHILD MOVEMENTS (${filteredParentMoves.size})",
                            color = UpdatedOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        filteredParentMoves.take(40).forEach { pch ->
                            ParentChildChangeCard(
                                item = pch,
                                onCopySubFlagName = { onCopy(pch.subFlagName, "Subflag Name") },
                                onInspectOldParent = { onOpenFlagDetailByName(pch.oldParentFlagName) },
                                onInspectNewParent = { onOpenFlagDetailByName(pch.newParentFlagName) },
                                onShare = { onShare("${pch.subFlagName}: ${pch.oldParentFlagName} -> ${pch.newParentFlagName}", "Share Parent Move") }
                            )
                        }
                    }

                    PaginatedSectionList(
                        items = report.updatedSubFlags.filter {
                            passesFlagFilters(it.subFlagName, it.category, 1, false, favoritesOnly, favoriteFlagNames, namespaceFilter, null, false, searchQuery, it.parentFlagName, it.newCode)
                        },
                        emptyText = "No updated subflags match current filters"
                    ) { item ->
                        UpdatedSubFlagCard(
                            item = item,
                            onCopySubFlagName = { onCopy(item.subFlagName, "Subflag Name") },
                            onCopyOldCode = { onCopy(item.oldCode, "Old Code") },
                            onCopyNewCode = { onCopy(item.newCode, "New Code") },
                            onShare = { onShare("${item.parentFlagName} -> ${item.subFlagName}: ${item.oldCode} -> ${item.newCode}", "Share Updated Subflag") },
                            onInspect = { onOpenFlagDetailByName(item.parentFlagName) }
                        )
                    }
                }
            }

            CompareResultSection.CATEGORIES -> {
                MappingAnalyticsSection(
                    report = report,
                    onSelectCategoryInCatalog = { cat ->
                        onSelectCatalogCategory(cat)
                        onSelectSection(CompareResultSection.COMPLETE_CATALOG)
                    },
                    onOpenFlagDetail = onOpenFlagDetail,
                    onCopyCategoryFlags = onCopyAllInCategory,
                    onExportCategory = { cat ->
                        onOpenExportDialog(ExportReportType.SINGLE_CATEGORY_EXPORT, cat)
                    },
                    onFilterByNamespace = { prefix ->
                        onSelectNamespaceFilter(prefix)
                        onSelectSection(CompareResultSection.COMPLETE_CATALOG)
                    }
                )
            }

            CompareResultSection.DUPLICATES -> {
                DuplicatesDiagnosticsView(
                    report = report,
                    onInspectFlagByName = onOpenFlagDetailByName,
                    onCopy = onCopy
                )
            }

            CompareResultSection.INVALID_ENTRIES -> {
                InvalidEntriesDiagnosticsView(
                    report = report,
                    onInspectFlagByName = onOpenFlagDetailByName,
                    onCopy = onCopy
                )
            }

            CompareResultSection.UNKNOWN_CATEGORIES -> {
                UnknownCategoriesDiagnosticsView(
                    report = report,
                    onInspectFlagByName = onOpenFlagDetailByName,
                    onCopy = onCopy
                )
            }

            CompareResultSection.MAPPING_HEALTH -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MappingAnalyticsSection(
                        report = report,
                        onSelectCategoryInCatalog = { cat ->
                            onSelectCatalogCategory(cat)
                            onSelectSection(CompareResultSection.COMPLETE_CATALOG)
                        },
                        onOpenFlagDetail = onOpenFlagDetail,
                        onCopyCategoryFlags = onCopyAllInCategory,
                        onExportCategory = { cat ->
                            onOpenExportDialog(ExportReportType.SINGLE_CATEGORY_EXPORT, cat)
                        },
                        onFilterByNamespace = { prefix ->
                            onSelectNamespaceFilter(prefix)
                            onSelectSection(CompareResultSection.COMPLETE_CATALOG)
                        }
                    )
                    MappingHealthDiagnosticsSection(
                        report = report,
                        onInspectFlagByName = onOpenFlagDetailByName,
                        onCopy = onCopy
                    )
                }
            }

            CompareResultSection.EXPORT_REPORTS -> {
                ExportReportsHub(
                    catalogCategory = catalogCategory,
                    selectedCount = selectedResultKeys.size,
                    hasMultiReport = multiFileReport != null,
                    onOpenExportDialog = onOpenExportDialog,
                    onDirectSave = onDirectSaveReport,
                    onDirectCopy = onDirectCopyReport,
                    onDirectShare = onDirectShareReport
                )
            }
        }
    }
}

@Composable
private fun ExportReportsHub(
    catalogCategory: String,
    selectedCount: Int,
    hasMultiReport: Boolean,
    onOpenExportDialog: (ExportReportType, String?) -> Unit,
    onDirectSave: (ExportReportType, ExportFileFormat, String?, Boolean) -> Unit,
    onDirectCopy: (ExportReportType, ExportFileFormat, String?) -> Unit,
    onDirectShare: (ExportReportType, ExportFileFormat, String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onOpenExportDialog(ExportReportType.COMPLETE_COMPARISON, null) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    .testTag("button_export_report")
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Custom Save As", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (selectedCount > 0) {
                OutlinedButton(
                    onClick = { onDirectSave(ExportReportType.COMPLETE_CATALOG, ExportFileFormat.JSON, null, true) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningYellow),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, WarningYellow.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Selected ($selectedCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        ExportReportType.entries.forEach { type ->
            if (type == ExportReportType.MULTI_FILE_MATRIX_REPORT && !hasMultiReport) return@forEach
            val catOverride = if (type == ExportReportType.SINGLE_CATEGORY_EXPORT) catalogCategory else null
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = type.title + if (catOverride != null) " ($catOverride)" else "",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = type.description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallExportAction("JSON", AccentGreen) {
                        onDirectSave(type, ExportFileFormat.JSON, catOverride, false)
                    }
                    SmallExportAction("TXT", InfoBlue) {
                        onDirectSave(type, ExportFileFormat.TXT, catOverride, false)
                    }
                    SmallExportAction("Copy", TextPrimary) {
                        onDirectCopy(type, ExportFileFormat.TXT, catOverride)
                    }
                    SmallExportAction("Share", TextSecondary) {
                        onDirectShare(type, ExportFileFormat.TXT, catOverride)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallExportAction(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilterToggleChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else AmoledCard)
            .border(1.dp, if (selected) accent else AmoledBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) accent else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun <T> PaginatedSectionList(
    items: List<T>,
    emptyText: String,
    itemContent: @Composable (T) -> Unit
) {
    var visibleLimit by remember(items.size) { mutableIntStateOf(60) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emptyText, color = TextTertiary, fontSize = 13.sp)
            }
        } else {
            Text(
                text = "Showing ${minOf(visibleLimit, items.size)} of ${items.size} entries",
                color = TextTertiary,
                fontSize = 11.sp
            )
            items.take(visibleLimit).forEach { itemContent(it) }
            if (items.size > visibleLimit) {
                OutlinedButton(
                    onClick = { visibleLimit += 100 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Load More (+${minOf(100, items.size - visibleLimit)} of ${items.size - visibleLimit} remaining)",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun passesFlagFilters(
    name: String,
    category: String,
    subFlagCount: Int,
    isOnlyInOne: Boolean,
    favoritesOnly: Boolean,
    favoriteNames: Set<String>,
    namespaceFilter: String,
    exactSubflagCount: Int?,
    onlyInOneMapping: Boolean,
    query: String,
    extra1: String = "",
    extra2: String = ""
): Boolean {
    if (favoritesOnly && !favoriteNames.contains(name) && !favoriteNames.contains(extra1)) return false
    if (onlyInOneMapping && !isOnlyInOne) return false
    if (exactSubflagCount != null && subFlagCount != exactSubflagCount) return false
    if (namespaceFilter != "All") {
        val target = extra1.ifEmpty { name }
        if (!MappingIntelligenceAnalyzer.extractNamespacePrefix(target).equals(namespaceFilter, ignoreCase = true)) {
            return false
        }
    }
    if (query.isBlank()) return true
    return MappingCompareEngine.matchesQuery(query, name, category, extra1, extra2)
}

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.MappingCompareEngine
import com.example.model.CompareFilterTab
import com.example.model.CompareQuickFilter
import com.example.model.CompareSectionTab
import com.example.model.ComparisonReport
import com.example.model.ExportFileFormat
import com.example.model.ExportReportType
import com.example.ui.components.ComparisonDashboard
import com.example.ui.components.FlagDetailInspectorDialog
import com.example.ui.components.MappingAnalyticsSection
import com.example.ui.components.MappingCatalogSection
import com.example.ui.components.MappingExportDialog
import com.example.ui.components.MappingHealthDiagnosticsSection
import com.example.ui.components.MappingHistorySection
import com.example.ui.components.MappingSourceSelectorCard
import com.example.ui.components.NewSubFlagCard
import com.example.ui.components.NewUniversalFlagCard
import com.example.ui.components.PasteMappingJsonDialog
import com.example.ui.components.RemovedSubFlagCard
import com.example.ui.components.RemovedUniversalFlagCard
import com.example.ui.components.UnchangedUniversalFlagCard
import com.example.ui.components.UpdatedSubFlagCard
import com.example.ui.components.UpdatedUniversalFlagCodeCard
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledCardHover
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UpdatedOrange
import com.example.ui.theme.WarningYellow
import com.example.viewmodel.AppTab
import com.example.viewmodel.MetaToolsViewModel

enum class ActiveToolTab {
    MAPPING_COMPARE,
    CODE_TO_NAME_CONVERTER,
    FLAG_INFORMATION,
    FLAG_ANALYSIS,
    IBACKUP_CONVERTER
}

@Composable
fun ToolsScreen(
    viewModel: MetaToolsViewModel,
    onPickOldFile: () -> Unit,
    onPickNewFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeToolTab by remember { mutableStateOf(ActiveToolTab.MAPPING_COMPARE) }

    if (activeToolTab == ActiveToolTab.CODE_TO_NAME_CONVERTER) {
        JsonCodeToNameConverterScreen(
            viewModel = viewModel,
            onBack = { activeToolTab = ActiveToolTab.MAPPING_COMPARE },
            modifier = modifier
        )
        return
    }

    if (activeToolTab == ActiveToolTab.FLAG_INFORMATION) {
        FlagInformationScreen(
            onBack = { activeToolTab = ActiveToolTab.MAPPING_COMPARE },
            modifier = modifier
        )
        return
    }

    if (activeToolTab == ActiveToolTab.IBACKUP_CONVERTER) {
        IBackupConverterScreen(
            onBack = { activeToolTab = ActiveToolTab.MAPPING_COMPARE },
            modifier = modifier
        )
        return
    }

    if (activeToolTab == ActiveToolTab.FLAG_ANALYSIS) {
        FlagAnalysisScreen(
            viewModel = viewModel,
            onBack = { activeToolTab = ActiveToolTab.MAPPING_COMPARE },
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()

    val availableSources by viewModel.availableCompareSources.collectAsStateWithLifecycle()
    val oldSource by viewModel.oldCompareSource.collectAsStateWithLifecycle()
    val newSource by viewModel.newCompareSource.collectAsStateWithLifecycle()
    val oldSourcePreview by viewModel.oldSourcePreview.collectAsStateWithLifecycle()
    val newSourcePreview by viewModel.newSourcePreview.collectAsStateWithLifecycle()
    val isComparing by viewModel.isComparing.collectAsStateWithLifecycle()
    val report by viewModel.comparisonReport.collectAsStateWithLifecycle()
    val sectionTab by viewModel.compareSectionTab.collectAsStateWithLifecycle()
    val filterTab by viewModel.compareFilterTab.collectAsStateWithLifecycle()
    val quickFilter by viewModel.compareQuickFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.compareSearchQuery.collectAsStateWithLifecycle()
    val catalogCategory by viewModel.catalogCategoryFilter.collectAsStateWithLifecycle()
    val catalogSort by viewModel.catalogSortOption.collectAsStateWithLifecycle()
    val catalogScope by viewModel.catalogScopeFilter.collectAsStateWithLifecycle()
    val selectedFlagDetail by viewModel.selectedCatalogFlagDetail.collectAsStateWithLifecycle()
    val compareHistory by viewModel.compareHistory.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var pasteTargetIsOld by remember { mutableStateOf<Boolean?>(null) }
    var pendingExportType by remember { mutableStateOf(ExportReportType.COMPLETE_COMPARISON) }
    var pendingExportFormat by remember { mutableStateOf(ExportFileFormat.JSON) }

    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val currentReport = report
        if (uri != null && currentReport != null) {
            viewModel.saveExportToUri(uri, currentReport, pendingExportType, pendingExportFormat)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAvailableCompareSources()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Tools",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mapping comparisons and MetaConfig inspection tools",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (report != null) {
                IconButton(
                    onClick = { viewModel.clearComparison() },
                    modifier = Modifier.testTag("button_clear_comparison")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear comparison",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Tool Mode Switcher Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolSwitcherTab(
                title = "Compare",
                icon = Icons.Default.CompareArrows,
                isSelected = activeToolTab == ActiveToolTab.MAPPING_COMPARE,
                onClick = { activeToolTab = ActiveToolTab.MAPPING_COMPARE },
                modifier = Modifier.weight(1f),
                testTag = "tab_mapping_compare"
            )
            ToolSwitcherTab(
                title = "Code➔Name",
                icon = Icons.Default.AutoFixHigh,
                isSelected = activeToolTab == ActiveToolTab.CODE_TO_NAME_CONVERTER,
                onClick = { activeToolTab = ActiveToolTab.CODE_TO_NAME_CONVERTER },
                modifier = Modifier.weight(1.1f),
                testTag = "tab_code_to_name"
            )
            ToolSwitcherTab(
                title = "Flag Info",
                icon = Icons.Default.Info,
                isSelected = activeToolTab == ActiveToolTab.FLAG_INFORMATION,
                onClick = { activeToolTab = ActiveToolTab.FLAG_INFORMATION },
                modifier = Modifier.weight(1f),
                testTag = "tab_flag_information"
            )
            ToolSwitcherTab(
                title = "Analysis",
                icon = Icons.Default.Search,
                isSelected = activeToolTab == ActiveToolTab.FLAG_ANALYSIS,
                onClick = { activeToolTab = ActiveToolTab.FLAG_ANALYSIS },
                modifier = Modifier.weight(1f),
                testTag = "tab_flag_analysis"
            )
            ToolSwitcherTab(
                title = "Converter",
                icon = Icons.Default.SwapVert,
                isSelected = activeToolTab == ActiveToolTab.IBACKUP_CONVERTER,
                onClick = { activeToolTab = ActiveToolTab.IBACKUP_CONVERTER },
                modifier = Modifier.weight(1f),
                testTag = "tab_ibackup_converter"
            )
        }

        // Quick Launch Card for JSON Code to Name Converter (New)
        Card(
            onClick = { activeToolTab = ActiveToolTab.CODE_TO_NAME_CONVERTER },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .testTag("card_open_code_to_name"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JSON Code to Name Converter",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Convert numeric-code JSON backups into readable name-based JSON backups with coverage analysis",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Launch Card for Flag Information (New)
        Card(
            onClick = { activeToolTab = ActiveToolTab.FLAG_INFORMATION },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                .testTag("card_open_flag_information"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Flag Information",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Paste raw flag info, separate into editable cards, copy fields individually, or copy all",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Launch Card for Flag Analysis
        Card(
            onClick = { activeToolTab = ActiveToolTab.FLAG_ANALYSIS },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                .testTag("card_open_flag_analysis"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Flag Analysis",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Import iBackup/JSON, resolve numeric codes with database, toggle flags, and export",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Launch Card for iBackup Converter
        Card(
            onClick = { activeToolTab = ActiveToolTab.IBACKUP_CONVERTER },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                .testTag("card_open_ibackup_converter"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "iBackup ↔ JSON Converter",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Convert Instagram iBackup to JSON or package JSON into .ibackup",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Section: Mapping File Compare & Analyzer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MAPPING FILE COMPARE & ANALYZER",
                color = AccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            if (compareHistory.isNotEmpty() && report == null) {
                Text(
                    text = "History (${compareHistory.size})",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { viewModel.loadHistorySession(compareHistory.first()) }
                )
            }
        }

        // Compare Setup Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Old Mapping Selector
            MappingSourceSelectorCard(
                title = "Old Mapping (Baseline)",
                selectedSource = oldSource,
                previewInfo = oldSourcePreview,
                availableSources = availableSources,
                onSelectSource = { viewModel.setOldCompareSource(it) },
                onPickStorageFile = onPickOldFile,
                onOpenPasteDialog = { pasteTargetIsOld = true },
                testTagPrefix = "old_mapping"
            )

            // Swap button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.swapCompareSources() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AmoledCardHover)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(18.dp))
                        .testTag("button_swap_sources")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap Old and New Mapping",
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. New Mapping Selector
            MappingSourceSelectorCard(
                title = "New Mapping (Target)",
                selectedSource = newSource,
                previewInfo = newSourcePreview,
                availableSources = availableSources,
                onSelectSource = { viewModel.setNewCompareSource(it) },
                onPickStorageFile = onPickNewFile,
                onOpenPasteDialog = { pasteTargetIsOld = false },
                testTagPrefix = "new_mapping"
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Prominent Compare Files Button
            Button(
                onClick = { viewModel.runComparison() },
                enabled = !isComparing && newSource != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = AmoledCardHover,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_compare_files")
            ) {
                if (isComparing) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Analyzing & Comparing...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Compare Files",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 3. Expanded Comparison Results, Catalog, Analytics, Health & History
        if (report != null) {
            val currentReport = report!!

            // Expanded Dashboard
            ComparisonDashboard(
                summary = currentReport.summary,
                selectedFilter = filterTab,
                onFilterSelected = { tab ->
                    viewModel.setCompareSectionTab(CompareSectionTab.CHANGES)
                    viewModel.setCompareFilterTab(tab)
                },
                onQuickFilterSelected = { qf ->
                    viewModel.setCompareSectionTab(CompareSectionTab.CATALOG)
                    viewModel.setCompareQuickFilter(qf)
                },
                onOpenHealthTab = {
                    viewModel.setCompareSectionTab(CompareSectionTab.HEALTH)
                },
                modifier = Modifier.testTag("comparison_dashboard")
            )

            // Workspace Section Tabs (Changes | Catalog | Analytics | Health | History)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionWorkspaceChip(
                    label = "Changes",
                    icon = Icons.Default.CompareArrows,
                    selected = sectionTab == CompareSectionTab.CHANGES,
                    onClick = { viewModel.setCompareSectionTab(CompareSectionTab.CHANGES) },
                    testTag = "section_tab_changes"
                )
                SectionWorkspaceChip(
                    label = "Catalog (${currentReport.catalogFlags.size})",
                    icon = Icons.Default.ViewList,
                    selected = sectionTab == CompareSectionTab.CATALOG,
                    onClick = { viewModel.setCompareSectionTab(CompareSectionTab.CATALOG) },
                    testTag = "section_tab_catalog"
                )
                SectionWorkspaceChip(
                    label = "Analytics",
                    icon = Icons.Default.Analytics,
                    selected = sectionTab == CompareSectionTab.ANALYTICS,
                    onClick = { viewModel.setCompareSectionTab(CompareSectionTab.ANALYTICS) },
                    testTag = "section_tab_analytics"
                )
                SectionWorkspaceChip(
                    label = "Health",
                    icon = Icons.Default.HealthAndSafety,
                    selected = sectionTab == CompareSectionTab.HEALTH,
                    onClick = { viewModel.setCompareSectionTab(CompareSectionTab.HEALTH) },
                    testTag = "section_tab_health"
                )
                SectionWorkspaceChip(
                    label = "History (${compareHistory.size})",
                    icon = Icons.Default.History,
                    selected = sectionTab == CompareSectionTab.HISTORY,
                    onClick = { viewModel.setCompareSectionTab(CompareSectionTab.HISTORY) },
                    testTag = "section_tab_history"
                )
            }

            // Global Search & Action Toolbar (for Changes & Catalog)
            if (sectionTab == CompareSectionTab.CHANGES || sectionTab == CompareSectionTab.CATALOG) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onCompareSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = "Search flag, code, subflag, or category...",
                                color = TextTertiary,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearCompareSearch() }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compare_search_input")
                    )

                    // Quick Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompareQuickFilter.entries.forEach { qf ->
                            val chipAccent = when (qf) {
                                CompareQuickFilter.NEW -> AccentGreen
                                CompareQuickFilter.REMOVED -> DangerRed
                                CompareQuickFilter.UPDATED -> UpdatedOrange
                                CompareQuickFilter.UNCHANGED -> TextSecondary
                                CompareQuickFilter.DUPLICATE -> WarningYellow
                                CompareQuickFilter.INVALID -> DangerRed
                                else -> AccentGreen
                            }
                            CompareFilterChip(
                                label = qf.label,
                                isSelected = quickFilter == qf,
                                accentColor = chipAccent,
                                onClick = {
                                    if (qf == CompareQuickFilter.INVALID) {
                                        viewModel.setCompareSectionTab(CompareSectionTab.HEALTH)
                                    } else {
                                        viewModel.setCompareQuickFilter(qf)
                                    }
                                },
                                testTag = "quick_filter_${qf.name.lowercase()}"
                            )
                        }
                    }
                }
            }

            // Action Buttons: Export Report & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                        .testTag("button_export_report")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export / Save As", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.shareComparisonReport(currentReport) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                        .testTag("button_share_report")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Report", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Active Section Content
            when (sectionTab) {
                CompareSectionTab.CHANGES -> {
                    ComparisonResultsList(
                        report = currentReport,
                        filterTab = filterTab,
                        quickFilter = quickFilter,
                        searchQuery = searchQuery,
                        onCopy = { text, label -> viewModel.copyText(text, label) },
                        onShare = { text, title -> viewModel.shareText(text, title) },
                        onInspectFlag = { flagName -> viewModel.openFlagDetailByName(flagName) }
                    )
                }

                CompareSectionTab.CATALOG -> {
                    MappingCatalogSection(
                        report = currentReport,
                        searchQuery = searchQuery,
                        quickFilter = quickFilter,
                        selectedCategory = catalogCategory,
                        sortOption = catalogSort,
                        scopeFilter = catalogScope,
                        onCategorySelected = { viewModel.setCatalogCategoryFilter(it) },
                        onSortSelected = { viewModel.setCatalogSortOption(it) },
                        onScopeSelected = { viewModel.setCatalogScopeFilter(it) },
                        onOpenDetail = { viewModel.openFlagDetail(it) },
                        onCopy = { text, label -> viewModel.copyText(text, label) }
                    )
                }

                CompareSectionTab.ANALYTICS -> {
                    MappingAnalyticsSection(
                        report = currentReport,
                        onSelectCategoryInCatalog = { cat ->
                            viewModel.setCatalogCategoryFilter(cat)
                            viewModel.setCompareSectionTab(CompareSectionTab.CATALOG)
                        },
                        onOpenFlagDetail = { viewModel.openFlagDetail(it) }
                    )
                }

                CompareSectionTab.HEALTH -> {
                    MappingHealthDiagnosticsSection(
                        report = currentReport,
                        onInspectFlagByName = { viewModel.openFlagDetailByName(it) },
                        onCopy = { text, label -> viewModel.copyText(text, label) }
                    )
                }

                CompareSectionTab.HISTORY -> {
                    MappingHistorySection(
                        history = compareHistory,
                        onLoadRecord = { viewModel.loadHistorySession(it) },
                        onDeleteRecord = { viewModel.deleteHistorySession(it) },
                        onClearAll = { viewModel.clearAllCompareHistory() }
                    )
                }
            }
        } else if (compareHistory.isNotEmpty()) {
            MappingHistorySection(
                history = compareHistory,
                onLoadRecord = { viewModel.loadHistorySession(it) },
                onDeleteRecord = { viewModel.deleteHistorySession(it) },
                onClearAll = { viewModel.clearAllCompareHistory() }
            )
        }

        // Section: Other Utilities
        Text(
            text = "OTHER UTILITIES",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Utility: Mapping Source Selector
        UtilityItem(
            title = "Manage Mapping Source",
            subtitle = "Switch between built-in Askxtylishh.json and imported custom JSON",
            icon = {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = { viewModel.openSourceDialog() },
            testTag = "tool_manage_mapping"
        )

        // Utility: Explore Flags
        UtilityItem(
            title = "Explore MetaConfig Flags",
            subtitle = "Access the comprehensive 66-category flag hierarchy",
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flags),
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = { viewModel.selectTab(AppTab.FLAGS) },
            testTag = "tool_explore_flags"
        )

        // Utility: Settings
        UtilityItem(
            title = "MetaTools Settings",
            subtitle = "Configure search preferences and file management",
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = { viewModel.openSettings() },
            testTag = "tool_open_settings"
        )
    }

    // Export Dialog
    if (showExportDialog && report != null) {
        val currentReport = report!!
        MappingExportDialog(
            onDismiss = { showExportDialog = false },
            onSaveViaSaf = { type, format, suggestedFileName ->
                pendingExportType = type
                pendingExportFormat = format
                exportSaveLauncher.launch(suggestedFileName)
            },
            onCopyReport = { type, format ->
                val content = MappingCompareEngine.generateExportContent(currentReport, type, format)
                viewModel.copyText(content, type.title)
            },
            onShareReport = { type, format ->
                val content = MappingCompareEngine.generateExportContent(currentReport, type, format)
                viewModel.shareText(content, type.title)
            }
        )
    }

    // Paste JSON Dialog
    if (pasteTargetIsOld != null) {
        val isOld = pasteTargetIsOld!!
        PasteMappingJsonDialog(
            targetLabel = if (isOld) "Old Mapping" else "New Mapping",
            onDismiss = { pasteTargetIsOld = null },
            onApplyPastedJson = { rawJson, name ->
                if (isOld) {
                    viewModel.setOldCompareSourceFromPaste(rawJson, name)
                } else {
                    viewModel.setNewCompareSourceFromPaste(rawJson, name)
                }
                pasteTargetIsOld = null
            }
        )
    }

    // Flag Detail Inspector Dialog
    if (selectedFlagDetail != null) {
        FlagDetailInspectorDialog(
            flag = selectedFlagDetail!!,
            onDismiss = { viewModel.closeFlagDetail() },
            onCopy = { text, label -> viewModel.copyText(text, label) }
        )
    }
}

@Composable
private fun SectionWorkspaceChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AccentGreenContainer else AmoledCard)
            .border(1.dp, if (selected) AccentGreen else AmoledBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) AccentGreen else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = if (selected) AccentGreen else TextSecondary,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CompareFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    accentColor: Color = AccentGreen
) {
    val borderColor = if (isSelected) accentColor else AmoledBorder
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else AmoledCard
    val textColor = if (isSelected) accentColor else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ComparisonResultsList(
    report: ComparisonReport,
    filterTab: CompareFilterTab,
    quickFilter: CompareQuickFilter,
    searchQuery: String,
    onCopy: (String, String) -> Unit,
    onShare: (String, String) -> Unit,
    onInspectFlag: (String) -> Unit
) {
    var visibleLimit by remember(filterTab, quickFilter, searchQuery) { mutableIntStateOf(60) }

    val dupOrUnknownSet = remember(report.catalogFlags, quickFilter) {
        when (quickFilter) {
            CompareQuickFilter.DUPLICATE -> report.catalogFlags.filter { it.isDuplicateInOld || it.isDuplicateInNew }.map { it.name }.toSet()
            CompareQuickFilter.UNKNOWN_CATEGORY -> report.catalogFlags.filter { it.isUnknownCategory }.map { it.name }.toSet()
            else -> null
        }
    }

    fun passesQuickSubCheck(parentFlagName: String, hasSubs: Boolean, category: String): Boolean {
        return when (quickFilter) {
            CompareQuickFilter.HAS_SUBFLAGS -> hasSubs
            CompareQuickFilter.NO_SUBFLAGS -> !hasSubs
            CompareQuickFilter.DUPLICATE -> dupOrUnknownSet?.contains(parentFlagName) == true
            CompareQuickFilter.UNKNOWN_CATEGORY -> category == "Other"
            else -> true
        }
    }

    val filteredNewFlags = remember(report.newFlags, searchQuery, filterTab, quickFilter) {
        if (filterTab == CompareFilterTab.REMOVED || filterTab == CompareFilterTab.UPDATED || filterTab == CompareFilterTab.UNCHANGED) {
            emptyList()
        } else {
            report.newFlags.filter {
                passesQuickSubCheck(it.flag.name, it.flag.subFlags.isNotEmpty(), it.flag.category) &&
                    (MappingCompareEngine.matchesQuery(searchQuery, it.flag.name, it.flag.code, it.flag.category) ||
                        it.flag.subFlags.any { s -> MappingCompareEngine.matchesQuery(searchQuery, s.name, s.code) })
            }
        }
    }

    val filteredNewSubFlags = remember(report.newSubFlags, searchQuery, filterTab, quickFilter) {
        if (filterTab == CompareFilterTab.REMOVED || filterTab == CompareFilterTab.UPDATED || filterTab == CompareFilterTab.UNCHANGED) {
            emptyList()
        } else {
            report.newSubFlags.filter {
                passesQuickSubCheck(it.parentFlagName, true, it.category) &&
                    MappingCompareEngine.matchesQuery(
                        searchQuery,
                        it.subFlag.name,
                        it.subFlag.code,
                        it.parentFlagName,
                        it.parentFlagCode,
                        it.category
                    )
            }
        }
    }

    val filteredRemovedFlags = remember(report.removedFlags, searchQuery, filterTab, quickFilter) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.UPDATED || filterTab == CompareFilterTab.UNCHANGED) {
            emptyList()
        } else {
            report.removedFlags.filter {
                passesQuickSubCheck(it.flag.name, it.flag.subFlags.isNotEmpty(), it.flag.category) &&
                    (MappingCompareEngine.matchesQuery(searchQuery, it.flag.name, it.flag.code, it.flag.category) ||
                        it.flag.subFlags.any { s -> MappingCompareEngine.matchesQuery(searchQuery, s.name, s.code) })
            }
        }
    }

    val filteredRemovedSubFlags = remember(report.removedSubFlags, searchQuery, filterTab, quickFilter) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.UPDATED || filterTab == CompareFilterTab.UNCHANGED) {
            emptyList()
        } else {
            report.removedSubFlags.filter {
                passesQuickSubCheck(it.parentFlagName, true, it.category) &&
                    MappingCompareEngine.matchesQuery(
                        searchQuery,
                        it.subFlag.name,
                        it.subFlag.code,
                        it.parentFlagName,
                        it.parentFlagCode,
                        it.category
                    )
            }
        }
    }

    val filteredUpdatedCodes = remember(report.updatedFlagCodes, searchQuery, filterTab, quickFilter) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.REMOVED || filterTab == CompareFilterTab.UNCHANGED) {
            emptyList()
        } else {
            report.updatedFlagCodes.filter {
                passesQuickSubCheck(it.flagName, true, it.category) &&
                    MappingCompareEngine.matchesQuery(searchQuery, it.flagName, it.oldCode, it.newCode, it.category)
            }
        }
    }

    val filteredUpdatedSubFlags = remember(report.updatedSubFlags, searchQuery, filterTab, quickFilter) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.REMOVED || filterTab == CompareFilterTab.UNCHANGED) {
            emptyList()
        } else {
            report.updatedSubFlags.filter {
                passesQuickSubCheck(it.parentFlagName, true, it.category) &&
                    MappingCompareEngine.matchesQuery(
                        searchQuery,
                        it.subFlagName,
                        it.parentFlagName,
                        it.oldCode,
                        it.newCode,
                        it.category
                    )
            }
        }
    }

    val filteredUnchangedFlags = remember(report.unchangedFlags, searchQuery, filterTab, quickFilter) {
        val includeUnchanged = filterTab == CompareFilterTab.UNCHANGED ||
            (filterTab == CompareFilterTab.ALL &&
                (searchQuery.isNotBlank() ||
                    quickFilter == CompareQuickFilter.HAS_SUBFLAGS ||
                    quickFilter == CompareQuickFilter.NO_SUBFLAGS ||
                    quickFilter == CompareQuickFilter.DUPLICATE ||
                    quickFilter == CompareQuickFilter.UNKNOWN_CATEGORY))
        if (!includeUnchanged) {
            emptyList()
        } else {
            report.unchangedFlags.filter {
                passesQuickSubCheck(it.flag.name, it.flag.subFlags.isNotEmpty(), it.flag.category) &&
                    (MappingCompareEngine.matchesQuery(searchQuery, it.flag.name, it.flag.code, it.flag.category) ||
                        it.flag.subFlags.any { s -> MappingCompareEngine.matchesQuery(searchQuery, s.name, s.code) })
            }
        }
    }

    val totalVisible = filteredNewFlags.size + filteredNewSubFlags.size +
        filteredRemovedFlags.size + filteredRemovedSubFlags.size +
        filteredUpdatedCodes.size + filteredUpdatedSubFlags.size +
        filteredUnchangedFlags.size

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (totalVisible == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No changes match \"$searchQuery\"" else "No entries in this filter",
                    color = TextTertiary,
                    fontSize = 13.sp
                )
            }
        } else {
            Text(
                text = "Showing ${minOf(visibleLimit, totalVisible)} of $totalVisible items (Tap any card for Flag Detail)",
                color = TextTertiary,
                fontSize = 11.sp
            )

            var remainingBudget = visibleLimit

            val sliceNewFlags = filteredNewFlags.take(remainingBudget).also { remainingBudget -= it.size }
            val sliceNewSubs = filteredNewSubFlags.take(remainingBudget).also { remainingBudget -= it.size }
            val sliceUpdCodes = filteredUpdatedCodes.take(remainingBudget).also { remainingBudget -= it.size }
            val sliceUpdSubs = filteredUpdatedSubFlags.take(remainingBudget).also { remainingBudget -= it.size }
            val sliceRemFlags = filteredRemovedFlags.take(remainingBudget).also { remainingBudget -= it.size }
            val sliceRemSubs = filteredRemovedSubFlags.take(remainingBudget).also { remainingBudget -= it.size }
            val sliceUnchanged = filteredUnchangedFlags.take(remainingBudget)

            sliceNewFlags.forEach { item ->
                NewUniversalFlagCard(
                    item = item,
                    onCopyName = { onCopy(item.flag.name, "Universal Flag Name") },
                    onCopyCode = { onCopy(item.flag.code, "Universal Flag Code") },
                    onShare = { onShare("${item.flag.name} (${item.flag.code})", "Share New Flag") },
                    onInspect = { onInspectFlag(item.flag.name) }
                )
            }

            sliceNewSubs.forEach { item ->
                NewSubFlagCard(
                    item = item,
                    onCopySubFlagName = { onCopy(item.subFlag.name, "Sub-Flag Name") },
                    onCopySubFlagCode = { onCopy(item.subFlag.code, "Sub-Flag Code") },
                    onShare = { onShare("${item.parentFlagName} -> ${item.subFlag.name} (${item.subFlag.code})", "Share New Sub-Flag") },
                    onInspect = { onInspectFlag(item.parentFlagName) }
                )
            }

            sliceUpdCodes.forEach { item ->
                UpdatedUniversalFlagCodeCard(
                    item = item,
                    onCopyFlagName = { onCopy(item.flagName, "Universal Flag Name") },
                    onCopyOldCode = { onCopy(item.oldCode, "Old Code") },
                    onCopyNewCode = { onCopy(item.newCode, "New Code") },
                    onShare = { onShare("${item.flagName}: Code changed from ${item.oldCode} to ${item.newCode}", "Share Updated Flag Code") },
                    onInspect = { onInspectFlag(item.flagName) }
                )
            }

            sliceUpdSubs.forEach { item ->
                UpdatedSubFlagCard(
                    item = item,
                    onCopySubFlagName = { onCopy(item.subFlagName, "Sub-Flag Name") },
                    onCopyOldCode = { onCopy(item.oldCode, "Old Code") },
                    onCopyNewCode = { onCopy(item.newCode, "New Code") },
                    onShare = { onShare("${item.parentFlagName} -> ${item.subFlagName}: Code changed from ${item.oldCode} to ${item.newCode}", "Share Updated Sub-Flag") },
                    onInspect = { onInspectFlag(item.parentFlagName) }
                )
            }

            sliceRemFlags.forEach { item ->
                RemovedUniversalFlagCard(
                    item = item,
                    onCopyName = { onCopy(item.flag.name, "Removed Flag Name") },
                    onCopyCode = { onCopy(item.flag.code, "Removed Flag Code") },
                    onShare = { onShare("${item.flag.name} (${item.flag.code})", "Share Removed Flag") },
                    onInspect = { onInspectFlag(item.flag.name) }
                )
            }

            sliceRemSubs.forEach { item ->
                RemovedSubFlagCard(
                    item = item,
                    onCopySubFlagName = { onCopy(item.subFlag.name, "Removed Sub-Flag Name") },
                    onCopySubFlagCode = { onCopy(item.subFlag.code, "Removed Sub-Flag Code") },
                    onShare = { onShare("${item.parentFlagName} -> ${item.subFlag.name} (${item.subFlag.code})", "Share Removed Sub-Flag") },
                    onInspect = { onInspectFlag(item.parentFlagName) }
                )
            }

            sliceUnchanged.forEach { item ->
                UnchangedUniversalFlagCard(
                    item = item,
                    onCopyName = { onCopy(item.flag.name, "Universal Flag Name") },
                    onCopyCode = { onCopy(item.flag.code, "Universal Flag Code") },
                    onInspect = { onInspectFlag(item.flag.name) }
                )
            }

            if (totalVisible > visibleLimit) {
                OutlinedButton(
                    onClick = { visibleLimit += 100 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Load More (+${minOf(100, totalVisible - visibleLimit)} of ${totalVisible - visibleLimit} remaining)",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun UtilityItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = AmoledCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledCardHover),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ToolSwitcherTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentGreenContainer else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) AccentGreen.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) AccentGreen else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}


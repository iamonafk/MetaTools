package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CompareFilterTab
import com.example.model.CompareSource
import com.example.model.ComparisonReport
import com.example.ui.components.ComparisonDashboard
import com.example.ui.components.NewSubFlagCard
import com.example.ui.components.NewUniversalFlagCard
import com.example.ui.components.RemovedSubFlagCard
import com.example.ui.components.RemovedUniversalFlagCard
import com.example.model.UpdatedFlagCodeItem
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
import com.example.ui.theme.WarningYellow
import com.example.viewmodel.AppTab
import com.example.viewmodel.MetaToolsViewModel

enum class ActiveToolTab {
    MAPPING_COMPARE,
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
    val isComparing by viewModel.isComparing.collectAsStateWithLifecycle()
    val report by viewModel.comparisonReport.collectAsStateWithLifecycle()
    val filterTab by viewModel.compareFilterTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.compareSearchQuery.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }

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
                title = "Flag Info",
                icon = Icons.Default.Info,
                isSelected = activeToolTab == ActiveToolTab.FLAG_INFORMATION,
                onClick = { activeToolTab = ActiveToolTab.FLAG_INFORMATION },
                modifier = Modifier.weight(1.05f),
                testTag = "tab_flag_information"
            )
            ToolSwitcherTab(
                title = "Analysis",
                icon = Icons.Default.Search,
                isSelected = activeToolTab == ActiveToolTab.FLAG_ANALYSIS,
                onClick = { activeToolTab = ActiveToolTab.FLAG_ANALYSIS },
                modifier = Modifier.weight(1.05f),
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

        // Quick Launch Card for Flag Information (New)
        Card(
            onClick = { activeToolTab = ActiveToolTab.FLAG_INFORMATION },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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

        // Section: Mapping File Compare
        Text(
            text = "MAPPING FILE COMPARE",
            color = AccentGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

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
            SourceSelectorCard(
                title = "Old Mapping (Baseline)",
                selectedSource = oldSource,
                availableSources = availableSources,
                onSelectSource = { viewModel.setOldCompareSource(it) },
                onPickStorageFile = onPickOldFile,
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
            SourceSelectorCard(
                title = "New Mapping (Target)",
                selectedSource = newSource,
                availableSources = availableSources,
                onSelectSource = { viewModel.setNewCompareSource(it) },
                onPickStorageFile = onPickNewFile,
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
                        text = "Comparing Mappings...",
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

        // 3. Comparison Results Section
        if (report != null) {
            val currentReport = report!!

            // Summary Dashboard
            ComparisonDashboard(
                summary = currentReport.summary,
                selectedFilter = filterTab,
                onFilterSelected = { viewModel.setCompareFilterTab(it) },
                modifier = Modifier.testTag("comparison_dashboard")
            )

            // Results Toolbar: Search & Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search in Results
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onCompareSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Filter changes by name, code, category...",
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

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompareFilterChip(
                        label = "All (${currentReport.totalNewCount + currentReport.totalRemovedCount + currentReport.totalUpdatedCount})",
                        isSelected = filterTab == CompareFilterTab.ALL,
                        onClick = { viewModel.setCompareFilterTab(CompareFilterTab.ALL) },
                        testTag = "filter_chip_all"
                    )

                    CompareFilterChip(
                        label = "New (${currentReport.totalNewCount})",
                        isSelected = filterTab == CompareFilterTab.NEW,
                        accentColor = AccentGreen,
                        onClick = { viewModel.setCompareFilterTab(CompareFilterTab.NEW) },
                        testTag = "filter_chip_new"
                    )

                    CompareFilterChip(
                        label = "Removed (${currentReport.totalRemovedCount})",
                        isSelected = filterTab == CompareFilterTab.REMOVED,
                        accentColor = DangerRed,
                        onClick = { viewModel.setCompareFilterTab(CompareFilterTab.REMOVED) },
                        testTag = "filter_chip_removed"
                    )

                    CompareFilterChip(
                        label = "Updated (${currentReport.totalUpdatedCount})",
                        isSelected = filterTab == CompareFilterTab.UPDATED,
                        accentColor = WarningYellow,
                        onClick = { viewModel.setCompareFilterTab(CompareFilterTab.UPDATED) },
                        testTag = "filter_chip_updated"
                    )
                }

                // Action Buttons: Export & Share Full Report
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
                        Text("Export Report", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            }

            // Filtered Comparison Items List
            ComparisonResultsList(
                report = currentReport,
                filterTab = filterTab,
                searchQuery = searchQuery,
                onCopy = { text, label -> viewModel.copyText(text, label) },
                onShare = { text, title -> viewModel.shareText(text, title) }
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

    // Export Format Dialog
    if (showExportDialog && report != null) {
        val currentReport = report!!
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = "Export Comparison Report",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose a format to export or share the complete comparison results:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportComparisonReportTxt(currentReport)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                            .testTag("export_txt_button")
                    ) {
                        Text(
                            text = "Formatted Text (.txt)",
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportComparisonReportJson(currentReport)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                            .testTag("export_json_button")
                    ) {
                        Text(
                            text = "Structured JSON (.json)",
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = AmoledCard,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun SourceSelectorCard(
    title: String,
    selectedSource: CompareSource?,
    availableSources: List<CompareSource>,
    onSelectSource: (CompareSource) -> Unit,
    onPickStorageFile: () -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Selected Source Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AmoledBackground)
                .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedSource?.getLabel() ?: "Not selected (Pick file or preset)",
                    color = if (selectedSource != null) TextPrimary else TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (selectedSource != null) FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        // Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Built-in preset
            PresetChip(
                label = "Built-in",
                isSelected = selectedSource is CompareSource.BuiltIn,
                onClick = { onSelectSource(CompareSource.BuiltIn) },
                testTag = "${testTagPrefix}_select_builtin"
            )

            // Custom active mapping preset if available
            availableSources.filterIsInstance<CompareSource.ActiveCustom>().firstOrNull()?.let { custom ->
                PresetChip(
                    label = "Active Custom",
                    isSelected = selectedSource is CompareSource.ActiveCustom,
                    onClick = { onSelectSource(custom) },
                    testTag = "${testTagPrefix}_select_custom"
                )
            }

            // Pick from Device Storage via SAF
            PresetChip(
                label = "Pick File...",
                icon = Icons.Default.FolderOpen,
                isSelected = selectedSource is CompareSource.DeviceFile,
                onClick = onPickStorageFile,
                testTag = "${testTagPrefix}_pick_file"
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val borderColor = if (isSelected) AccentGreen else AmoledBorder
    val bgColor = if (isSelected) AccentGreenContainer else AmoledBackground
    val textColor = if (isSelected) AccentGreen else TextSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
    searchQuery: String,
    onCopy: (String, String) -> Unit,
    onShare: (String, String) -> Unit
) {
    val query = searchQuery.trim().lowercase()

    // Filter New Flags
    val filteredNewFlags = remember(report.newFlags, query, filterTab) {
        if (filterTab == CompareFilterTab.REMOVED || filterTab == CompareFilterTab.UPDATED) {
            emptyList()
        } else if (query.isEmpty()) {
            report.newFlags
        } else {
            report.newFlags.filter {
                it.flag.name.lowercase().contains(query) ||
                    it.flag.code.lowercase().contains(query) ||
                    it.flag.category.lowercase().contains(query)
            }
        }
    }

    // Filter New Sub-Flags
    val filteredNewSubFlags = remember(report.newSubFlags, query, filterTab) {
        if (filterTab == CompareFilterTab.REMOVED || filterTab == CompareFilterTab.UPDATED) {
            emptyList()
        } else if (query.isEmpty()) {
            report.newSubFlags
        } else {
            report.newSubFlags.filter {
                it.subFlag.name.lowercase().contains(query) ||
                    it.subFlag.code.lowercase().contains(query) ||
                    it.parentFlagName.lowercase().contains(query)
            }
        }
    }

    // Filter Removed Flags
    val filteredRemovedFlags = remember(report.removedFlags, query, filterTab) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.UPDATED) {
            emptyList()
        } else if (query.isEmpty()) {
            report.removedFlags
        } else {
            report.removedFlags.filter {
                it.flag.name.lowercase().contains(query) ||
                    it.flag.code.lowercase().contains(query) ||
                    it.flag.category.lowercase().contains(query)
            }
        }
    }

    // Filter Removed Sub-Flags
    val filteredRemovedSubFlags = remember(report.removedSubFlags, query, filterTab) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.UPDATED) {
            emptyList()
        } else if (query.isEmpty()) {
            report.removedSubFlags
        } else {
            report.removedSubFlags.filter {
                it.subFlag.name.lowercase().contains(query) ||
                    it.subFlag.code.lowercase().contains(query) ||
                    it.parentFlagName.lowercase().contains(query)
            }
        }
    }

    // Filter Updated Codes
    val filteredUpdatedCodes = remember(report.updatedFlagCodes, query, filterTab) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.REMOVED) {
            emptyList()
        } else if (query.isEmpty()) {
            report.updatedFlagCodes
        } else {
            report.updatedFlagCodes.filter {
                it.flagName.lowercase().contains(query) ||
                    it.oldCode.lowercase().contains(query) ||
                    it.newCode.lowercase().contains(query)
            }
        }
    }

    // Filter Updated Sub-Flags
    val filteredUpdatedSubFlags = remember(report.updatedSubFlags, query, filterTab) {
        if (filterTab == CompareFilterTab.NEW || filterTab == CompareFilterTab.REMOVED) {
            emptyList()
        } else if (query.isEmpty()) {
            report.updatedSubFlags
        } else {
            report.updatedSubFlags.filter {
                it.subFlagName.lowercase().contains(query) ||
                    it.parentFlagName.lowercase().contains(query) ||
                    it.oldCode.lowercase().contains(query) ||
                    it.newCode.lowercase().contains(query)
            }
        }
    }

    val totalVisible = filteredNewFlags.size + filteredNewSubFlags.size +
        filteredRemovedFlags.size + filteredRemovedSubFlags.size +
        filteredUpdatedCodes.size + filteredUpdatedSubFlags.size

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
                    text = if (query.isNotEmpty()) "No changes match \"$searchQuery\"" else "No changes in this category",
                    color = TextTertiary,
                    fontSize = 13.sp
                )
            }
        } else {
            // New Universal Flags
            filteredNewFlags.forEach { item ->
                NewUniversalFlagCard(
                    item = item,
                    onCopyName = { onCopy(item.flag.name, "Universal Flag Name") },
                    onCopyCode = { onCopy(item.flag.code, "Universal Flag Code") },
                    onShare = {
                        onShare("${item.flag.name} (${item.flag.code})", "Share New Flag")
                    }
                )
            }

            // New Sub-Flags
            filteredNewSubFlags.forEach { item ->
                NewSubFlagCard(
                    item = item,
                    onCopySubFlagName = { onCopy(item.subFlag.name, "Sub-Flag Name") },
                    onCopySubFlagCode = { onCopy(item.subFlag.code, "Sub-Flag Code") },
                    onShare = {
                        onShare(
                            "${item.parentFlagName} -> ${item.subFlag.name} (${item.subFlag.code})",
                            "Share New Sub-Flag"
                        )
                    }
                )
            }

            // Updated Flag Codes
            filteredUpdatedCodes.forEach { item ->
                UpdatedUniversalFlagCodeCard(
                    item = item,
                    onCopyFlagName = { onCopy(item.flagName, "Universal Flag Name") },
                    onCopyOldCode = { onCopy(item.oldCode, "Old Code") },
                    onCopyNewCode = { onCopy(item.newCode, "New Code") },
                    onShare = {
                        onShare(
                            "${item.flagName}: Code changed from ${item.oldCode} to ${item.newCode}",
                            "Share Updated Flag Code"
                        )
                    }
                )
            }

            // Updated Sub-Flags
            filteredUpdatedSubFlags.forEach { item ->
                UpdatedSubFlagCard(
                    item = item,
                    onCopySubFlagName = { onCopy(item.subFlagName, "Sub-Flag Name") },
                    onCopyOldCode = { onCopy(item.oldCode, "Old Code") },
                    onCopyNewCode = { onCopy(item.newCode, "New Code") },
                    onShare = {
                        onShare(
                            "${item.parentFlagName} -> ${item.subFlagName}: Code changed from ${item.oldCode} to ${item.newCode}",
                            "Share Updated Sub-Flag"
                        )
                    }
                )
            }

            // Removed Universal Flags
            filteredRemovedFlags.forEach { item ->
                RemovedUniversalFlagCard(
                    item = item,
                    onCopyName = { onCopy(item.flag.name, "Removed Flag Name") },
                    onCopyCode = { onCopy(item.flag.code, "Removed Flag Code") },
                    onShare = {
                        onShare("${item.flag.name} (${item.flag.code})", "Share Removed Flag")
                    }
                )
            }

            // Removed Sub-Flags
            filteredRemovedSubFlags.forEach { item ->
                RemovedSubFlagCard(
                    item = item,
                    onCopySubFlagName = { onCopy(item.subFlag.name, "Removed Sub-Flag Name") },
                    onCopySubFlagCode = { onCopy(item.subFlag.code, "Removed Sub-Flag Code") },
                    onShare = {
                        onShare(
                            "${item.parentFlagName} -> ${item.subFlag.name} (${item.subFlag.code})",
                            "Share Removed Sub-Flag"
                        )
                    }
                )
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


package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CompareMode
import com.example.model.ExportFileFormat
import com.example.model.ExportReportType
import com.example.ui.components.CompareModeSelector
import com.example.ui.components.FlagDetailInspectorDialog
import com.example.ui.components.MappingExportDialog
import com.example.ui.components.MappingHistorySection
import com.example.ui.components.MappingResultSectionsHub
import com.example.ui.components.MappingSourceSelectorCard
import com.example.ui.components.MultiCategoryFilterDialog
import com.example.ui.components.MultiFileCompareSetupSection
import com.example.ui.components.PasteMappingJsonDialog
import com.example.ui.components.SessionDiffSetupSection
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledCardHover
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.AppTab
import com.example.viewmodel.MetaToolsViewModel
import kotlinx.coroutines.launch

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

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val shortcutScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val availableSources by viewModel.availableCompareSources.collectAsStateWithLifecycle()
    val compareMode by viewModel.compareMode.collectAsStateWithLifecycle()
    val oldSource by viewModel.oldCompareSource.collectAsStateWithLifecycle()
    val newSource by viewModel.newCompareSource.collectAsStateWithLifecycle()
    val oldSourcePreview by viewModel.oldSourcePreview.collectAsStateWithLifecycle()
    val newSourcePreview by viewModel.newSourcePreview.collectAsStateWithLifecycle()
    val multiSlots by viewModel.multiCompareSlots.collectAsStateWithLifecycle()
    val isComparing by viewModel.isComparing.collectAsStateWithLifecycle()
    val report by viewModel.comparisonReport.collectAsStateWithLifecycle()
    val multiFileReport by viewModel.multiFileReport.collectAsStateWithLifecycle()
    val sessionDiffReport by viewModel.sessionDiffReport.collectAsStateWithLifecycle()
    val activeResultSection by viewModel.activeResultSection.collectAsStateWithLifecycle()
    val quickFilter by viewModel.compareQuickFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.compareSearchQuery.collectAsStateWithLifecycle()
    val compareSearchHistory by viewModel.compareSearchHistory.collectAsStateWithLifecycle()
    val catalogCategory by viewModel.catalogCategoryFilter.collectAsStateWithLifecycle()
    val selectedCatalogCategories by viewModel.selectedCatalogCategories.collectAsStateWithLifecycle()
    val catalogSort by viewModel.catalogSortOption.collectAsStateWithLifecycle()
    val catalogScope by viewModel.catalogScopeFilter.collectAsStateWithLifecycle()
    val namespaceFilter by viewModel.namespaceFilter.collectAsStateWithLifecycle()
    val exactSubflagCount by viewModel.exactSubflagCountFilter.collectAsStateWithLifecycle()
    val onlyInOneMapping by viewModel.onlyInOneMappingFilter.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnlyFilter.collectAsStateWithLifecycle()
    val favoriteFlagNames by viewModel.favoriteFlagNames.collectAsStateWithLifecycle()
    val catalogDisplayMode by viewModel.catalogDisplayMode.collectAsStateWithLifecycle()
    val expandAllCategories by viewModel.expandAllCatalogCategories.collectAsStateWithLifecycle()
    val selectedResultKeys by viewModel.selectedResultKeys.collectAsStateWithLifecycle()
    val searchIndex by viewModel.compareSearchIndex.collectAsStateWithLifecycle()
    val selectedFlagDetail by viewModel.selectedCatalogFlagDetail.collectAsStateWithLifecycle()
    val compareHistory by viewModel.compareHistory.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showMultiCategoryDialog by remember { mutableStateOf(false) }
    var pasteTargetIsOld by remember { mutableStateOf<Boolean?>(null) }
    var pasteMultiSlotId by remember { mutableStateOf<String?>(null) }
    var pendingMultiFileSlotId by remember { mutableStateOf<String?>(null) }
    var pendingExportType by remember { mutableStateOf(ExportReportType.COMPLETE_COMPARISON) }
    var pendingExportFormat by remember { mutableStateOf(ExportFileFormat.JSON) }
    var pendingExportCategory by remember { mutableStateOf<String?>(null) }
    var pendingExportSelectedOnly by remember { mutableStateOf(false) }

    val multiSlotFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val slotId = pendingMultiFileSlotId
        if (uri != null && slotId != null) {
            val fileName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
                }
            }.getOrNull() ?: "mapping.json"
            viewModel.setMultiSlotFromUri(slotId, uri, fileName)
        }
        pendingMultiFileSlotId = null
    }

    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val currentReport = report
        if (uri != null && currentReport != null) {
            viewModel.saveExportToUri(
                uri = uri,
                report = currentReport,
                type = pendingExportType,
                format = pendingExportFormat,
                categoryOverride = pendingExportCategory,
                selectedOnly = pendingExportSelectedOnly
            )
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

        // Tool Mode Switcher Bar (Single-line horizontally scrollable tabs, no wrapping/truncation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                .horizontalScroll(shortcutScrollState)
                .padding(4.dp)
                .testTag("tools_shortcut_bar"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolSwitcherTab(
                title = "Compare",
                icon = Icons.Default.CompareArrows,
                isSelected = activeToolTab == ActiveToolTab.MAPPING_COMPARE,
                onClick = {
                    activeToolTab = ActiveToolTab.MAPPING_COMPARE
                    coroutineScope.launch { shortcutScrollState.animateScrollTo(0) }
                },
                testTag = "tab_mapping_compare"
            )
            ToolSwitcherTab(
                title = "Code → Name",
                icon = Icons.Default.AutoFixHigh,
                isSelected = activeToolTab == ActiveToolTab.CODE_TO_NAME_CONVERTER,
                onClick = { activeToolTab = ActiveToolTab.CODE_TO_NAME_CONVERTER },
                testTag = "tab_code_to_name"
            )
            ToolSwitcherTab(
                title = "Flag Info",
                icon = Icons.Default.Info,
                isSelected = activeToolTab == ActiveToolTab.FLAG_INFORMATION,
                onClick = { activeToolTab = ActiveToolTab.FLAG_INFORMATION },
                testTag = "tab_flag_information"
            )
            ToolSwitcherTab(
                title = "Analysis",
                icon = Icons.Default.Search,
                isSelected = activeToolTab == ActiveToolTab.FLAG_ANALYSIS,
                onClick = { activeToolTab = ActiveToolTab.FLAG_ANALYSIS },
                testTag = "tab_flag_analysis"
            )
            ToolSwitcherTab(
                title = "Converter",
                icon = Icons.Default.SwapVert,
                isSelected = activeToolTab == ActiveToolTab.IBACKUP_CONVERTER,
                onClick = { activeToolTab = ActiveToolTab.IBACKUP_CONVERTER },
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
                    Text(
                        text = "JSON Code to Name Converter",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    Text(
                        text = "Flag Information",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    Text(
                        text = "Flag Analysis",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
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

        // Compare Setup Box (2-File Diff | Multi-File 3+ | Session Diff)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompareModeSelector(
                selectedMode = compareMode,
                onModeSelected = { viewModel.setCompareMode(it) }
            )

            when (compareMode) {
                CompareMode.TWO_FILE -> {
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

                CompareMode.MULTI_FILE -> {
                    MultiFileCompareSetupSection(
                        slots = multiSlots,
                        availableSources = availableSources,
                        isComparing = isComparing,
                        onSelectSlotSource = { id, src -> viewModel.setMultiSlotSource(id, src) },
                        onPickSlotFile = { id ->
                            pendingMultiFileSlotId = id
                            multiSlotFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        onPasteSlotJson = { id -> pasteMultiSlotId = id },
                        onRemoveSlot = { id -> viewModel.removeMultiCompareSlot(id) },
                        onAddSlot = { viewModel.addMultiCompareSlot() },
                        onRunMultiCompare = { viewModel.runMultiFileComparison() }
                    )
                }

                CompareMode.SESSION_DIFF -> {
                    SessionDiffSetupSection(
                        history = compareHistory,
                        isComparing = isComparing,
                        onCompareSessions = { recA, recB -> viewModel.compareTwoHistorySessions(recA, recB) }
                    )
                }
            }
        }

        // 15 Dedicated Comparison Results, Catalog, Intelligence & Export Hub
        if (report != null) {
            val currentReport = report!!
            MappingResultSectionsHub(
                report = currentReport,
                multiFileReport = multiFileReport,
                sessionDiffReport = sessionDiffReport,
                activeSection = activeResultSection,
                onSelectSection = { viewModel.setActiveResultSection(it) },
                searchQuery = searchQuery,
                onSearchQueryChanged = { viewModel.onCompareSearchQueryChanged(it) },
                onCommitSearchHistory = { viewModel.commitCompareSearchToHistory(it) },
                onClearSearch = { viewModel.clearCompareSearch() },
                compareSearchHistory = compareSearchHistory,
                onClearSearchHistory = { viewModel.clearCompareSearchHistory() },
                quickFilter = quickFilter,
                onQuickFilterSelected = { viewModel.setCompareQuickFilter(it) },
                catalogCategory = catalogCategory,
                selectedCatalogCategories = selectedCatalogCategories,
                onSelectCatalogCategory = { viewModel.setCatalogCategoryFilter(it) },
                onOpenMultiCategoryDialog = { showMultiCategoryDialog = true },
                catalogSort = catalogSort,
                onSelectCatalogSort = { viewModel.setCatalogSortOption(it) },
                catalogScope = catalogScope,
                onSelectCatalogScope = { viewModel.setCatalogScopeFilter(it) },
                namespaceFilter = namespaceFilter,
                onSelectNamespaceFilter = { viewModel.setNamespaceFilter(it) },
                exactSubflagCount = exactSubflagCount,
                onSelectExactSubflagCount = { viewModel.setExactSubflagCountFilter(it) },
                onlyInOneMapping = onlyInOneMapping,
                onToggleOnlyInOneMapping = { viewModel.setOnlyInOneMappingFilter(it) },
                favoritesOnly = favoritesOnly,
                onToggleFavoritesOnly = { viewModel.setFavoritesOnlyFilter(it) },
                favoriteFlagNames = favoriteFlagNames,
                onToggleFavoriteFlag = { viewModel.toggleFavoriteFlag(it) },
                catalogDisplayMode = catalogDisplayMode,
                onSelectCatalogDisplayMode = { viewModel.setCatalogDisplayMode(it) },
                expandAllCategories = expandAllCategories,
                onToggleExpandAllCategories = { viewModel.toggleExpandAllCatalogCategories() },
                selectedResultKeys = selectedResultKeys,
                onToggleResultSelection = { viewModel.toggleResultSelection(it) },
                onClearResultSelection = { viewModel.clearResultSelection() },
                onBulkCopySelected = { viewModel.bulkCopySelected(currentReport) },
                onOpenExportDialog = { type, cat ->
                    pendingExportType = type
                    pendingExportCategory = cat
                    pendingExportSelectedOnly = false
                    showExportDialog = true
                },
                onDirectSaveReport = { type, fmt, cat, selOnly ->
                    pendingExportType = type
                    pendingExportFormat = fmt
                    pendingExportCategory = cat
                    pendingExportSelectedOnly = selOnly
                    val name = when {
                        selOnly -> "Selected_Flags_${selectedResultKeys.size}.${fmt.extension}"
                        cat != null -> "Category_${cat.replace(" ", "_")}.${fmt.extension}"
                        else -> "${type.defaultFileName}.${fmt.extension}"
                    }
                    exportSaveLauncher.launch(name)
                },
                onDirectCopyReport = { type, fmt, cat ->
                    val content = viewModel.buildExportContentForReport(currentReport, type, fmt, cat)
                    viewModel.copyText(content, type.title)
                },
                onDirectShareReport = { type, fmt, cat ->
                    val content = viewModel.buildExportContentForReport(currentReport, type, fmt, cat)
                    viewModel.shareText(content, type.title)
                },
                onCopyAllInCategory = { cat -> viewModel.copyAllFlagsInCategory(currentReport, cat) },
                onOpenFlagDetail = { viewModel.openFlagDetail(it) },
                onOpenFlagDetailByName = { viewModel.openFlagDetailByName(it) },
                onCopy = { text, label -> viewModel.copyText(text, label) },
                onShare = { text, title -> viewModel.shareText(text, title) },
                onRefreshReindex = { viewModel.refreshAndReindexComparison() },
                searchIndex = searchIndex,
                compareHistory = compareHistory,
                onLoadHistoryRecord = { viewModel.loadHistorySession(it) },
                onDeleteHistoryRecord = { viewModel.deleteHistorySession(it) },
                onClearAllHistory = { viewModel.clearAllCompareHistory() }
            )
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
            initialType = pendingExportType,
            initialCategory = pendingExportCategory ?: catalogCategory,
            selectedEntriesCount = selectedResultKeys.size,
            onDismiss = { showExportDialog = false },
            onSaveViaSaf = { type, format, suggestedFileName, categoryName, selectedOnly ->
                pendingExportType = type
                pendingExportFormat = format
                pendingExportCategory = categoryName
                pendingExportSelectedOnly = selectedOnly
                exportSaveLauncher.launch(suggestedFileName)
            },
            onCopyReport = { type, format, categoryName, selectedOnly ->
                val content = viewModel.buildExportContentForReport(
                    report = currentReport,
                    type = type,
                    format = format,
                    categoryOverride = categoryName,
                    selectedOnly = selectedOnly
                )
                viewModel.copyText(content, type.title)
            },
            onShareReport = { type, format, categoryName, selectedOnly ->
                val content = viewModel.buildExportContentForReport(
                    report = currentReport,
                    type = type,
                    format = format,
                    categoryOverride = categoryName,
                    selectedOnly = selectedOnly
                )
                viewModel.shareText(content, type.title)
            }
        )
    }

    // Multi-Category Filter Dialog
    if (showMultiCategoryDialog) {
        MultiCategoryFilterDialog(
            selectedCategories = selectedCatalogCategories,
            onToggleCategory = { viewModel.toggleCatalogCategorySelection(it) },
            onClearAll = { viewModel.clearCatalogCategoryFilters() },
            onDismiss = { showMultiCategoryDialog = false }
        )
    }

    // Paste JSON Dialog (Old / New / Multi Slot)
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
    } else if (pasteMultiSlotId != null) {
        val targetSlotId = pasteMultiSlotId!!
        PasteMappingJsonDialog(
            targetLabel = "Mapping Slot",
            onDismiss = { pasteMultiSlotId = null },
            onApplyPastedJson = { rawJson, name ->
                viewModel.setMultiSlotFromPaste(targetSlotId, rawJson, name)
                pasteMultiSlotId = null
            }
        )
    }

    // Flag Detail Inspector Dialog
    if (selectedFlagDetail != null) {
        val flagDetail = selectedFlagDetail!!
        FlagDetailInspectorDialog(
            flag = flagDetail,
            isFavorite = favoriteFlagNames.contains(flagDetail.name),
            onToggleFavorite = { viewModel.toggleFavoriteFlag(flagDetail.name) },
            onShare = { text, title -> viewModel.shareText(text, title) },
            onDismiss = { viewModel.closeFlagDetail() },
            onCopy = { text, label -> viewModel.copyText(text, label) }
        )
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
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AccentGreenContainer else Color.Transparent,
        label = "shortcutBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentGreen.copy(alpha = 0.6f) else Color.Transparent,
        label = "shortcutBorder"
    )

    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}


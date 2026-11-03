package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FlagAnalysisEngine
import com.example.model.AnalyzedSubFlag
import com.example.model.AnalyzedUniversalFlag
import com.example.model.BackupFileType
import com.example.model.FlagAnalysisFilter
import com.example.model.ParsedBackupState
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
import com.example.viewmodel.MetaToolsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun FlagAnalysisScreen(
    viewModel: MetaToolsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val state by viewModel.flagAnalysisState.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingBackup.collectAsStateWithLifecycle()
    val searchQuery by viewModel.flagAnalysisSearchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.flagAnalysisFilter.collectAsStateWithLifecycle()

    var showCompareDialog by remember { mutableStateOf(false) }
    var showUnmappedDialog by remember { mutableStateOf(false) }
    var showCategorySummaryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFileNameInput by remember { mutableStateOf("") }
    var exportValidationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }

    // Intercept hardware/system back button
    BackHandler {
        onBack()
    }

    // SAF Import Launcher (iBackup and JSON)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val displayName = queryDisplayName(context, uri) ?: "imported_backup.json"
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        } ?: throw IllegalStateException("Could not read file from storage")
                    }
                    viewModel.loadBackupForAnalysis(content, displayName)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(pendingExportContent!!.toByteArray(Charsets.UTF_8))
                            outputStream.flush()
                        } ?: throw IllegalStateException("Could not open destination file")
                    }
                    Toast.makeText(context, "Exported backup saved successfully!", Toast.LENGTH_LONG).show()
                    showExportDialog = false
                    pendingExportContent = null
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
    ) {
        // Top App Bar
        FlagAnalysisTopBar(
            backupState = state,
            isAnalyzing = isAnalyzing,
            onBack = onBack,
            onClear = { viewModel.clearFlagAnalysis() }
        )

        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = AccentGreen)
                    Text(
                        text = "Analyzing MetaConfig backup and resolving flag names...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (state == null) {
            // Import Welcome Screen
            EmptyAnalysisImportView(
                onImportIBackup = {
                    importLauncher.launch(arrayOf("*/*", "application/json", "application/octet-stream"))
                },
                onImportJson = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
        } else {
            // Backup Loaded View
            val currentState = state!!
            val stats = remember(currentState) {
                FlagAnalysisEngine.calculateDashboardStats(currentState.flags)
            }

            // Filter flags
            val filteredFlags = remember(currentState.flags, searchQuery, activeFilter) {
                filterFlags(currentState.flags, searchQuery, activeFilter)
            }

            // Group by category
            val categoryGroups = remember(filteredFlags) {
                filteredFlags.groupBy { it.category }
            }

            // Expanded categories state
            var expandedCategorySet by remember { mutableStateOf<Set<String>>(emptySet()) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Dashboard Card
                item {
                    AnalysisDashboardCard(
                        state = currentState,
                        stats = stats,
                        onExportClick = {
                            val validationErrors = FlagAnalysisEngine.validateBackup(currentState)
                            exportValidationErrors = validationErrors
                            val exportResult = FlagAnalysisEngine.generateExportContent(currentState)
                            if (exportResult.isSuccess) {
                                pendingExportContent = exportResult.getOrThrow()
                                exportFileNameInput = FlagAnalysisEngine.getSuggestedExportFileName(currentState)
                                showExportDialog = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error preparing export: ${exportResult.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onCompareClick = { showCompareDialog = true },
                        onUnmappedClick = { showUnmappedDialog = true },
                        onCategorySummaryClick = { showCategorySummaryDialog = true },
                        onEnableAll = { viewModel.setAnalysisAllFlagsState(true) },
                        onDisableAll = { viewModel.setAnalysisAllFlagsState(false) },
                        onResetAll = { viewModel.resetAllAnalysisFlags() }
                    )
                }

                // Search & Filter Section
                item {
                    AnalysisSearchAndFilterBar(
                        searchQuery = searchQuery,
                        onQueryChanged = { viewModel.setFlagAnalysisSearchQuery(it) },
                        activeFilter = activeFilter,
                        onFilterSelected = { viewModel.setFlagAnalysisFilter(it) },
                        stats = stats
                    )
                }

                if (filteredFlags.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = AmoledCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No flags match current filter",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Try clearing your search query or selecting 'All Flags'",
                                    color = TextSecondary,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                } else {
                    // Category Accordion List
                    items(categoryGroups.keys.toList()) { categoryName ->
                        val flagsInCat = categoryGroups[categoryName] ?: emptyList()
                        val isExpanded = expandedCategorySet.contains(categoryName)
                        val catModifiedCount = flagsInCat.sumOf { f -> f.subFlags.count { it.isModified } }
                        val catEnabledCount = flagsInCat.sumOf { it.enabledSubFlags }
                        val catTotalSub = flagsInCat.sumOf { it.totalSubFlags }

                        CategoryAccordionCard(
                            categoryName = categoryName,
                            flags = flagsInCat,
                            totalSubFlags = catTotalSub,
                            enabledSubFlags = catEnabledCount,
                            modifiedCount = catModifiedCount,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedCategorySet = if (isExpanded) {
                                    expandedCategorySet - categoryName
                                } else {
                                    expandedCategorySet + categoryName
                                }
                            },
                            onEnableAllInCat = {
                                viewModel.setAnalysisCategoryState(categoryName, true)
                            },
                            onDisableAllInCat = {
                                viewModel.setAnalysisCategoryState(categoryName, false)
                            },
                            onToggleSubFlag = { uCode, sCode ->
                                viewModel.toggleAnalysisSubFlag(uCode, sCode)
                            },
                            onResetSubFlag = { uCode, sCode ->
                                viewModel.resetAnalysisSubFlag(uCode, sCode)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Compare Dialog
    if (showCompareDialog && state != null) {
        val modifiedList = remember(state) {
            FlagAnalysisEngine.getModifiedFlags(state!!.flags)
        }
        CompareChangesDialog(
            modifiedFlags = modifiedList,
            onDismiss = { showCompareDialog = false },
            onRevert = { uCode, sCode ->
                viewModel.resetAnalysisSubFlag(uCode, sCode)
            },
            onResetAll = {
                viewModel.resetAllAnalysisFlags()
                showCompareDialog = false
            }
        )
    }

    // Unmapped Flags Dialog
    if (showUnmappedDialog && state != null) {
        val unmappedList = remember(state) {
            FlagAnalysisEngine.getUnmappedFlags(state!!.flags)
        }
        UnmappedFlagsDialog(
            unmappedFlags = unmappedList,
            onDismiss = { showUnmappedDialog = false },
            onCopy = { text, label ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Category Summary Dialog
    if (showCategorySummaryDialog && state != null) {
        val summaries = remember(state) {
            FlagAnalysisEngine.getCategorySummaries(state!!.flags)
        }
        CategorySummaryDialog(
            summaries = summaries,
            onDismiss = { showCategorySummaryDialog = false }
        )
    }

    // Export Dialog
    if (showExportDialog && state != null) {
        ExportBackupDialog(
            state = state!!,
            fileNameInput = exportFileNameInput,
            validationErrors = exportValidationErrors,
            onFileNameChanged = { exportFileNameInput = it },
            onDismiss = { showExportDialog = false },
            onConfirmExport = {
                val finalName = exportFileNameInput.trim().ifEmpty {
                    FlagAnalysisEngine.getSuggestedExportFileName(state!!)
                }
                exportLauncher.launch(finalName)
            }
        )
    }
}

/**
 * Filter utility for flags based on search text and filter chip.
 */
private fun filterFlags(
    flags: List<AnalyzedUniversalFlag>,
    query: String,
    filter: FlagAnalysisFilter
): List<AnalyzedUniversalFlag> {
    val trimmed = query.trim().lowercase()

    return flags.mapNotNull { uFlag ->
        val matchesQuery = if (trimmed.isEmpty()) {
            true
        } else {
            uFlag.name.lowercase().contains(trimmed) ||
                uFlag.code.contains(trimmed) ||
                uFlag.category.lowercase().contains(trimmed) ||
                uFlag.subFlags.any {
                    it.name.lowercase().contains(trimmed) || it.subCode.contains(trimmed)
                }
        }

        if (!matchesQuery) return@mapNotNull null

        val filteredSubFlags = when (filter) {
            FlagAnalysisFilter.ALL -> uFlag.subFlags
            FlagAnalysisFilter.ENABLED -> uFlag.subFlags.filter { it.isEnabled }
            FlagAnalysisFilter.DISABLED -> uFlag.subFlags.filter { !it.isEnabled }
            FlagAnalysisFilter.MODIFIED -> uFlag.subFlags.filter { it.isModified }
            FlagAnalysisFilter.UNKNOWN -> uFlag.subFlags.filter { it.isUnknown || uFlag.isUnknown }
        }

        // If subflags match filter or if filter is UNKNOWN and the parent flag is unknown
        if (filteredSubFlags.isNotEmpty() || (filter == FlagAnalysisFilter.UNKNOWN && uFlag.isUnknown)) {
            uFlag.copy(subFlags = if (filteredSubFlags.isNotEmpty()) filteredSubFlags else uFlag.subFlags)
        } else {
            null
        }
    }
}

@Composable
private fun FlagAnalysisTopBar(
    backupState: ParsedBackupState?,
    isAnalyzing: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("button_flag_analysis_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Tools",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Flag Analysis",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (backupState != null) {
                    "${backupState.fileName} • ${backupState.fileType.name}"
                } else {
                    "MetaConfig backup inspector & flag manager"
                },
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        if (backupState != null && !isAnalyzing) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.testTag("button_clear_analysis")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Backup",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyAnalysisImportView(
    onImportIBackup: () -> Unit,
    onImportJson: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Column {
                        Text(
                            text = "MetaConfig Flag Analysis",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Import & inspect Instagram backup files",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Import an .ibackup or JSON backup file to automatically resolve numeric codes into readable flag names using the MetaTools mapping database, analyze enabled states by category, toggle flags, and export an updated backup file.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Import Options Title
        Text(
            text = "CHOOSE BACKUP TYPE TO IMPORT",
            color = TextTertiary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        // Card 1: Import iBackup
        Card(
            onClick = onImportIBackup,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp))
                .testTag("card_import_ibackup"),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import iBackup",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Standard Instagram backup file containing manifest_version and info block",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Button(
                    onClick = onImportIBackup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("button_import_ibackup_action")
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Card 2: Import JSON
        Card(
            onClick = onImportJson,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp))
                .testTag("card_import_json"),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import JSON Backup",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Standalone JSON dictionary mapping universal flag codes to sub-flag states",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Button(
                    onClick = onImportJson,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("button_import_json_action")
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Features Checklist
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "WHAT THIS TOOL DOES",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                FeatureBenefitRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Automatic Name Resolution",
                    description = "Resolves numeric universal codes and sub-flag codes to official human-readable names."
                )
                FeatureBenefitRow(
                    icon = Icons.Default.Layers,
                    title = "Category-Wise Grouping",
                    description = "Flags are organized into Camera, Direct, Reels, Feed, GenAI, and other categories."
                )
                FeatureBenefitRow(
                    icon = Icons.Default.Refresh,
                    title = "Interactive State Toggles & Batch Control",
                    description = "Toggle individual sub-flags or batch enable/disable by category with 1-click reset."
                )
                FeatureBenefitRow(
                    icon = Icons.Default.FileDownload,
                    title = "Lossless Export",
                    description = "Exports back to valid .ibackup or .json preserving original manifest, IDs, and qe_overrides."
                )
            }
        }
    }
}

@Composable
private fun FeatureBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AnalysisDashboardCard(
    state: ParsedBackupState,
    stats: com.example.model.AnalysisDashboardStats,
    onExportClick: () -> Unit,
    onCompareClick: () -> Unit,
    onUnmappedClick: () -> Unit,
    onCategorySummaryClick: () -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onResetAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = AmoledCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with file info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.info?.name ?: state.fileName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (state.info != null && state.info.author.isNotBlank()) {
                        Text(
                            text = "Author: ${state.info.author} • v${state.info.version}",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (state.fileType == BackupFileType.IBACKUP) AccentGreenContainer else Color(0xFF1E3A5F))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.fileType == BackupFileType.IBACKUP) "iBackup v${state.manifestVersion ?: 2}" else "JSON Backup",
                        color = if (state.fileType == BackupFileType.IBACKUP) AccentGreen else Color(0xFF64B5F6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stats grid (5 metrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricItem(
                    label = "Universal",
                    value = stats.totalUniversalFlags.toString(),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Sub-Flags",
                    value = stats.totalSubFlags.toString(),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Enabled",
                    value = stats.enabledSubFlags.toString(),
                    color = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Disabled",
                    value = stats.disabledSubFlags.toString(),
                    color = DangerRed,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Modified",
                    value = stats.modifiedCount.toString(),
                    color = if (stats.modifiedCount > 0) Color(0xFFBA68C8) else TextTertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enabled Ratio",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = String.format("%.1f%%", stats.enabledPercentage),
                        color = AccentGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { stats.enabledPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AccentGreen,
                    trackColor = Color(0xFF262626)
                )
            }

            HorizontalDivider(color = AmoledBorder)

            // Quick Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEnableAll,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_enable_all_backup"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
                    ) {
                        Text("Enable All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onDisableAll,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_disable_all_backup"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                    ) {
                        Text("Disable All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (stats.modifiedCount > 0) {
                        OutlinedButton(
                            onClick = onResetAll,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_reset_all_backup"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningYellow),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningYellow.copy(alpha = 0.5f))
                        ) {
                            Text("Reset (${stats.modifiedCount})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Sub-tools pills row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionPillButton(
                        icon = Icons.Default.CompareArrows,
                        title = "Changes (${stats.modifiedCount})",
                        onClick = onCompareClick,
                        highlight = stats.modifiedCount > 0
                    )
                    ActionPillButton(
                        icon = Icons.Default.Warning,
                        title = "Unmapped (${stats.unknownCount})",
                        onClick = onUnmappedClick,
                        highlight = stats.unknownCount > 0
                    )
                    ActionPillButton(
                        icon = Icons.Default.Layers,
                        title = "Categories",
                        onClick = onCategorySummaryClick
                    )
                }

                // Primary Export Button
                Button(
                    onClick = onExportClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_export_analyzed_backup"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Updated Backup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181818))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = TextTertiary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlight) AccentGreenContainer.copy(alpha = 0.4f) else Color(0xFF1E1E1E))
            .border(
                1.dp,
                if (highlight) AccentGreen.copy(alpha = 0.5f) else AmoledBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) AccentGreen else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                color = if (highlight) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AnalysisSearchAndFilterBar(
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    activeFilter: FlagAnalysisFilter,
    onFilterSelected: (FlagAnalysisFilter) -> Unit,
    stats: com.example.model.AnalysisDashboardStats
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_flag_analysis_search"),
            placeholder = {
                Text("Search flag name, sub-flag, or code...", color = TextTertiary, fontSize = 13.sp)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmoledCard,
                unfocusedContainerColor = AmoledCard,
                focusedBorderColor = AccentGreen,
                unfocusedBorderColor = AmoledBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalysisFilterChip(
                label = "All Flags (${stats.totalSubFlags})",
                selected = activeFilter == FlagAnalysisFilter.ALL,
                onClick = { onFilterSelected(FlagAnalysisFilter.ALL) }
            )
            AnalysisFilterChip(
                label = "Enabled (${stats.enabledSubFlags})",
                selected = activeFilter == FlagAnalysisFilter.ENABLED,
                onClick = { onFilterSelected(FlagAnalysisFilter.ENABLED) }
            )
            AnalysisFilterChip(
                label = "Disabled (${stats.disabledSubFlags})",
                selected = activeFilter == FlagAnalysisFilter.DISABLED,
                onClick = { onFilterSelected(FlagAnalysisFilter.DISABLED) }
            )
            AnalysisFilterChip(
                label = "Modified (${stats.modifiedCount})",
                selected = activeFilter == FlagAnalysisFilter.MODIFIED,
                onClick = { onFilterSelected(FlagAnalysisFilter.MODIFIED) }
            )
            AnalysisFilterChip(
                label = "Unknown (${stats.unknownCount})",
                selected = activeFilter == FlagAnalysisFilter.UNKNOWN,
                onClick = { onFilterSelected(FlagAnalysisFilter.UNKNOWN) }
            )
        }
    }
}

@Composable
private fun AnalysisFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AmoledCard,
            labelColor = TextSecondary,
            selectedContainerColor = AccentGreenContainer,
            selectedLabelColor = AccentGreen
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AmoledBorder,
            selectedBorderColor = AccentGreen
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun CategoryAccordionCard(
    categoryName: String,
    flags: List<AnalyzedUniversalFlag>,
    totalSubFlags: Int,
    enabledSubFlags: Int,
    modifiedCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEnableAllInCat: () -> Unit,
    onDisableAllInCat: () -> Unit,
    onToggleSubFlag: (String, String) -> Unit,
    onResetSubFlag: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = AmoledCard)
    ) {
        Column {
            // Category Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = categoryName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (modifiedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF4A148C))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$modifiedCount modified",
                                    color = Color(0xFFE1BEE7),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${flags.size} flags • $totalSubFlags sub-flags ($enabledSubFlags enabled)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Quick Enable/Disable buttons for category
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onEnableAllInCat,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("All ON", fontSize = 11.sp, color = AccentGreen)
                    }

                    TextButton(
                        onClick = onDisableAllInCat,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("All OFF", fontSize = 11.sp, color = DangerRed)
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expanded Flags List
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = AmoledBorder)

                    flags.forEach { uFlag ->
                        UniversalFlagCard(
                            flag = uFlag,
                            onToggleSubFlag = onToggleSubFlag,
                            onResetSubFlag = onResetSubFlag
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversalFlagCard(
    flag: AnalyzedUniversalFlag,
    onToggleSubFlag: (String, String) -> Unit,
    onResetSubFlag: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (flag.isModified) AccentGreen.copy(alpha = 0.5f) else Color(0xFF262626), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Universal Flag Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flag.name,
                        color = if (flag.isUnknown) WarningYellow else TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Code: ${flag.code}",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (flag.isUnknown) {
                            Text(
                                text = "• Unmapped",
                                color = WarningYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (flag.isModified) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2E7D32).copy(alpha = 0.3f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "MODIFIED",
                            color = AccentGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF262626))

            // Sub-flags list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                flag.subFlags.forEach { sFlag ->
                    SubFlagRow(
                        universalCode = flag.code,
                        subFlag = sFlag,
                        onToggle = { onToggleSubFlag(flag.code, sFlag.subCode) },
                        onReset = { onResetSubFlag(flag.code, sFlag.subCode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubFlagRow(
    universalCode: String,
    subFlag: AnalyzedSubFlag,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E1E1E))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subFlag.name,
                color = if (subFlag.isUnknown) WarningYellow else TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ID: ${subFlag.subCode}",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (subFlag.isModified) {
                    Text(
                        text = "• was: ${if (subFlag.originalEnabled) "ON" else "OFF"}",
                        color = Color(0xFFBA68C8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (subFlag.isModified) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo change",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Switch(
            checked = subFlag.isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = Color(0xFF757575),
                uncheckedTrackColor = Color(0xFF2E2E2E)
            ),
            modifier = Modifier.testTag("switch_${universalCode}_${subFlag.subCode}")
        )
    }
}

// =========================================================================
// Dialogs: Compare Changes, Unmapped Flags, Category Summary, Export
// =========================================================================

@Composable
private fun CompareChangesDialog(
    modifiedFlags: List<com.example.model.ModifiedFlagItem>,
    onDismiss: () -> Unit,
    onRevert: (String, String) -> Unit,
    onResetAll: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Modified Flags",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${modifiedFlags.size} flag${if (modifiedFlags.size != 1) "s" else ""} changed from original",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = AmoledBorder)

                if (modifiedFlags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No changes have been made yet.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(modifiedFlags) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.universalName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.subName} (${item.subCode})",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (item.originalState) "ON" else "OFF",
                                            color = if (item.originalState) AccentGreen else DangerRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = "➔", color = TextTertiary, fontSize = 11.sp)
                                        Text(
                                            text = if (item.currentState) "ON" else "OFF",
                                            color = if (item.currentState) AccentGreen else DangerRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRevert(item.universalCode, item.subCode) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "Revert",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetAll,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) {
                            Text("Reset All", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnmappedFlagsDialog(
    unmappedFlags: List<com.example.model.UnmappedFlagItem>,
    onDismiss: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Unmapped Codes",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${unmappedFlags.size} unmapped universal code entries",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = AmoledBorder)

                if (unmappedFlags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All flags in this backup are recognized by the mapping database!",
                            color = AccentGreen,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(unmappedFlags) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Universal Code: ${item.universalCode}",
                                        color = WarningYellow,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (item.unmappedSubCodes.isNotEmpty()) {
                                        Text(
                                            text = "Unmapped sub-codes: ${item.unmappedSubCodes.joinToString(", ")}",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onCopy(item.universalCode, "Code ${item.universalCode}") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val allCodes = unmappedFlags.joinToString("\n") { u ->
                                "${u.universalCode}: ${u.unmappedSubCodes.joinToString(",")}"
                            }
                            onCopy(allCodes, "All unmapped codes")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626), contentColor = TextPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy All Unmapped Codes", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryDialog(
    summaries: List<com.example.model.CategoryAnalysisSummary>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Breakdown",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = AmoledBorder)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summaries) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.category,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cat.universalCount} flags • ${cat.subFlagCount} sub-flags",
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentGreenContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${cat.enabledCount} ON",
                                        color = AccentGreen,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF331414))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${cat.disabledCount} OFF",
                                        color = DangerRed,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExportBackupDialog(
    state: ParsedBackupState,
    fileNameInput: String,
    validationErrors: List<String>,
    onFileNameChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirmExport: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export Updated Backup",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "Specify a filename to save the updated backup to your device storage. All original structures, metadata, and untouched entries are preserved.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                // Validation Status
                if (validationErrors.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreenContainer.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Validation passed: structure is intact and ready for MetaConfig import.",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF331414))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Validation Warnings:",
                            color = DangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        validationErrors.forEach { err ->
                            Text(text = "• $err", color = TextPrimary, fontSize = 11.5.sp)
                        }
                    }
                }

                // File name input
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = onFileNameChanged,
                    label = { Text("Output Filename") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_export_filename"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = AmoledBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirmExport,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_confirm_export"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
                    ) {
                        Text("Save File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Helper to query file display name from SAF content Uri.
 */
private fun queryDisplayName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        } catch (_: Exception) {}
    }
    return uri.lastPathSegment
}

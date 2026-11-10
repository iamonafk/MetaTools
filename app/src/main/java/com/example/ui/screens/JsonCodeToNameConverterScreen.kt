package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.JsonCodeToNameEngine
import com.example.model.CodeToNameMappingSource
import com.example.model.CodeToNameResult
import com.example.model.ConversionHistoryItem
import com.example.model.JsonFormatStyle
import com.example.model.UniversalFlag
import com.example.model.UnmappedCodeItem
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
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

enum class ConverterViewTab {
    INPUT_JSON,
    CONVERTED_JSON,
    SIDE_BY_SIDE
}

@Composable
fun JsonCodeToNameConverterScreen(
    viewModel: MetaToolsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("code_to_name_prefs", Context.MODE_PRIVATE) }

    // State
    var inputJsonText by remember { mutableStateOf("") }
    var importedFileName by remember { mutableStateOf("backup.json") }
    var selectedMappingSource by remember { mutableStateOf(CodeToNameMappingSource.BUILT_IN) }
    var formatStyle by remember { mutableStateOf(JsonFormatStyle.PRETTY) }
    var viewTab by remember { mutableStateOf(ConverterViewTab.INPUT_JSON) }
    var isConverting by remember { mutableStateOf(false) }
    var conversionResult by remember { mutableStateOf<CodeToNameResult?>(null) }
    var searchConvertedQuery by remember { mutableStateOf("") }

    // Modals
    var showUnmappedReportDialog by remember { mutableStateOf(false) }
    var showLookupDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFileNameInput by remember { mutableStateOf("Askxtylishh_Named_Backup") }
    var validationNotice by remember { mutableStateOf<String?>(null) }

    // Mapping flags cached
    var activeMappingFlags by remember { mutableStateOf<List<UniversalFlag>>(emptyList()) }
    var hasCustomMapping by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    // Load mapping flags on launch or mapping switch
    LaunchedEffect(selectedMappingSource) {
        withContext(Dispatchers.IO) {
            hasCustomMapping = viewModel.hasCustomMapping.value
            activeMappingFlags = if (selectedMappingSource == CodeToNameMappingSource.CUSTOM && hasCustomMapping) {
                viewModel.categoryFlagsMap.value.values.flatten().ifEmpty {
                    // Load custom mapping flags
                    viewModel.availableCompareSources.value.find { it is com.example.model.CompareSource.ActiveCustom }?.let {
                        // All flags from custom
                        null
                    } ?: emptyList()
                }
            } else {
                emptyList()
            }
            if (activeMappingFlags.isEmpty()) {
                activeMappingFlags = viewModel.categoryFlagsMap.value.values.flatten()
            }
        }
    }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val name = queryDisplayName(context, uri) ?: "backup.json"
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream)).readText()
                        } ?: throw IllegalStateException("Unable to read file")
                    }
                    inputJsonText = content
                    importedFileName = name
                    validationNotice = "Imported $name successfully"
                    Toast.makeText(context, "Loaded $name", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error importing: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && conversionResult != null) {
            coroutineScope.launch {
                try {
                    val content = if (formatStyle == JsonFormatStyle.PRETTY) {
                        conversionResult!!.convertedJsonPretty
                    } else {
                        conversionResult!!.convertedJsonCompact
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(content.toByteArray(Charsets.UTF_8))
                            out.flush()
                        } ?: throw IllegalStateException("Could not write destination file")
                    }
                    Toast.makeText(context, "Exported JSON saved successfully!", Toast.LENGTH_LONG).show()
                    showExportDialog = false
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun executeConversion() {
        if (inputJsonText.isBlank()) {
            Toast.makeText(context, "Please import or paste code-based JSON first", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isConverting = true
            try {
                // Fetch actual flags from repository
                val flags = withContext(Dispatchers.IO) {
                    if (selectedMappingSource == CodeToNameMappingSource.CUSTOM && viewModel.hasCustomMapping.value) {
                        viewModel.categoryFlagsMap.value.values.flatten()
                    } else {
                        // Load built-in flags
                        viewModel.categoryFlagsMap.value.values.flatten()
                    }
                }

                val result = withContext(Dispatchers.Default) {
                    JsonCodeToNameEngine.convert(inputJsonText, flags)
                }

                if (result.isSuccess) {
                    val res = result.getOrThrow()
                    conversionResult = res
                    viewTab = ConverterViewTab.CONVERTED_JSON

                    // Save to local history
                    saveConversionHistory(
                        prefs = prefs,
                        fileName = importedFileName,
                        mapping = if (selectedMappingSource == CodeToNameMappingSource.BUILT_IN) "Askxtylishh.json" else "Custom Mapping",
                        stats = res.stats
                    )

                    Toast.makeText(
                        context,
                        "Converted! ${res.stats.mappedUniversalFlags}/${res.stats.totalUniversalFlags} flags (${String.format("%.1f", res.stats.mappingCoveragePercentage)}% mapped)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "Conversion failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isConverting = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("button_code_to_name_back")
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
                    text = "JSON Code to Name Converter",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Convert numeric codes to readable flag names",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }

            // Quick History & Lookup icons
            IconButton(
                onClick = { showLookupDialog = true },
                modifier = Modifier.testTag("button_code_lookup")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Code Lookup",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { showHistoryDialog = true },
                modifier = Modifier.testTag("button_conversion_history")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Mapping Database Selector & Stats Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = AmoledCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MAPPING DATABASE SOURCE",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Text(
                                text = "Askxtylishh Built-in",
                                color = AccentGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedMappingSource == CodeToNameMappingSource.BUILT_IN,
                                onClick = {
                                    selectedMappingSource = CodeToNameMappingSource.BUILT_IN
                                    if (conversionResult != null) executeConversion()
                                },
                                label = { Text("Built-in (Askxtylishh.json)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF1E1E1E),
                                    labelColor = TextSecondary,
                                    selectedContainerColor = AccentGreenContainer,
                                    selectedLabelColor = AccentGreen
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedMappingSource == CodeToNameMappingSource.BUILT_IN,
                                    borderColor = AmoledBorder,
                                    selectedBorderColor = AccentGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = selectedMappingSource == CodeToNameMappingSource.CUSTOM,
                                onClick = {
                                    selectedMappingSource = CodeToNameMappingSource.CUSTOM
                                    if (conversionResult != null) executeConversion()
                                },
                                label = { Text("Custom Mapping", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF1E1E1E),
                                    labelColor = TextSecondary,
                                    selectedContainerColor = AccentGreenContainer,
                                    selectedLabelColor = AccentGreen
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedMappingSource == CodeToNameMappingSource.CUSTOM,
                                    borderColor = AmoledBorder,
                                    selectedBorderColor = AccentGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 2: Input Controls Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = AmoledCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CODE-BASED INPUT JSON",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        importLauncher.launch(arrayOf("application/json", "*/*"))
                                    },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import", color = AccentGreen, fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()
                                        if (clip != null && clip.text.isNotBlank()) {
                                            inputJsonText = clip.text
                                            validationNotice = "Pasted from clipboard"
                                        } else {
                                            Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste", color = Color(0xFF64B5F6), fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = {
                                        inputJsonText = JsonCodeToNameEngine.EXAMPLE_INPUT
                                        importedFileName = "example_backup.json"
                                        validationNotice = "Loaded example JSON"
                                    },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Example", color = TextTertiary, fontSize = 11.sp)
                                }
                            }
                        }

                        // Multiline text editor for Input JSON
                        OutlinedTextField(
                            value = inputJsonText,
                            onValueChange = {
                                inputJsonText = it
                                validationNotice = null
                            },
                            placeholder = {
                                Text(
                                    text = "{\n  \"69718:\": [\"4: : true\", \"1: : true\"],\n  \"53982:\": [\"0: : true\"],\n  \"_qe_overrides_\": []\n}",
                                    color = TextTertiary,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("input_code_json"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF161616),
                                unfocusedContainerColor = Color(0xFF161616),
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = AmoledBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Action Bar: Validate, Clear, and Convert
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val map = JsonCodeToNameEngine.parseOrderedJson(inputJsonText)
                                        validationNotice = "Valid JSON with ${map.size} entries"
                                        Toast.makeText(context, validationNotice, Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        validationNotice = "Invalid JSON: ${e.message}"
                                        Toast.makeText(context, validationNotice, Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Validate", fontSize = 11.5.sp)
                            }

                            if (inputJsonText.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        inputJsonText = ""
                                        conversionResult = null
                                        validationNotice = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Text("Clear", fontSize = 11.5.sp)
                                }
                            }

                            Button(
                                onClick = { executeConversion() },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("button_execute_convert"),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isConverting) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Convert", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }

                        if (validationNotice != null) {
                            Text(
                                text = validationNotice!!,
                                color = if (validationNotice!!.startsWith("Invalid")) DangerRed else AccentGreen,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Section 3: Analysis Dashboard (When conversionResult != null)
            if (conversionResult != null) {
                val stats = conversionResult!!.stats

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = AmoledCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "CONVERSION ANALYSIS",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )

                                Text(
                                    text = "${String.format("%.1f", stats.mappingCoveragePercentage)}% Mapped",
                                    color = if (stats.mappingCoveragePercentage >= 80f) AccentGreen else WarningYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { stats.mappingCoveragePercentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentGreen,
                                trackColor = Color(0xFF262626)
                            )

                            // 6 Statistics metric boxes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatBox(label = "Universal", value = "${stats.totalUniversalFlags}", modifier = Modifier.weight(1f))
                                StatBox(label = "Mapped U.", value = "${stats.mappedUniversalFlags}", color = AccentGreen, modifier = Modifier.weight(1f))
                                StatBox(label = "Sub-Flags", value = "${stats.totalSubFlags}", modifier = Modifier.weight(1f))
                                StatBox(label = "Mapped S.", value = "${stats.mappedSubFlags}", color = AccentGreen, modifier = Modifier.weight(1f))
                                StatBox(
                                    label = "Unmapped",
                                    value = "${stats.unmappedUniversalCodes + stats.unmappedSubflagCodes}",
                                    color = if (stats.unmappedUniversalCodes + stats.unmappedSubflagCodes > 0) DangerRed else TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Sub-actions: View Unmapped Codes Report, Share Report
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showUnmappedReportDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unmapped (${conversionResult!!.unmappedItems.size})", fontSize = 11.5.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val reportText = JsonCodeToNameEngine.generateReportText(
                                            fileName = importedFileName,
                                            mappingSource = if (selectedMappingSource == CodeToNameMappingSource.BUILT_IN) "Askxtylishh.json" else "Custom Mapping",
                                            stats = stats,
                                            unmappedItems = conversionResult!!.unmappedItems
                                        )
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, reportText)
                                            putExtra(Intent.EXTRA_SUBJECT, "Conversion Report: $importedFileName")
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Conversion Report"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share Report", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }

                // Section 4: Converted Output Controls & Preview
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = AmoledCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header & Style Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "NAME-BASED CONVERTED JSON",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = formatStyle == JsonFormatStyle.PRETTY,
                                        onClick = { formatStyle = JsonFormatStyle.PRETTY },
                                        label = { Text("Pretty", fontSize = 10.5.sp) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1E1E1E),
                                            labelColor = TextSecondary,
                                            selectedContainerColor = AccentGreenContainer,
                                            selectedLabelColor = AccentGreen
                                        ),
                                        modifier = Modifier.height(26.dp)
                                    )
                                    FilterChip(
                                        selected = formatStyle == JsonFormatStyle.COMPACT,
                                        onClick = { formatStyle = JsonFormatStyle.COMPACT },
                                        label = { Text("Compact", fontSize = 10.5.sp) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1E1E1E),
                                            labelColor = TextSecondary,
                                            selectedContainerColor = AccentGreenContainer,
                                            selectedLabelColor = AccentGreen
                                        ),
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }

                            // View Tabs: Converted JSON vs Side by Side vs Original
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF161616))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ViewModeTab(
                                    title = "Converted Output",
                                    selected = viewTab == ConverterViewTab.CONVERTED_JSON,
                                    onClick = { viewTab = ConverterViewTab.CONVERTED_JSON },
                                    modifier = Modifier.weight(1f)
                                )
                                ViewModeTab(
                                    title = "Side by Side",
                                    selected = viewTab == ConverterViewTab.SIDE_BY_SIDE,
                                    onClick = { viewTab = ConverterViewTab.SIDE_BY_SIDE },
                                    modifier = Modifier.weight(1f)
                                )
                                ViewModeTab(
                                    title = "Original Code",
                                    selected = viewTab == ConverterViewTab.INPUT_JSON,
                                    onClick = { viewTab = ConverterViewTab.INPUT_JSON },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val activeDisplayContent = when (viewTab) {
                                ConverterViewTab.CONVERTED_JSON -> {
                                    if (formatStyle == JsonFormatStyle.PRETTY) {
                                        conversionResult!!.convertedJsonPretty
                                    } else {
                                        conversionResult!!.convertedJsonCompact
                                    }
                                }
                                ConverterViewTab.INPUT_JSON -> inputJsonText
                                ConverterViewTab.SIDE_BY_SIDE -> {
                                    "// BEFORE (Code-Based JSON):\n" +
                                        inputJsonText.take(600) + (if (inputJsonText.length > 600) "\n... [truncated for side-by-side view]" else "") +
                                        "\n\n// AFTER (Name-Based JSON):\n" +
                                        conversionResult!!.convertedJsonPretty
                                }
                            }

                            // Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF121212))
                                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = activeDisplayContent,
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Primary Action Buttons: Export JSON, Copy JSON, Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        exportFileNameInput = "Askxtylishh_Named_Backup"
                                        showExportDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("button_export_named_json"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export JSON", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val content = if (formatStyle == JsonFormatStyle.PRETTY) {
                                            conversionResult!!.convertedJsonPretty
                                        } else {
                                            conversionResult!!.convertedJsonCompact
                                        }
                                        clipboardManager.setText(AnnotatedString(content))
                                        Toast.makeText(context, "Copied converted JSON to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy All", fontSize = 11.5.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val content = if (formatStyle == JsonFormatStyle.PRETTY) {
                                            conversionResult!!.convertedJsonPretty
                                        } else {
                                            conversionResult!!.convertedJsonCompact
                                        }
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_TEXT, content)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Converted JSON"))
                                    },
                                    modifier = Modifier.weight(0.9f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }

                // Section 5: Converted Flag Entries Cards with Search
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CONVERTED ENTRIES (${conversionResult!!.convertedFlags.size})",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Search converted output
                        OutlinedTextField(
                            value = searchConvertedQuery,
                            onValueChange = { searchConvertedQuery = it },
                            placeholder = {
                                Text("Search universal or subflag name/code...", color = TextTertiary, fontSize = 12.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (searchConvertedQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchConvertedQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AmoledCard,
                                unfocusedContainerColor = AmoledCard,
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = AmoledBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }

                // Filtered flags list
                val filteredFlags = conversionResult!!.convertedFlags.filter { uFlag ->
                    if (searchConvertedQuery.isBlank()) true
                    else {
                        val q = searchConvertedQuery.trim().lowercase()
                        uFlag.formattedKey.lowercase().contains(q) ||
                            uFlag.numericCode.contains(q) ||
                            uFlag.subFlags.any {
                                it.formattedOutputString.lowercase().contains(q) || it.numericCode.contains(q)
                            }
                    }
                }

                items(filteredFlags) { flag ->
                    ConvertedFlagCard(
                        flag = flag,
                        onCopyEntry = {
                            val flagBlock = "\"${flag.formattedKey}\": [\n  " +
                                flag.subFlags.joinToString(",\n  ") { "\"${it.formattedOutputString}\"" } +
                                "\n]"
                            clipboardManager.setText(AnnotatedString(flagBlock))
                            Toast.makeText(context, "Copied entry ${flag.formattedKey}", Toast.LENGTH_SHORT).show()
                        },
                        onCopySubFlag = { sub ->
                            clipboardManager.setText(AnnotatedString("\"${sub.formattedOutputString}\""))
                            Toast.makeText(context, "Copied subflag ${sub.formattedOutputString}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal: Unmapped Codes Report
    if (showUnmappedReportDialog && conversionResult != null) {
        val unmappedItems = conversionResult!!.unmappedItems
        Dialog(onDismissRequest = { showUnmappedReportDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = AmoledCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unmapped Codes Report",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showUnmappedReportDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Text(
                        text = "${unmappedItems.size} codes were not found in ${if (selectedMappingSource == CodeToNameMappingSource.BUILT_IN) "Askxtylishh.json" else "Custom Mapping"}.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (unmappedItems.isEmpty()) {
                            Text(
                                text = "All flags and subflags were successfully mapped!",
                                color = AccentGreen,
                                fontSize = 12.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                unmappedItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (item.isUniversalUnmapped) "Universal Code: ${item.universalCode}" else "Subflag: ${item.subFlagCode} (Under: ${item.universalName ?: item.universalCode})",
                                                color = WarningYellow,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(text = "Entry: ${item.rawEntry}", color = TextTertiary, fontSize = 10.5.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                val toCopy = if (item.isUniversalUnmapped) item.universalCode else "${item.subFlagCode}"
                                                clipboardManager.setText(AnnotatedString(toCopy))
                                                Toast.makeText(context, "Copied $toCopy", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val report = unmappedItems.joinToString("\n") {
                                    if (it.isUniversalUnmapped) "Universal: ${it.universalCode} (${it.rawEntry})"
                                    else "Subflag: ${it.subFlagCode} in ${it.universalCode} (${it.rawEntry})"
                                }
                                clipboardManager.setText(AnnotatedString(report))
                                Toast.makeText(context, "Copied unmapped report", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626), contentColor = TextPrimary)
                        ) {
                            Text("Copy List", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showUnmappedReportDialog = false },
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

    // Modal: Individual Code Lookup
    if (showLookupDialog) {
        var lookupQuery by remember { mutableStateOf("") }
        var lookupResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

        Dialog(onDismissRequest = { showLookupDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = AmoledCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Individual Code Lookup",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showLookupDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    OutlinedTextField(
                        value = lookupQuery,
                        onValueChange = {
                            lookupQuery = it
                            lookupResults = JsonCodeToNameEngine.lookupCode(it, activeMappingFlags)
                        },
                        placeholder = { Text("Enter numeric universal or subflag code...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161616),
                            unfocusedContainerColor = Color(0xFF161616),
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = AmoledBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (lookupQuery.isBlank()) {
                            Text("Type any code to find its mapped name.", color = TextTertiary, fontSize = 12.sp)
                        } else if (lookupResults.isEmpty()) {
                            Text("No mapping found for code '$lookupQuery'.", color = WarningYellow, fontSize = 12.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                lookupResults.forEach { (title, subtitle) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = title, color = AccentGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(title.substringAfter(": ").trim()))
                                                Toast.makeText(context, "Copied name", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showLookupDialog = false },
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

    // Modal: Conversion History
    if (showHistoryDialog) {
        val historyList = remember { loadConversionHistory(prefs) }

        Dialog(onDismissRequest = { showHistoryDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = AmoledCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Conversion History",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showHistoryDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (historyList.isEmpty()) {
                            Text("No conversions recorded yet.", color = TextTertiary, fontSize = 12.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.forEach { h ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF222222))
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = h.sourceFileName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "${String.format("%.1f", h.coveragePercentage)}%", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(text = "Source: ${h.mappingSourceName} • ${h.totalUniversal} flags, ${h.totalSubFlags} subflags", color = TextSecondary, fontSize = 11.sp)
                                        Text(text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(h.timestamp)), color = TextTertiary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showHistoryDialog = false },
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

    // Modal: Export Dialog
    if (showExportDialog && conversionResult != null) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = AmoledCard)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Export Name-Based JSON",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Specify a file name to save the converted JSON backup to your device.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = exportFileNameInput,
                        onValueChange = { exportFileNameInput = it },
                        label = { Text("Output Filename") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_export_named_filename"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161616),
                            unfocusedContainerColor = Color(0xFF161616),
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = AmoledBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExportDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val cleanName = exportFileNameInput.trim().ifEmpty { "Askxtylishh_Named_Backup" }
                                val finalName = if (cleanName.endsWith(".json")) cleanName else "$cleanName.json"
                                exportLauncher.launch(finalName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_confirm_export_named_json"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    color: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181818))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = TextTertiary, fontSize = 9.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ViewModeTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) AccentGreenContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) AccentGreen else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ConvertedFlagCard(
    flag: com.example.model.ConvertedUniversalFlag,
    onCopyEntry: () -> Unit,
    onCopySubFlag: (com.example.model.ConvertedSubFlag) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flag.formattedKey,
                        color = if (flag.isMapped) TextPrimary else WarningYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Original: ${flag.originalKey} • ${flag.subFlags.size} subflags (${flag.mappedSubFlags} mapped)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = onCopyEntry, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Flag", tint = AccentGreen, modifier = Modifier.size(15.dp))
                }
            }

            HorizontalDivider(color = Color(0xFF262626))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                flag.subFlags.forEach { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sub.formattedOutputString,
                            color = if (sub.isMapped) TextPrimary else WarningYellow,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onCopySubFlag(sub) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun saveConversionHistory(
    prefs: android.content.SharedPreferences,
    fileName: String,
    mapping: String,
    stats: com.example.model.ConversionAnalysisStats
) {
    try {
        val existing = prefs.getString("history_items", "") ?: ""
        val itemStr = "$fileName|#|$mapping|#|${stats.totalUniversalFlags}|#|${stats.totalSubFlags}|#|${stats.mappingCoveragePercentage}|#|${System.currentTimeMillis()}"
        val updated = if (existing.isEmpty()) itemStr else "$itemStr\n$existing"
        // Keep up to 20 items
        val limited = updated.lines().take(20).joinToString("\n")
        prefs.edit().putString("history_items", limited).apply()
    } catch (_: Exception) {}
}

private fun loadConversionHistory(prefs: android.content.SharedPreferences): List<ConversionHistoryItem> {
    val raw = prefs.getString("history_items", "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw.lines().mapNotNull { line ->
        val parts = line.split("|#|")
        if (parts.size >= 6) {
            ConversionHistoryItem(
                sourceFileName = parts[0],
                mappingSourceName = parts[1],
                totalUniversal = parts[2].toIntOrNull() ?: 0,
                totalSubFlags = parts[3].toIntOrNull() ?: 0,
                coveragePercentage = parts[4].toFloatOrNull() ?: 0f,
                timestamp = parts[5].toLongOrNull() ?: 0L
            )
        } else null
    }
}

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

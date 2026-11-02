package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IBackupConverterEngine
import com.example.model.ConversionMode
import com.example.model.ConversionPreview
import com.example.model.ConverterState
import com.example.model.IBackupInfo
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenBorder
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledCardHover
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun IBackupConverterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var state by remember { mutableStateOf(ConverterState()) }
    var showAdvancedMetadata by remember { mutableStateOf(false) }
    var showRawOutputPreview by remember { mutableStateOf(false) }

    // Intercept back button to return to Tools overview
    BackHandler {
        onBack()
    }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                state = state.copy(isProcessing = true, errorMessage = null)
                try {
                    val displayName = queryDisplayName(context, uri) ?: "imported_file"
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        } ?: throw IllegalStateException("Could not read file from device")
                    }

                    // Auto-fill metadata if switching to JSON_TO_IBACKUP or pre-fill suggested name
                    val inferredName = displayName.substringBeforeLast('.')
                    state = state.copy(
                        sourceFileName = displayName,
                        rawInputContent = content,
                        isProcessing = false,
                        inputBackupName = if (state.inputBackupName.isBlank()) inferredName else state.inputBackupName
                    )

                    Toast.makeText(context, "Loaded $displayName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    state = state.copy(
                        isProcessing = false,
                        errorMessage = "Error reading file: ${e.message}"
                    )
                }
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (state.mode == ConversionMode.IBACKUP_TO_JSON) "application/json" else "application/octet-stream"
        )
    ) { uri: Uri? ->
        if (uri != null && state.convertedOutput != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(state.convertedOutput!!.toByteArray(Charsets.UTF_8))
                            outputStream.flush()
                        } ?: throw IllegalStateException("Could not write to destination")
                    }
                    Toast.makeText(context, "Saved file successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun triggerConversion() {
        if (state.rawInputContent.isBlank()) {
            state = state.copy(errorMessage = "Please import a file first before converting.")
            return
        }

        coroutineScope.launch {
            state = state.copy(isProcessing = true, errorMessage = null)
            withContext(Dispatchers.IO) {
                if (state.mode == ConversionMode.IBACKUP_TO_JSON) {
                    val result = IBackupConverterEngine.convertIbackupToJson(
                        inputContent = state.rawInputContent,
                        sourceFileName = state.sourceFileName
                    )
                    withContext(Dispatchers.Main) {
                        state = if (result.isSuccess) {
                            val (output, preview, suggestedName) = result.getOrThrow()
                            state.copy(
                                isProcessing = false,
                                convertedOutput = output,
                                preview = preview,
                                suggestedFileName = suggestedName,
                                errorMessage = null
                            )
                        } else {
                            state.copy(
                                isProcessing = false,
                                errorMessage = result.exceptionOrNull()?.message ?: "Conversion failed"
                            )
                        }
                    }
                } else {
                    // JSON to iBackup mode
                    val backupName = state.inputBackupName.trim().ifEmpty {
                        state.sourceFileName?.substringBeforeLast('.') ?: "My Backup"
                    }
                    val authorName = state.inputAuthor.trim().ifEmpty { "MetaTools User" }
                    val version = state.inputVersion.trim().ifEmpty { "1" }
                    val changelog = state.inputChangelog.trim()

                    val backupId = state.inputBackupId.toLongOrNull() ?: IBackupConverterEngine.generateRandomId()
                    val authorId = state.inputAuthorId.toLongOrNull() ?: 0L

                    val info = IBackupInfo(
                        id = backupId,
                        releaseId = IBackupConverterEngine.generateReleaseId(),
                        name = backupName,
                        author = authorName,
                        authorId = authorId,
                        version = version,
                        releaseDate = IBackupConverterEngine.generateUtcTimestamp(),
                        changelog = changelog
                    )

                    val result = IBackupConverterEngine.convertJsonToIbackup(
                        inputContent = state.rawInputContent,
                        info = info,
                        sourceFileName = state.sourceFileName
                    )

                    withContext(Dispatchers.Main) {
                        state = if (result.isSuccess) {
                            val (output, preview, suggestedName) = result.getOrThrow()
                            state.copy(
                                isProcessing = false,
                                convertedOutput = output,
                                preview = preview,
                                suggestedFileName = suggestedName,
                                errorMessage = null
                            )
                        } else {
                            state.copy(
                                isProcessing = false,
                                errorMessage = result.exceptionOrNull()?.message ?: "Conversion failed"
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(20.dp))
                    .testTag("button_back_to_tools")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Tools",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "iBackup ↔ JSON Converter",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Convert Instagram iBackup & JSON configuration files",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Section Title: Conversion Mode
        Text(
            text = "SELECT CONVERSION DIRECTION",
            color = AccentGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        // 1. Two Clean Mode Selection Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeSelectionCard(
                title = "iBackup to JSON",
                description = "Extract pure backup object from Instagram .ibackup file",
                icon = Icons.Default.FileDownload,
                isSelected = state.mode == ConversionMode.IBACKUP_TO_JSON,
                onClick = {
                    if (state.mode != ConversionMode.IBACKUP_TO_JSON) {
                        state = state.copy(
                            mode = ConversionMode.IBACKUP_TO_JSON,
                            convertedOutput = null,
                            preview = null,
                            errorMessage = null
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "card_mode_ibackup_to_json"
            )

            ModeSelectionCard(
                title = "JSON to iBackup",
                description = "Wrap normal JSON backup into a complete .ibackup with metadata",
                icon = Icons.Default.FileUpload,
                isSelected = state.mode == ConversionMode.JSON_TO_IBACKUP,
                onClick = {
                    if (state.mode != ConversionMode.JSON_TO_IBACKUP) {
                        state = state.copy(
                            mode = ConversionMode.JSON_TO_IBACKUP,
                            convertedOutput = null,
                            preview = null,
                            errorMessage = null
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "card_mode_json_to_ibackup"
            )
        }

        // Error Banner (if any)
        if (state.errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF261010))
                    .border(1.dp, DangerRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = state.errorMessage ?: "",
                    color = Color(0xFFFFB4B4),
                    fontSize = 13.sp
                )
            }
        }

        // Section: File Import & Source
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (state.mode == ConversionMode.IBACKUP_TO_JSON) "Source iBackup File" else "Source JSON File",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (state.mode == ConversionMode.IBACKUP_TO_JSON) {
                            "Select .ibackup or .json from device storage"
                        } else {
                            "Select standalone JSON backup file"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("*/*", "application/json", "text/*"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreenContainer,
                        contentColor = AccentGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag(if (state.mode == ConversionMode.IBACKUP_TO_JSON) "button_import_ibackup" else "button_import_json")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.sourceFileName != null) "Change File" else "Import File",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Loaded file badge
            if (state.sourceFileName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmoledCardHover)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.sourceFileName ?: "",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${state.rawInputContent.length} bytes",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Mode 2 specific: Metadata Input Fields
            if (state.mode == ConversionMode.JSON_TO_IBACKUP) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "METADATA CONFIGURATION",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = state.inputBackupName,
                    onValueChange = { state = state.copy(inputBackupName = it) },
                    label = { Text("Backup Name *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Askxtylishh Backup 🚀", color = TextTertiary) },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_backup_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.inputAuthor,
                        onValueChange = { state = state.copy(inputAuthor = it) },
                        label = { Text("Author Name *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Askxtylishh", color = TextTertiary) },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_author_name")
                    )

                    OutlinedTextField(
                        value = state.inputVersion,
                        onValueChange = { state = state.copy(inputVersion = it) },
                        label = { Text("Version", fontSize = 12.sp) },
                        placeholder = { Text("1", color = TextTertiary) },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("input_backup_version")
                    )
                }

                OutlinedTextField(
                    value = state.inputChangelog,
                    onValueChange = { state = state.copy(inputChangelog = it) },
                    label = { Text("Changelog (Optional)", fontSize = 12.sp) },
                    placeholder = { Text("Describe updates in this backup...", color = TextTertiary) },
                    maxLines = 3,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_changelog")
                )

                // Advanced Fields Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedMetadata = !showAdvancedMetadata }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advanced Identifiers (Auto-generated if empty)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (showAdvancedMetadata) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle advanced fields",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showAdvancedMetadata) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.inputBackupId,
                            onValueChange = { state = state.copy(inputBackupId = it.filter { ch -> ch.isDigit() }) },
                            label = { Text("Backup ID", fontSize = 12.sp) },
                            placeholder = { Text("Auto ID", color = TextTertiary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_backup_id")
                        )

                        OutlinedTextField(
                            value = state.inputAuthorId,
                            onValueChange = { state = state.copy(inputAuthorId = it.filter { ch -> ch.isDigit() }) },
                            label = { Text("Author ID", fontSize = 12.sp) },
                            placeholder = { Text("0", color = TextTertiary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_author_id")
                        )
                    }
                }
            }

            // Convert Button
            Button(
                onClick = { triggerConversion() },
                enabled = !state.isProcessing && state.rawInputContent.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = AmoledBackground,
                    disabledContainerColor = AmoledBorder,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(if (state.mode == ConversionMode.IBACKUP_TO_JSON) "button_convert_to_json" else "button_convert_to_ibackup")
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AmoledBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Converting...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Transform,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.mode == ConversionMode.IBACKUP_TO_JSON) "Extract & Convert to JSON" else "Package into .ibackup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Section: Conversion Preview Card
        AnimatedVisibility(visible = state.preview != null && state.convertedOutput != null) {
            state.preview?.let { preview ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmoledCard)
                        .border(1.dp, AccentGreenBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Conversion Preview",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Type Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGreenContainer)
                                .border(1.dp, AccentGreenBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (preview.mode == ConversionMode.IBACKUP_TO_JSON) "iBackup → JSON" else "JSON → iBackup",
                                color = AccentGreenBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Backup Name Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmoledCardHover)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Backup Name: ",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = preview.backupName,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3 Metric Dashboard Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Universal Flags",
                            value = preview.totalUniversalFlags.toString(),
                            accentColor = AccentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Sub-Flag Entries",
                            value = preview.totalSubFlagEntries.toString(),
                            accentColor = InfoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Total Entries",
                            value = preview.totalBackupEntries.toString(),
                            accentColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Optional details (version, author, changelog)
                    if (preview.author.isNotBlank() || preview.releaseVersion.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (preview.author.isNotBlank()) {
                                Text(
                                    text = "Author: ${preview.author}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            if (preview.releaseVersion.isNotBlank()) {
                                Text(
                                    text = "Version: ${preview.releaseVersion}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (preview.changelog.isNotBlank()) {
                        Text(
                            text = "Changelog: ${preview.changelog}",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }

                    // Action Buttons: Export, Copy, Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch(state.suggestedFileName)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = AmoledBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("button_export_file")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export ${if (state.mode == ConversionMode.IBACKUP_TO_JSON) ".json" else ".ibackup"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("MetaTools Output", state.convertedOutput ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied converted output to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("button_copy_output")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, state.convertedOutput ?: "")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Converted File"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("button_share_output")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }

                    // Inspect Raw Output Accordion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRawOutputPreview = !showRawOutputPreview }
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showRawOutputPreview) "Hide JSON payload" else "Inspect formatted payload snippet",
                            color = InfoBlue,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (showRawOutputPreview) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = InfoBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showRawOutputPreview) {
                        val snippet = (state.convertedOutput ?: "").take(2500)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmoledBackground)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (snippet.length < (state.convertedOutput?.length ?: 0)) "$snippet\n\n... (truncated preview)" else snippet,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelectionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentGreen else AmoledBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentGreenContainer.copy(alpha = 0.5f) else AmoledCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AccentGreen else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentGreen)
                    )
                }
            }

            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                color = TextTertiary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCardHover)
            .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = accentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGreen,
    unfocusedBorderColor = AmoledBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = AccentGreen,
    unfocusedLabelColor = TextSecondary,
    cursorColor = AccentGreen
)

private fun queryDisplayName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
    return uri.path?.substringAfterLast('/')
}

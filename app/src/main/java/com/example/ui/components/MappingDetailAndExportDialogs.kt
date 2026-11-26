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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MappingDiagnosticsParser
import com.example.data.MetaConfigParser
import com.example.model.CatalogFlagItem
import com.example.model.ExportFileFormat
import com.example.model.ExportReportType
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
import com.example.ui.theme.WarningYellow

@Composable
fun FlagDetailInspectorDialog(
    flag: CatalogFlagItem,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onShare: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    val accent = statusColor(flag.status)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag("dialog_flag_detail")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = flag.status.label.uppercase(),
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = flag.category,
                        color = InfoBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Pin favorite",
                            tint = if (isFavorite) WarningYellow else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val summary = buildString {
                                appendLine("Flag: ${flag.name}")
                                appendLine("Status: ${flag.status.label} | Category: ${flag.category}")
                                appendLine("Old Code: ${flag.oldCode ?: "-"} | New Code: ${flag.newCode ?: "-"}")
                                if (flag.subFlags.isNotEmpty()) {
                                    appendLine("Subflags (${flag.subFlags.size}):")
                                    flag.subFlags.forEach { sf ->
                                        appendLine(" - ${sf.name}: Old=${sf.oldCode ?: "-"} New=${sf.newCode ?: "-"}")
                                    }
                                }
                            }
                            onShare(summary, "Share Flag ${flag.name}")
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share flag info",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close detail dialog",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Universal Flag Name + Copy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledBackground)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    .clickable { onCopy(flag.name, "Universal Flag Name") }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("UNIVERSAL FLAG NAME", color = TextTertiary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = flag.name,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy universal flag name",
                    tint = AccentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Old vs New Universal Codes + Availability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CodeCopyBox(
                    label = "OLD CODE",
                    code = flag.oldCode ?: "—",
                    color = if (flag.oldCode != null) TextPrimary else TextTertiary,
                    enabled = flag.oldCode != null,
                    onClick = { flag.oldCode?.let { onCopy(it, "Old Universal Code") } },
                    modifier = Modifier.weight(1f)
                )
                CodeCopyBox(
                    label = "NEW CODE",
                    code = flag.newCode ?: "—",
                    color = if (flag.newCode != null) AccentGreen else TextTertiary,
                    enabled = flag.newCode != null,
                    onClick = { flag.newCode?.let { onCopy(it, "New Universal Code") } },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Presence: ${flag.availability.label} • Old Subflags: ${flag.oldSubFlagCount} | New Subflags: ${flag.newSubFlagCount}",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AmoledBorder)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ASSOCIATED SUBFLAGS (${flag.subFlags.size})",
                color = TextTertiary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (flag.subFlags.isEmpty()) {
                    Text(
                        text = "No subflags associated with this universal flag.",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    flag.subFlags.forEach { sub ->
                        val sColor = statusColor(sub.status)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmoledBackground)
                                .border(1.dp, sColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onCopy(sub.name, "Subflag Name") },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sub.name,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy subflag name",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                Text(
                                    text = sub.status.label.uppercase(),
                                    color = sColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Old: ${sub.oldCode ?: "—"}",
                                    color = if (sub.oldCode != null) TextSecondary else TextTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmoledCard)
                                        .clickable(enabled = sub.oldCode != null) {
                                            sub.oldCode?.let { onCopy(it, "Old Subflag Code") }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = "New: ${sub.newCode ?: "—"}",
                                    color = if (sub.newCode != null) AccentGreen else TextTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmoledCard)
                                        .clickable(enabled = sub.newCode != null) {
                                            sub.newCode?.let { onCopy(it, "New Subflag Code") }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!flag.oldOriginalEntry.isNullOrBlank() || !flag.newOriginalEntry.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = AmoledBorder)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ORIGINAL MAPPING ENTRY",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                flag.newOriginalEntry?.let { raw ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmoledBackground)
                            .clickable { onCopy(raw, "New Original Entry") }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New: $raw",
                            color = AccentGreen,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 3,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy new raw entry",
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                flag.oldOriginalEntry?.let { raw ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmoledBackground)
                            .clickable { onCopy(raw, "Old Original Entry") }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Old: $raw",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 3,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy old raw entry",
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val raw = flag.newOriginalEntry ?: flag.oldOriginalEntry
                        val entry = raw ?: buildString {
                            val activeCode = flag.newCode ?: flag.oldCode ?: ""
                            append(activeCode).append(':').append(flag.name)
                            flag.subFlags.forEach { sf ->
                                val c = sf.newCode ?: sf.oldCode
                                if (c != null) append(':').append(c).append(':').append(sf.name)
                            }
                        }
                        onCopy(entry, "Raw MetaConfig Entry")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Entry", color = AccentGreen, fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CodeCopyBox(
    label: String,
    code: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                text = code,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = TextTertiary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun PasteMappingJsonDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onApplyPastedJson: (rawJson: String, sourceName: String) -> Unit
) {
    var sourceName by remember { mutableStateOf("$targetLabel Pasted") }
    var jsonText by remember { mutableStateOf("") }
    val quickDiagnostics = remember(jsonText) {
        if (jsonText.isBlank()) null else MappingDiagnosticsParser.parseJsonString(jsonText, sourceName)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag("dialog_paste_mapping_json"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paste $targetLabel JSON",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        jsonText = """
                            [
                              "142330:ig4a_maa_server_driven_headers:0:is_killswitched",
                              "130058:ig_loot_android:0:is_enabled:1:profile_toolbar_entrypoint_enabled:3:canvas_item_scale_enabled",
                              "112851:ig_android_carousel_async_publish:2:carousel_enable_live_query:3:carousel_live_query_timeout_seconds"
                            ]
                        """.trimIndent()
                    }
                ) {
                    Text("Load Example", color = AccentGreen, fontSize = 11.5.sp)
                }
            }

            OutlinedTextField(
                value = sourceName,
                onValueChange = { sourceName = it },
                label = { Text("Source Label", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmoledBackground,
                    unfocusedContainerColor = AmoledBackground,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = AmoledBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                placeholder = {
                    Text(
                        "[\"142330:ig4a_maa_server_driven_headers:0:is_killswitched\"]",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmoledBackground,
                    unfocusedContainerColor = AmoledBackground,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = AmoledBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            if (quickDiagnostics != null) {
                Text(
                    text = if (quickDiagnostics.isValid) {
                        "Valid JSON: ${quickDiagnostics.validEntryCount} universal flags, ${quickDiagnostics.totalSubFlagsCount} subflags"
                    } else {
                        "Error: ${quickDiagnostics.validationError ?: "Invalid format"}"
                    },
                    color = if (quickDiagnostics.isValid) AccentGreen else DangerRed,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
                Button(
                    onClick = { onApplyPastedJson(jsonText, sourceName) },
                    enabled = quickDiagnostics?.isValid == true,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Mapping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MultiCategoryFilterDialog(
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    val allCategories = remember(filterQuery) {
        val list = MetaConfigParser.CATEGORY_NAMES.filter { it != "See All Flags" }
        val q = filterQuery.trim()
        if (q.isEmpty()) list else list.filter { it.contains(q, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Categories (${selectedCategories.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearAll) {
                    Text("Reset All", color = DangerRed, fontSize = 11.5.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Search categories...", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmoledBackground,
                        unfocusedContainerColor = AmoledBackground,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = AmoledBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allCategories.forEach { cat ->
                        val checked = selectedCategories.contains(cat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (checked) AccentGreenContainer else AmoledBackground)
                                .clickable { onToggleCategory(cat) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (checked) AccentGreen else TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat,
                                color = if (checked) AccentGreen else TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
            ) {
                Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        containerColor = AmoledCard,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun MappingExportDialog(
    initialType: ExportReportType = ExportReportType.COMPLETE_COMPARISON,
    initialCategory: String = "See All Flags",
    selectedEntriesCount: Int = 0,
    onDismiss: () -> Unit,
    onSaveViaSaf: (ExportReportType, ExportFileFormat, String, String, Boolean) -> Unit,
    onCopyReport: (ExportReportType, ExportFileFormat, String, Boolean) -> Unit,
    onShareReport: (ExportReportType, ExportFileFormat, String, Boolean) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedFormat by remember { mutableStateOf(ExportFileFormat.JSON) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var exportSelectedOnly by remember { mutableStateOf(selectedEntriesCount > 0 && initialType == ExportReportType.COMPLETE_CATALOG) }
    var fileName by remember {
        mutableStateOf(
            if (exportSelectedOnly) "Selected_Flags_${selectedEntriesCount}" else initialType.defaultFileName
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Report / Catalog",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedEntriesCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (exportSelectedOnly) AccentGreenContainer else AmoledBackground)
                            .border(1.dp, if (exportSelectedOnly) AccentGreen else AmoledBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                exportSelectedOnly = !exportSelectedOnly
                                fileName = if (exportSelectedOnly) {
                                    "Selected_Flags_$selectedEntriesCount"
                                } else {
                                    selectedType.defaultFileName
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (exportSelectedOnly) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (exportSelectedOnly) AccentGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Only Selected Items ($selectedEntriesCount checked)",
                            color = if (exportSelectedOnly) AccentGreen else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!exportSelectedOnly) {
                    Text("Select Report Type:", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    ExportReportType.entries.forEach { type ->
                        val isSelected = type == selectedType
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreenContainer else AmoledBackground)
                                .border(1.dp, if (isSelected) AccentGreen else AmoledBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedType = type
                                    fileName = type.defaultFileName
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = type.title,
                                color = if (isSelected) AccentGreen else TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = type.description,
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    if (selectedType == ExportReportType.SINGLE_CATEGORY_EXPORT) {
                        Text("Category to Export:", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            MetaConfigParser.CATEGORY_NAMES.filter { it != "See All Flags" }.forEach { cat ->
                                val isCat = selectedCategory == cat
                                Text(
                                    text = if (isCat) "✓ $cat" else cat,
                                    color = if (isCat) AccentGreen else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isCat) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isCat) AccentGreenContainer else AmoledBackground)
                                        .clickable {
                                            selectedCategory = cat
                                            fileName = "Category_${cat.replace(" ", "_")}"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Output Format:", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFileFormat.entries.forEach { fmt ->
                        val isSelected = fmt == selectedFormat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreenContainer else AmoledBackground)
                                .border(1.dp, if (isSelected) AccentGreen else AmoledBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedFormat = fmt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fmt.label,
                                color = if (isSelected) AccentGreen else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Export Filename", fontSize = 11.sp) },
                    suffix = { Text(".${selectedFormat.extension}", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmoledBackground,
                        unfocusedContainerColor = AmoledBackground,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = AmoledBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    onCopyReport(selectedType, selectedFormat, selectedCategory, exportSelectedOnly)
                    onDismiss()
                }) {
                    Text("Copy", color = TextPrimary, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    onShareReport(selectedType, selectedFormat, selectedCategory, exportSelectedOnly)
                    onDismiss()
                }) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = TextPrimary, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        val finalName = fileName.trim().ifEmpty { selectedType.defaultFileName }
                        val fullName = if (finalName.endsWith(".${selectedFormat.extension}", ignoreCase = true)) {
                            finalName
                        } else {
                            "$finalName.${selectedFormat.extension}"
                        }
                        onSaveViaSaf(selectedType, selectedFormat, fullName, selectedCategory, exportSelectedOnly)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("export_saf_save_button")
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save As", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = AmoledCard,
        shape = RoundedCornerShape(14.dp)
    )
}

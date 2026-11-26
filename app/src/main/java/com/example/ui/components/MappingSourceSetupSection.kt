package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MappingDiagnosticsParser
import com.example.model.CompareHistoryRecord
import com.example.model.CompareMode
import com.example.model.CompareSource
import com.example.model.CompareSourceSlot
import com.example.model.SourcePreviewInfo
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCardHover
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningYellow

@Composable
fun CompareModeSelector(
    selectedMode: CompareMode,
    onModeSelected: (CompareMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CompareMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) AccentGreenContainer else Color.Transparent)
                    .border(
                        1.dp,
                        if (selected) AccentGreen.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 7.dp, horizontal = 6.dp)
                    .testTag("compare_mode_${mode.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    color = if (selected) AccentGreen else TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun MappingSourceSelectorCard(
    title: String,
    selectedSource: CompareSource?,
    previewInfo: SourcePreviewInfo?,
    availableSources: List<CompareSource>,
    onSelectSource: (CompareSource) -> Unit,
    onPickStorageFile: () -> Unit,
    onOpenPasteDialog: () -> Unit,
    testTagPrefix: String
) {
    var showPreviewEntries by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
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
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (previewInfo != null) {
                val badgeColor = when {
                    !previewInfo.isValid -> DangerRed
                    previewInfo.invalidCount > 0 || previewInfo.duplicateCount > 0 -> WarningYellow
                    else -> AccentGreen
                }
                val badgeIcon = when {
                    !previewInfo.isValid -> Icons.Default.ErrorOutline
                    previewInfo.invalidCount > 0 || previewInfo.duplicateCount > 0 -> Icons.Default.Warning
                    else -> Icons.Default.CheckCircle
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (previewInfo.isValid) {
                            "${previewInfo.universalFlagCount} flags • ${MappingDiagnosticsParser.formatBytes(previewInfo.byteSize)}"
                        } else {
                            "Invalid JSON"
                        },
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Selected Source Display & Preview Toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AmoledBackground)
                .border(
                    1.dp,
                    if (previewInfo != null && !previewInfo.isValid) DangerRed.copy(alpha = 0.6f) else AmoledBorder,
                    RoundedCornerShape(8.dp)
                )
                .clickable(enabled = previewInfo != null && previewInfo.previewEntries.isNotEmpty()) {
                    showPreviewEntries = !showPreviewEntries
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedSource?.getLabel() ?: "Not selected (Pick file, preset, or paste JSON)",
                        color = if (selectedSource != null) TextPrimary else TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = if (selectedSource != null) FontFamily.Monospace else FontFamily.Default
                    )
                    if (previewInfo != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (previewInfo.isValid) {
                                "${previewInfo.validationMessage} • ${previewInfo.subFlagCount} subflags"
                            } else {
                                previewInfo.validationMessage
                            },
                            color = if (previewInfo.isValid) TextSecondary else DangerRed,
                            fontSize = 11.sp
                        )
                    }
                }

                if (previewInfo != null && previewInfo.previewEntries.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showPreviewEntries) "Hide" else "Preview",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (showPreviewEntries) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Preview sample entries",
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showPreviewEntries && previewInfo != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "SAMPLE ENTRIES (FIRST ${previewInfo?.previewEntries?.size ?: 0}):",
                        color = TextTertiary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    previewInfo?.previewEntries?.forEach { entry ->
                        Text(
                            text = entry,
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Source Mode Chips: Built-in, Active Custom, Pick File (SAF), Paste JSON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceModeChip(
                label = "Built-in",
                isSelected = selectedSource is CompareSource.BuiltIn,
                onClick = { onSelectSource(CompareSource.BuiltIn) },
                testTag = "${testTagPrefix}_select_builtin"
            )

            availableSources.filterIsInstance<CompareSource.ActiveCustom>().firstOrNull()?.let { custom ->
                SourceModeChip(
                    label = "Active Custom",
                    isSelected = selectedSource is CompareSource.ActiveCustom,
                    onClick = { onSelectSource(custom) },
                    testTag = "${testTagPrefix}_select_custom"
                )
            }

            SourceModeChip(
                label = "Pick File...",
                icon = Icons.Default.FolderOpen,
                isSelected = selectedSource is CompareSource.DeviceFile,
                onClick = onPickStorageFile,
                testTag = "${testTagPrefix}_pick_file"
            )

            SourceModeChip(
                label = "Paste JSON",
                icon = Icons.Default.ContentPaste,
                isSelected = selectedSource is CompareSource.PastedJson,
                onClick = onOpenPasteDialog,
                testTag = "${testTagPrefix}_paste_json"
            )
        }
    }
}

@Composable
private fun SourceModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    icon: ImageVector? = null
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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun MultiFileCompareSetupSection(
    slots: List<CompareSourceSlot>,
    availableSources: List<CompareSource>,
    isComparing: Boolean,
    onSelectSlotSource: (slotId: String, source: CompareSource) -> Unit,
    onPickSlotFile: (slotId: String) -> Unit,
    onPasteSlotJson: (slotId: String) -> Unit,
    onRemoveSlot: (slotId: String) -> Unit,
    onAddSlot: () -> Unit,
    onRunMultiCompare: () -> Unit
) {
    val selectedCount = slots.count { it.source != null }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Compare 3 to 8 mappings side-by-side (Built-in, Custom, Storage files, or Pasted JSON).",
            color = TextSecondary,
            fontSize = 11.5.sp
        )

        slots.forEachIndexed { idx, slot ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmoledBackground)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slot.label,
                        color = AccentGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (slots.size > 2) {
                        IconButton(
                            onClick = { onRemoveSlot(slot.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove mapping slot",
                                tint = TextTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                MappingSourceSelectorCard(
                    title = "Slot #${idx + 1}",
                    selectedSource = slot.source,
                    previewInfo = slot.preview,
                    availableSources = availableSources,
                    onSelectSource = { onSelectSlotSource(slot.id, it) },
                    onPickStorageFile = { onPickSlotFile(slot.id) },
                    onOpenPasteDialog = { onPasteSlotJson(slot.id) },
                    testTagPrefix = "multi_${slot.id}"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (slots.size < 8) {
                OutlinedButton(
                    onClick = onAddSlot,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Mapping", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }

            Button(
                onClick = onRunMultiCompare,
                enabled = !isComparing && selectedCount >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = AmoledCardHover,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
                    .testTag("button_compare_multi_files")
            ) {
                if (isComparing) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Comparing...", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                } else {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compare ($selectedCount)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
fun SessionDiffSetupSection(
    history: List<CompareHistoryRecord>,
    isComparing: Boolean,
    onCompareSessions: (CompareHistoryRecord, CompareHistoryRecord) -> Unit
) {
    val snapshotHistory = remember(history) { history.filter { it.hasSnapshotFile } }
    var sessionAId by remember(snapshotHistory) { mutableStateOf(snapshotHistory.getOrNull(1)?.id) }
    var sessionBId by remember(snapshotHistory) { mutableStateOf(snapshotHistory.firstOrNull()?.id) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Compare two saved comparison sessions to track newly appearing or shifting flags over time.",
            color = TextSecondary,
            fontSize = 11.5.sp
        )

        if (snapshotHistory.size < 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledBackground)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Need at least 2 saved comparison sessions with snapshots (${snapshotHistory.size}/2 available). Run 2-File comparisons first.",
                    color = TextTertiary,
                    fontSize = 11.5.sp
                )
            }
        } else {
            Text("SESSION A (EARLIER):", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            snapshotHistory.take(6).forEach { rec ->
                val isA = sessionAId == rec.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isA) AccentGreenContainer else AmoledBackground)
                        .border(1.dp, if (isA) AccentGreen else AmoledBorder, RoundedCornerShape(8.dp))
                        .clickable { sessionAId = rec.id }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${rec.oldSourceName} → ${rec.newSourceName}",
                        color = if (isA) AccentGreen else TextPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "+${rec.newCount} / -${rec.removedCount}",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = AmoledBorder)
            Text("SESSION B (LATER):", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            snapshotHistory.take(6).forEach { rec ->
                val isB = sessionBId == rec.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isB) AccentGreenContainer else AmoledBackground)
                        .border(1.dp, if (isB) AccentGreen else AmoledBorder, RoundedCornerShape(8.dp))
                        .clickable { sessionBId = rec.id }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${rec.oldSourceName} → ${rec.newSourceName}",
                        color = if (isB) AccentGreen else TextPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "+${rec.newCount} / -${rec.removedCount}",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            val recA = snapshotHistory.firstOrNull { it.id == sessionAId }
            val recB = snapshotHistory.firstOrNull { it.id == sessionBId }
            Button(
                onClick = { if (recA != null && recB != null) onCompareSessions(recA, recB) },
                enabled = !isComparing && recA != null && recB != null && recA.id != recB.id,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = AmoledCardHover,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compare Selected Sessions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

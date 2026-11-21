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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import com.example.model.CompareSource
import com.example.model.SourcePreviewInfo
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningYellow

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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

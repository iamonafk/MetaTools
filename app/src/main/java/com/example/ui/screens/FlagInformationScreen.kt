package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.FlagInformationParser
import com.example.model.DetectedFlagType
import com.example.model.FlagParameter
import com.example.model.ParsedFlagItem
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
import java.util.UUID

@Composable
fun FlagInformationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var inputText by remember { mutableStateOf("") }
    var parsedFlags by remember { mutableStateOf<List<ParsedFlagItem>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showOriginalPreview by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }

    // Dialog for editing the selected flag
    var flagToEdit by remember { mutableStateOf<ParsedFlagItem?>(null) }

    BackHandler {
        onBack()
    }

    fun copyToClipboard(text: String, label: String) {
        if (text.isBlank()) {
            Toast.makeText(context, "$label is empty", Toast.LENGTH_SHORT).show()
            return
        }
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
    }

    fun shareFlag(flag: ParsedFlagItem) {
        val shareText = flag.toStandardTextFormat()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Flag Information: ${flag.universe.ifBlank { "MetaConfig Flag" }}")
        }
        context.startActivity(Intent.createChooser(intent, "Share Flag Information"))
    }

    fun triggerParse() {
        if (inputText.isBlank()) {
            Toast.makeText(context, "Please paste flag information first", Toast.LENGTH_SHORT).show()
            return
        }
        val result = FlagInformationParser.parse(inputText)
        if (result.isEmpty()) {
            Toast.makeText(context, "No recognizable flag fields found. Try pasting in standard format.", Toast.LENGTH_LONG).show()
        } else {
            parsedFlags = result
            selectedIndex = 0
            Toast.makeText(context, "Successfully parsed ${result.size} flag${if (result.size > 1) "s" else ""}!", Toast.LENGTH_SHORT).show()
        }
    }

    // Filter flags if search query is entered
    val filteredFlags = remember(parsedFlags, searchQuery) {
        if (searchQuery.isBlank()) {
            parsedFlags
        } else {
            val q = searchQuery.trim().lowercase()
            parsedFlags.filter { f ->
                f.universe.lowercase().contains(q) ||
                    f.stableId.lowercase().contains(q) ||
                    f.application.lowercase().contains(q) ||
                    f.useDescription.lowercase().contains(q) ||
                    f.parameters.any { p ->
                        p.name.lowercase().contains(q) || p.value.lowercase().contains(q)
                    }
            }
        }
    }

    val activeFlag = filteredFlags.getOrNull(selectedIndex) ?: filteredFlags.firstOrNull()

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
                modifier = Modifier.testTag("button_flag_info_back")
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
                    text = "Flag Information",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Parse, edit, copy, and export MetaConfig flags",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (parsedFlags.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("button_clear_flag_info")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear All",
                        tint = TextSecondary
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Paste Input Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
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
                                text = "PASTE FLAG INFORMATION",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()
                                        if (clip != null && clip.text.isNotBlank()) {
                                            inputText = clip.text
                                            triggerParse()
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste", color = AccentGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = {
                                        inputText = FlagInformationParser.EXAMPLE_INPUT
                                        triggerParse()
                                    },
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Load Example", color = Color(0xFF64B5F6), fontSize = 11.5.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Paste complete flag information here...",
                                    color = TextTertiary,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("input_paste_flag_info"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF161616),
                                unfocusedContainerColor = Color(0xFF161616),
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = AmoledBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { triggerParse() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_parse_information"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parse Information", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            if (inputText.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        inputText = ""
                                        parsedFlags = emptyList()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                                ) {
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: When Flags Are Parsed
            if (activeFlag != null) {
                // Multi-Flag Switcher Tabs (if > 1 flag)
                if (parsedFlags.size > 1) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PARSED FLAGS (${parsedFlags.size})",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )

                                // Duplicate Alert Badge
                                val duplicateCount = parsedFlags.count { it.isDuplicateUniverse || it.isDuplicateStableId }
                                if (duplicateCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = WarningYellow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$duplicateCount duplicates",
                                            color = WarningYellow,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredFlags.forEachIndexed { idx, flag ->
                                    val isSelected = (idx == selectedIndex)
                                    val displayName = flag.universe.ifBlank { "Flag #${idx + 1}" }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedIndex = idx },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = displayName,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (flag.isDuplicateUniverse || flag.isDuplicateStableId) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = "Duplicate",
                                                        tint = WarningYellow,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = AmoledCard,
                                            labelColor = TextSecondary,
                                            selectedContainerColor = AccentGreenContainer,
                                            selectedLabelColor = AccentGreen
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = AmoledBorder,
                                            selectedBorderColor = AccentGreen
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            // Quick Search Input
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Search Universe, ID, Parameter...", color = TextTertiary, fontSize = 12.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
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
                        }
                    }
                }

                // Section 3: Flag Header, Detected Type & Action Toolbar
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = AmoledCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Row: Flag Type Indicator & Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Flag Type Badge
                                val badgeColor = when (activeFlag.detectedType) {
                                    DetectedFlagType.SUB_FLAG -> AccentGreen
                                    DetectedFlagType.MULTIPLE_PARAMETERS -> Color(0xFFBA68C8)
                                    DetectedFlagType.UNIVERSAL_FLAG -> Color(0xFF64B5F6)
                                }
                                val badgeBg = when (activeFlag.detectedType) {
                                    DetectedFlagType.SUB_FLAG -> AccentGreenContainer
                                    DetectedFlagType.MULTIPLE_PARAMETERS -> Color(0xFF3E1A47)
                                    DetectedFlagType.UNIVERSAL_FLAG -> Color(0xFF1E3A5F)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeFlag.detectedType.displayName,
                                        color = badgeColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Edit Flag button
                                TextButton(
                                    onClick = { flagToEdit = activeFlag },
                                    modifier = Modifier.testTag("button_edit_flag")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Flag", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Duplicate Warning if present
                            if (activeFlag.isDuplicateUniverse || activeFlag.isDuplicateStableId) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF332A14))
                                        .border(1.dp, WarningYellow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = WarningYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Duplicate warning: Universe or Stable ID matches another pasted flag.",
                                        color = WarningYellow,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            // Primary Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Prominent COPY ALL Button
                                Button(
                                    onClick = {
                                        copyToClipboard(
                                            activeFlag.toStandardTextFormat(),
                                            "complete flag information"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("button_copy_all_flag"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Share Flag
                                OutlinedButton(
                                    onClick = { shareFlag(activeFlag) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_share_flag"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", fontSize = 12.sp)
                                }

                                // JSON Export
                                OutlinedButton(
                                    onClick = { showJsonDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_json_export"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder)
                                ) {
                                    Text("JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Sub-actions: Auto Format & Original Preview Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val formatted = FlagInformationParser.autoFormatText(inputText)
                                        inputText = formatted
                                        parsedFlags = FlagInformationParser.parse(formatted)
                                        Toast.makeText(context, "Auto formatted text", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto Format Text", color = Color(0xFF64B5F6), fontSize = 11.5.sp)
                                }

                                TextButton(
                                    onClick = { showOriginalPreview = !showOriginalPreview }
                                ) {
                                    Text(
                                        text = if (showOriginalPreview) "Show Formatted Cards" else "Original vs Formatted",
                                        color = TextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Original vs Formatted Preview (if toggled)
                if (showOriginalPreview) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AmoledBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = AmoledCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "ORIGINAL RAW TEXT",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF161616))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = activeFlag.rawPastedText.ifBlank { "N/A" },
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "STANDARD FORMATTED TEXT",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF161616))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = activeFlag.toStandardTextFormat(),
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 5: Individual Information Cards
                // CRITICAL DESIGN SPECIFICATION:
                // "The field label must appear ABOVE its box, not inside the box."
                // "Every field must have its own independent rounded rectangle with a subtle dark-gray border,
                // AMOLED background, white text, and a small copy icon on the right."

                // 1. Application
                item {
                    InformationFieldBox(
                        label = "Application",
                        value = activeFlag.application,
                        isMissing = activeFlag.application.isBlank(),
                        onCopy = { copyToClipboard(activeFlag.application, "Application") }
                    )
                }

                // 2. Universe
                item {
                    InformationFieldBox(
                        label = "Universe",
                        value = activeFlag.universe,
                        isMissing = activeFlag.universe.isBlank(),
                        onCopy = { copyToClipboard(activeFlag.universe, "Universe") }
                    )
                }

                // 3. Stable Id
                item {
                    InformationFieldBox(
                        label = "Stable Id",
                        value = activeFlag.stableId,
                        isMissing = activeFlag.stableId.isBlank(),
                        isMonospace = true,
                        onCopy = { copyToClipboard(activeFlag.stableId, "Stable ID") }
                    )
                }

                // 4. Parameters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Parameters",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeFlag.parameters.isEmpty()) {
                                MissingBadge()
                            } else {
                                Text(
                                    text = "(${activeFlag.parameters.size})",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (activeFlag.parameters.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmoledCard)
                                    .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "No parameters defined",
                                    color = TextTertiary,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            // Display each parameter in its own separate rounded box
                            activeFlag.parameters.forEach { param ->
                                ParameterItemBox(
                                    parameter = param,
                                    onCopy = {
                                        copyToClipboard(param.toFormattedString(), "Parameter ${param.name}")
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Use Description
                item {
                    InformationFieldBox(
                        label = "Use",
                        value = activeFlag.useDescription,
                        isMissing = activeFlag.useDescription.isBlank(),
                        isMultiline = true,
                        onCopy = { copyToClipboard(activeFlag.useDescription, "Use description") }
                    )
                }

                // 6. Version
                item {
                    InformationFieldBox(
                        label = activeFlag.versionLabel.ifBlank { "Instagram Version" },
                        value = activeFlag.appVersion,
                        isMissing = activeFlag.appVersion.isBlank(),
                        isMonospace = true,
                        onCopy = { copyToClipboard(activeFlag.appVersion, activeFlag.versionLabel) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Edit Flag Dialog
    if (flagToEdit != null) {
        EditFlagDialog(
            initialFlag = flagToEdit!!,
            onDismiss = { flagToEdit = null },
            onSave = { updated ->
                // Update in parsedFlags list
                parsedFlags = parsedFlags.map { if (it.id == updated.id) updated else it }
                flagToEdit = null
                Toast.makeText(context, "Saved changes to flag", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // JSON Export Dialog
    if (showJsonDialog) {
        val jsonOutput = remember(parsedFlags) {
            FlagInformationParser.exportToJson(parsedFlags)
        }
        Dialog(onDismissRequest = { showJsonDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AmoledCard)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exported JSON",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showJsonDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = jsonOutput,
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, jsonOutput)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share JSON"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Share JSON", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                copyToClipboard(jsonOutput, "JSON")
                                showJsonDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
                        ) {
                            Text("Copy JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(text = "Clear All Information?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "This will remove the current input text and all parsed flags. Are you sure?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        inputText = ""
                        parsedFlags = emptyList()
                        showClearDialog = false
                        Toast.makeText(context, "Cleared all information", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = AmoledCard,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

/**
 * Standard individual information card conforming to requirement:
 * "The field label must appear ABOVE its box, not inside the box."
 * "Every field must have its own independent rounded rectangle with a subtle dark-gray border,
 * AMOLED background, white text, and a small copy icon on the right."
 */
@Composable
private fun InformationFieldBox(
    label: String,
    value: String,
    isMissing: Boolean,
    isMultiline: Boolean = false,
    isMonospace: Boolean = false,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label ABOVE the box
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (isMissing) {
                MissingBadge()
            }
        }

        // Rounded Box with AMOLED background, dark-gray border, white text, and copy icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AmoledCard)
                .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = if (isMultiline) 12.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (value.isNotBlank()) value else "Not specified",
                    color = if (value.isNotBlank()) TextPrimary else TextTertiary,
                    fontSize = 13.5.sp,
                    lineHeight = if (isMultiline) 19.sp else 16.sp,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("copy_${label.lowercase().replace(" ", "_")}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $label",
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual Box for a parameter item with name, value, subflag tag, and copy button.
 */
@Composable
private fun ParameterItemBox(
    parameter: FlagParameter,
    onCopy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = parameter.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (parameter.isSubFlag) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentGreenContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Sub Flag",
                                color = AccentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (parameter.value.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Value: ${parameter.value}",
                        color = Color(0xFF64B5F6),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy parameter",
                    tint = AccentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MissingBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF332A14))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Missing",
            color = WarningYellow,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Edit Dialog allowing full editing of every field with immediate save/reset.
 */
@Composable
private fun EditFlagDialog(
    initialFlag: ParsedFlagItem,
    onDismiss: () -> Unit,
    onSave: (ParsedFlagItem) -> Unit
) {
    var app by remember { mutableStateOf(initialFlag.application) }
    var universe by remember { mutableStateOf(initialFlag.universe) }
    var stableId by remember { mutableStateOf(initialFlag.stableId) }
    var useDesc by remember { mutableStateOf(initialFlag.useDescription) }
    var version by remember { mutableStateOf(initialFlag.appVersion) }
    var versionLabel by remember { mutableStateOf(initialFlag.versionLabel) }

    var paramsList by remember { mutableStateOf(initialFlag.parameters) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Flag Information",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = AmoledBorder)

                // Application
                EditTextField(
                    label = "Application",
                    value = app,
                    onValueChange = { app = it }
                )

                // Universe
                EditTextField(
                    label = "Universe",
                    value = universe,
                    onValueChange = { universe = it }
                )

                // Stable Id
                EditTextField(
                    label = "Stable Id",
                    value = stableId,
                    onValueChange = { stableId = it }
                )

                // Parameters Editor
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parameters (${paramsList.size})",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                paramsList = paramsList + FlagParameter(
                                    name = "new_param",
                                    value = "true",
                                    isSubFlag = true
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Param", color = AccentGreen, fontSize = 11.5.sp)
                        }
                    }

                    paramsList.forEachIndexed { pIdx, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161616))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = p.name,
                                onValueChange = { newName ->
                                    paramsList = paramsList.toMutableList().also {
                                        it[pIdx] = p.copy(name = newName)
                                    }
                                },
                                label = { Text("Name", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = AmoledBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = p.value,
                                onValueChange = { newVal ->
                                    paramsList = paramsList.toMutableList().also {
                                        it[pIdx] = p.copy(value = newVal)
                                    }
                                },
                                label = { Text("Value", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(0.9f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = AmoledBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            // Sub Flag Checkbox
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SubFlag", fontSize = 9.sp, color = TextTertiary)
                                Checkbox(
                                    checked = p.isSubFlag,
                                    onCheckedChange = { checked ->
                                        paramsList = paramsList.toMutableList().also {
                                            it[pIdx] = p.copy(isSubFlag = checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = {
                                    paramsList = paramsList.filterIndexed { i, _ -> i != pIdx }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Use Description
                OutlinedTextField(
                    value = useDesc,
                    onValueChange = { useDesc = it },
                    label = { Text("Use Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
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

                // Version
                EditTextField(
                    label = versionLabel.ifBlank { "Instagram Version" },
                    value = version,
                    onValueChange = { version = it }
                )

                // Buttons: Save Changes & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset back to initial
                            app = initialFlag.application
                            universe = initialFlag.universe
                            stableId = initialFlag.stableId
                            useDesc = initialFlag.useDescription
                            version = initialFlag.appVersion
                            paramsList = initialFlag.parameters
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningYellow)
                    ) {
                        Text("Reset", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val updated = initialFlag.copy(
                                application = app.trim(),
                                universe = universe.trim(),
                                stableId = stableId.trim(),
                                parameters = paramsList,
                                useDescription = useDesc.trim(),
                                appVersion = version.trim(),
                                versionLabel = versionLabel
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
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
}

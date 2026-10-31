package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledBorderHighlight
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledCardHover
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun FlagCard(
    flag: UniversalFlag,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyUniversalName: () -> Unit,
    onCopyUniversalCode: () -> Unit,
    onCopySubFlagName: (SubFlag) -> Unit,
    onCopySubFlagCode: (SubFlag) -> Unit,
    onShareFlag: () -> Unit,
    onShareSubFlag: (SubFlag) -> Unit,
    modifier: Modifier = Modifier,
    highlightedSubFlag: SubFlag? = null
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "flag_chevron_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledBackground)
            .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
            .testTag("flag_card_${flag.code}")
    ) {
        // Flag Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Code Badge + Sub-flags count + Share & Expand Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Universal Flag Code Badge (clickable to copy code)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGreenContainer)
                        .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onCopyUniversalCode)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("copy_code_badge_${flag.code}")
                ) {
                    Text(
                        text = "CODE ",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = flag.code,
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy universal code",
                        tint = AccentGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Sub-flags count pill + Share & Expand button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (flag.subFlags.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmoledCardHover)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${flag.subFlags.size} sub-flag${if (flag.subFlags.size > 1) "s" else ""}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onShareFlag,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Flag",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (flag.subFlags.isNotEmpty()) {
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse sub-flags" else "Expand sub-flags",
                                tint = TextPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotationState)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Universal Flag Name (Tap anywhere on name chip to copy universal name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmoledCardHover)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                    .clickable(onClick = onCopyUniversalName)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("copy_name_box_${flag.code}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = flag.name,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy universal name",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Sub-flags section
        AnimatedVisibility(
            visible = isExpanded && flag.subFlags.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmoledCard)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                HorizontalDivider(color = AmoledBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ASSOCIATED SUB-FLAGS (${flag.subFlags.size})",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                flag.subFlags.forEach { subFlag ->
                    val isHighlighted = highlightedSubFlag?.name == subFlag.name &&
                            highlightedSubFlag.code == subFlag.code

                    SubFlagRow(
                        subFlag = subFlag,
                        isHighlighted = isHighlighted,
                        onCopyName = { onCopySubFlagName(subFlag) },
                        onCopyCode = { onCopySubFlagCode(subFlag) },
                        onShare = { onShareSubFlag(subFlag) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubFlagRow(
    subFlag: SubFlag,
    isHighlighted: Boolean,
    onCopyName: () -> Unit,
    onCopyCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isHighlighted) AccentGreen else AmoledBorder
    val backgroundColor = if (isHighlighted) AccentGreenContainer.copy(alpha = 0.5f) else AmoledBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Code Badge (Clickable to copy code)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(AmoledCard)
                .border(1.dp, if (isHighlighted) AccentGreen else AmoledBorderHighlight, RoundedCornerShape(4.dp))
                .clickable(onClick = onCopyCode)
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .testTag("copy_sub_code_${subFlag.code}"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subFlag.code,
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = TextTertiary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sub-flag name (Clickable to copy name)
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onCopyName)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("copy_sub_name_${subFlag.name}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subFlag.name,
                color = if (isHighlighted) AccentGreen else TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy sub-flag name",
                tint = TextTertiary,
                modifier = Modifier.size(12.dp)
            )
        }

        // Share button
        IconButton(
            onClick = onShare,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share sub-flag",
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

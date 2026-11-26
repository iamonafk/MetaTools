package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NewFlagItem
import com.example.model.NewSubFlagItem
import com.example.model.ParentChildChangeItem
import com.example.model.PotentialRenameCandidate
import com.example.model.RemovedFlagItem
import com.example.model.RemovedSubFlagItem
import com.example.model.UnchangedFlagItem
import com.example.model.UpdatedFlagCodeItem
import com.example.model.UpdatedSubFlagItem
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
import com.example.ui.theme.UpdatedOrange
import com.example.ui.theme.WarningYellow

// 1. Newly Added Universal Flag Card
@Composable
fun NewUniversalFlagCard(
    item: NewFlagItem,
    onCopyName: () -> Unit,
    onCopyCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onInspect)
            .padding(12.dp)
            .testTag("compare_new_flag_${item.flag.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentGreenContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW FLAG",
                        color = AccentGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AmoledBackground)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onCopyCode)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.flag.code,
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
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (item.flag.subFlags.isNotEmpty()) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle sub-flags",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Flag Name with Copy Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(AmoledBackground)
                .clickable(onClick = onCopyName)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.flag.name,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Name",
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }

        if (item.flag.subFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.flag.subFlags.size} sub-flag(s) included",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                HorizontalDivider(color = AmoledBorder)
                Spacer(modifier = Modifier.height(6.dp))
                item.flag.subFlags.forEach { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${sub.name}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "[${sub.code}]",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 2. Newly Added Sub-Flag in Existing Universal Flag
@Composable
fun NewSubFlagCard(
    item: NewSubFlagItem,
    onCopySubFlagName: () -> Unit,
    onCopySubFlagCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, AccentGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onInspect)
            .padding(12.dp)
            .testTag("compare_new_subflag_${item.subFlag.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentGreenContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+ NEW SUB-FLAG",
                        color = AccentGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "in ${item.parentFlagName}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        HorizontalDivider(color = AmoledBorder, modifier = Modifier.padding(vertical = 6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCopySubFlagName)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.subFlag.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy subflag name",
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier
                    .clickable(onClick = onCopySubFlagCode)
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Code: ${item.subFlag.code}",
                    color = AccentGreen,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy subflag code",
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// 3. Removed Universal Flag Card
@Composable
fun RemovedUniversalFlagCard(
    item: RemovedFlagItem,
    onCopyName: () -> Unit,
    onCopyCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onInspect)
            .padding(12.dp)
            .testTag("compare_removed_flag_${item.flag.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DangerRed.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "REMOVED FLAG",
                        color = DangerRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AmoledBackground)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onCopyCode)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.flag.code,
                            color = DangerRed,
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
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(AmoledBackground)
                .clickable(onClick = onCopyName)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.flag.name,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy name",
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }

        if (item.flag.subFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Had ${item.flag.subFlags.size} associated sub-flag(s)",
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

// 4. Removed Sub-Flag from Existing Flag
@Composable
fun RemovedSubFlagCard(
    item: RemovedSubFlagItem,
    onCopySubFlagName: () -> Unit,
    onCopySubFlagCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, DangerRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onInspect)
            .padding(12.dp)
            .testTag("compare_removed_subflag_${item.subFlag.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DangerRed.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "- REMOVED SUB-FLAG",
                        color = DangerRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "from ${item.parentFlagName}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        HorizontalDivider(color = AmoledBorder, modifier = Modifier.padding(vertical = 6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCopySubFlagName)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.subFlag.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy subflag name",
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier
                    .clickable(onClick = onCopySubFlagCode)
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Code: ${item.subFlag.code}",
                    color = DangerRed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy subflag code",
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// 5. Updated Sub-Flag Code Card (Side by Side comparison matching user prompt)
@Composable
fun UpdatedSubFlagCard(
    item: UpdatedSubFlagItem,
    onCopySubFlagName: () -> Unit,
    onCopyOldCode: () -> Unit,
    onCopyNewCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, UpdatedOrange.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .clickable(onClick = onInspect)
            .padding(12.dp)
            .testTag("compare_updated_subflag_${item.subFlagName}")
    ) {
        // Status header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(UpdatedOrange.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "STATUS: UPDATED",
                    color = UpdatedOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Universal Flag name
        Text(
            text = "Universal Flag: \"${item.parentFlagName}\"",
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Sub-flag name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopySubFlagName),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sub-Flag: \"${item.subFlagName}\"",
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy name",
                tint = TextTertiary,
                modifier = Modifier.size(13.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Side-by-side Old Code vs New Code comparison
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AmoledBackground)
                .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Old Code
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCopyOldCode)
            ) {
                Text(
                    text = "OLD CODE",
                    color = TextTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.oldCode,
                        color = DangerRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy old code",
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Changed to",
                tint = UpdatedOrange,
                modifier = Modifier.size(18.dp)
            )

            // New Code
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCopyNewCode),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "NEW CODE",
                    color = TextTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy new code",
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.newCode,
                        color = AccentGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// 6. Updated Universal Flag Code Card
@Composable
fun UpdatedUniversalFlagCodeCard(
    item: UpdatedFlagCodeItem,
    onCopyFlagName: () -> Unit,
    onCopyOldCode: () -> Unit,
    onCopyNewCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, UpdatedOrange.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .clickable(onClick = onInspect)
            .padding(12.dp)
            .testTag("compare_updated_flagcode_${item.flagName}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(UpdatedOrange.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "UNIVERSAL CODE CHANGED",
                    color = UpdatedOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopyFlagName),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Universal Flag: \"${item.flagName}\"",
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy name",
                tint = TextTertiary,
                modifier = Modifier.size(13.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AmoledBackground)
                .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCopyOldCode)
            ) {
                Text(
                    text = "OLD UNIVERSAL CODE",
                    color = TextTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.oldCode,
                        color = DangerRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy old code",
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Changed to",
                tint = UpdatedOrange,
                modifier = Modifier.size(18.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCopyNewCode),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "NEW UNIVERSAL CODE",
                    color = TextTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy new code",
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.newCode,
                        color = AccentGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// 7. Unchanged Universal Flag Card (Neutral Gray)
@Composable
fun UnchangedUniversalFlagCard(
    item: UnchangedFlagItem,
    onCopyName: () -> Unit,
    onCopyCode: () -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onInspect)
            .padding(12.dp)
            .testTag("compare_unchanged_flag_${item.flag.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TextSecondary.copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "UNCHANGED",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.flag.category,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AmoledBackground)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(4.dp))
                    .clickable(onClick = onCopyCode)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.flag.code,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(AmoledBackground)
                .clickable(onClick = onCopyName)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.flag.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${item.flag.subFlags.size} subflags",
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

// 8. Potential Rename Candidate Card
@Composable
fun PotentialRenameCandidateCard(
    candidate: PotentialRenameCandidate,
    onInspectOld: () -> Unit,
    onInspectNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, WarningYellow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(WarningYellow.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "POSSIBLE RENAME (${candidate.scope})",
                    color = WarningYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Shared Code: ${candidate.sharedCode}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (candidate.parentFlagName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Parent: ${candidate.parentFlagName}",
                color = TextTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(AmoledBackground)
                .clickable(onClick = onInspectOld)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OLD: ", color = DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = candidate.oldName,
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(AmoledBackground)
                .clickable(onClick = onInspectNew)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEW: ", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = candidate.newName,
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 9. Parent-Child Subflag Re-parenting Card
@Composable
fun ParentChildChangeCard(
    item: ParentChildChangeItem,
    onCopySubFlagName: () -> Unit,
    onInspectOldParent: () -> Unit,
    onInspectNewParent: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, UpdatedOrange.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("compare_parent_child_${item.subFlagName}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = UpdatedOrange,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "PARENT MOVED",
                    color = UpdatedOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.category,
                    color = TextTertiary,
                    fontSize = 10.5.sp
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share parent change",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopySubFlagName),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.subFlagName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy subflag name",
                tint = TextTertiary,
                modifier = Modifier.size(13.dp)
            )
        }

        HorizontalDivider(color = AmoledBorder, modifier = Modifier.padding(vertical = 6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onInspectOldParent)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OLD PARENT: ", color = DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${item.oldParentFlagName} (${item.oldParentCode}) • sub:${item.oldSubCode}",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onInspectNewParent)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEW PARENT: ", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${item.newParentFlagName} (${item.newParentCode}) • sub:${item.newSubCode}",
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

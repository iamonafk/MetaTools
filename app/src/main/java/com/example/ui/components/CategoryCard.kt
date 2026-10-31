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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FlagCategory
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CategoryCard(
    category: FlagCategory,
    isExpanded: Boolean,
    flags: List<UniversalFlag>?,
    expandedFlagCodes: Set<String>,
    onToggleCategory: () -> Unit,
    onToggleFlag: (String) -> Unit,
    onCopyUniversalName: (UniversalFlag) -> Unit,
    onCopyUniversalCode: (UniversalFlag) -> Unit,
    onCopySubFlagName: (SubFlag) -> Unit,
    onCopySubFlagCode: (SubFlag) -> Unit,
    onShareFlag: (UniversalFlag) -> Unit,
    onShareSubFlag: (SubFlag) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
            .testTag("category_card_${category.id}")
    ) {
        // Main row header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCategory)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Category Name aligned to left
            Text(
                text = category.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (category.isAllFlagsSpecial) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("category_title_${category.id}")
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: Compact black count badge with green count + chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Compact black badge with green number
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmoledBackground)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${category.flagCount}",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier.testTag("category_count_${category.id}")
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Chevron icon
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse category" else "Expand category",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationState)
                )
            }
        }

        // Expanded content: flags list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (flags == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentGreen,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (flags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flags available in this category",
                            color = TextTertiary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // For performance, display the list of flags
                    flags.forEach { flag ->
                        FlagCard(
                            flag = flag,
                            isExpanded = expandedFlagCodes.contains(flag.code),
                            onToggleExpand = { onToggleFlag(flag.code) },
                            onCopyUniversalName = { onCopyUniversalName(flag) },
                            onCopyUniversalCode = { onCopyUniversalCode(flag) },
                            onCopySubFlagName = onCopySubFlagName,
                            onCopySubFlagCode = onCopySubFlagCode,
                            onShareFlag = { onShareFlag(flag) },
                            onShareSubFlag = onShareSubFlag,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

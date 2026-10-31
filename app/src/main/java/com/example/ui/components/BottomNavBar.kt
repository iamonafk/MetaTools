package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.AppTab

@Composable
fun StickyBottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AmoledBackground)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = AmoledBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            }
            .padding(bottom = navBarPadding)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                label = "Home",
                iconRes = R.drawable.ic_home,
                isSelected = selectedTab == AppTab.HOME,
                onClick = { onTabSelected(AppTab.HOME) },
                testTag = "nav_tab_home"
            )

            NavBarItem(
                label = "Flags",
                iconRes = R.drawable.ic_flags,
                isSelected = selectedTab == AppTab.FLAGS,
                onClick = { onTabSelected(AppTab.FLAGS) },
                testTag = "nav_tab_flags"
            )

            NavBarItem(
                label = "Tools",
                iconRes = R.drawable.ic_tools,
                isSelected = selectedTab == AppTab.TOOLS,
                onClick = { onTabSelected(AppTab.TOOLS) },
                testTag = "nav_tab_tools"
            )

            NavBarItem(
                label = "Profile",
                iconRes = R.drawable.ic_profile,
                isSelected = selectedTab == AppTab.PROFILE,
                onClick = { onTabSelected(AppTab.PROFILE) },
                testTag = "nav_tab_profile"
            )
        }
    }
}

@Composable
private fun NavBarItem(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (isSelected) TextPrimary else TextTertiary
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Column(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = fontWeight,
            letterSpacing = 0.2.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

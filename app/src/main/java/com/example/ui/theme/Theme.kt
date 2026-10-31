package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AmoledColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = AmoledBackground,
    primaryContainer = AccentGreenContainer,
    onPrimaryContainer = AccentGreen,
    secondary = AccentGreenBright,
    onSecondary = AmoledBackground,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = AmoledCard,
    onSurface = TextPrimary,
    surfaceVariant = AmoledCardHover,
    onSurfaceVariant = TextSecondary,
    outline = AmoledBorder,
    outlineVariant = AmoledBorderHighlight
)

@Composable
fun MetaToolsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledColorScheme,
        typography = Typography,
        content = content
    )
}

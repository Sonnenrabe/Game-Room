package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = Color(0xFF003844),
    onPrimaryContainer = Color(0xFF8CF3FF),

    secondary = NeonPurple,
    onSecondary = Color(0xFF280058),
    secondaryContainer = Color(0xFF3B1578),
    onSecondaryContainer = Color(0xFFE9DDFF),

    tertiary = NeonGreen,
    onTertiary = Color(0xFF00391A),
    tertiaryContainer = Color(0xFF005228),
    onTertiaryContainer = Color(0xFF6BFF9E),

    background = GameDarkBackground,
    onBackground = TextPrimary,

    surface = GameDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = GameDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    error = NeonRed,
    onError = Color.White,

    outline = Color(0xFF2D3C5C),
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

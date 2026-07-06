package com.android.launcher3.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAFF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1DFFF),
    onSecondaryContainer = Color(0xFF11006E),
    tertiary = Color(0xFF34C759),
    onTertiary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xE6F2F2F7),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFD1D1D6),
    surfaceTint = Color(0xFF007AFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00376B),
    onPrimaryContainer = Color(0xFFD6EAFF),
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B1A6E),
    onSecondaryContainer = Color(0xFFE1DFFF),
    tertiary = Color(0xFF30D158),
    onTertiary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xE61C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF3A3A3C),
    surfaceTint = Color(0xFF0A84FF),
)

@Composable
fun LauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun rememberLauncherHazeState(): HazeState {
    return androidx.compose.runtime.remember { HazeState() }
}

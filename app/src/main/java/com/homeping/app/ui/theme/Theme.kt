package com.homeping.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = HomePingBlue,
    onPrimary = HomePingOnBlue,
    primaryContainer = HomePingBlueDark,
    onPrimaryContainer = HomePingOnBlue,
    secondary = HomePingAccent,
    onSecondary = Color.White,
    background = HomePingSurface,
    onBackground = HomePingOnSurface,
    surface = HomePingSurface,
    onSurface = HomePingOnSurface,
    onSurfaceVariant = HomePingMuted,
    error = HomePingAccent,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DADE2),
    onPrimary = Color(0xFF0B1C28),
    primaryContainer = HomePingBlue,
    onPrimaryContainer = HomePingOnBlue,
    secondary = Color(0xFFE74C3C),
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFE74C3C),
    onError = Color.White,
)

@Composable
fun HomePingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HomePingTypography,
        content = content,
    )
}

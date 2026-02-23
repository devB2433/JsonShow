package com.jsonshow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    secondary = Teal40,
    tertiary = Orange40,
    surface = Surface,
    surfaceVariant = Color(0xFFE7E8EC),
    background = Surface,
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Color(0xFF2A4A9A),
    secondary = Teal80,
    tertiary = Orange80,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF44474E),
    background = SurfaceDark,
)

@Composable
fun JsonShowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

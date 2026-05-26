package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CustomLightColorScheme = lightColorScheme(
    primary = MinimalGreen,
    onPrimary = Color.White,
    primaryContainer = LightSageContainer,
    onPrimaryContainer = AccentSageText,
    secondary = TextMuted,
    onSecondary = Color.White,
    background = BaseBackground,
    onBackground = TextDark,
    surface = CreamSurface,
    onSurface = TextDark,
    surfaceVariant = MutedGreyBorder,
    onSurfaceVariant = TextMuted,
    outline = TextMuted,
    error = SoftAlertRed
)

private val CustomDarkColorScheme = darkColorScheme(
    primary = LightSageContainer,
    onPrimary = AccentSageText,
    primaryContainer = MinimalGreen,
    onPrimaryContainer = Color.White,
    secondary = TextMuted,
    onSecondary = Color.White,
    background = TextDark,
    onBackground = BaseBackground,
    surface = Color(0xFF1E293B),
    onSurface = BaseBackground,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextMuted,
    outline = TextMuted,
    error = SoftAlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to strictly enforce the brand minimalist design
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CustomDarkColorScheme else CustomLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

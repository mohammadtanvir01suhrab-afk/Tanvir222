package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CandyColorScheme = darkColorScheme(
    primary = CandyPrimary,
    onPrimary = Color.White,
    primaryContainer = CandyPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = CandySecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE65100),
    onSecondaryContainer = Color.White,
    tertiary = CandyTertiary,
    onTertiary = Color.Black,
    background = CandyBackground,
    onBackground = Color.White,
    surface = CandySurface,
    onSurface = Color.White,
    surfaceVariant = CandySurfaceVariant,
    onSurfaceVariant = Color(0xFFE1BEE7)
)

private val CandyLightScheme = lightColorScheme(
    primary = CandyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8BBD0),
    onPrimaryContainer = CandyPrimaryVariant,
    secondary = CandySecondary,
    onSecondary = Color.White,
    tertiary = CandyTertiary,
    onTertiary = Color.White,
    background = Color(0xFFFFF0F5),
    onBackground = Color(0xFF2A0845),
    surface = Color.White,
    onSurface = Color(0xFF2A0845),
    surfaceVariant = Color(0xFFF3E5F5),
    onSurfaceVariant = Color(0xFF4A1A6B)
)

@Composable
fun TanvirCandyCrushTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CandyColorScheme else CandyLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

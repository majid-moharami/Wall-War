package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WallWarPurple,
    onPrimary = WallWarPurpleDark,
    primaryContainer = WallWarPurpleContainer,
    onPrimaryContainer = Color.White,
    secondary = WallWarPurpleLight,
    onSecondary = WallWarPurpleDark,
    tertiary = WallWarAmber,
    onTertiary = Color.Black,
    background = WallWarDarkBg,
    onBackground = Color(0xFFE2E2E6),
    surface = WallWarDarkSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = WallWarDarkCard,
    onSurfaceVariant = Color(0xFF919194),
    outline = WallWarBorder
)

private val LightColorScheme = darkColorScheme(
    primary = WallWarPurple,
    onPrimary = WallWarPurpleDark,
    primaryContainer = WallWarPurpleContainer,
    onPrimaryContainer = Color.White,
    secondary = WallWarPurpleLight,
    onSecondary = WallWarPurpleDark,
    tertiary = WallWarAmber,
    onTertiary = Color.Black,
    background = WallWarDarkBg,
    onBackground = Color(0xFFE2E2E6),
    surface = WallWarDarkSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = WallWarDarkCard,
    onSurfaceVariant = Color(0xFF919194),
    outline = WallWarBorder
)

@Composable
fun WallWarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

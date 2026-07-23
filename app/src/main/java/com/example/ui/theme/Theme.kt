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
    primary = WallRushPurple,
    onPrimary = WallRushPurpleDark,
    primaryContainer = WallRushPurpleContainer,
    onPrimaryContainer = Color.White,
    secondary = WallRushPurpleLight,
    onSecondary = WallRushPurpleDark,
    tertiary = WallRushAmber,
    onTertiary = Color.Black,
    background = WallRushDarkBg,
    onBackground = Color(0xFFE2E2E6),
    surface = WallRushDarkSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = WallRushDarkCard,
    onSurfaceVariant = Color(0xFF919194),
    outline = WallRushBorder
)

private val LightColorScheme = darkColorScheme(
    primary = WallRushPurple,
    onPrimary = WallRushPurpleDark,
    primaryContainer = WallRushPurpleContainer,
    onPrimaryContainer = Color.White,
    secondary = WallRushPurpleLight,
    onSecondary = WallRushPurpleDark,
    tertiary = WallRushAmber,
    onTertiary = Color.Black,
    background = WallRushDarkBg,
    onBackground = Color(0xFFE2E2E6),
    surface = WallRushDarkSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = WallRushDarkCard,
    onSurfaceVariant = Color(0xFF919194),
    outline = WallRushBorder
)

@Composable
fun WallRushTheme(
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

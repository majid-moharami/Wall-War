package com.wallwar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003847),
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4D0026),
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonAmber,
    onTertiary = Color.Black,
    background = NeonDarkBg,
    onBackground = Color(0xFFF0F4FF),
    surface = NeonDarkSurface,
    onSurface = Color(0xFFF0F4FF),
    surfaceVariant = NeonDarkCard,
    onSurfaceVariant = Color(0xFFA0ACCC),
    outline = NeonBorder
)

@Composable
fun WallWarTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeonColorScheme,
        typography = Typography,
        content = content
    )
}

package com.wallwar.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // App uses deep neon dark canvas; ensure status bar and navigation bar icons remain bright and clear in both light and dark system settings
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    val currentDensity = LocalDensity.current
    val nonScaledDensity = Density(
        density = currentDensity.density,
        fontScale = 1.0f
    )

    CompositionLocalProvider(
        LocalDensity provides nonScaledDensity
    ) {
        MaterialTheme(
            colorScheme = NeonColorScheme,
            typography = Typography,
            content = content
        )
    }
}

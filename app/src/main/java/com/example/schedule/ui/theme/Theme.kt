package com.example.schedule.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Composable
fun ScheduleTheme(
    themeKey: String = "morandi",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preset = (if (themeKey.startsWith("custom_")) ThemeManager.loadCustomTheme(context, themeKey)
                  else null) ?: getPreset(themeKey)
    val view = LocalView.current
    val isDark = preset.isDark

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = preset.scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = preset.scheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}

private val Shapes = Shapes(
    small = RoundedCornerShape(CornerSize(8.dp)),
    medium = RoundedCornerShape(CornerSize(16.dp)),
    large = RoundedCornerShape(CornerSize(24.dp)),
)

private val Typography = Typography(
    titleLarge = Typography().titleLarge,
    titleMedium = Typography().titleMedium,
    bodyLarge = Typography().bodyLarge,
    bodyMedium = Typography().bodyMedium,
    bodySmall = Typography().bodySmall,
    labelSmall = Typography().labelSmall,
)

package com.example.schedule.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val MorandiColorScheme = lightColorScheme(
    primary = MorandiPrimary,
    onPrimary = MorandiOnPrimary,
    secondary = MorandiSecondary,
    tertiary = MorandiTertiary,
    background = MorandiBackground,
    surface = MorandiSurface,
    surfaceVariant = MorandiSurfaceVariant,
    onSurface = MorandiOnSurface,
    onSurfaceVariant = MorandiOnSurfaceVariant,
    error = MorandiError,
    onBackground = MorandiOnSurface,
    onSecondary = MorandiOnPrimary,
    onTertiary = MorandiOnPrimary,
    onError = Color.White,
    outline = Color(0xFFD5CFC4),
    outlineVariant = Color(0xFFE8E2D8),
)

@Composable
fun ScheduleTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MorandiBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = MorandiColorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}

// 大圆角形状
private val Shapes = Shapes(
    small = RoundedCornerShape(CornerSize(8.dp)),
    medium = RoundedCornerShape(CornerSize(16.dp)),
    large = RoundedCornerShape(CornerSize(24.dp)),
)

// 定制排版
private val Typography = Typography(
    titleLarge = androidx.compose.material3.Typography().titleLarge.copy(
        color = MorandiOnSurface
    ),
    titleMedium = androidx.compose.material3.Typography().titleMedium.copy(
        color = MorandiOnSurface
    ),
    bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(
        color = MorandiOnSurface
    ),
    bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(
        color = MorandiOnSurfaceVariant
    ),
    bodySmall = androidx.compose.material3.Typography().bodySmall.copy(
        color = MorandiOnSurfaceVariant
    ),
    labelSmall = androidx.compose.material3.Typography().labelSmall.copy(
        color = MorandiOnSurfaceVariant
    ),
)

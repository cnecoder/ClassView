package com.example.schedule.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class ThemePreset(
    val key: String,
    val label: String,
    val scheme: ColorScheme,
    val isDark: Boolean = false
)

val AllThemePresets = listOf(
    ThemePreset(
        key = "morandi",
        label = "莫兰迪",
        scheme = lightColorScheme(
            primary = Color(0xFF7B8FA1),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE5EC),
            secondary = Color(0xFFA8B5A2),
            tertiary = Color(0xFFC4A882),
            background = Color(0xFFF5F0EB),
            surface = Color(0xFFFDFBF8),
            surfaceVariant = Color(0xFFF0EBE3),
            onSurface = Color(0xFF3D3929),
            onSurfaceVariant = Color(0xFF8B8578),
            error = Color(0xFFC2857C),
            outline = Color(0xFFD5CFC4),
            outlineVariant = Color(0xFFE8E2D8),
        )
    ),
    ThemePreset(
        key = "mint",
        label = "薄荷绿",
        scheme = lightColorScheme(
            primary = Color(0xFF5B8C7A),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD0EBE0),
            secondary = Color(0xFF7BA89A),
            tertiary = Color(0xFF8BA89A),
            background = Color(0xFFF3F8F5),
            surface = Color(0xFFFAFDFB),
            surfaceVariant = Color(0xFFE8F2EC),
            onSurface = Color(0xFF1A2B24),
            onSurfaceVariant = Color(0xFF6B8076),
            error = Color(0xFFC2857C),
            outline = Color(0xFFC5D5CC),
            outlineVariant = Color(0xFFDAE7E0),
        )
    ),
    ThemePreset(
        key = "ocean",
        label = "海洋蓝",
        scheme = lightColorScheme(
            primary = Color(0xFF3A6B8C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD2E4F2),
            secondary = Color(0xFF5B8CA8),
            tertiary = Color(0xFF6B8CB0),
            background = Color(0xFFF2F6FA),
            surface = Color(0xFFF8FBFE),
            surfaceVariant = Color(0xFFE4EDF5),
            onSurface = Color(0xFF1A2835),
            onSurfaceVariant = Color(0xFF5E7A94),
            error = Color(0xFFC2857C),
            outline = Color(0xFFC0D0E0),
            outlineVariant = Color(0xFFD5E2EE),
        )
    ),
    ThemePreset(
        key = "macaron",
        label = "马卡龙",
        scheme = lightColorScheme(
            primary = Color(0xFFE04090),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD0E8),
            secondary = Color(0xFF40C080),
            tertiary = Color(0xFFFFA040),
            background = Color(0xFFFFF0F5),
            surface = Color(0xFFFFF8FC),
            surfaceVariant = Color(0xFFFFD8E8),
            onSurface = Color(0xFF2D1525),
            onSurfaceVariant = Color(0xFF754055),
            error = Color(0xFFE04060),
            outline = Color(0xFFD090B0),
            outlineVariant = Color(0xFFE8B8D0),
        )
    ),
    ThemePreset(
        key = "memphis",
        label = "孟菲斯",
        scheme = lightColorScheme(
            primary = Color(0xFFF04020),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD0C0),
            secondary = Color(0xFF0088DD),
            tertiary = Color(0xFFFFB800),
            background = Color(0xFFFFF9F0),
            surface = Color(0xFFFFFDF8),
            surfaceVariant = Color(0xFFFFE8D0),
            onSurface = Color(0xFF251510),
            onSurfaceVariant = Color(0xFF6B4030),
            error = Color(0xFFE83030),
            outline = Color(0xFFD0A090),
            outlineVariant = Color(0xFFE8C8B8),
        )
    ),
    ThemePreset(
        key = "dopamine",
        label = "多巴胺",
        scheme = lightColorScheme(
            primary = Color(0xFF46E1FD),
            onPrimary = Color(0xFF003545),
            primaryContainer = Color(0xFFD5F8FF),
            secondary = Color(0xFFF43F5E),
            tertiary = Color(0xFF06B6D4),
            background = Color(0xFFFEFCE8),
            surface = Color(0xFFFFFEF9),
            surfaceVariant = Color(0xFFFFF0E0),
            onSurface = Color(0xFF1A1228),
            onSurfaceVariant = Color(0xFF5B4A6B),
            error = Color(0xFFF43F5E),
            outline = Color(0xFFD0C0E0),
            outlineVariant = Color(0xFFE8DCF0),
        )
    ),
    ThemePreset(
        key = "dark",
        label = "暗夜",
        scheme = darkColorScheme(
            primary = Color(0xFF8DB5D0),
            onPrimary = Color(0xFF1A2A35),
            primaryContainer = Color(0xFF2A4050),
            secondary = Color(0xFFA8C8B5),
            tertiary = Color(0xFFD4C0A0),
            background = Color(0xFF1A1D20),
            surface = Color(0xFF232629),
            surfaceVariant = Color(0xFF2D3135),
            onSurface = Color(0xFFE0DDD8),
            onSurfaceVariant = Color(0xFFB0ACA6),
            error = Color(0xFFD49088),
            outline = Color(0xFF504940),
            outlineVariant = Color(0xFF3D3832),
        ),
        isDark = true
    ),
)

fun getPreset(key: String) = AllThemePresets.find { it.key == key } ?: AllThemePresets[0]

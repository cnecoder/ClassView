package com.example.schedule.ui.theme

import android.content.Context
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject

object ThemeManager {
    private const val PREFS_NAME = "schedule_prefs"
    private const val KEY_THEME = "theme_key"
    private const val KEY_CUSTOM_THEMES = "custom_themes"

    fun getCurrentTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "morandi") ?: "morandi"
    }

    fun setCurrentTheme(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, key).apply()
    }

    fun getCurrentPreset(context: Context): ThemePreset {
        val key = getCurrentTheme(context)
        if (key.startsWith("custom_")) {
            val theme = loadCustomTheme(context, key)
            if (theme != null) return theme
        }
        return getPreset(key)
    }

    // === 自定义主题管理 ===

    data class SavedTheme(
        val key: String,
        val name: String,
        val primary: Int, val secondary: Int, val tertiary: Int,
        val background: Int, val surface: Int, val surfaceVariant: Int,
        val onSurface: Int, val onSurfaceVariant: Int, val outline: Int,
    )

    fun loadAllCustomThemes(context: Context): List<SavedTheme> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_THEMES, "[]") ?: "[]"
        val result = mutableListOf<SavedTheme>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(SavedTheme(
                    key = obj.getString("key"),
                    name = obj.getString("name"),
                    primary = obj.getInt("primary"),
                    secondary = obj.getInt("secondary"),
                    tertiary = obj.getInt("tertiary"),
                    background = obj.getInt("background"),
                    surface = obj.getInt("surface"),
                    surfaceVariant = obj.getInt("surfaceVariant"),
                    onSurface = obj.getInt("onSurface"),
                    onSurfaceVariant = obj.getInt("onSurfaceVariant"),
                    outline = obj.getInt("outline"),
                ))
            }
        } catch (_: Exception) {}
        return result
    }

    fun saveCustomTheme(context: Context, key: String, name: String, colors: SavedTheme) {
        val themes = loadAllCustomThemes(context).toMutableList()
        val idx = themes.indexOfFirst { it.key == key }
        val updated = colors.copy(key = key, name = name)
        if (idx >= 0) themes[idx] = updated else themes.add(updated)
        writeThemes(context, themes)
    }

    fun deleteCustomTheme(context: Context, key: String) {
        val themes = loadAllCustomThemes(context).filter { it.key != key }
        writeThemes(context, themes)
    }

    private fun writeThemes(context: Context, themes: List<SavedTheme>) {
        val arr = JSONArray()
        for (t in themes) {
            val obj = JSONObject()
            obj.put("key", t.key)
            obj.put("name", t.name)
            obj.put("primary", t.primary)
            obj.put("secondary", t.secondary)
            obj.put("tertiary", t.tertiary)
            obj.put("background", t.background)
            obj.put("surface", t.surface)
            obj.put("surfaceVariant", t.surfaceVariant)
            obj.put("onSurface", t.onSurface)
            obj.put("onSurfaceVariant", t.onSurfaceVariant)
            obj.put("outline", t.outline)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_THEMES, arr.toString()).apply()
    }

    fun loadCustomTheme(context: Context, key: String): ThemePreset? {
        val t = loadAllCustomThemes(context).find { it.key == key } ?: return null
        return ThemePreset(
            key = t.key,
            label = t.name,
            scheme = lightColorScheme(
                primary = Color(t.primary), onPrimary = Color.White,
                primaryContainer = Color(t.primary).copy(alpha = 0.15f),
                secondary = Color(t.secondary), tertiary = Color(t.tertiary),
                background = Color(t.background), surface = Color(t.surface),
                surfaceVariant = Color(t.surfaceVariant),
                onSurface = Color(t.onSurface),
                onSurfaceVariant = Color(t.onSurfaceVariant),
                error = Color(0xFFC2857C.toInt()),
                outline = Color(t.outline),
                outlineVariant = Color(t.outline).copy(alpha = 0.5f),
            )
        )
    }

    fun savedThemeToColors(t: SavedTheme): Map<String, Color> = mapOf(
        "primary" to Color(t.primary), "secondary" to Color(t.secondary),
        "tertiary" to Color(t.tertiary), "background" to Color(t.background),
        "surface" to Color(t.surface), "surfaceVariant" to Color(t.surfaceVariant),
        "onSurface" to Color(t.onSurface), "onSurfaceVariant" to Color(t.onSurfaceVariant),
        "outline" to Color(t.outline),
    )

    fun colorsToSavedTheme(key: String, name: String, colors: Map<String, Color>): SavedTheme {
        return SavedTheme(
            key = key, name = name,
            primary = colors["primary"]?.toArgb() ?: 0,
            secondary = colors["secondary"]?.toArgb() ?: 0,
            tertiary = colors["tertiary"]?.toArgb() ?: 0,
            background = colors["background"]?.toArgb() ?: 0,
            surface = colors["surface"]?.toArgb() ?: 0,
            surfaceVariant = colors["surfaceVariant"]?.toArgb() ?: 0,
            onSurface = colors["onSurface"]?.toArgb() ?: 0,
            onSurfaceVariant = colors["onSurfaceVariant"]?.toArgb() ?: 0,
            outline = colors["outline"]?.toArgb() ?: 0,
        )
    }
}

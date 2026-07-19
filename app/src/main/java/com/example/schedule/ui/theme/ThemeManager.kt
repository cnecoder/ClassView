package com.example.schedule.ui.theme

import android.content.Context

object ThemeManager {
    private const val PREFS_NAME = "schedule_prefs"
    private const val KEY_THEME = "theme_key"

    fun getCurrentTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "morandi") ?: "morandi"
    }

    fun setCurrentTheme(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, key).apply()
    }

    fun getCurrentPreset(context: Context) = getPreset(getCurrentTheme(context))
}

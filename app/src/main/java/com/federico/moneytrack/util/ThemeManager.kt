package com.federico.moneytrack.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREFS_NAME = "moneytrack_preferences"
    private const val KEY_THEME = "theme"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun saveThemePreference(context: Context, theme: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
        applyTheme(theme)
    }

    fun getThemePreference(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_LIGHT) ?: THEME_LIGHT
    }

    fun applyTheme(theme: String) {
        when (theme) {
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}

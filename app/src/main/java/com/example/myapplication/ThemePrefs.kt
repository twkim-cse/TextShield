package com.example.myapplication

import android.content.Context

private const val PREFS_NAME = "theme_prefs"
private const val KEY_DARK_MODE = "dark_mode_enabled"

object ThemePrefs {
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDarkModeEnabled(context: Context, default: Boolean): Boolean =
        prefs(context).getBoolean(KEY_DARK_MODE, default)

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
}

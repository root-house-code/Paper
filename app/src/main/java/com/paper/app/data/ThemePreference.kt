package com.paper.app.data

import android.content.Context

/** Persists the user's explicit light/dark choice, overriding the system default. */
object ThemePreference {
    private const val PREFS = "paper_theme"
    private const val KEY_DARK_MODE = "dark_mode"

    /** Null means the user has never chosen — caller should fall back to the system setting. */
    fun load(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
    }

    fun save(context: Context, isDarkMode: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, isDarkMode)
            .apply()
    }
}

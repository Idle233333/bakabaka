package com.touhou.aichat

import android.content.Context

object SettingsStore {
    private const val PREFS = "touhou_aichat_settings"
    private const val KEY_WALLPAPER = "wallpaper_index" // -1 = 纯白

    fun loadWallpaperIndex(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WALLPAPER, -1)

    fun saveWallpaperIndex(ctx: Context, index: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_WALLPAPER, index).apply()
    }
}

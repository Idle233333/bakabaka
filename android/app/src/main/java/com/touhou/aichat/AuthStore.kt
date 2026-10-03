package com.touhou.aichat

import android.content.Context
import android.util.Base64

object AuthStore {
    private const val PREFS = "touhou_aichat_auth"
    private const val KEY_TOKEN = "token_obf"
    private const val KEY_USERNAME = "username"
    private val XOR_KEY: Byte = 0x5A.toByte()

    fun saveSession(ctx: Context, token: String, username: String) {
        val obf = NativeToken.xorBytes(token.toByteArray(Charsets.UTF_8), XOR_KEY)
        val b64 = Base64.encodeToString(obf, Base64.NO_WRAP)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, b64)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun loadToken(ctx: Context): String? {
        val b64 = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null) ?: return null
        return try {
            val obf = Base64.decode(b64, Base64.NO_WRAP)
            String(NativeToken.xorBytes(obf, XOR_KEY), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun loadUsername(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

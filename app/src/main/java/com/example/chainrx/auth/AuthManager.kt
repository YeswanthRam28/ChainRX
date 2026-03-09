package com.example.chainrx.auth

import android.content.Context
import android.content.SharedPreferences

object AuthManager {
    private const val PREFS_NAME = "ChainRX_Prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_ROLE = "user_role"
    private const val KEY_NAME = "user_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveAuth(context: Context, token: String, role: String, name: String?) {
        getPrefs(context).edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_ROLE, role)
            putString(KEY_NAME, name)
            apply()
        }
    }

    fun getToken(context: Context): String? = getPrefs(context).getString(KEY_TOKEN, null)
    fun getRole(context: Context): String? = getPrefs(context).getString(KEY_ROLE, null)
    fun getName(context: Context): String? = getPrefs(context).getString(KEY_NAME, null)

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}

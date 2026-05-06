package com.example.pesalens.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "tinxel_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var anthropicApiKey: String
        get() = prefs.getString(KEY_ANTHROPIC, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANTHROPIC, value).apply()

    fun hasApiKey(): Boolean = anthropicApiKey.isNotBlank()

    fun clearApiKey() = prefs.edit().remove(KEY_ANTHROPIC).apply()

    companion object {
        private const val KEY_ANTHROPIC = "anthropic_api_key"
    }
}

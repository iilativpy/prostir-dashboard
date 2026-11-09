package com.prostir.dashboard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Keeps the API token in encrypted storage. The password is never stored. */
class TokenStore(private val context: Context) {
    private val prefs: SharedPreferences by lazy { open() }

    @Volatile private var cachedToken: String? = null
    @Volatile private var loaded = false

    val token: String?
        get() {
            if (!loaded) {
                cachedToken = prefs.getString(KEY_TOKEN, null)
                loaded = true
            }
            return cachedToken
        }

    /** ISO-8601 time when the token stops working, or null when unknown. */
    val expiresAt: String?
        get() = prefs.getString(KEY_EXPIRES_AT, null)

    fun save(token: String, expiresAt: String?) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_EXPIRES_AT, expiresAt).apply()
        cachedToken = token
        loaded = true
    }

    fun clear() {
        prefs.edit().clear().apply()
        cachedToken = null
        loaded = true
    }

    private fun open(): SharedPreferences = try {
        create()
    } catch (e: Exception) {
        // The keystore can lose its key (restore, lock screen reset). Start with a clean session.
        context.deleteSharedPreferences(FILE)
        create()
    }

    private fun create(): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val FILE = "session"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}

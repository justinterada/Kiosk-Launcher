package com.osamaalek.kiosklauncher.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KioskPreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "kiosk_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences if encryption fails
        context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_PIN = "kiosk_pin"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val KEY_ALLOWED_APPS = "allowed_apps"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_SHOW_TOASTS = "show_toasts"
        private const val KEY_BACKGROUND_COLOR = "background_color"
        private const val DEFAULT_PIN = "0000"
    }

    var pin: String
        get() = prefs.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var isLocked: Boolean
        get() = prefs.getBoolean(KEY_IS_LOCKED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOCKED, value).apply()

    var allowedApps: Set<String>
        get() = prefs.getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ALLOWED_APPS, value).apply()

    var lockTimeoutMinutes: Int
        get() = prefs.getInt(KEY_LOCK_TIMEOUT, 5) // 5 minutes default
        set(value) = prefs.edit().putInt(KEY_LOCK_TIMEOUT, value).apply()

    var iconSizeDp: Int
        get() = prefs.getInt(KEY_ICON_SIZE, 80) // Default 80dp
        set(value) = prefs.edit().putInt(KEY_ICON_SIZE, value).apply()

    var showToasts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOASTS, true) // Default: show toasts
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TOASTS, value).apply()

    var backgroundColor: Int
        get() = prefs.getInt(KEY_BACKGROUND_COLOR, 0xFF303030.toInt()) // Default: dark gray
        set(value) = prefs.edit().putInt(KEY_BACKGROUND_COLOR, value).apply()

    fun isAppAllowed(packageName: String): Boolean {
        val allowed = allowedApps
        if (allowed.isEmpty()) return true // No restrictions
        return allowed.contains(packageName)
    }
}
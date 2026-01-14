// ============================================================================
// Updated SettingsActivity.kt - With Android Settings button
// Location: app/src/main/java/com/osamaalek/kiosklauncher/ui/SettingsActivity.kt
// REPLACE YOUR CURRENT FILE WITH THIS
// ============================================================================

package com.osamaalek.kiosklauncher.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var kioskPrefs: KioskPreferences
    private lateinit var etPin: EditText
    private lateinit var etTimeout: EditText
    private lateinit var btnSave: Button
    private lateinit var btnManageApps: Button
    private lateinit var btnAndroidSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        kioskPrefs = KioskPreferences(this)

        etPin = findViewById(R.id.etPin)
        etTimeout = findViewById(R.id.etTimeout)
        btnSave = findViewById(R.id.btnSave)
        btnManageApps = findViewById(R.id.btnManageApps)
        btnAndroidSettings = findViewById(R.id.btnAndroidSettings)

        loadSettings()

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnManageApps.setOnClickListener {
            val intent = Intent(this, AppWhitelistActivity::class.java)
            startActivity(intent)
        }

        btnAndroidSettings.setOnClickListener {
            openAndroidSettings()
        }
    }

    private fun loadSettings() {
        etPin.setText(kioskPrefs.pin)
        etTimeout.setText(kioskPrefs.lockTimeoutMinutes.toString())
    }

    private fun saveSettings() {
        val newPin = etPin.text.toString()
        val timeout = etTimeout.text.toString().toIntOrNull() ?: 5

        if (newPin.length < 4) {
            Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        kioskPrefs.pin = newPin
        kioskPrefs.lockTimeoutMinutes = timeout

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun openAndroidSettings() {
        try {
            // Open main Android Settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open Android Settings", Toast.LENGTH_SHORT).show()
        }
    }
}
package com.osamaalek.kiosklauncher.ui

import android.content.Intent
import android.os.Bundle
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
    private lateinit var etMediaPlayer: EditText
    private lateinit var btnSave: Button
    private lateinit var btnManageApps: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        kioskPrefs = KioskPreferences(this)

        etPin = findViewById(R.id.etPin)
        etTimeout = findViewById(R.id.etTimeout)
        etMediaPlayer = findViewById(R.id.etMediaPlayer)
        btnSave = findViewById(R.id.btnSave)
        btnManageApps = findViewById(R.id.btnManageApps)

        loadSettings()

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnManageApps.setOnClickListener {
            val intent = Intent(this, AppWhitelistActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadSettings() {
        etPin.setText(kioskPrefs.pin)
        etTimeout.setText(kioskPrefs.lockTimeoutMinutes.toString())
        etMediaPlayer.setText(kioskPrefs.defaultMediaPlayer ?: "")
    }

    private fun saveSettings() {
        val newPin = etPin.text.toString()
        val timeout = etTimeout.text.toString().toIntOrNull() ?: 5
        val mediaPlayer = etMediaPlayer.text.toString()

        if (newPin.length < 4) {
            Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        kioskPrefs.pin = newPin
        kioskPrefs.lockTimeoutMinutes = timeout
        kioskPrefs.defaultMediaPlayer = mediaPlayer.ifEmpty { null }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
// ============================================================================
// Fixed SettingsActivity.kt - Proper save behavior & debug info
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
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnDebugInfo: Button

    // Temporary storage for selected apps (not saved until user clicks Save)
    private var tempSelectedApps: Set<String> = emptySet()

    // Modern activity result API
    private val appSelectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Get the selected apps from AppWhitelistActivity
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            if (selectedApps != null) {
                tempSelectedApps = selectedApps.toSet()
                Toast.makeText(this, "${selectedApps.size} apps selected (not saved yet)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        kioskPrefs = KioskPreferences(this)

        etPin = findViewById(R.id.etPin)
        etTimeout = findViewById(R.id.etTimeout)
        btnSave = findViewById(R.id.btnSave)
        btnManageApps = findViewById(R.id.btnManageApps)
        btnAndroidSettings = findViewById(R.id.btnAndroidSettings)
        btnDebugInfo = findViewById(R.id.btnDebugInfo)

        loadSettings()

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnManageApps.setOnClickListener {
            val intent = Intent(this, AppWhitelistActivity::class.java)
            intent.putExtra("CURRENT_SELECTION", ArrayList(tempSelectedApps))
            appSelectionLauncher.launch(intent)
        }

        btnAndroidSettings.setOnClickListener {
            openAndroidSettings()
        }

        btnDebugInfo.setOnClickListener {
            showDebugInfo()
        }
    }

    private fun loadSettings() {
        etPin.setText(kioskPrefs.pin)
        etTimeout.setText(kioskPrefs.lockTimeoutMinutes.toString())

        // Load current saved apps into temp storage
        tempSelectedApps = kioskPrefs.allowedApps.toSet()
    }

    private fun saveSettings() {
        val newPin = etPin.text.toString()
        val timeout = etTimeout.text.toString().toIntOrNull() ?: 5

        if (newPin.length < 4) {
            Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        // Save all settings at once
        kioskPrefs.pin = newPin
        kioskPrefs.lockTimeoutMinutes = timeout
        kioskPrefs.allowedApps = tempSelectedApps

        Toast.makeText(this, "Settings saved: ${tempSelectedApps.size} apps allowed", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun openAndroidSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            Toast.makeText(this, "Opening Android Settings...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDebugInfo() {
        val savedApps = kioskPrefs.allowedApps
        val currentApps = tempSelectedApps

        val debugText = buildString {
            append("=== SAVED VALUES ===\n")
            append("PIN: ${kioskPrefs.pin}\n")
            append("Timeout: ${kioskPrefs.lockTimeoutMinutes} min\n")
            append("Locked: ${kioskPrefs.isLocked}\n")
            append("Saved Apps (${savedApps.size}):\n")
            savedApps.sorted().forEach { pkg ->
                append("  • $pkg\n")
            }

            append("\n=== CURRENT FORM VALUES ===\n")
            append("PIN: ${etPin.text}\n")
            append("Timeout: ${etTimeout.text}\n")
            append("Selected Apps (${currentApps.size}):\n")
            currentApps.sorted().forEach { pkg ->
                append("  • $pkg\n")
            }

            append("\n=== DIFFERENCES ===\n")
            val addedApps = currentApps - savedApps
            val removedApps = savedApps - currentApps

            if (addedApps.isNotEmpty()) {
                append("Will ADD (${addedApps.size}):\n")
                addedApps.sorted().forEach { append("  + $it\n") }
            }
            if (removedApps.isNotEmpty()) {
                append("Will REMOVE (${removedApps.size}):\n")
                removedApps.sorted().forEach { append("  - $it\n") }
            }
            if (addedApps.isEmpty() && removedApps.isEmpty()) {
                append("No changes to apps\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Debug Info")
            .setMessage(debugText)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy to Clipboard") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Debug Info", debugText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
package com.osamaalek.kiosklauncher.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskPreferences
import com.osamaalek.kiosklauncher.util.KioskUtil.showToast

class SettingsActivity : AppCompatActivity() {

    private lateinit var kioskPrefs: KioskPreferences
    private lateinit var etPin: EditText
    private lateinit var etTimeout: EditText
    private lateinit var iconSizeSeekBar: SeekBar
    private lateinit var iconSizeText: TextView
    private lateinit var iconPreview: ImageView
    private lateinit var cbShowToasts: CheckBox
    private lateinit var btnBackgroundColor: Button
    private lateinit var btnSave: Button
    private lateinit var btnManageApps: Button
    private lateinit var btnAndroidSettings: Button
    private lateinit var btnDebugInfo: Button

    // Temporary storage for selected apps (not saved until user clicks Save)
    private var tempSelectedApps: Set<String> = emptySet()
    private var tempBackgroundColor: Int = 0xFFF5F5F5.toInt()

    // Predefined icon sizes
    private val iconSizes = listOf(48, 64, 80, 96, 128, 160)

    // Modern activity result API
    private val appSelectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Get the selected apps from AppWhitelistActivity
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            if (selectedApps != null) {
                tempSelectedApps = selectedApps.toSet()
                showToast(kioskPrefs, this, "${selectedApps.size} apps selected (not saved yet)", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        kioskPrefs = KioskPreferences(this)

        etPin = findViewById(R.id.etPin)
        etTimeout = findViewById(R.id.etTimeout)
        iconSizeSeekBar = findViewById(R.id.iconSizeSeekBar)
        iconSizeText = findViewById(R.id.iconSizeText)
        iconPreview = findViewById(R.id.iconPreview)
        cbShowToasts = findViewById(R.id.cbShowToasts)
        btnBackgroundColor = findViewById(R.id.btnBackgroundColor)
        btnSave = findViewById(R.id.btnSave)
        btnManageApps = findViewById(R.id.btnManageApps)
        btnAndroidSettings = findViewById(R.id.btnAndroidSettings)
        btnDebugInfo = findViewById(R.id.btnDebugInfo)

        setupIconSizeSlider()
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

        btnBackgroundColor.setOnClickListener {
            showColorPicker()
        }
    }

    private fun loadSettings() {
        etPin.setText(kioskPrefs.pin)
        etTimeout.setText(kioskPrefs.lockTimeoutMinutes.toString())
        cbShowToasts.isChecked = kioskPrefs.showToasts
        tempBackgroundColor = kioskPrefs.backgroundColor
        updateBackgroundColorButton()

        // Load current saved apps into temp storage
        tempSelectedApps = kioskPrefs.allowedApps.toSet()

        // Set icon size slider
        val currentSize = kioskPrefs.iconSizeDp
        val index = iconSizes.indexOfFirst { it >= currentSize }.takeIf { it >= 0 } ?: (iconSizes.size - 1)
        iconSizeSeekBar.progress = index
    }

    private fun setupIconSizeSlider() {
        iconSizeSeekBar.max = iconSizes.size - 1

        iconSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sizeDp = iconSizes[progress]
                iconSizeText.text = "${sizeDp}dp"

                // Update preview icon size
                val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
                iconPreview.layoutParams.width = sizePx
                iconPreview.layoutParams.height = sizePx
                iconPreview.requestLayout()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Don't set initial progress here - loadSettings() will do it
    }

    private fun showColorPicker() {
        val colors = listOf(
            0xFFFFFFFF.toInt() to "White",
            0xFFF5F5F5.toInt() to "Light Gray",
            0xFFE0E0E0.toInt() to "Gray",
            0xFF303030.toInt() to "Dark Gray",
            0xFF000000.toInt() to "Black",
            0xFF1976D2.toInt() to "Blue",
            0xFF388E3C.toInt() to "Green",
            0xFFD32F2F.toInt() to "Red",
            0xFFF57C00.toInt() to "Orange",
            0xFF7B1FA2.toInt() to "Purple"
        )

        val colorNames = colors.map { it.second }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Background Color")
            .setItems(colorNames) { _, which ->
                tempBackgroundColor = colors[which].first
                updateBackgroundColorButton()
            }
            .show()
    }

    private fun updateBackgroundColorButton() {
        btnBackgroundColor.setBackgroundColor(tempBackgroundColor)

        // Set text color based on background brightness
        val r = Color.red(tempBackgroundColor)
        val g = Color.green(tempBackgroundColor)
        val b = Color.blue(tempBackgroundColor)
        val brightness = (r * 299 + g * 587 + b * 114) / 1000

        btnBackgroundColor.setTextColor(if (brightness > 128) Color.BLACK else Color.WHITE)
    }

    private fun saveSettings() {
        val newPin = etPin.text.toString()
        val timeout = etTimeout.text.toString().toIntOrNull() ?: 5
        val iconSize = iconSizes[iconSizeSeekBar.progress]

        if (newPin.length < 4) {
            showToast(kioskPrefs, this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT)
            return
        }

        // Save all settings at once
        kioskPrefs.pin = newPin
        kioskPrefs.lockTimeoutMinutes = timeout
        kioskPrefs.iconSizeDp = iconSize
        kioskPrefs.showToasts = cbShowToasts.isChecked
        kioskPrefs.backgroundColor = tempBackgroundColor
        kioskPrefs.allowedApps = tempSelectedApps

        showToast(kioskPrefs, this, "Settings saved: ${tempSelectedApps.size} apps allowed", Toast.LENGTH_LONG)
        finish()
    }

    private fun openAndroidSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            showToast(kioskPrefs, this, "Opening Android Settings...", Toast.LENGTH_SHORT)
        } catch (e: Exception) {
            showToast(kioskPrefs, this, "Error: ${e.message}", Toast.LENGTH_LONG)
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
            append("Icon Size: ${kioskPrefs.iconSizeDp}dp\n")
            append("Show Toasts: ${kioskPrefs.showToasts}\n")
            append("Background: #${Integer.toHexString(kioskPrefs.backgroundColor)}\n")
            append("Saved Apps (${savedApps.size}):\n")
            savedApps.sorted().forEach { pkg ->
                append("  • $pkg\n")
            }

            append("\n=== CURRENT FORM VALUES ===\n")
            append("PIN: ${etPin.text}\n")
            append("Timeout: ${etTimeout.text}\n")
            append("Icon Size: ${iconSizes[iconSizeSeekBar.progress]}dp\n")
            append("Show Toasts: ${cbShowToasts.isChecked}\n")
            append("Background: #${Integer.toHexString(tempBackgroundColor)}\n")
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
                showToast(kioskPrefs, this, "Copied to clipboard", Toast.LENGTH_SHORT)
            }
            .show()
    }
}
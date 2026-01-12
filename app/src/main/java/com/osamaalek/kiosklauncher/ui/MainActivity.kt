package com.osamaalek.kiosklauncher.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskUtil
import com.osamaalek.kiosklauncher.util.KioskPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var kioskPrefs: KioskPreferences
    private lateinit var btnLockUnlock: Button
    private lateinit var btnSettings: ImageButton
    private lateinit var btnLaunchApp: Button

    private val autoLockHandler = Handler(Looper.getMainLooper())
    private var autoLockRunnable: Runnable? = null

    // Modern back button handling
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Only allow back press when unlocked
            if (!kioskPrefs.isLocked) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
            // When locked, do nothing (prevents back navigation)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        kioskPrefs = KioskPreferences(this)

        // Register the modern back button handler
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // Initialize views
        btnLockUnlock = findViewById(R.id.btnLockUnlock)
        btnSettings = findViewById(R.id.btnSettings)
        btnLaunchApp = findViewById(R.id.btnLaunchApp)

        updateLockUI()
        setupClickListeners()

        // Start kiosk mode
        KioskUtil.startKioskMode(this)
    }

    private fun setupClickListeners() {
        btnLockUnlock.setOnClickListener {
            if (kioskPrefs.isLocked) {
                showUnlockDialog()
            } else {
                lockKiosk()
            }
        }

        btnSettings.setOnClickListener {
            if (kioskPrefs.isLocked) {
                Toast.makeText(this, "Unlock kiosk to access settings", Toast.LENGTH_SHORT).show()
                showUnlockDialog()
            } else {
                openSettings()
            }
        }

        btnLaunchApp.setOnClickListener {
            launchMediaPlayer()
        }
    }

    private fun showUnlockDialog() {
        UnlockDialog(this) {
            updateLockUI()
            scheduleAutoLock()
        }.show()
    }

    private fun lockKiosk() {
        kioskPrefs.isLocked = true
        updateLockUI()
        cancelAutoLock()
        Toast.makeText(this, "Kiosk Locked", Toast.LENGTH_SHORT).show()
    }

    private fun updateLockUI() {
        if (kioskPrefs.isLocked) {
            btnLockUnlock.text = "🔒 Unlock"
            btnSettings.isEnabled = false
            btnSettings.alpha = 0.3f
        } else {
            btnLockUnlock.text = "🔓 Lock"
            btnSettings.isEnabled = true
            btnSettings.alpha = 1.0f
        }
    }

    private fun scheduleAutoLock() {
        cancelAutoLock()

        val timeoutMinutes = kioskPrefs.lockTimeoutMinutes
        if (timeoutMinutes > 0) {
            autoLockRunnable = Runnable {
                lockKiosk()
            }
            autoLockHandler.postDelayed(
                autoLockRunnable!!,
                timeoutMinutes * 60 * 1000L
            )
        }
    }

    private fun cancelAutoLock() {
        autoLockRunnable?.let {
            autoLockHandler.removeCallbacks(it)
        }
    }

    private fun launchMediaPlayer() {
        val mediaPlayerPackage = kioskPrefs.defaultMediaPlayer
            ?: "org.videolan.vlc" // Default to VLC if not set

        if (!kioskPrefs.isAppAllowed(mediaPlayerPackage)) {
            Toast.makeText(this, "App not allowed", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = packageManager.getLaunchIntentForPackage(mediaPlayerPackage)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Media player not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!kioskPrefs.isLocked) {
            scheduleAutoLock()
        }
    }

    override fun onPause() {
        super.onPause()
        // Keep auto-lock timer running
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoLock()
    }
}
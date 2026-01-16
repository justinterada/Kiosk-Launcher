// ============================================================================
// Fixed MainActivity.kt - Properly exits lock task mode when unlocking
// Location: app/src/main/java/com/osamaalek/kiosklauncher/ui/MainActivity.kt
// REPLACE YOUR CURRENT FILE WITH THIS
// ============================================================================

package com.osamaalek.kiosklauncher.ui

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskUtil
import com.osamaalek.kiosklauncher.util.KioskPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var kioskPrefs: KioskPreferences
    private lateinit var appGrid: GridLayout
    private lateinit var unlockArea: View

    private val autoLockHandler = Handler(Looper.getMainLooper())
    private var autoLockRunnable: Runnable? = null

    private var wifiManager: WifiManager? = null
    private var wasWifiEnabled = false

    private var devicePolicyManager: DevicePolicyManager? = null
    private var adminComponent: ComponentName? = null
    private var activityManager: ActivityManager? = null

    // Hidden unlock gesture detection
    private var unlockTapCount = 0
    private val unlockTapHandler = Handler(Looper.getMainLooper())

    // Modern activity result API
    private val settingsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // When returning from settings, immediately lock
        lockKiosk()
    }

    // Modern back button handling
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // When locked, do nothing (prevents exit)
            // When unlocked, allow exit to normal Android
            if (!kioskPrefs.isLocked) {
                // Exit kiosk mode when unlocked
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        kioskPrefs = KioskPreferences(this)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        adminComponent = ComponentName(this, com.osamaalek.kiosklauncher.MyDeviceAdminReceiver::class.java)
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        // Register the modern back button handler
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // Initialize views
        appGrid = findViewById(R.id.appGrid)
        unlockArea = findViewById(R.id.unlockArea)

        setupUnlockGesture()
        updateUI()

        // Start kiosk mode only when locked
        if (kioskPrefs.isLocked) {
            KioskUtil.startKioskMode(this)
            startLockTaskIfNeeded()
        }
    }

    private fun setupUnlockGesture() {
        // Hidden unlock: tap the unlock area 5 times quickly
        unlockArea.setOnClickListener {
            unlockTapCount++

            if (unlockTapCount >= 5) {
                unlockTapCount = 0
                unlockTapHandler.removeCallbacksAndMessages(null)
                showUnlockDialog()
            } else {
                // Reset counter after 3 seconds if not completed
                unlockTapHandler.removeCallbacksAndMessages(null)
                unlockTapHandler.postDelayed({
                    unlockTapCount = 0
                }, 3000)
            }
        }
    }

    private fun showUnlockDialog() {
        if (!kioskPrefs.isLocked) {
            // Already unlocked, go straight to settings
            openSettings()
            return
        }

        UnlockDialog(this) {
            // On successful unlock
            kioskPrefs.isLocked = false

            // CRITICAL: Stop lock task BEFORE opening settings
            stopLockTaskIfNeeded()

            Toast.makeText(this, "Kiosk Unlocked - Full access enabled", Toast.LENGTH_LONG).show()

            // Now open settings
            openSettings()
        }.show()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        settingsLauncher.launch(intent)
    }

    private fun lockKiosk() {
        kioskPrefs.isLocked = true
        updateUI()
        cancelAutoLock()

        // Start lock task mode when locking
        startLockTaskIfNeeded()

        Toast.makeText(this, "Kiosk Locked", Toast.LENGTH_SHORT).show()
    }

    private fun startLockTaskIfNeeded() {
        try {
            val isInLockTaskMode = activityManager?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

            if (!isInLockTaskMode) {
                if (devicePolicyManager?.isDeviceOwnerApp(packageName) == true) {
                    // Set this app as allowed in lock task mode
                    devicePolicyManager?.setLockTaskPackages(adminComponent!!, arrayOf(packageName))

                    // Start lock task mode
                    startLockTask()
                    Toast.makeText(this, "Lock Task Mode: STARTED", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Not Device Owner - Lock Task unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lock Task Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopLockTaskIfNeeded() {
        try {
            val isInLockTaskMode = activityManager?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

            if (isInLockTaskMode) {
                stopLockTask()
                Toast.makeText(this, "Lock Task Mode: STOPPED", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unlock Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUI() {
        if (kioskPrefs.isLocked) {
            // Locked mode: show only allowed apps
            loadAllowedApps()
            disableWifi()
        } else {
            // Unlocked mode: show all apps, enable normal Android
            loadAllApps()
            enableWifi()
            scheduleAutoLock()
        }
    }

    private fun loadAllowedApps() {
        appGrid.removeAllViews()

        val allowedPackages = kioskPrefs.allowedApps
        if (allowedPackages.isEmpty()) {
            // If no apps configured, show a message
            showEmptyMessage("No apps configured. Unlock to add apps.")
            return
        }

        val apps = packageManager.getInstalledApplications(0)
            .filter {
                allowedPackages.contains(it.packageName) &&
                        packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .sortedBy { packageManager.getApplicationLabel(it).toString() }

        if (apps.isEmpty()) {
            showEmptyMessage("No allowed apps installed")
            return
        }

        apps.forEach { app ->
            addAppIcon(app)
        }
    }

    private fun loadAllApps() {
        appGrid.removeAllViews()

        // Show ALL apps (system and user) when unlocked
        val apps = packageManager.getInstalledApplications(0)
            .filter {
                // Show all apps that have a launcher intent
                packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .sortedBy { packageManager.getApplicationLabel(it).toString() }

        if (apps.isEmpty()) {
            showEmptyMessage("No apps found")
            return
        }

        apps.forEach { app ->
            addAppIcon(app)
        }
    }

    private fun addAppIcon(app: ApplicationInfo) {
        val iconView = layoutInflater.inflate(R.layout.item_app_icon, appGrid, false)
        val icon = iconView.findViewById<ImageView>(R.id.appIcon)
        val name = iconView.findViewById<TextView>(R.id.appName)

        try {
            icon.setImageDrawable(packageManager.getApplicationIcon(app))
            name.text = packageManager.getApplicationLabel(app)

            iconView.setOnClickListener {
                launchApp(app.packageName)
            }

            appGrid.addView(iconView)
        } catch (e: Exception) {
            // Skip apps that cause errors
        }
    }

    private fun showEmptyMessage(message: String) {
        val textView = TextView(this).apply {
            text = message
            textSize = 18f
            setPadding(32, 32, 32, 32)
            gravity = android.view.Gravity.CENTER
        }
        appGrid.addView(textView)
    }

    private fun launchApp(packageName: String) {
        // When locked, verify app is allowed
        if (kioskPrefs.isLocked && !kioskPrefs.isAppAllowed(packageName)) {
            Toast.makeText(this, "App not allowed", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disableWifi() {
        wifiManager?.let { wifi ->
            if (wifi.isWifiEnabled) {
                wasWifiEnabled = true
                @Suppress("DEPRECATION")
                wifi.isWifiEnabled = false
            }
        }
    }

    private fun enableWifi() {
        wifiManager?.let { wifi ->
            if (wasWifiEnabled && !wifi.isWifiEnabled) {
                @Suppress("DEPRECATION")
                wifi.isWifiEnabled = true
            }
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

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        // Keep auto-lock timer running
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoLock()
        unlockTapHandler.removeCallbacksAndMessages(null)
    }
}
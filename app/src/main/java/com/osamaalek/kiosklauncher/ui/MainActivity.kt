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
import com.osamaalek.kiosklauncher.util.KioskPreferences
import com.osamaalek.kiosklauncher.util.KioskUtil
import com.osamaalek.kiosklauncher.util.KioskUtil.showToast

class MainActivity : AppCompatActivity() {

    private lateinit var kioskPrefs: KioskPreferences
    private lateinit var appGrid: GridLayout
    private lateinit var unlockArea: View
    private lateinit var mainContainer: View

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
        // When returning from settings, update whitelist (removes temp Settings access) and lock
        updateLockTaskWhitelist(includeSettings = false)
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
        mainContainer = findViewById(R.id.mainContainer)

        // Set wallpaper as background
        setWallpaperBackground()

        setupUnlockGesture()

        // Update whitelist with current settings
        updateLockTaskWhitelist()

        updateUI()

        // Start lock task only when locked
        if (kioskPrefs.isLocked) {
            KioskUtil.startKioskMode(this)
            startLockTaskIfNeeded()
        } else {
            // Make sure UI is shown when unlocked
            KioskUtil.stopKioskMode(this)
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

    private fun setWallpaperBackground() {
        // Use saved background color instead of wallpaper
        mainContainer.setBackgroundColor(kioskPrefs.backgroundColor)
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

            // CRITICAL: Stop lock task BEFORE anything else
            stopLockTaskIfNeeded()

            // CRITICAL: Restore system UI (status bar, navigation)
            KioskUtil.stopKioskMode(this)

            // Update UI to unlocked state
            updateUI()

            showToast(kioskPrefs, this, "Unlocked - Pull down status bar now available", Toast.LENGTH_LONG)

            // Temporarily add Settings to whitelist before opening
            updateLockTaskWhitelist(includeSettings = true)

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

        // Re-hide system UI when locking
        KioskUtil.startKioskMode(this)

        updateUI()
        cancelAutoLock()

        // Start lock task mode when locking
        startLockTaskIfNeeded()

        showToast(kioskPrefs, this, "Kiosk Locked")
    }

    private fun updateLockTaskWhitelist(includeSettings: Boolean = false) {
        if (devicePolicyManager?.isDeviceOwnerApp(packageName) != true) {
            return
        }

        try {
            // Get custom whitelist from preferences
            val allowedApps = kioskPrefs.allowedApps.toMutableSet()

            // CRITICAL: Always include the kiosk launcher itself
            allowedApps.add(packageName)

            // Conditionally include Settings based on parameter
            if (includeSettings) {
                // Temporarily add Settings when opening preferences
                allowedApps.add("com.android.settings")
            } else {
                // Only include Settings if user explicitly selected it
                if (kioskPrefs.allowedApps.contains("com.android.settings")) {
                    allowedApps.add("com.android.settings")
                }
            }

            // Update the whitelist
            devicePolicyManager?.setLockTaskPackages(
                adminComponent!!,
                allowedApps.toTypedArray()
            )

            val settingsStatus = if (includeSettings) "(temp)" else ""
            showToast(
                kioskPrefs,
                this,
                "Whitelist: ${allowedApps.size} apps $settingsStatus",
                Toast.LENGTH_SHORT
            )
        } catch (e: Exception) {
            showToast(kioskPrefs, this, "Whitelist update error: ${e.message}", Toast.LENGTH_LONG)
        }
    }

    private fun startLockTaskIfNeeded() {
        try {
            val lockTaskMode = activityManager?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
            val isInLockTaskMode = lockTaskMode != ActivityManager.LOCK_TASK_MODE_NONE

            if (!isInLockTaskMode) {
                if (devicePolicyManager?.isDeviceOwnerApp(packageName) == true) {
                    // Set lock task features to allow power button
                    devicePolicyManager?.setLockTaskFeatures(
                        adminComponent!!,
                        DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS
                    )

                    startLockTask()
                    showToast(kioskPrefs, this, "Lock Task: ENABLED")
                } else {
                    showToast(kioskPrefs, this, "⚠️ Not Device Owner - Limited kiosk mode")
                }
            }
        } catch (e: Exception) {
            showToast(kioskPrefs, this, "Lock Task start error: ${e.message}", Toast.LENGTH_LONG)
        }
    }

    private fun stopLockTaskIfNeeded() {
        try {
            val lockTaskMode = activityManager?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
            val isInLockTaskMode = lockTaskMode != ActivityManager.LOCK_TASK_MODE_NONE

            if (isInLockTaskMode) {
                // Before stopping, enable system info to allow status bar
                if (devicePolicyManager?.isDeviceOwnerApp(packageName) == true) {
                    devicePolicyManager?.setLockTaskFeatures(
                        adminComponent!!,
                        DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO or
                                DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                                DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS
                    )
                }

                stopLockTask()
                showToast(kioskPrefs, this, "Lock Task: DISABLED - Status bar available", Toast.LENGTH_LONG)
            } else {
                showToast(kioskPrefs, this, "ℹ️ Lock task already stopped", Toast.LENGTH_SHORT)
            }
        } catch (e: Exception) {
            showToast(kioskPrefs, this, "Lock Task stop error: ${e.message}", Toast.LENGTH_LONG)
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

        // Set dynamic column count based on screen width
        appGrid.columnCount = calculateColumnCount()

        val allowedPackages = kioskPrefs.allowedApps
        if (allowedPackages.isEmpty()) {
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

        // Set dynamic column count based on screen width
        appGrid.columnCount = calculateColumnCount()

        val apps = packageManager.getInstalledApplications(0)
            .filter {
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

            // Apply custom icon size
            val iconSizeDp = kioskPrefs.iconSizeDp
            val iconSizePx = (iconSizeDp * resources.displayMetrics.density).toInt()
            icon.layoutParams.width = iconSizePx
            icon.layoutParams.height = iconSizePx

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
            showToast(kioskPrefs, this, "App not allowed", Toast.LENGTH_SHORT)
            return
        }

        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                showToast(kioskPrefs, this, "Cannot launch app", Toast.LENGTH_SHORT)
            }
        } catch (e: Exception) {
            showToast(kioskPrefs, this, "Error launching app: ${e.message}", Toast.LENGTH_SHORT)
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
        // Update whitelist in case it changed (don't include temp Settings access)
        if (kioskPrefs.isLocked) {
            updateLockTaskWhitelist(includeSettings = false)
        }
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

    private fun calculateColumnCount(): Int {
        // Get screen width in pixels
        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels

        // Calculate item width: icon size + padding
        val iconSizeDp = kioskPrefs.iconSizeDp
        val iconSizePx = (iconSizeDp * displayMetrics.density).toInt()

        // Add padding from item layout (8dp each side = 16dp total)
        val itemPaddingPx = (16 * displayMetrics.density).toInt()

        // Add grid margin/spacing (approximate 8dp per item)
        val gridSpacingPx = (8 * displayMetrics.density).toInt()

        // Total width per item
        val itemWidthPx = iconSizePx + itemPaddingPx + gridSpacingPx

        // Calculate how many columns fit, minimum 2, maximum 8
        val columns = (screenWidthPx / itemWidthPx).coerceIn(2, 8)

        return columns
    }


}
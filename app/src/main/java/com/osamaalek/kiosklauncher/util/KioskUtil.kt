package com.osamaalek.kiosklauncher.util

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.Toast

object KioskUtil {

    fun startKioskMode(activity: Activity) {
        // Hide navigation and status bars
        hideSystemUI(activity)

        // Set up lock task mode with custom whitelist
        setupLockTaskMode(activity)
    }

    private fun hideSystemUI(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }

        // Keep screen on
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupLockTaskMode(activity: Activity) {
        try {
            val devicePolicyManager = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = ComponentName(activity, com.osamaalek.kiosklauncher.MyDeviceAdminReceiver::class.java)

            if (devicePolicyManager?.isDeviceOwnerApp(activity.packageName) == true) {
                // Get custom whitelist from preferences
                val kioskPrefs = KioskPreferences(activity)
                val allowedApps = kioskPrefs.allowedApps.toMutableSet()

                // IMPORTANT: Always include the kiosk launcher itself
                allowedApps.add(activity.packageName)

                // IMPORTANT: Include Settings if it's in the whitelist
                // This allows Settings to work properly

                // Set the whitelist for lock task mode
                devicePolicyManager.setLockTaskPackages(
                    adminComponent,
                    allowedApps.toTypedArray()
                )

                Toast.makeText(
                    activity,
                    "Lock task whitelist: ${allowedApps.size} apps",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Lock task setup error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun stopKioskMode(activity: Activity) {
        // Show system UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.show(
                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
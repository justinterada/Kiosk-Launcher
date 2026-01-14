// ============================================================================
// Fixed AppWhitelistActivity.kt - All apps start UNCHECKED (disallowed)
// Location: app/src/main/java/com/osamaalek/kiosklauncher/ui/AppWhitelistActivity.kt
// REPLACE YOUR CURRENT FILE WITH THIS
// ============================================================================

package com.osamaalek.kiosklauncher.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskPreferences

class AppWhitelistActivity : AppCompatActivity() {

    private lateinit var kioskPrefs: KioskPreferences
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_whitelist)

        kioskPrefs = KioskPreferences(this)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        // Get ALL apps that have a launcher intent (both system and user apps)
        val allApps = packageManager.getInstalledApplications(0)

        val launchableApps = allApps.filter { app ->
            // Only show apps that can actually be launched
            packageManager.getLaunchIntentForPackage(app.packageName) != null
        }.sortedBy {
            packageManager.getApplicationLabel(it).toString()
        }

        recyclerView.adapter = AppAdapter(launchableApps, kioskPrefs)
    }

    inner class AppAdapter(
        private val apps: List<ApplicationInfo>,
        private val prefs: KioskPreferences
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val appIcon: ImageView = view.findViewById(R.id.appIcon)
            val appName: TextView = view.findViewById(R.id.appName)
            val appPackage: TextView = view.findViewById(R.id.appPackage)
            val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]

            try {
                holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(app))
                holder.appName.text = packageManager.getApplicationLabel(app)
                holder.appPackage.text = app.packageName

                // Get current allowed apps set
                val allowedApps = prefs.allowedApps

                // Apps are UNCHECKED by default (not in the allowed set)
                holder.checkbox.isChecked = allowedApps.contains(app.packageName)

                // Clear any previous listeners to avoid issues
                holder.checkbox.setOnCheckedChangeListener(null)

                // Update the allowed apps when checkbox changes
                holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                    val allowed = prefs.allowedApps.toMutableSet()
                    if (isChecked) {
                        // Add to allowed list
                        allowed.add(app.packageName)
                    } else {
                        // Remove from allowed list
                        allowed.remove(app.packageName)
                    }
                    prefs.allowedApps = allowed
                }
            } catch (e: Exception) {
                // Handle any errors gracefully
                holder.appName.text = app.packageName
                holder.appPackage.text = "Error loading app info"
            }
        }

        override fun getItemCount() = apps.size
    }
}
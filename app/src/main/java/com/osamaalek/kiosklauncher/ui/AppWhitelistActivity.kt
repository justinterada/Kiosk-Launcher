// ============================================================================
// Fixed AppWhitelistActivity.kt - Proper selection tracking & return results
// Location: app/src/main/java/com/osamaalek/kiosklauncher/ui/AppWhitelistActivity.kt
// REPLACE YOUR CURRENT FILE WITH THIS
// ============================================================================

package com.osamaalek.kiosklauncher.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskPreferences

class AppWhitelistActivity : AppCompatActivity() {

    private lateinit var kioskPrefs: KioskPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnDone: Button
    private lateinit var btnSelectAll: Button
    private lateinit var btnSelectNone: Button

    // Track selections in memory (don't save until user returns to Settings and clicks Save)
    private val selectedApps = mutableSetOf<String>()

    // Modern back button handling
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            returnResults()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_whitelist)

        kioskPrefs = KioskPreferences(this)
        recyclerView = findViewById(R.id.recyclerView)
        btnDone = findViewById(R.id.btnDone)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnSelectNone = findViewById(R.id.btnSelectNone)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Register the modern back button handler
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // Get current selection passed from SettingsActivity
        val currentSelection = intent.getStringArrayListExtra("CURRENT_SELECTION")
        if (currentSelection != null) {
            selectedApps.addAll(currentSelection)
        }

        loadInstalledApps()

        btnDone.setOnClickListener {
            returnResults()
        }

        btnSelectAll.setOnClickListener {
            selectAllApps()
        }

        btnSelectNone.setOnClickListener {
            selectedApps.clear()
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun selectAllApps() {
        val adapter = recyclerView.adapter as? AppAdapter
        adapter?.apps?.forEach { app ->
            selectedApps.add(app.packageName)
        }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun returnResults() {
        val resultIntent = Intent()
        resultIntent.putStringArrayListExtra("SELECTED_APPS", ArrayList(selectedApps))
        setResult(RESULT_OK, resultIntent)
        finish()
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

        recyclerView.adapter = AppAdapter(launchableApps, selectedApps)

        Toast.makeText(this, "Found ${launchableApps.size} apps, ${selectedApps.size} selected", Toast.LENGTH_SHORT).show()
    }

    inner class AppAdapter(
        val apps: List<ApplicationInfo>,
        private val selectedSet: MutableSet<String>
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

                // Remove listener before updating state
                holder.checkbox.setOnCheckedChangeListener(null)

                // Set checkbox state based on selected set
                holder.checkbox.isChecked = selectedSet.contains(app.packageName)

                // Add listener for changes
                holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedSet.add(app.packageName)
                    } else {
                        selectedSet.remove(app.packageName)
                    }
                }

                // Also allow clicking the whole row
                holder.itemView.setOnClickListener {
                    holder.checkbox.toggle()
                }

            } catch (e: Exception) {
                holder.appName.text = app.packageName
                holder.appPackage.text = "Error loading: ${e.message}"
            }
        }

        override fun getItemCount() = apps.size
    }
}
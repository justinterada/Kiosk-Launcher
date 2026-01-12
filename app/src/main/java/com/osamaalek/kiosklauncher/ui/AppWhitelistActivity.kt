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
        val apps = packageManager.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Non-system apps only
            .sortedBy { packageManager.getApplicationLabel(it).toString() }

        recyclerView.adapter = AppAdapter(apps, kioskPrefs)
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
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(app))
            holder.appName.text = packageManager.getApplicationLabel(app)
            holder.appPackage.text = app.packageName
            holder.checkbox.isChecked = prefs.isAppAllowed(app.packageName)

            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                val allowed = prefs.allowedApps.toMutableSet()
                if (isChecked) {
                    allowed.add(app.packageName)
                } else {
                    allowed.remove(app.packageName)
                }
                prefs.allowedApps = allowed
            }
        }

        override fun getItemCount() = apps.size
    }
}
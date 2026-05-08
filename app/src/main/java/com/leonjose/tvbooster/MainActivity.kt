package com.leonjose.tvbooster

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : FragmentActivity() {

    private lateinit var tvRamUsed: TextView
    private lateinit var tvRamFree: TextView
    private lateinit var tvRamTotal: TextView
    private lateinit var tvRamPercent: TextView
    private lateinit var tvAppCount: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressRam: ProgressBar
    private lateinit var btnKillAll: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnKillSelected: Button
    private lateinit var rvApps: RecyclerView
    private lateinit var appAdapter: AppListAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 3000L

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateRamInfo()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupRecyclerView()
        setupButtons()
        checkUsagePermission()
        loadRunningApps()
        handler.post(updateRunnable)
    }

    private fun bindViews() {
        tvRamUsed = findViewById(R.id.tvRamUsed)
        tvRamFree = findViewById(R.id.tvRamFree)
        tvRamTotal = findViewById(R.id.tvRamTotal)
        tvRamPercent = findViewById(R.id.tvRamPercent)
        tvAppCount = findViewById(R.id.tvAppCount)
        tvStatus = findViewById(R.id.tvStatus)
        progressRam = findViewById(R.id.progressRam)
        btnKillAll = findViewById(R.id.btnKillAll)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnKillSelected = findViewById(R.id.btnKillSelected)
        rvApps = findViewById(R.id.rvApps)
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter(mutableListOf()) { app, isChecked ->
            app.isSelected = isChecked
            updateSelectedCount()
        }
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = appAdapter
    }

    private fun setupButtons() {
        btnKillAll.setOnClickListener { killAllBackgroundApps() }
        btnRefresh.setOnClickListener {
            tvStatus.text = "Refreshing..."
            loadRunningApps()
        }
        btnKillSelected.setOnClickListener { killSelectedApps() }
    }

    private fun checkUsagePermission() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time
        )
        if (stats == null || stats.isEmpty()) {
            tvStatus.text = "⚠ Usage Access permission needed for full functionality"
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun updateRamInfo() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / 1048576L
        val availMb = memInfo.availMem / 1048576L
        val usedMb = totalMb - availMb
        val percent = ((usedMb.toFloat() / totalMb.toFloat()) * 100).toInt()

        tvRamTotal.text = "Total: ${totalMb} MB"
        tvRamUsed.text = "Used: ${usedMb} MB"
        tvRamFree.text = "Free: ${availMb} MB"
        tvRamPercent.text = "${percent}%"
        progressRam.progress = percent

        progressRam.progressTintList = when {
            percent >= 85 -> android.content.res.ColorStateList.valueOf(0xFFE53935.toInt())
            percent >= 60 -> android.content.res.ColorStateList.valueOf(0xFFFFA726.toInt())
            else -> android.content.res.ColorStateList.valueOf(0xFF43A047.toInt())
        }
    }

    private fun loadRunningApps() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = packageManager
        val runningApps = mutableListOf<AppInfo>()
        val myPackage = packageName

        val runningProcesses = am.runningAppProcesses ?: emptyList()

        for (process in runningProcesses) {
            if (process.pkgList == null) continue
            for (pkg in process.pkgList) {
                if (pkg == myPackage) continue
                if (isSystemCritical(pkg)) continue
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    runningApps.add(AppInfo(pkg, label, isSystemApp, false))
                } catch (e: PackageManager.NameNotFoundException) {
                    // skip
                }
            }
        }

        runningApps.sortWith(compareBy({ it.isSystem }, { it.label }))

        appAdapter.updateList(runningApps)
        tvAppCount.text = "${runningApps.size} background apps"
        tvStatus.text = if (runningApps.isEmpty()) "✓ No background apps running" else "Found ${runningApps.size} background app(s)"
        updateSelectedCount()
        updateRamInfo()
    }

    private fun isSystemCritical(pkg: String): Boolean {
        val critical = setOf(
            "android", "com.android.systemui", "com.android.launcher",
            "com.android.launcher3", "com.google.android.tv.frameworkpackagestubs",
            "com.android.phone", "com.android.settings"
        )
        return critical.contains(pkg)
    }

    private fun killAllBackgroundApps() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val apps = appAdapter.getAppList()
        var killed = 0

        tvStatus.text = "Killing all background apps..."
        btnKillAll.isEnabled = false

        for (app in apps) {
            try {
                am.killBackgroundProcesses(app.packageName)
                killed++
            } catch (e: Exception) {
                // skip
            }
        }

        handler.postDelayed({
            loadRunningApps()
            tvStatus.text = "✓ Killed $killed app(s). RAM freed!"
            btnKillAll.isEnabled = true
            Toast.makeText(this, "Boost complete! $killed apps closed.", Toast.LENGTH_SHORT).show()
        }, 1000)
    }

    private fun killSelectedApps() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val selected = appAdapter.getAppList().filter { it.isSelected }

        if (selected.isEmpty()) {
            Toast.makeText(this, "No apps selected. Check apps first.", Toast.LENGTH_SHORT).show()
            return
        }

        var killed = 0
        for (app in selected) {
            try {
                am.killBackgroundProcesses(app.packageName)
                killed++
            } catch (e: Exception) { }
        }

        handler.postDelayed({
            loadRunningApps()
            tvStatus.text = "✓ Killed $killed selected app(s)"
            Toast.makeText(this, "$killed selected app(s) closed.", Toast.LENGTH_SHORT).show()
        }, 800)
    }

    private fun updateSelectedCount() {
        val count = appAdapter.getAppList().count { it.isSelected }
        btnKillSelected.text = if (count > 0) "Kill Selected ($count)" else "Kill Selected"
    }

    override fun onResume() {
        super.onResume()
        loadRunningApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }
}

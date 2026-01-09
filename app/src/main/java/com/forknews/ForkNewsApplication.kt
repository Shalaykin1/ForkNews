package com.forknews

import android.app.Application
import android.content.Context
import com.forknews.utils.PreferencesManager
import com.forknews.workers.UpdateCheckWorker

class ForkNewsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PreferencesManager
        PreferencesManager.init(this)
        
        // Apply saved theme
        PreferencesManager.applyTheme()
        
        // Mark that we need to initialize default repos
        // This will be done in MainActivity to avoid database instance issues
        markNeedsDefaultReposInit()
        
        // Initialize WorkManager
        initWorkManager()
    }
    
    private fun markNeedsDefaultReposInit() {
        val prefs = getSharedPreferences("forknews_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("default_repos_initialized")) {
            prefs.edit().putBoolean("needs_init", true).apply()
        }
    }
    

    
    private fun initWorkManager() {
        try {
            // Schedule periodic update checks with default interval (1 minute)
            UpdateCheckWorker.schedulePeriodicWork(this, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

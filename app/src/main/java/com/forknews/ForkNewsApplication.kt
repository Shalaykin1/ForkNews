package com.forknews

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.forknews.utils.PreferencesManager
import com.forknews.workers.UpdateCheckWorker

class ForkNewsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PreferencesManager
        PreferencesManager.init(this)
        
        // Apply saved theme
        PreferencesManager.applyTheme()
        
        // Initialize WorkManager
        initWorkManager()
    }
    
    private fun initWorkManager() {
        try {
            // Schedule periodic update checks with default interval (1 hour)
            UpdateCheckWorker.schedulePeriodicWork(this, 60)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

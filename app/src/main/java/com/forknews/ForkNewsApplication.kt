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
        
        // Initialize WorkManager
        initWorkManager()
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

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
        
        // Initialize WorkManager
        initWorkManager()
    }    

    
    private fun initWorkManager() {
        try {
            // Schedule periodic update checks (5 minutes)
            UpdateCheckWorker.schedulePeriodicWork(this, 5L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

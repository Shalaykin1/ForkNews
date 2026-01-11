package com.forknews

import android.app.Application
import android.content.Context
import com.forknews.utils.PreferencesManager
import com.forknews.utils.AlarmScheduler

class ForkNewsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize DiagnosticLogger first
        com.forknews.utils.DiagnosticLogger.init(this)
        
        // Initialize PreferencesManager
        PreferencesManager.init(this)
        
        // Initialize AlarmManager for 3-minute checks
        initAlarmScheduler()
    }    

    
    private fun initAlarmScheduler() {
        try {
            // Schedule exact 5-minute alarm
            AlarmScheduler.scheduleAlarm(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

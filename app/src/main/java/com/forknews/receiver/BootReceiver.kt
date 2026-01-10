package com.forknews.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.forknews.utils.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Перезапускаем AlarmScheduler после перезагрузки устройства (5 минут)
            AlarmScheduler.scheduleAlarm(context)
            com.forknews.utils.DiagnosticLogger.log("BootReceiver", "Alarm перезапущен после BOOT_COMPLETED")
        }
    }
}

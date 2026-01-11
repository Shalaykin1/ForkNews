package com.forknews.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.forknews.utils.AlarmScheduler
import com.forknews.utils.DiagnosticLogger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        DiagnosticLogger.log("BootReceiver", "Получен broadcast: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                // Перезапускаем AlarmScheduler после перезагрузки устройства (5 минут)
                AlarmScheduler.scheduleAlarm(context)
                DiagnosticLogger.log("BootReceiver", "✓ Alarm перезапущен после перезагрузки ($action)")
            }
            else -> {
                DiagnosticLogger.log("BootReceiver", "Неизвестный action: $action")
            }
        }
    }
}

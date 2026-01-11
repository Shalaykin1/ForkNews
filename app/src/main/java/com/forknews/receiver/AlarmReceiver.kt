package com.forknews.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.forknews.service.UpdateCheckService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.forknews.CHECK_UPDATES") {
            com.forknews.utils.DiagnosticLogger.log("AlarmReceiver", "=== ALARM ПОЛУЧЕН ===")
            
            // Запускаем Foreground Service для надёжной работы
            UpdateCheckService.start(context)
            
            // Перепланируем следующий alarm
            com.forknews.utils.AlarmScheduler.scheduleAlarm(context)
        }
    }
}

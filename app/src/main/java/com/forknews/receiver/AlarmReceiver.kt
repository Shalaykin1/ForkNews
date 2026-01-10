package com.forknews.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.forknews.workers.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.forknews.CHECK_UPDATES") {
            com.forknews.utils.DiagnosticLogger.log("AlarmReceiver", "Получен сигнал проверки обновлений")
            
            // Запускаем OneTimeWorkRequest для проверки
            val workRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            
            // Перепланируем следующий alarm
            com.forknews.utils.AlarmScheduler.scheduleAlarm(context)
        }
    }
}

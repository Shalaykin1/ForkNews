package com.forknews.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.forknews.receiver.AlarmReceiver

object AlarmScheduler {
    private const val REQUEST_CODE = 1001
    private const val INTERVAL_MILLIS = 5 * 60 * 1000L // 5 минут
    
    fun scheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.forknews.CHECK_UPDATES"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = System.currentTimeMillis() + INTERVAL_MILLIS
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ требует разрешение SCHEDULE_EXACT_ALARM
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    DiagnosticLogger.log("AlarmScheduler", "Запланирован точный alarm через 5 минут (setExactAndAllowWhileIdle)")
                } else {
                    // Fallback если нет разрешения
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    DiagnosticLogger.log("AlarmScheduler", "Запланирован неточный alarm через ~5 минут (setAndAllowWhileIdle)")
                }
            } else {
                // Android 11 и ниже
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                DiagnosticLogger.log("AlarmScheduler", "Запланирован точный alarm через 5 минут")
            }
        } catch (e: SecurityException) {
            DiagnosticLogger.error("AlarmScheduler", "Ошибка планирования alarm: ${e.message}", e)
            // Fallback на неточный alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            DiagnosticLogger.log("AlarmScheduler", "Использован fallback setAndAllowWhileIdle")
        }
    }
    
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.forknews.CHECK_UPDATES"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        DiagnosticLogger.log("AlarmScheduler", "Alarm отменён")
    }
}

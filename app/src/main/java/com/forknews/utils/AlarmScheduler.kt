package com.forknews.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.forknews.receiver.AlarmReceiver

object AlarmScheduler {
    private const val REQUEST_CODE = 1001
    private const val INTERVAL_MILLIS = 3 * 60 * 1000L // 3 минуты
    
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
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+ требует разрешение SCHEDULE_EXACT_ALARM или USE_EXACT_ALARM
                    if (alarmManager.canScheduleExactAlarms()) {
                        // Используем setAlarmClock для максимального приоритета (обходит Doze Mode)
                        val alarmClockInfo = AlarmManager.AlarmClockInfo(
                            triggerTime,
                            pendingIntent
                        )
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                        DiagnosticLogger.log("AlarmScheduler", "✓ Запланирован высокоприоритетный alarm через 3 минуты (setAlarmClock)")
                    } else {
                        // Fallback если нет разрешения - используем setAndAllowWhileIdle
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        DiagnosticLogger.log("AlarmScheduler", "⚠ Запланирован неточный alarm через ~3 минуты (setAndAllowWhileIdle)")
                        DiagnosticLogger.log("AlarmScheduler", "Рекомендация: разрешите точные alarms в настройках приложения")
                    }
                }
                else -> {
                    // Android 11 и ниже - используем setAlarmClock для обхода Doze Mode
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(
                        triggerTime,
                        pendingIntent
                    )
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    DiagnosticLogger.log("AlarmScheduler", "✓ Запланирован высокоприоритетный alarm через 3 минуты")
                }
            }
            
            // Дополнительные проверки для специфичных производителей
            checkManufacturerRestrictions(context)
            
        } catch (e: SecurityException) {
            DiagnosticLogger.error("AlarmScheduler", "Ошибка планирования alarm: ${e.message}", e)
            // Fallback на неточный alarm
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                DiagnosticLogger.log("AlarmScheduler", "Использован fallback setAndAllowWhileIdle")
            } catch (e2: Exception) {
                DiagnosticLogger.error("AlarmScheduler", "Критическая ошибка: ${e2.message}", e2)
            }
        }
    }
    
    private fun checkManufacturerRestrictions(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        
        DiagnosticLogger.log("AlarmScheduler", "Производитель: $manufacturer, Android: ${Build.VERSION.SDK_INT}")
        
        // Проверка оптимизации батареи
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
            if (!isIgnoringBatteryOptimizations) {
                DiagnosticLogger.log("AlarmScheduler", "⚠ Оптимизация батареи ВКЛЮЧЕНА - может блокировать фоновую работу")
            } else {
                DiagnosticLogger.log("AlarmScheduler", "✓ Оптимизация батареи отключена")
            }
        }
        
        // Специфичные предупреждения для производителей
        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                DiagnosticLogger.log("AlarmScheduler", "⚠ XIAOMI: Проверьте настройки:")
                DiagnosticLogger.log("AlarmScheduler", "  1. Настройки → Приложения → ForkNews → Автозапуск → ВКЛЮЧИТЬ")
                DiagnosticLogger.log("AlarmScheduler", "  2. Настройки → Приложения → ForkNews → Ограничения → Нет ограничений")
                DiagnosticLogger.log("AlarmScheduler", "  3. Безопасность → Разрешения → Автозапуск → ForkNews → РАЗРЕШИТЬ")
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> {
                DiagnosticLogger.log("AlarmScheduler", "⚠ OPPO/OnePlus: Проверьте настройки:")
                DiagnosticLogger.log("AlarmScheduler", "  1. Настройки → Батарея → Оптимизация батареи → ForkNews → Не оптимизировать")
                DiagnosticLogger.log("AlarmScheduler", "  2. Настройки → Приложения → ForkNews → Автозапуск → ВКЛЮЧИТЬ")
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                DiagnosticLogger.log("AlarmScheduler", "⚠ VIVO/iQOO: Проверьте настройки:")
                DiagnosticLogger.log("AlarmScheduler", "  1. i Manager → Автозапуск приложений → ForkNews → ВКЛЮЧИТЬ")
                DiagnosticLogger.log("AlarmScheduler", "  2. Настройки → Батарея → Фоновая работа → ForkNews → Разрешить высокое энергопотребление")
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                DiagnosticLogger.log("AlarmScheduler", "⚠ HUAWEI/Honor: Проверьте настройки:")
                DiagnosticLogger.log("AlarmScheduler", "  1. Настройки → Батарея → Запуск приложений → ForkNews → Управление вручную")
                DiagnosticLogger.log("AlarmScheduler", "  2. Включите: Автозапуск, Вторичный запуск, Работа в фоне")
            }
            manufacturer.contains("samsung") -> {
                DiagnosticLogger.log("AlarmScheduler", "⚠ SAMSUNG: Проверьте настройки:")
                DiagnosticLogger.log("AlarmScheduler", "  1. Настройки → Приложения → ForkNews → Батарея → Не оптимизировать")
                DiagnosticLogger.log("AlarmScheduler", "  2. Настройки → Обслуживание устройства → Батарея → Исключения → Добавить ForkNews")
            }
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

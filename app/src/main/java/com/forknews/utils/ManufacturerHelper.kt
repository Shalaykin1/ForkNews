package com.forknews.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object ManufacturerHelper {
    
    /**
     * Открывает настройки автозапуска для конкретного производителя
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        val intent = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
            }
            manufacturer.contains("oppo") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
            }
            manufacturer.contains("oneplus") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                }
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }
            }
            manufacturer.contains("samsung") -> {
                // Samsung использует стандартные настройки батареи
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
            else -> {
                // Fallback - открываем общие настройки приложения
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
        }
        
        return try {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            DiagnosticLogger.log("ManufacturerHelper", "Открыты настройки автозапуска для $manufacturer")
            true
        } catch (e: Exception) {
            DiagnosticLogger.error("ManufacturerHelper", "Не удалось открыть настройки автозапуска: ${e.message}", e)
            // Пробуем открыть общие настройки приложения
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                DiagnosticLogger.log("ManufacturerHelper", "Открыты общие настройки приложения")
                true
            } catch (e2: Exception) {
                DiagnosticLogger.error("ManufacturerHelper", "Критическая ошибка открытия настроек: ${e2.message}", e2)
                false
            }
        }
    }
    
    /**
     * Получает инструкции для конкретного производителя
     */
    fun getManufacturerInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                """
                📱 XIAOMI/REDMI/POCO - Инструкция:
                
                1. Автозапуск:
                   Настройки → Приложения → Управление приложениями → ForkNews → Автозапуск → ВКЛЮЧИТЬ
                
                2. Батарея:
                   Настройки → Приложения → ForkNews → Экономия энергии → Нет ограничений
                
                3. Разрешения:
                   Безопасность → Разрешения → Автозапуск → ForkNews → РАЗРЕШИТЬ
                
                4. Блокировка в фоне:
                   Недавние приложения → ForkNews → Замок (чтобы не закрывалось)
                """.trimIndent()
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                """
                📱 OPPO/REALME - Инструкция:
                
                1. Батарея:
                   Настройки → Батарея → Оптимизация батареи → ForkNews → Не оптимизировать
                
                2. Автозапуск:
                   Настройки → Приложения → Управление приложениями → ForkNews → Автозапуск → ВКЛЮЧИТЬ
                
                3. Фоновая работа:
                   Настройки → Приложения → ForkNews → Ограничения фоновой работы → Нет ограничений
                """.trimIndent()
            }
            manufacturer.contains("oneplus") -> {
                """
                📱 ONEPLUS - Инструкция:
                
                1. Батарея:
                   Настройки → Батарея → Оптимизация батареи → ForkNews → Не оптимизировать
                
                2. Автозапуск:
                   Настройки → Приложения → ForkNews → Автозапуск → ВКЛЮЧИТЬ
                
                3. Недавние приложения:
                   Недавние → ForkNews → Замок (чтобы не закрывалось)
                """.trimIndent()
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                """
                📱 VIVO/iQOO - Инструкция:
                
                1. i Manager:
                   i Manager → Автозапуск приложений → ForkNews → ВКЛЮЧИТЬ
                
                2. Батарея:
                   Настройки → Батарея → Энергопотребление приложений → ForkNews → Высокое энергопотребление в фоне
                
                3. Фоновая работа:
                   Настройки → Приложения → ForkNews → Разрешить работу в фоне
                """.trimIndent()
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                """
                📱 HUAWEI/HONOR - Инструкция:
                
                1. Запуск приложений:
                   Настройки → Батарея → Запуск приложений → ForkNews → Управление вручную
                   Включите: Автозапуск, Вторичный запуск, Работа в фоне
                
                2. Защищённые приложения:
                   Диспетчер телефона → Защищённые приложения → ForkNews → ВКЛЮЧИТЬ
                """.trimIndent()
            }
            manufacturer.contains("samsung") -> {
                """
                📱 SAMSUNG - Инструкция:
                
                1. Батарея:
                   Настройки → Приложения → ForkNews → Батарея → Не оптимизировать
                
                2. Обслуживание устройства:
                   Настройки → Обслуживание устройства → Батарея → Ограничения фонового использования → 
                   Спящий режим → Убедитесь что ForkNews НЕ в списке
                
                3. Исключения:
                   Настройки → Обслуживание устройства → Батарея → ... → Настройки → 
                   Приложения, не переводимые в спящий режим → Добавить ForkNews
                """.trimIndent()
            }
            else -> {
                """
                📱 Общие рекомендации:
                
                1. Отключите оптимизацию батареи:
                   Настройки → Батарея → Оптимизация батареи → ForkNews → Не оптимизировать
                
                2. Разрешите фоновую работу:
                   Настройки → Приложения → ForkNews → Батарея → Нет ограничений
                
                3. Проверьте разрешения на уведомления:
                   Настройки → Приложения → ForkNews → Уведомления → Включить все
                """.trimIndent()
            }
        }
    }
    
    /**
     * Проверяет, есть ли известные ограничения у производителя
     */
    fun hasKnownRestrictions(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
               manufacturer.contains("redmi") ||
               manufacturer.contains("poco") ||
               manufacturer.contains("oppo") ||
               manufacturer.contains("realme") ||
               manufacturer.contains("oneplus") ||
               manufacturer.contains("vivo") ||
               manufacturer.contains("iqoo") ||
               manufacturer.contains("huawei") ||
               manufacturer.contains("honor")
    }
    
    /**
     * Получает короткое название производителя
     */
    fun getManufacturerName(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") -> "Xiaomi"
            manufacturer.contains("redmi") -> "Redmi"
            manufacturer.contains("poco") -> "Poco"
            manufacturer.contains("oppo") -> "Oppo"
            manufacturer.contains("realme") -> "Realme"
            manufacturer.contains("oneplus") -> "OnePlus"
            manufacturer.contains("vivo") -> "Vivo"
            manufacturer.contains("iqoo") -> "iQOO"
            manufacturer.contains("huawei") -> "Huawei"
            manufacturer.contains("honor") -> "Honor"
            manufacturer.contains("samsung") -> "Samsung"
            else -> Build.MANUFACTURER
        }
    }
}

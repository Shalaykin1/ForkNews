package com.forknews.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.*
import com.forknews.R
import com.forknews.data.local.AppDatabase
import com.forknews.data.repository.RepositoryRepository
import com.forknews.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val WORK_NAME = "update_check_work"
        private const val CHANNEL_ID = "forknews_updates"
        private const val CHANNEL_NAME = "Repository Updates"
        
        fun schedulePeriodicWork(context: Context, intervalMinutes: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    15,
                    TimeUnit.MINUTES
                )
                .addTag("update_check")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "Запланирована периодическая работа: интервал $intervalMinutes мин")
        }
        
        fun scheduleCustomTimeWork(context: Context, hour: Int, minute: Int) {
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                
                if (before(currentDate)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_custom",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            // Reschedule for next day
            val dailyWorkRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                24,
                TimeUnit.HOURS
            )
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_NAME}_daily",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWorkRequest
            )
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "=== WORKER ЗАПУЩЕН ===")
            PreferencesManager.init(applicationContext)
            
            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "forknews_database"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
            
            val repository = RepositoryRepository(database.repositoryDao())
            val repos = repository.getRepositoriesWithNotifications()
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "Найдено репозиториев с уведомлениями: ${repos.size}")
            
            var updatesFound = 0
            for (repo in repos) {
                com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "Проверяем: ${repo.owner}/${repo.name}")
                val hasUpdate = repository.checkForUpdates(repo)
                com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "Обновление найдено: $hasUpdate")
                if (hasUpdate) {
                    updatesFound++
                    com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "Показываем уведомление для: ${repo.name}")
                    showNotification(
                        repo.id.toInt(),
                        repo.name,
                        repo.latestRelease ?: "",
                        repo.latestReleaseUrl ?: ""
                    )
                }
            }
            
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "=== WORKER ЗАВЕРШЁН === (обновлений: $updatesFound)")
            Result.success()
        } catch (e: Exception) {
            com.forknews.utils.DiagnosticLogger.error("UpdateCheckWorker", "ОШИБКА: ${e.message}", e)
            e.printStackTrace()
            Result.retry()
        }
    }
    
    private fun showNotification(id: Int, repoName: String, releaseName: String, url: String) {
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "showNotification вызван: id=$id, repo=$repoName, release=$releaseName")
        
        // Check notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "POST_NOTIFICATIONS разрешение: $hasPermission")
            
            if (!hasPermission) {
                com.forknews.utils.DiagnosticLogger.error("UpdateCheckWorker", "⚠️ Нет разрешения POST_NOTIFICATIONS! Уведомление не будет показано.")
                return
            }
        }
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check if notifications are enabled
        if (!notificationManager.areNotificationsEnabled()) {
            com.forknews.utils.DiagnosticLogger.error("UpdateCheckWorker", "⚠️ Уведомления отключены в системных настройках!")
            return
        }
        
        // Create notification channel for Android O and above
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления об обновлениях репозиториев"
            enableLights(true)
            lightColor = android.graphics.Color.BLUE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            
            // Обходить режим "Не беспокоить"
            setBypassDnd(true)
            
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            setSound(
                soundUri,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
            
            // Для Android 13+ - принудительно включаем всплывающие уведомления
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setBlockable(false)
            }
        }
        notificationManager.createNotificationChannel(channel)
        
        // Check channel importance
        val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "Канал создан: importance=${createdChannel?.importance}")
        
        if (createdChannel?.importance == NotificationManager.IMPORTANCE_NONE) {
            com.forknews.utils.DiagnosticLogger.error("UpdateCheckWorker", "⚠️ Канал уведомлений отключен пользователем!")
        }
        
        // Create intent to open URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Full screen intent для всплывающих уведомлений
        val fullScreenIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            applicationContext,
            id + 1000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        
        // Проверяем настройку звука
        val soundEnabled = runBlocking {
            PreferencesManager.getNotificationSoundEnabled().first()
        }
        
        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 $repoName: новый релиз")
            .setContentText(releaseName)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Доступна новая версия: $releaseName\n\nНажмите для просмотра на GitHub"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setLights(android.graphics.Color.BLUE, 1000, 1000)
            .setDefaults(0)
            .setTimeoutAfter(30000)
            .setGroup("forknews_releases")
            .setGroupSummary(false)
        
        // Добавляем звук только если включен
        if (soundEnabled) {
            notificationBuilder.setSound(soundUri)
        }
        
        // Специальные флаги для Xiaomi/OnePlus/iQOO
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer.contains("xiaomi") || 
            manufacturer.contains("redmi") || 
            manufacturer.contains("poco") ||
            manufacturer.contains("oppo") || 
            manufacturer.contains("realme") || 
            manufacturer.contains("oneplus") ||
            manufacturer.contains("vivo") || 
            manufacturer.contains("iqoo")) {
            
            // Для китайских производителей используем максимальный приоритет
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "Применены специальные настройки для $manufacturer")
        }
        
        val notification = notificationBuilder.build()
        
        // Добавляем флаги для всплывающих окон (FLAG_INSISTENT только если звук включен)
        notification.flags = notification.flags or 
            android.app.Notification.FLAG_AUTO_CANCEL or
            android.app.Notification.FLAG_SHOW_LIGHTS
            
        if (soundEnabled) {
            notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
        }
        
        try {
            notificationManager.notify(id, notification)
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckWorker", "✓ Уведомление отправлено: $repoName - $releaseName")
            android.util.Log.d("UpdateCheckWorker", "Notification shown: $repoName - $releaseName")
        } catch (e: Exception) {
            com.forknews.utils.DiagnosticLogger.error("UpdateCheckWorker", "✗ Ошибка отправки уведомления: ${e.message}", e)
            android.util.Log.e("UpdateCheckWorker", "Failed to show notification", e)
        }
    }
}

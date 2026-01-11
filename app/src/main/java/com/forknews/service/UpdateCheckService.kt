package com.forknews.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.forknews.R
import com.forknews.data.local.AppDatabase
import com.forknews.data.repository.RepositoryRepository
import com.forknews.utils.PreferencesManager
import com.forknews.ui.main.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class UpdateCheckService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "forknews_service"
        private const val CHANNEL_NAME = "ForkNews Background Service"
        
        fun start(context: Context) {
            val intent = Intent(context, UpdateCheckService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "=== SERVICE СОЗДАН ===")
        
        createNotificationChannel()
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "=== SERVICE ЗАПУЩЕН ===")
        
        scope.launch {
            try {
                checkForUpdates()
            } catch (e: Exception) {
                com.forknews.utils.DiagnosticLogger.error("UpdateCheckService", "Ошибка проверки: ${e.message}", e)
            } finally {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "=== SERVICE ОСТАНОВЛЕН ===")
        scope.cancel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Фоновая проверка обновлений"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ForkNews")
            .setContentText("Проверка обновлений...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private suspend fun checkForUpdates() {
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Начало проверки обновлений")
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
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Найдено репозиториев: ${repos.size}")
        
        var updatesFound = 0
        for (repo in repos) {
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Проверяем: ${repo.owner}/${repo.name}")
            val hasUpdate = repository.checkForUpdates(repo)
            
            if (hasUpdate) {
                updatesFound++
                com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Найдено обновление для: ${repo.name}")
                showUpdateNotification(
                    repo.id.toInt(),
                    repo.name,
                    repo.latestRelease ?: "",
                    repo.latestReleaseUrl ?: ""
                )
            }
        }
        
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "=== ПРОВЕРКА ЗАВЕРШЕНА === (обновлений: $updatesFound)")
    }
    
    private suspend fun showUpdateNotification(id: Int, repoName: String, releaseName: String, url: String) {
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Показываем уведомление: $repoName - $releaseName")
        
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                com.forknews.utils.DiagnosticLogger.error("UpdateCheckService", "⚠️ Нет разрешения POST_NOTIFICATIONS")
                return
            }
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (!notificationManager.areNotificationsEnabled()) {
            com.forknews.utils.DiagnosticLogger.error("UpdateCheckService", "⚠️ Уведомления отключены")
            return
        }
        
        // Create update channel
        val updateChannel = NotificationChannel(
            "forknews_updates",
            "Repository Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления об обновлениях репозиториев"
            enableLights(true)
            lightColor = android.graphics.Color.BLUE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
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
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setBlockable(false)
            }
        }
        notificationManager.createNotificationChannel(updateChannel)
        
        // Create intent
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val fullScreenIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            id + 1000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val soundEnabled = PreferencesManager.getNotificationSoundEnabled().first()
        
        val notificationBuilder = NotificationCompat.Builder(this, "forknews_updates")
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
        
        if (soundEnabled) {
            notificationBuilder.setSound(soundUri)
        }
        
        // Special flags for Chinese manufacturers
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer.contains("xiaomi") || 
            manufacturer.contains("redmi") || 
            manufacturer.contains("poco") ||
            manufacturer.contains("oppo") || 
            manufacturer.contains("realme") || 
            manufacturer.contains("oneplus") ||
            manufacturer.contains("vivo") || 
            manufacturer.contains("iqoo")) {
            
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Применены специальные настройки для $manufacturer")
        }
        
        val notification = notificationBuilder.build()
        notification.flags = notification.flags or 
            android.app.Notification.FLAG_AUTO_CANCEL or
            android.app.Notification.FLAG_SHOW_LIGHTS
            
        if (soundEnabled) {
            notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
        }
        
        try {
            notificationManager.notify(id, notification)
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "✓ Уведомление отправлено: $repoName")
        } catch (e: Exception) {
            com.forknews.utils.DiagnosticLogger.error("UpdateCheckService", "✗ Ошибка: ${e.message}", e)
        }
    }
}

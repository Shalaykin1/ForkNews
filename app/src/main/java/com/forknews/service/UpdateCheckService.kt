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
        com.forknews.utils.DiagnosticLogger.init(applicationContext)
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
                // Убираем foreground уведомление сразу после проверки
                stopForeground(STOP_FOREGROUND_REMOVE)
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
            NotificationManager.IMPORTANCE_MIN  // Минимальная важность - не показывать в панели
        ).apply {
            description = "Фоновая проверка обновлений"
            setShowBadge(false)
            setSound(null, null)  // Без звука
            enableVibration(false)  // Без вибрации
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
            .setPriority(NotificationCompat.PRIORITY_MIN)  // Минимальный приоритет
            .setContentIntent(pendingIntent)
            .setOngoing(false)  // Можно смахнуть
            .setSilent(true)
            .setShowWhen(false)  // Не показывать время
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
        
        // Логируем каждый репозиторий
        repos.forEachIndexed { index, repo ->
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "  [$index] ${repo.owner}/${repo.name} - последний релиз: ${repo.latestRelease ?: "нет данных"}, hasNewRelease: ${repo.hasNewRelease}")
        }
        
        var updatesFound = 0
        for (repo in repos) {
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "========================================")
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Проверяем: ${repo.owner}/${repo.name}")
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Текущий релиз: ${repo.latestRelease}")
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Флаг hasNewRelease: ${repo.hasNewRelease}")
            
            // Проверяем обновления
            val hasUpdate = repository.checkForUpdates(repo)
            
            // Перечитываем репозиторий для актуальных данных
            val updatedRepo = repository.getRepositoryById(repo.id) ?: repo
            
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Результат проверки API: hasUpdate=$hasUpdate")
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Флаг hasNewRelease после проверки: ${updatedRepo.hasNewRelease}")
            
            // Отправляем уведомление, если есть новый релиз (флаг hasNewRelease=true)
            if (updatedRepo.hasNewRelease) {
                updatesFound++
                com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "✓ НАЙДЕНО ОБНОВЛЕНИЕ!")
                com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "  Новый релиз: ${updatedRepo.latestRelease}")
                com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "  URL: ${updatedRepo.latestReleaseUrl}")
                
                showUpdateNotification(
                    updatedRepo.id.toInt(),
                    updatedRepo.name,
                    updatedRepo.latestRelease ?: "",
                    updatedRepo.latestReleaseUrl ?: ""
                )
            } else {
                com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Обновлений нет")
            }
        }
        
        com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "========================================")
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
        
        // Создаем или получаем канал уведомлений
        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        
        // Проверяем существующий канал
        val existingChannel = notificationManager.getNotificationChannel("forknews_updates")
        if (existingChannel == null) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()
            
            val updateChannel = NotificationChannel(
                "forknews_updates",
                "Обновления репозиториев",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых релизах"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(soundUri, audioAttributes)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setBlockable(false)
                }
            }
            notificationManager.createNotificationChannel(updateChannel)
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Канал уведомлений создан с максимальными настройками")
        } else {
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "Канал уведомлений уже существует")
        }
        
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
        
        val notificationBuilder = NotificationCompat.Builder(this, "forknews_updates")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 $repoName: новый релиз")
            .setContentText(releaseName)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Доступна новая версия: $releaseName\n\nНажмите для просмотра на GitHub"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(android.graphics.Color.BLUE, 1000, 1000)
            .setDefaults(0)
        
        val notification = notificationBuilder.build()
        
        // Добавляем флаги для принудительного показа
        notification.flags = notification.flags or 
            android.app.Notification.FLAG_AUTO_CANCEL or
            android.app.Notification.FLAG_INSISTENT
        
        try {
            notificationManager.notify(id, notification)
            com.forknews.utils.DiagnosticLogger.log("UpdateCheckService", "✓ Уведомление отправлено: $repoName")
        } catch (e: Exception) {
            com.forknews.utils.DiagnosticLogger.error("UpdateCheckService", "✗ Ошибка отправки уведомления: ${e.message}", e)
        }
    }
}
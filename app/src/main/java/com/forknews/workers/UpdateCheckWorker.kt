package com.forknews.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.*
import com.forknews.R
import com.forknews.data.local.AppDatabase
import com.forknews.data.repository.RepositoryRepository
import com.forknews.utils.PreferencesManager
import kotlinx.coroutines.flow.first
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
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
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
            PreferencesManager.init(applicationContext)
            val notificationsEnabled = PreferencesManager.getNotificationsEnabled().first()
            
            if (!notificationsEnabled) {
                return Result.success()
            }
            
            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "forknews_database"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
            
            val repository = RepositoryRepository(database.repositoryDao())
            val repos = repository.getRepositoriesWithNotifications()
            
            for (repo in repos) {
                val hasUpdate = repository.checkForUpdates(repo)
                if (hasUpdate) {
                    showNotification(
                        repo.id.toInt(),
                        repo.name,
                        repo.latestRelease ?: "",
                        repo.latestReleaseUrl ?: ""
                    )
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    
    private fun showNotification(id: Int, repoName: String, releaseName: String, url: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления об обновлениях репозиториев"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
        
        // Create intent to open URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$repoName: новый релиз")
            .setContentText(releaseName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            notificationManager.notify(id, notification)
            android.util.Log.d("UpdateCheckWorker", "Notification shown: $repoName - $releaseName")
        } catch (e: Exception) {
            android.util.Log.e("UpdateCheckWorker", "Failed to show notification", e)
        }
    }
}

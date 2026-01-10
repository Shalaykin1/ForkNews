package com.forknews.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.forknews.workers.UpdateCheckWorker
import com.forknews.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Перезапускаем WorkManager задачу после перезагрузки устройства (5 минут)
            UpdateCheckWorker.schedulePeriodicWork(context, 5L)
        }
    }
}

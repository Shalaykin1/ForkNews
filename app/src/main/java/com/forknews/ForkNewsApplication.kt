package com.forknews

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.forknews.data.local.AppDatabase
import com.forknews.data.model.Repository
import com.forknews.data.model.RepositoryType
import com.forknews.data.repository.RepositoryRepository
import com.forknews.utils.PreferencesManager
import com.forknews.workers.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForkNewsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PreferencesManager
        PreferencesManager.init(this)
        
        // Apply saved theme
        PreferencesManager.applyTheme()
        
        // Initialize default repositories
        initDefaultRepositories()
        
        // Initialize WorkManager
        initWorkManager()
    }
    
    private fun initDefaultRepositories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "forknews_database"
                ).build()
                
                val repository = RepositoryRepository(database.repositoryDao())
                val existingRepos = repository.getAllRepositoriesList()
                
                // Список репозиториев для добавления
                val defaultRepos = listOf(
                    Repository(
                        name = "AdrenoToolsDrivers",
                        owner = "K11MCH1",
                        url = "https://github.com/K11MCH1/AdrenoToolsDrivers",
                        type = RepositoryType.GITHUB,
                        notificationsEnabled = true
                    ),
                    Repository(
                        name = "winlator",
                        owner = "coffincolors",
                        url = "https://github.com/coffincolors/winlator",
                        type = RepositoryType.GITHUB,
                        notificationsEnabled = true
                    ),
                    Repository(
                        name = "Winlator-Ludashi",
                        owner = "StevenMXZ",
                        url = "https://github.com/StevenMXZ/Winlator-Ludashi",
                        type = RepositoryType.GITHUB,
                        notificationsEnabled = true
                    ),
                    Repository(
                        name = "GameHub",
                        owner = "",
                        url = "https://gamehub.xiaoji.com/download/",
                        type = RepositoryType.GAMEHUB,
                        notificationsEnabled = true
                    )
                )
                
                // Добавляем только те репозитории, которых еще нет
                for (repo in defaultRepos) {
                    val exists = existingRepos.any { 
                        it.url == repo.url
                    }
                    if (!exists) {
                        val repoId = repository.addRepository(repo)
                        // Сразу проверяем релизы для нового репозитория
                        val addedRepo = repository.getRepositoryById(repoId)
                        if (addedRepo != null) {
                            repository.checkForUpdates(addedRepo)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun initWorkManager() {
        try {
            // Schedule periodic update checks with default interval (1 minute)
            UpdateCheckWorker.schedulePeriodicWork(this, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.forknews.data.repository

import com.forknews.data.local.RepositoryDao
import com.forknews.data.model.Repository
import com.forknews.data.model.RepositoryType
import com.forknews.data.remote.GameHubService
import com.forknews.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class RepositoryRepository(
    private val repositoryDao: RepositoryDao
) {
    private val githubApi = RetrofitClient.githubApi
    private val gameHubService = GameHubService()
    
    val allRepositories: Flow<List<Repository>> = repositoryDao.getAllRepositories()
    
    suspend fun getAllRepositoriesList(): List<Repository> {
        return repositoryDao.getAllRepositoriesList()
    }
    
    suspend fun addRepository(repository: Repository): Long {
        return repositoryDao.insertRepository(repository)
    }
    
    suspend fun updateRepository(repository: Repository) {
        repositoryDao.updateRepository(repository)
    }
    
    suspend fun deleteRepository(repository: Repository) {
        repositoryDao.deleteRepository(repository)
    }
    
    suspend fun getRepositoriesWithNotifications(): List<Repository> {
        return repositoryDao.getRepositoriesWithNotifications()
    }
    
    suspend fun getRepositoryById(id: Long): Repository? {
        return repositoryDao.getRepositoryById(id)
    }
    
    suspend fun markReleaseAsViewed(id: Long) {
        repositoryDao.markReleaseAsViewed(id)
    }
    
    suspend fun checkForUpdates(repository: Repository): Boolean {
        return when (repository.type) {
            RepositoryType.GITHUB -> checkGitHubUpdate(repository)
            RepositoryType.GAMEHUB -> checkGameHubUpdate(repository)
        }
    }
    
    private suspend fun checkGitHubUpdate(repository: Repository): Boolean {
        return try {
            val response = githubApi.getLatestRelease(repository.owner, repository.name)
            if (response.isSuccessful) {
                val release = response.body()
                if (release != null) {
                    val newRelease = release.tag_name
                    
                    // Если это первая проверка (latestRelease == null), сохраняем релиз без флага обновления
                    if (repository.latestRelease == null) {
                        repositoryDao.updateReleaseWithoutNotification(
                            repository.id,
                            newRelease,
                            release.html_url,
                            System.currentTimeMillis()
                        )
                        return false // Не показываем как "новое обновление"
                    }
                    
                    // Если релиз изменился, помечаем как новое обновление
                    if (newRelease != repository.latestRelease) {
                        repositoryDao.updateRelease(
                            repository.id,
                            newRelease,
                            release.html_url,
                            System.currentTimeMillis()
                        )
                        return true
                    } else {
                        // Релиз не изменился, но обновляем время последней проверки
                        repositoryDao.updateReleaseWithoutNotification(
                            repository.id,
                            newRelease,
                            release.html_url,
                            System.currentTimeMillis()
                        )
                    }
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private suspend fun checkGameHubUpdate(repository: Repository): Boolean {
        return try {
            val newApkName = gameHubService.getLatestApkName()
            if (newApkName != null) {
                // Если это первая проверка (latestRelease == null), сохраняем релиз без флага обновления
                if (repository.latestRelease == null) {
                    repositoryDao.updateReleaseWithoutNotification(
                        repository.id,
                        newApkName,
                        "https://gamehub.xiaoji.com/download/",
                        System.currentTimeMillis()
                    )
                    return false // Не показываем как "новое обновление"
                }
                
                // Если APK изменился, помечаем как новое обновление
                if (newApkName != repository.latestRelease) {
                    repositoryDao.updateRelease(
                        repository.id,
                        newApkName,
                        "https://gamehub.xiaoji.com/download/",
                        System.currentTimeMillis()
                    )
                    return true
                } else {
                    // APK не изменился, но обновляем время последней проверки
                    repositoryDao.updateReleaseWithoutNotification(
                        repository.id,
                        newApkName,
                        "https://gamehub.xiaoji.com/download/",
                        System.currentTimeMillis()
                    )
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

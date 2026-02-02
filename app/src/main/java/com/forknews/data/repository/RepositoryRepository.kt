package com.forknews.data.repository

import com.forknews.data.local.RepositoryDao
import com.forknews.data.model.Repository
import com.forknews.data.model.RepositoryType
import com.forknews.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class RepositoryRepository(
    private val repositoryDao: RepositoryDao
) {
    private val githubApi = RetrofitClient.githubApi
    
    val allRepositories: Flow<List<Repository>> = repositoryDao.getAllRepositories()
    
    suspend fun getAllRepositoriesList(): List<Repository> {
        return repositoryDao.getAllRepositoriesList()
    }
    
    suspend fun getRepositoryCount(): Int {
        return repositoryDao.getRepositoryCount()
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
    
    suspend fun updatePosition(id: Long, position: Int) {
        repositoryDao.updatePosition(id, position)
    }
    
    suspend fun checkForUpdates(repository: Repository): Boolean {
        com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "checkForUpdates начинается для: ${repository.owner}/${repository.name}")
        val result = checkGitHubUpdate(repository)
        com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "checkForUpdates завершён для: ${repository.owner}/${repository.name}, result=$result")
        return result
    }
    
    private suspend fun checkGitHubUpdate(repository: Repository): Boolean {
        return try {
            val token = com.forknews.utils.PreferencesManager.getGitHubTokenSync()
            com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "checkGitHubUpdate: Начинаем загрузку для ${repository.owner}/${repository.name}")
            com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "URL: https://api.github.com/repos/${repository.owner}/${repository.name}/releases")
            com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "GitHub Token: ${if (token.isEmpty()) "НЕ УСТАНОВЛЕН" else "установлен (${token.take(4)}...)"}")
            var release: com.forknews.data.model.GitHubRelease? = null
            
            // Сначала пробуем получить все релизы (для Pre-release)
            try {
                val allReleasesResponse = githubApi.getAllReleases(repository.owner, repository.name)
                com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "getAllReleases ответ: ${allReleasesResponse.isSuccessful}, код: ${allReleasesResponse.code()}, body: ${allReleasesResponse.body()?.size ?: 0} релизов")
                if (!allReleasesResponse.isSuccessful) {
                    com.forknews.utils.DiagnosticLogger.error("RepositoryRepository", "getAllReleases ошибка HTTP ${allReleasesResponse.code()}: ${allReleasesResponse.errorBody()?.string()}")
                }
                if (allReleasesResponse.isSuccessful && !allReleasesResponse.body().isNullOrEmpty()) {
                    release = allReleasesResponse.body()!!.first()
                    com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "Получен релиз из getAllReleases: ${release.tag_name}")
                }
            } catch (e: Exception) {
                com.forknews.utils.DiagnosticLogger.error("RepositoryRepository", "Ошибка getAllReleases: ${e.message}", e)
                e.printStackTrace()
            }
            
            // Если не получилось, используем /releases/latest
            if (release == null) {
                com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "release == null, пробуем getLatestRelease")
                try {
                    val latestResponse = githubApi.getLatestRelease(repository.owner, repository.name)
                    com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "getLatestRelease ответ: ${latestResponse.isSuccessful}, код: ${latestResponse.code()}")
                    if (!latestResponse.isSuccessful) {
                        com.forknews.utils.DiagnosticLogger.error("RepositoryRepository", "getLatestRelease ошибка HTTP ${latestResponse.code()}: ${latestResponse.errorBody()?.string()}")
                    }
                    if (latestResponse.isSuccessful) {
                        release = latestResponse.body()
                        com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "Получен релиз из getLatestRelease: ${release?.tag_name}")
                    }
                } catch (e: Exception) {
                    com.forknews.utils.DiagnosticLogger.error("RepositoryRepository", "Ошибка getLatestRelease: ${e.message}", e)
                    e.printStackTrace()
                }
            }
            
            // Если релиз получен - сохраняем
            if (release != null) {
                com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "Релиз получен: ${release.tag_name}")
                val newRelease = release.tag_name
                val releaseName = release.name?.takeIf { it.isNotBlank() }
                val isPrerelease = release.prerelease
                
                // Получаем текущее состояние из БД для надежности
                val currentRepo = repositoryDao.getRepositoryById(repository.id)
                val oldRelease = currentRepo?.latestRelease
                
                com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "Сравнение: старый='$oldRelease', новый='$newRelease'")
                
                // Релиз изменился?
                val hasNewUpdate = oldRelease != null && oldRelease != newRelease
                
                if (oldRelease == null) {
                    // Первая проверка - сохраняем без уведомления
                    com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "Первая проверка, сохраняем без уведомления")
                    repositoryDao.updateReleaseWithoutNotification(
                        repository.id,
                        newRelease,
                        release.html_url,
                        releaseName,
                        System.currentTimeMillis(),
                        isPrerelease,
                        release.published_at
                    )
                    return false
                } else if (hasNewUpdate) {
                    // Релиз изменился - отправляем уведомление
                    com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "Релиз изменился! Старый: $oldRelease -> Новый: $newRelease")
                    repositoryDao.updateRelease(
                        repository.id,
                        newRelease,
                        release.html_url,
                        releaseName,
                        System.currentTimeMillis(),
                        isPrerelease,
                        release.published_at
                    )
                    return true
                } else {
                    // Релиз не изменился - только обновляем время
                    com.forknews.utils.DiagnosticLogger.log("RepositoryRepository", "Релиз не изменился, обновляем время проверки")
                    repositoryDao.updateLastChecked(
                        repository.id,
                        newRelease,
                        release.html_url,
                        releaseName,
                        System.currentTimeMillis(),
                        isPrerelease,
                        release.published_at
                    )
                    return false
                }
            } else {
                com.forknews.utils.DiagnosticLogger.error("RepositoryRepository", "Релиз не получен для ${repository.owner}/${repository.name}")
            }
            
            false
        } catch (e: Exception) {
            com.forknews.utils.DiagnosticLogger.error("RepositoryRepository", "Исключение в checkGitHubUpdate: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

}

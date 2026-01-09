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
        return checkGitHubUpdate(repository)
    }
    
    private suspend fun checkGitHubUpdate(repository: Repository): Boolean {
        return try {
            var release: com.forknews.data.model.GitHubRelease? = null
            
            // Сначала пробуем получить все релизы (для Pre-release)
            try {
                val allReleasesResponse = githubApi.getAllReleases(repository.owner, repository.name)
                if (allReleasesResponse.isSuccessful && !allReleasesResponse.body().isNullOrEmpty()) {
                    release = allReleasesResponse.body()!!.first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Если не получилось, используем /releases/latest
            if (release == null) {
                try {
                    val latestResponse = githubApi.getLatestRelease(repository.owner, repository.name)
                    if (latestResponse.isSuccessful) {
                        release = latestResponse.body()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Если релиз получен - сохраняем
            if (release != null) {
                val newRelease = release.tag_name
                val releaseName = release.name?.takeIf { it.isNotBlank() }
                val isPrerelease = release.prerelease
                
                // Если это первая проверка или релиз изменился
                if (repository.latestRelease == null || newRelease != repository.latestRelease) {
                    val hasNewUpdate = repository.latestRelease != null && newRelease != repository.latestRelease
                    
                    if (hasNewUpdate) {
                        // Есть обновление - помечаем как новое
                        repositoryDao.updateRelease(
                            repository.id,
                            newRelease,
                            release.html_url,
                            releaseName,
                            System.currentTimeMillis(),
                            isPrerelease
                        )
                    } else {
                        // Первая проверка - сохраняем без уведомления
                        repositoryDao.updateReleaseWithoutNotification(
                            repository.id,
                            newRelease,
                            release.html_url,
                            releaseName,
                            System.currentTimeMillis(),
                            isPrerelease
                        )
                    }
                    return hasNewUpdate
                } else {
                    // Релиз не изменился - обновляем только время
                    repositoryDao.updateReleaseWithoutNotification(
                        repository.id,
                        newRelease,
                        release.html_url,
                        releaseName,
                        System.currentTimeMillis(),
                        isPrerelease
                    )
                    return false
                }
            }
            
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}

package com.forknews.data.local

import androidx.room.*
import com.forknews.data.model.Repository
import kotlinx.coroutines.flow.Flow

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM repositories ORDER BY id DESC")
    fun getAllRepositories(): Flow<List<Repository>>
    
    @Query("SELECT * FROM repositories ORDER BY id DESC")
    suspend fun getAllRepositoriesList(): List<Repository>
    
    @Query("SELECT * FROM repositories WHERE id = :id")
    suspend fun getRepositoryById(id: Long): Repository?
    
    @Query("SELECT * FROM repositories WHERE notificationsEnabled = 1")
    suspend fun getRepositoriesWithNotifications(): List<Repository>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repository: Repository): Long
    
    @Update
    suspend fun updateRepository(repository: Repository)
    
    @Delete
    suspend fun deleteRepository(repository: Repository)
    
    @Query("UPDATE repositories SET hasNewRelease = 0 WHERE id = :id")
    suspend fun markReleaseAsViewed(id: Long)
    
    @Query("UPDATE repositories SET latestRelease = :release, latestReleaseUrl = :url, releaseName = :releaseName, hasNewRelease = 1, lastChecked = :timestamp, isPrerelease = :isPrerelease WHERE id = :id")
    suspend fun updateRelease(id: Long, release: String, url: String, releaseName: String?, timestamp: Long, isPrerelease: Boolean)
    
    @Query("UPDATE repositories SET latestRelease = :release, latestReleaseUrl = :url, releaseName = :releaseName, hasNewRelease = 0, lastChecked = :timestamp, isPrerelease = :isPrerelease WHERE id = :id")
    suspend fun updateReleaseWithoutNotification(id: Long, release: String, url: String, releaseName: String?, timestamp: Long, isPrerelease: Boolean)
}

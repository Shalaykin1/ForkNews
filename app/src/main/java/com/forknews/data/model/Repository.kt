package com.forknews.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repositories")
data class Repository(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val owner: String,
    val url: String,
    val latestRelease: String? = null,
    val latestReleaseUrl: String? = null,
    val releaseName: String? = null,
    val hasNewRelease: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val type: RepositoryType = RepositoryType.GITHUB,
    val lastChecked: Long = 0,
    val isPrerelease: Boolean = false,
    val publishedAt: String? = null
)

enum class RepositoryType {
    GITHUB
}

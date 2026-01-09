package com.forknews.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.forknews.data.model.Repository

@Database(
    entities = [Repository::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repositoryDao(): RepositoryDao
}

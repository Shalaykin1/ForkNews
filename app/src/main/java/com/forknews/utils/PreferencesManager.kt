package com.forknews.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesManager {
    private lateinit var context: Context
    
    private val NOTIFICATION_SOUND_ENABLED_KEY = booleanPreferencesKey("notification_sound_enabled")
    private val GITHUB_TOKEN_KEY = stringPreferencesKey("github_token")
    
    fun init(context: Context) {
        this.context = context.applicationContext
    }
    
    // Notification sound
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND_ENABLED_KEY] = enabled
        }
    }
    
    fun getNotificationSoundEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[NOTIFICATION_SOUND_ENABLED_KEY] ?: true // Default enabled
        }
    }
    
    // GitHub Token
    suspend fun setGitHubToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[GITHUB_TOKEN_KEY] = token
        }
    }
    
    fun getGitHubToken(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[GITHUB_TOKEN_KEY] ?: ""
        }
    }
    
    fun getGitHubTokenSync(): String {
        return runBlocking {
            getGitHubToken().first()
        }
    }
}

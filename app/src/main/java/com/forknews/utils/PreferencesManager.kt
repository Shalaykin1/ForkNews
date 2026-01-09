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
    
    private val CHECK_INTERVAL_KEY = longPreferencesKey("check_interval")
    private val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
    private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    private val CUSTOM_TIME_ENABLED_KEY = booleanPreferencesKey("custom_time_enabled")
    private val CUSTOM_TIME_HOUR_KEY = intPreferencesKey("custom_time_hour")
    private val CUSTOM_TIME_MINUTE_KEY = intPreferencesKey("custom_time_minute")
    
    fun init(context: Context) {
        this.context = context.applicationContext
    }
    
    // Check interval (in minutes)
    suspend fun setCheckInterval(minutes: Long) {
        context.dataStore.edit { preferences ->
            preferences[CHECK_INTERVAL_KEY] = minutes
        }
    }
    
    fun getCheckInterval(): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[CHECK_INTERVAL_KEY] ?: 1L // Default 1 minute
        }
    }
    
    suspend fun getCheckIntervalSync(): Long {
        return runBlocking {
            getCheckInterval().first()
        }
    }
    
    // Theme settings
    suspend fun setUseSystemTheme(useSystem: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SYSTEM_THEME_KEY] = useSystem
        }
    }
    
    fun getUseSystemTheme(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[USE_SYSTEM_THEME_KEY] ?: true
        }
    }
    
    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDark
        }
    }
    
    fun getDarkTheme(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_THEME_KEY] ?: false
        }
    }
    
    // Notifications
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
    
    fun getNotificationsEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
        }
    }
    
    // Custom time settings
    suspend fun setCustomTimeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_TIME_ENABLED_KEY] = enabled
        }
    }
    
    fun getCustomTimeEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_TIME_ENABLED_KEY] ?: false
        }
    }
    
    suspend fun setCustomTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_TIME_HOUR_KEY] = hour
            preferences[CUSTOM_TIME_MINUTE_KEY] = minute
        }
    }
    
    fun getCustomTimeHour(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_TIME_HOUR_KEY] ?: 9
        }
    }
    
    fun getCustomTimeMinute(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_TIME_MINUTE_KEY] ?: 0
        }
    }
    
    // Apply theme
    fun applyTheme() {
        runBlocking {
            val useSystem = getUseSystemTheme().first()
            if (useSystem) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                val isDark = getDarkTheme().first()
                AppCompatDelegate.setDefaultNightMode(
                    if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }
}

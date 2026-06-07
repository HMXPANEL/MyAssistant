package com.gitsync.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.gitsync.domain.model.SyncInterval
import com.gitsync.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val syncIntervalKey = longPreferencesKey("sync_interval")
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")

    override val syncInterval: Flow<SyncInterval> = dataStore.data.map { prefs ->
        val minutes = prefs[syncIntervalKey] ?: 0L
        SyncInterval.fromMinutes(minutes)
    }

    override val isDarkTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[darkThemeKey] ?: true
    }

    override val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[notificationsKey] ?: true
    }

    override suspend fun setSyncInterval(interval: SyncInterval) {
        dataStore.edit { prefs ->
            prefs[syncIntervalKey] = interval.minutes
        }
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[darkThemeKey] = enabled
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[notificationsKey] = enabled
        }
    }

    override suspend fun getSyncInterval(): SyncInterval {
        val prefs = dataStore.data.first()
        val minutes = prefs[syncIntervalKey] ?: 0L
        return SyncInterval.fromMinutes(minutes)
    }

    override suspend fun isDarkThemeValue(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[darkThemeKey] ?: true
    }

    override suspend fun areNotificationsEnabled(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[notificationsKey] ?: true
    }
}

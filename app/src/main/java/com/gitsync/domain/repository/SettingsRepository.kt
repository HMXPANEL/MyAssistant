package com.gitsync.domain.repository

import com.gitsync.domain.model.SyncInterval
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val syncInterval: Flow<SyncInterval>
    val isDarkTheme: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>

    suspend fun setSyncInterval(interval: SyncInterval)
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun getSyncInterval(): SyncInterval
    suspend fun isDarkThemeValue(): Boolean
    suspend fun areNotificationsEnabled(): Boolean
}

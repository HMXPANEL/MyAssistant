package com.gitsync

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GitSyncApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SYNC,
                    "Sync Status",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for Git sync operations"
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WORKFLOW,
                    "Workflow Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for GitHub Actions workflow status"
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ARTIFACTS,
                    "Artifact Downloads",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for new APK artifacts"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_SYNC = "channel_sync"
        const val CHANNEL_WORKFLOW = "channel_workflow"
        const val CHANNEL_ARTIFACTS = "channel_artifacts"
    }
}

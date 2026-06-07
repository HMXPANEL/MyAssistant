package com.gitsync.core.util

object Constants {
    const val GITHUB_API_BASE_URL = "https://api.github.com/"
    const val GITHUB_BASE_URL = "https://github.com/"
    const val DATABASE_NAME = "gitsync_db"
    const val PREFS_NAME = "gitsync_prefs"
    const val ENCRYPTED_PREFS_NAME = "gitsync_secure_prefs"

    // Notification IDs
    const val NOTIFICATION_SYNC = 1001
    const val NOTIFICATION_WORKFLOW = 1002
    const val NOTIFICATION_ARTIFACT = 1003

    // WorkManager
    const val SYNC_WORK_NAME = "gitsync_auto_sync"
    const val ARTIFACT_WORK_NAME = "gitsync_artifact_check"

    // Sync intervals in minutes
    val SYNC_INTERVALS = listOf(5L, 15L, 30L, 60L)
}

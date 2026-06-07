package com.gitsync.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gitsync.core.util.Constants
import com.gitsync.domain.model.SyncInterval
import java.util.concurrent.TimeUnit

object SyncScheduler {

    fun scheduleSync(context: Context, interval: SyncInterval) {
        val workManager = WorkManager.getInstance(context)

        if (interval == SyncInterval.DISABLED) {
            workManager.cancelUniqueWork(Constants.SYNC_WORK_NAME)
            return
        }

        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            interval.minutes,
            TimeUnit.MINUTES
        ).addTag(Constants.SYNC_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            Constants.SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.Keep,
            workRequest
        )
    }

    fun scheduleArtifactCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val workRequest = OneTimeWorkRequestBuilder<ArtifactCheckWorker>()
            .addTag(Constants.ARTIFACT_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            Constants.ARTIFACT_WORK_NAME,
            ExistingWorkPolicy.Replace,
            workRequest
        )
    }

    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.SYNC_WORK_NAME)
    }

    fun cancelArtifactCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.ARTIFACT_WORK_NAME)
    }
}

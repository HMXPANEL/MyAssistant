package com.gitsync.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gitsync.GitSyncApplication
import com.gitsync.MainActivity
import com.gitsync.R
import com.gitsync.core.util.Constants
import com.gitsync.domain.model.SyncStatus
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.GitRepository
import com.gitsync.domain.repository.ProjectRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val projectRepository: ProjectRepository,
    private val gitRepository: GitRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val projectList = projectRepository.getAllProjects().first()
        var successCount = 0
        var failCount = 0

        if (projectList.isEmpty()) return Result.success()

        val username = authRepository.getUsername()
        val token = authRepository.getToken()

        if (token.isBlank()) return Result.failure()

        for (project in projectList) {
            try {
                val repoDir = File(project.localPath)
                if (!repoDir.exists()) continue

                if (!gitRepository.isGitRepository(project.localPath)) {
                    gitRepository.initRepository(project.localPath)
                }

                val status = gitRepository.getSyncStatus(project.localPath)
                if (status == SyncStatus.SYNCED) {
                    successCount++
                    continue
                }

                gitRepository.addAll(project.localPath)
                val hash = gitRepository.commit(
                    project.localPath,
                    "Auto-sync from GitSync [${System.currentTimeMillis()}]"
                ).getOrThrow()

                gitRepository.push(
                    project.localPath,
                    username,
                    token,
                    project.branch
                ).getOrThrow()

                projectRepository.updateProject(
                    project.copy(
                        lastSyncTime = System.currentTimeMillis(),
                        lastCommitHash = hash.take(7),
                        lastCommitMessage = "Auto-sync from GitSync"
                    )
                )

                successCount++

                showPushNotification(
                    project.name,
                    "Successfully pushed to ${project.branch}"
                )
            } catch (e: Exception) {
                failCount++

                showPushNotification(
                    project.name,
                    "Sync failed: ${e.message?.take(100) ?: "Unknown error"}",
                    isError = true
                )
            }
        }

        return if (failCount == 0) Result.success() else Result.retry()
    }

    private fun showPushNotification(projectName: String, message: String, isError: Boolean = false) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = GitSyncApplication.CHANNEL_SYNC
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(projectName)
            .setContentText(message)
            .setPriority(
                if (isError) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(
                Constants.NOTIFICATION_SYNC + projectName.hashCode(),
                notification
            )
        }
    }
}

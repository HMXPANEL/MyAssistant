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
import java.net.ConnectException
import java.net.UnknownHostException

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
        var hasRetryableFailure = false
        var hasPermanentFailure = false

        if (projectList.isEmpty()) return Result.success()

        val username = authRepository.getUsername()
        val token = authRepository.getToken()

        if (token.isBlank()) return Result.failure()

        for (project in projectList) {
            try {
                val repoDir = java.io.File(project.localPath)
                if (!repoDir.exists()) {
                    hasPermanentFailure = true
                    continue
                }

                val gitDir = java.io.File(repoDir, ".git")
                val useRestApi = !gitDir.exists()

                if (useRestApi) {
                    // REST API path: re-push entire project via Git Tree API
                    // This is safe — it creates a new commit on top of the existing HEAD
                    val remoteUrl = "https://github.com/${project.repoOwner}/${project.repoName}.git"
                    val result = gitRepository.setupAndPushProject(
                        projectPath = project.localPath,
                        remoteUrl = remoteUrl,
                        username = username,
                        token = token,
                        branch = project.branch
                    )
                    if (result.isSuccess) {
                        val commitHash = result.getOrNull() ?: ""
                        projectRepository.updateProject(
                            project.copy(
                                lastSyncTime = System.currentTimeMillis(),
                                lastCommitHash = commitHash,
                                lastCommitMessage = "Auto-sync from GitSync"
                            )
                        )
                        successCount++
                        showPushNotification(project.name, "Successfully synced to ${project.branch}")
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                        hasPermanentFailure = true
                        showPushNotification(project.name, "Sync failed: ${msg.take(100)}", isError = true)
                    }
                } else {
                    // JGit fallback path for projects with local .git
                    val status = gitRepository.getSyncStatus(project.localPath)
                    if (status == SyncStatus.SYNCED) {
                        successCount++
                        continue
                    }

                    val hasChanges = gitRepository.hasChanges(project.localPath).getOrDefault(false)
                    if (!hasChanges) {
                        successCount++
                        continue
                    }

                    gitRepository.addAll(project.localPath)
                    val hash = gitRepository.commit(
                        project.localPath,
                        "Auto-sync from GitSync [${System.currentTimeMillis()}]"
                    ).getOrThrow()

                    val currentBranch = gitRepository.getCurrentBranch(project.localPath)
                        .getOrDefault(project.branch)

                    gitRepository.push(project.localPath, username, token, currentBranch).getOrThrow()

                    projectRepository.updateProject(
                        project.copy(
                            lastSyncTime = System.currentTimeMillis(),
                            lastCommitHash = hash.take(7),
                            lastCommitMessage = "Auto-sync from GitSync",
                            branch = currentBranch
                        )
                    )
                    successCount++
                    showPushNotification(project.name, "Successfully pushed to $currentBranch")
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                when {
                    e is java.net.UnknownHostException || e is java.net.ConnectException -> {
                        hasRetryableFailure = true
                    }
                    msg.contains("timeout", ignoreCase = true) -> {
                        hasRetryableFailure = true
                    }
                    else -> {
                        hasPermanentFailure = true
                        showPushNotification(
                            project.name,
                            "Sync failed: ${msg.take(100)}",
                            isError = true
                        )
                    }
                }
            }
        }

        return when {
            hasRetryableFailure -> Result.retry()
            hasPermanentFailure && successCount == 0 -> Result.failure()
            else -> Result.success()
        }
    }

    private fun showPushNotification(
        projectName: String,
        message: String,
        isError: Boolean = false
    ) {
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

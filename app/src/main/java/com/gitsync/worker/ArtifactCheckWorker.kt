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
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.ProjectRepository
import com.gitsync.domain.repository.WorkflowRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ArtifactCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val projectRepository: ProjectRepository,
    private val workflowRepository: WorkflowRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val projectList = projectRepository.getAllProjects().first()

        if (projectList.isEmpty()) return Result.success()

        val token = authRepository.getToken()
        if (token.isBlank()) return Result.failure()

        for (project in projectList) {
            try {
                val artifacts = workflowRepository.getArtifacts(
                    project.repoOwner,
                    project.repoName,
                    token
                ).getOrDefault(emptyList())

                val apkArtifacts = artifacts.filter {
                    it.name.contains("apk", ignoreCase = true) && !it.expired
                }

                if (apkArtifacts.isNotEmpty()) {
                    val latest = apkArtifacts.maxBy { it.createdAt }
                    showArtifactNotification(
                        project.name,
                        "${latest.name} (${latest.sizeInBytes / 1024} KB)"
                    )
                }
            } catch (e: Exception) {
                // Silently handle - this is a background check
            }
        }

        return Result.success()
    }

    private fun showArtifactNotification(projectName: String, artifactInfo: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, GitSyncApplication.CHANNEL_ARTIFACTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Artifact: $projectName")
            .setContentText(artifactInfo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(
                Constants.NOTIFICATION_ARTIFACT + projectName.hashCode(),
                notification
            )
        }
    }
}

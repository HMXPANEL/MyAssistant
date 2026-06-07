package com.gitsync.data.repository

import com.gitsync.core.network.GitHubApi
import com.gitsync.data.local.dao.WorkflowRunDao
import com.gitsync.data.remote.mapper.toEntity
import com.gitsync.domain.model.Artifact
import com.gitsync.domain.model.WorkflowConclusion
import com.gitsync.domain.model.WorkflowRun
import com.gitsync.domain.model.WorkflowStatus
import com.gitsync.data.local.entity.WorkflowRunEntity
import com.gitsync.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowRepositoryImpl @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val workflowRunDao: WorkflowRunDao
) : WorkflowRepository {

    override suspend fun getWorkflowRuns(
        owner: String,
        repo: String,
        token: String
    ): Result<List<WorkflowRun>> {
        return try {
            val response = gitHubApi.getWorkflowRuns(owner, repo)
            val runs = response.workflowRuns.map { dto ->
                WorkflowRun(
                    runId = dto.id,
                    workflowName = dto.name,
                    headBranch = dto.headBranch,
                    status = WorkflowStatus.fromString(dto.status),
                    createdAt = parseDate(dto.createdAt),
                    updatedAt = parseDate(dto.updatedAt),
                    htmlUrl = dto.htmlUrl,
                    runNumber = dto.runNumber,
                    durationMs = dto.durationMs
                )
            }
            Result.success(runs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStoredWorkflowRuns(projectId: Long): Flow<List<WorkflowRun>> {
        return workflowRunDao.getWorkflowRunsForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshWorkflowRuns(projectId: Long): Result<List<WorkflowRun>> {
        return try {
            val entities = workflowRunDao.getRecentRuns(projectId)
            val runs = entities.map { it.toDomain() }
            Result.success(runs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveWorkflowRuns(projectId: Long, runs: List<WorkflowRun>) {
        val entities = runs.map { run ->
            WorkflowRunEntity(
                runId = run.runId,
                projectId = projectId,
                workflowName = run.workflowName,
                headBranch = run.headBranch,
                status = run.status.name.lowercase(),
                conclusion = null,
                createdAt = run.createdAt,
                updatedAt = run.updatedAt,
                htmlUrl = run.htmlUrl,
                runNumber = run.runNumber
            )
        }
        workflowRunDao.insertAll(entities)
    }

    override suspend fun getLatestWorkflowStatus(projectId: Long): WorkflowStatus {
        val run = workflowRunDao.getLatestCompletedRun(projectId) ?: return WorkflowStatus.UNKNOWN
        val conclusion = WorkflowConclusion.fromString(run.conclusion)
        return when (conclusion) {
            WorkflowConclusion.SUCCESS -> WorkflowStatus.COMPLETED
            else -> WorkflowStatus.fromString(run.status)
        }
    }

    override suspend fun getArtifacts(
        owner: String,
        repo: String,
        token: String
    ): Result<List<Artifact>> {
        return try {
            val response = gitHubApi.getArtifacts(owner, repo)
            val artifacts = response.artifacts.map { dto ->
                Artifact(
                    id = dto.id,
                    name = dto.name,
                    sizeInBytes = dto.sizeInBytes,
                    downloadUrl = dto.archiveDownloadUrl,
                    expired = dto.expired,
                    createdAt = parseDate(dto.createdAt),
                    workflowRunId = dto.workflowRun?.id,
                    runNumber = dto.workflowRun?.runNumber
                )
            }
            Result.success(artifacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadArtifact(
        owner: String,
        repo: String,
        artifactId: Long,
        token: String,
        destinationDir: File
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val responseBody = gitHubApi.downloadArtifact(owner, repo, artifactId)
                destinationDir.mkdirs()
                val outputFile = File(destinationDir, "artifact_$artifactId.zip")

                FileOutputStream(outputFile).use { outputStream ->
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                Result.success(outputFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            val format = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                java.util.Locale.US
            )
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun WorkflowRunEntity.toDomain(): WorkflowRun {
        return WorkflowRun(
            runId = runId,
            workflowName = workflowName,
            headBranch = headBranch,
            status = WorkflowStatus.fromString(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
            htmlUrl = htmlUrl,
            runNumber = runNumber
        )
    }
}

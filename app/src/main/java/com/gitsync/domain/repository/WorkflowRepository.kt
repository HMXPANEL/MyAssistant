package com.gitsync.domain.repository

import com.gitsync.domain.model.Artifact
import com.gitsync.domain.model.WorkflowRun
import com.gitsync.domain.model.WorkflowStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

interface WorkflowRepository {
    suspend fun getWorkflowRuns(
        owner: String,
        repo: String,
        token: String
    ): Result<List<WorkflowRun>>

    fun getStoredWorkflowRuns(projectId: Long): Flow<List<WorkflowRun>>

    suspend fun refreshWorkflowRuns(projectId: Long): Result<List<WorkflowRun>>

    suspend fun saveWorkflowRuns(projectId: Long, runs: List<WorkflowRun>)

    suspend fun getLatestWorkflowStatus(projectId: Long): WorkflowStatus

    suspend fun getArtifacts(
        owner: String,
        repo: String,
        token: String
    ): Result<List<Artifact>>

    suspend fun downloadArtifact(
        owner: String,
        repo: String,
        artifactId: Long,
        token: String,
        destinationDir: File
    ): Result<File>
}

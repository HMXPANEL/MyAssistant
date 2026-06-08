package com.gitsync.domain.repository

import com.gitsync.domain.model.GitCommit
import com.gitsync.domain.model.SyncStatus
import java.io.File

interface GitRepository {
    suspend fun isGitRepository(projectPath: String): Boolean
    suspend fun hasGitDirectory(projectPath: String): Boolean
    suspend fun validateRepository(projectPath: String): Result<Unit>
    suspend fun initRepository(projectPath: String): Result<Unit>
    suspend fun getStatus(projectPath: String): Result<List<String>>
    suspend fun hasChanges(projectPath: String): Result<Boolean>
    suspend fun addAll(projectPath: String): Result<Unit>
    suspend fun commit(projectPath: String, message: String): Result<String>
    suspend fun push(
        projectPath: String,
        username: String,
        token: String,
        branch: String
    ): Result<Unit>
    suspend fun pull(
        projectPath: String,
        username: String,
        token: String,
        branch: String
    ): Result<Unit>
    suspend fun getCurrentBranch(projectPath: String): Result<String>
    suspend fun getBranches(projectPath: String): Result<List<String>>
    suspend fun getRecentCommits(projectPath: String, maxCount: Int): Result<List<GitCommit>>
    suspend fun getSyncStatus(projectPath: String): SyncStatus
    suspend fun getModifiedFiles(projectPath: String): Result<List<String>>
    suspend fun getOriginUrl(projectPath: String): Result<String>
    suspend fun setOriginUrl(projectPath: String, url: String): Result<Unit>
    suspend fun getRemoteUrl(projectPath: String): Result<String>
    suspend fun setupAndPushProject(
        projectPath: String,
        remoteUrl: String,
        username: String,
        token: String,
        branch: String
    ): Result<String>
}

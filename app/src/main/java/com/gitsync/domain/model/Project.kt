package com.gitsync.domain.model

data class Project(
    val id: Long = 0,
    val name: String,
    val localPath: String,
    val safUri: String = "",
    val repoOwner: String,
    val repoName: String,
    val branch: String = "main",
    val lastSyncTime: Long = 0L,
    val lastCommitHash: String = "",
    val lastCommitMessage: String = "",
    val modifiedFiles: List<String> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.UNKNOWN
)

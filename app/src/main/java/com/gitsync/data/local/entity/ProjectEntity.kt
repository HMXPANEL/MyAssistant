package com.gitsync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val localPath: String,
    val safUri: String = "",
    val repoOwner: String,
    val repoName: String,
    val branch: String = "main",
    val uriPermission: String = "",
    val lastSyncTime: Long = 0L,
    val lastCommitHash: String = "",
    val lastCommitMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

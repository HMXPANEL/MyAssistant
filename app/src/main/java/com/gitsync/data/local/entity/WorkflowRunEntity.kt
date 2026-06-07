package com.gitsync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workflow_runs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class WorkflowRunEntity(
    @PrimaryKey
    val runId: Long,
    val projectId: Long,
    val workflowName: String = "",
    val headBranch: String = "",
    val status: String = "queued",
    val conclusion: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val htmlUrl: String = "",
    val runNumber: Int = 0
)

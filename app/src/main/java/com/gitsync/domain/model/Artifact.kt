package com.gitsync.domain.model

data class Artifact(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val downloadUrl: String,
    val expired: Boolean,
    val createdAt: Long,
    val workflowRunId: Long? = null,
    val runNumber: Int? = null
)

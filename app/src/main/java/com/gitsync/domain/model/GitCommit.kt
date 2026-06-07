package com.gitsync.domain.model

data class GitCommit(
    val hash: String,
    val message: String,
    val author: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

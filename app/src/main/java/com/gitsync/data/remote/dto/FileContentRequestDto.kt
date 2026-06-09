package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FileContentRequestDto(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String? = null
)

data class FileContentResponseDto(
    val content: FileContentMeta? = null,
    val commit: FileCommitMeta? = null
)

data class FileContentMeta(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0
)

data class FileCommitMeta(
    val sha: String = "",
    val message: String = ""
)
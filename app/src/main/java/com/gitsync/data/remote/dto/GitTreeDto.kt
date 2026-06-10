package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateBlobRequestDto(
    val content: String,
    val encoding: String = "base64"
)

data class CreateBlobResponseDto(
    val sha: String = "",
    val url: String = ""
)

data class TreeEntryDto(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob",
    val sha: String
)

data class CreateTreeRequestDto(
    @SerializedName("base_tree") val baseTree: String? = null,
    val tree: List<TreeEntryDto>
)

data class CreateTreeResponseDto(
    val sha: String = "",
    val url: String = ""
)

data class CreateCommitRequestDto(
    val message: String,
    val tree: String,
    val parents: List<String>
)

data class CreateCommitResponseDto(
    val sha: String = "",
    val url: String = ""
)

data class UpdateRefRequestDto(
    val sha: String,
    val force: Boolean = true
)

data class RefResponseDto(
    @SerializedName("object") val refObject: RefObjectDto? = null
)

data class RefObjectDto(
    val sha: String = ""
)

data class CreateRefRequestDto(
    val ref: String,
    val sha: String
)
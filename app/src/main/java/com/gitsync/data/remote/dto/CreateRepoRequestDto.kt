package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateRepoRequestDto(
    val name: String,
    val description: String = "Synced by GitSync",
    @SerializedName("private") val isPrivate: Boolean = false,
    @SerializedName("auto_init") val autoInit: Boolean = true
)
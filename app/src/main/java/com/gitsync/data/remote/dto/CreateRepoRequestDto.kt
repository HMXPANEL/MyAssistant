package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateRepoRequestDto(
    val name: String,
    val description: String = "Created by GitSync",
    @SerializedName("private") val isPrivate: Boolean = true,
    @SerializedName("auto_init") val autoInit: Boolean = false
)
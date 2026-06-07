package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GitHubUserDto(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url") val avatarUrl: String,
    val name: String?,
    val email: String?,
    @SerializedName("public_repos") val publicRepos: Int
)

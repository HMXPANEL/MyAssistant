package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RepoDto(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("private") val isPrivate: Boolean,
    @SerializedName("html_url") val htmlUrl: String,
    val description: String?,
    val fork: Boolean,
    @SerializedName("default_branch") val defaultBranch: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("pushed_at") val pushedAt: String,
    val language: String?,
    val owner: Owner
) {
    data class Owner(
        val login: String,
        val id: Long,
        @SerializedName("avatar_url") val avatarUrl: String
    )

    data class Branch(
        val name: String,
        val commit: Commit
    ) {
        data class Commit(
            val sha: String,
            val url: String
        )
    }
}

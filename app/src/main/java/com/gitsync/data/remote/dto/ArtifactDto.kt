package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ArtifactsResponse(
    @SerializedName("total_count") val totalCount: Int,
    val artifacts: List<ArtifactDto>
)

data class ArtifactDto(
    val id: Long,
    val name: String,
    @SerializedName("size_in_bytes") val sizeInBytes: Long,
    @SerializedName("archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("workflow_run") val workflowRun: WorkflowRunRef?
) {
    data class WorkflowRunRef(
        val id: Long,
        @SerializedName("head_branch") val headBranch: String?,
        @SerializedName("run_number") val runNumber: Int?
    )
}

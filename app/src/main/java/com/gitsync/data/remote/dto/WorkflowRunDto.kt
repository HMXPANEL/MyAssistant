package com.gitsync.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WorkflowRunsResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("workflow_runs") val workflowRuns: List<WorkflowRunDto>
)

data class WorkflowRunDto(
    val id: Long,
    val name: String,
    @SerializedName("head_branch") val headBranch: String,
    @SerializedName("head_sha") val headSha: String,
    val status: String,
    val conclusion: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("run_number") val runNumber: Int,
    @SerializedName("event") val event: String,
    @SerializedName("display_title") val displayTitle: String?,
    @SerializedName("run_started_at") val runStartedAt: String?,
    @SerializedName("jobs_url") val jobsUrl: String
) {
    val durationMs: Long?
        get() {
            if (status != "completed" || conclusion == null) return null
            val start = runStartedAt ?: return null
            return try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                format.apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                val startTime = format.parse(start)?.time ?: return null
                val endTime = format.parse(updatedAt)?.time ?: return null
                endTime - startTime
            } catch (e: Exception) {
                null
            }
        }
}

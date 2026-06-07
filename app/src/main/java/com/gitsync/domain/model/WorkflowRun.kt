package com.gitsync.domain.model

data class WorkflowRun(
    val runId: Long,
    val workflowName: String,
    val headBranch: String,
    val status: WorkflowStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val htmlUrl: String,
    val runNumber: Int,
    val durationMs: Long? = null
)

enum class WorkflowStatus(val displayName: String) {
    QUEUED("Queued"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    WAITING("Waiting"),
    PENDING("Pending"),
    REQUESTED("Requested"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(value: String): WorkflowStatus {
            return when (value.lowercase()) {
                "queued" -> QUEUED
                "in_progress" -> IN_PROGRESS
                "completed" -> COMPLETED
                "waiting" -> WAITING
                "pending" -> PENDING
                "requested" -> REQUESTED
                else -> UNKNOWN
            }
        }
    }
}

enum class WorkflowConclusion(val displayName: String) {
    SUCCESS("Success"),
    FAILURE("Failure"),
    CANCELLED("Cancelled"),
    SKIPPED("Skipped"),
    TIMED_OUT("Timed Out"),
    ACTION_REQUIRED("Action Required"),
    NEUTRAL("Neutral"),
    STALE("Stale"),
    STARTUP_FAILURE("Startup Failure"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(value: String?): WorkflowConclusion {
            if (value == null) return UNKNOWN
            return when (value.lowercase()) {
                "success" -> SUCCESS
                "failure" -> FAILURE
                "cancelled" -> CANCELLED
                "skipped" -> SKIPPED
                "timed_out" -> TIMED_OUT
                "action_required" -> ACTION_REQUIRED
                "neutral" -> NEUTRAL
                "stale" -> STALE
                "startup_failure" -> STARTUP_FAILURE
                else -> UNKNOWN
            }
        }
    }
}

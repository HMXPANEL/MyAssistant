package com.gitsync.data.remote.mapper

import com.gitsync.data.local.entity.CommitEntity
import com.gitsync.data.local.entity.WorkflowRunEntity
import com.gitsync.data.remote.dto.WorkflowRunDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun WorkflowRunDto.toEntity(projectId: Long): WorkflowRunEntity {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")

    val createdAtMs = try {
        format.parse(createdAt)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    val updatedAtMs = try {
        format.parse(updatedAt)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    return WorkflowRunEntity(
        runId = id,
        projectId = projectId,
        workflowName = name,
        headBranch = headBranch,
        status = status,
        conclusion = conclusion,
        createdAt = createdAtMs,
        updatedAt = updatedAtMs,
        htmlUrl = htmlUrl,
        runNumber = runNumber
    )
}

fun WorkflowRunDto.toCommitEntity(projectId: Long): CommitEntity {
    return CommitEntity(
        projectId = projectId,
        hash = headSha.take(7),
        message = displayTitle ?: "Workflow run #$runNumber",
        author = "",
        timestamp = try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )
}

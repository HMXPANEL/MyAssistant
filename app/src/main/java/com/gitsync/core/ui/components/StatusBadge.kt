package com.gitsync.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gitsync.core.ui.theme.StatusCancelled
import com.gitsync.core.ui.theme.StatusFailure
import com.gitsync.core.ui.theme.StatusPending
import com.gitsync.core.ui.theme.StatusRunning
import com.gitsync.core.ui.theme.StatusSuccess
import com.gitsync.domain.model.SyncStatus
import com.gitsync.domain.model.WorkflowConclusion
import com.gitsync.domain.model.WorkflowStatus

@Composable
fun SyncStatusBadge(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        SyncStatus.SYNCED -> StatusSuccess
        SyncStatus.PENDING -> StatusPending
        SyncStatus.SYNCING -> StatusRunning
        SyncStatus.ERROR -> StatusFailure
        SyncStatus.UNKNOWN -> StatusCancelled
    }

    val label = when (status) {
        SyncStatus.SYNCED -> "Synced"
        SyncStatus.PENDING -> "Pending"
        SyncStatus.SYNCING -> "Syncing"
        SyncStatus.ERROR -> "Error"
        SyncStatus.UNKNOWN -> "Unknown"
    }

    StatusDot(color = color, label = label, modifier = modifier)
}

@Composable
fun WorkflowStatusBadge(
    status: WorkflowStatus,
    conclusion: WorkflowConclusion? = null,
    modifier: Modifier = Modifier
) {
    val displayStatus = if (status == WorkflowStatus.COMPLETED && conclusion != null) {
        conclusion
    } else {
        status
    }

    val color = when (displayStatus) {
        is WorkflowConclusion -> when (displayStatus) {
            WorkflowConclusion.SUCCESS -> StatusSuccess
            WorkflowConclusion.FAILURE -> StatusFailure
            WorkflowConclusion.CANCELLED -> StatusCancelled
            WorkflowConclusion.SKIPPED -> StatusCancelled
            WorkflowConclusion.TIMED_OUT -> StatusFailure
            WorkflowConclusion.ACTION_REQUIRED -> StatusPending
            WorkflowConclusion.NEUTRAL -> StatusCancelled
            WorkflowConclusion.STALE -> StatusCancelled
            WorkflowConclusion.STARTUP_FAILURE -> StatusFailure
            WorkflowConclusion.UNKNOWN -> StatusCancelled
        }
        is WorkflowStatus -> when (displayStatus) {
            WorkflowStatus.QUEUED -> StatusPending
            WorkflowStatus.IN_PROGRESS -> StatusRunning
            WorkflowStatus.COMPLETED -> StatusSuccess
            WorkflowStatus.WAITING -> StatusPending
            WorkflowStatus.PENDING -> StatusPending
            WorkflowStatus.REQUESTED -> StatusPending
            WorkflowStatus.UNKNOWN -> StatusCancelled
        }
        else -> StatusCancelled
    }

    val label = when (displayStatus) {
        is WorkflowConclusion -> displayStatus.displayName
        is WorkflowStatus -> displayStatus.displayName
        else -> "Unknown"
    }

    StatusDot(color = color, label = label, modifier = modifier)
}

@Composable
private fun StatusDot(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        label = "statusColor"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor
        )
    }
}

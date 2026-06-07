package com.gitsync.core.util

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.formatDisplay(): String {
    val now = System.currentTimeMillis()
    val diff = now - this.time

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(this)
    }
}

fun String.maskToken(): String {
    if (length <= 8) return "••••••••"
    return "${take(4)}••••${takeLast(4)}"
}

fun String.toSafeFileName(): String {
    return replace(Regex("[/\\\\:*?\"<>|]"), "_")
}

fun Uri.isContentUri(): Boolean = scheme == "content"

fun Long.toFileSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> String.format("%.1f MB", this / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", this / (1024.0 * 1024.0 * 1024.0))
    }
}

fun Long.toDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

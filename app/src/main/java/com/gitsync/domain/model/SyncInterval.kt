package com.gitsync.domain.model

enum class SyncInterval(val minutes: Long, val displayName: String) {
    DISABLED(0, "Disabled"),
    FIVE_MINUTES(5, "5 minutes"),
    FIFTEEN_MINUTES(15, "15 minutes"),
    THIRTY_MINUTES(30, "30 minutes"),
    ONE_HOUR(60, "1 hour");

    companion object {
        fun fromMinutes(minutes: Long): SyncInterval {
            return entries.find { it.minutes == minutes } ?: DISABLED
        }
    }
}

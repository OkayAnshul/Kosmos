package com.example.kosmos.shared.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility object for formatting dates and times in the UI
 */
object DateTimeUtils {

    /**
     * Format timestamp as relative time (e.g., "2 hours ago", "Yesterday", "Dec 15")
     */
    fun formatRelativeTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Never"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 0 -> "Just now" // Future timestamp
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes min ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hour${if (hours > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days days ago"
            }
            diff < TimeUnit.DAYS.toMillis(30) -> {
                val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
                "$weeks week${if (weeks > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(365) -> {
                // Format as "Dec 15" for same year
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                // Format as "Dec 15, 2023" for previous years
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Format timestamp as full date and time (e.g., "Dec 15, 2023 at 3:45 PM")
     */
    fun formatDateTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Never"
        val format = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    /**
     * Format timestamp as short date (e.g., "Dec 15, 2023")
     */
    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "No date"
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    /**
     * Format timestamp as time only (e.g., "3:45 PM")
     */
    fun formatTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return ""
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        val todayStart = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(
            System.currentTimeMillis() % TimeUnit.DAYS.toMillis(1),
            TimeUnit.MILLISECONDS
        )
        return timestamp >= todayStart
    }

    /**
     * Check if timestamp is within the last 24 hours
     */
    fun isLast24Hours(timestamp: Long): Boolean {
        return (System.currentTimeMillis() - timestamp) < TimeUnit.DAYS.toMillis(1)
    }
}

/**
 * Extension function to format Long timestamp as relative time
 */
fun Long?.toRelativeTime(): String = DateTimeUtils.formatRelativeTime(this)

/**
 * Extension function to format Long timestamp as full date/time
 */
fun Long?.toDateTime(): String = DateTimeUtils.formatDateTime(this)

/**
 * Extension function to format Long timestamp as date only
 */
fun Long?.toDateOnly(): String = DateTimeUtils.formatDate(this)

/**
 * Extension function to format Long timestamp as time only
 */
fun Long?.toTimeOnly(): String = DateTimeUtils.formatTime(this)

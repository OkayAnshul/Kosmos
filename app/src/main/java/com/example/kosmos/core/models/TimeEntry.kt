package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Time Entry Model
 *
 * Represents a time tracking entry for a task.
 * Can be created by:
 * 1. Starting/stopping a timer
 * 2. Manual entry by user
 *
 * Features:
 * - Running timer tracking (end_time = null)
 * - Duration auto-calculated on stop
 * - Billable hours support
 * - Hourly rate stored for history
 */
@Serializable
@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Foreign keys
    @SerialName("task_id") val taskId: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String,

    // Time tracking
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,

    // Entry details
    val description: String? = null,
    @SerialName("is_billable") val isBillable: Boolean = true,
    @SerialName("hourly_rate") val hourlyRate: Float? = null,
    @SerialName("is_manual") val isManual: Boolean = false,

    // Metadata
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if this time entry is currently running
     */
    fun isRunning(): Boolean = endTime == null

    /**
     * Calculate current duration in seconds
     * If running, calculates from start to now
     * If stopped, returns stored duration
     */
    fun calculateDuration(): Int {
        return if (isRunning()) {
            val elapsedMs = System.currentTimeMillis() - startTime
            (elapsedMs / 1000).toInt()
        } else {
            durationSeconds ?: 0
        }
    }

    /**
     * Calculate duration in hours (float)
     */
    fun calculateDurationHours(): Float {
        return calculateDuration() / 3600f
    }

    /**
     * Calculate billable amount based on hourly rate
     */
    fun calculateBillableAmount(): Float {
        if (!isBillable || hourlyRate == null) return 0f
        return calculateDurationHours() * hourlyRate
    }

    /**
     * Format duration as HH:MM:SS
     */
    fun formatDuration(): String {
        val totalSeconds = calculateDuration()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format duration as human-readable string
     * Examples: "2h 30m", "45m", "1h 15m 30s"
     */
    fun formatDurationHumanReadable(): String {
        val totalSeconds = calculateDuration()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (hours == 0 && seconds > 0) append("${seconds}s")
        }.trim().ifEmpty { "0s" }
    }

    /**
     * Stop the timer and calculate final duration
     */
    fun stop(endTimestamp: Long = System.currentTimeMillis()): TimeEntry {
        require(isRunning()) { "Timer is not running" }
        require(endTimestamp > startTime) { "End time must be after start time" }

        val duration = ((endTimestamp - startTime) / 1000).toInt()

        return copy(
            endTime = endTimestamp,
            durationSeconds = duration,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Create a new running timer
         */
        fun createTimer(
            taskId: String,
            projectId: String,
            userId: String,
            description: String? = null,
            isBillable: Boolean = true,
            hourlyRate: Float? = null,
            startTime: Long = System.currentTimeMillis()
        ): TimeEntry {
            return TimeEntry(
                taskId = taskId,
                projectId = projectId,
                userId = userId,
                startTime = startTime,
                endTime = null,
                durationSeconds = null,
                description = description,
                isBillable = isBillable,
                hourlyRate = hourlyRate,
                isManual = false
            )
        }

        /**
         * Create a manual time entry (already stopped)
         */
        fun createManualEntry(
            taskId: String,
            projectId: String,
            userId: String,
            startTime: Long,
            endTime: Long,
            description: String? = null,
            isBillable: Boolean = true,
            hourlyRate: Float? = null
        ): TimeEntry {
            require(endTime > startTime) { "End time must be after start time" }

            val durationSeconds = ((endTime - startTime) / 1000).toInt()

            return TimeEntry(
                taskId = taskId,
                projectId = projectId,
                userId = userId,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                description = description,
                isBillable = isBillable,
                hourlyRate = hourlyRate,
                isManual = true
            )
        }
    }
}

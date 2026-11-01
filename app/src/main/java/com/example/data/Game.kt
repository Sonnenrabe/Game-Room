package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val platform: String = "",
    val genre: String = "",
    val totalPlaytimeSeconds: Long = 0L,
    val isArchived: Boolean = false,
    val colorHex: Long = 0xFF00E5FF,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val notes: String = ""
) {
    /**
     * Formats total seconds into HH:MM:SS
     */
    fun formattedPlaytime(extraSeconds: Long = 0L): String {
        val totalSec = totalPlaytimeSeconds + extraSeconds
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Returns a human readable breakdown, e.g. "12h 45m"
     */
    fun readablePlaytime(extraSeconds: Long = 0L): String {
        val totalSec = totalPlaytimeSeconds + extraSeconds
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

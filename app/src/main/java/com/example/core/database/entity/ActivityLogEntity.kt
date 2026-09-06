package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.ActivityLogEntry

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "specific_reason")
    val specificReason: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "details")
    val details: String? = null
) {
    fun toDomain(): ActivityLogEntry {
        val type = try {
            ActivityLogEntry.EventType.valueOf(eventType)
        } catch (_: Exception) {
            ActivityLogEntry.EventType.SYSTEM_EVENT
        }

        val reason = try {
            ActivityLogEntry.SpecificCutReason.valueOf(specificReason)
        } catch (_: Exception) {
            ActivityLogEntry.SpecificCutReason.CONNECTION_RESTORED
        }

        return ActivityLogEntry(
            id = id,
            timestamp = timestamp,
            eventType = type,
            specificReason = reason,
            description = description,
            details = details
        )
    }

    companion object {
        fun fromDomain(domain: ActivityLogEntry): ActivityLogEntity {
            return ActivityLogEntity(
                id = domain.id,
                timestamp = domain.timestamp,
                eventType = domain.eventType.name,
                specificReason = domain.specificReason.name,
                description = domain.description,
                details = domain.details
            )
        }
    }
}

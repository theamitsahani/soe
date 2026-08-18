package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visit_events")
data class VisitEvent(
    @PrimaryKey val eventId: String,
    val visitId: String,
    val taskId: String = "",
    val eventType: String, // e.g. CREATED, ASSIGNED, STARTED, AUTOSAVE, PHOTO_UPLOADED, SUBMITTED, SYNCED, REVIEWED, REJECTED
    val actorId: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val statusFrom: String = "",
    val statusTo: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

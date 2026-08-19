package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncOperationType {
    VISIT_SUBMIT,
    VISIT_UPDATE,
    PHOTO_UPLOAD,
    AUDIT_EVENT,
    TASK_UPDATE
}

enum class SyncQueueStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey val operationId: String,
    val entityType: String, // VISIT, PHOTO, EVENT, TASK
    val entityId: String,
    val operationType: SyncOperationType,
    val payloadJson: String = "{}",
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long = 0L,
    val nextRetryAt: Long = 0L,
    val lastError: String = ""
)

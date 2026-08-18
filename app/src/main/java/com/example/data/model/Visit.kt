package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VisitStatus {
    CREATED,
    ASSIGNED,
    ACCEPTED,
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    SUBMITTED,
    REVIEWED,
    REJECTED,
    CANCELLED
}

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey val visitId: String,
    val taskId: String = "",
    val schoolId: String,
    val employeeId: String,
    val employeeName: String = "",
    val schoolName: String = "",
    val state: String = "Rajasthan",
    val district: String = "",
    val block: String = "",
    val villageName: String = "",
    val schoolType: String = "",
    val udiseCode: String = "",
    val principalName: String = "",
    val principalMobile: String = "",
    val visitDate: String = "",
    val status: VisitStatus = VisitStatus.ASSIGNED,
    // Store answers and photos json/string representations for Room
    val answersJson: String = "{}",
    val photosJson: String = "{}", // Map of Category ID -> List of image URIs/URLs
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val submittedAt: Long? = null,
    val reviewedAt: Long? = null,
    val reviewedBy: String = "",
    val reviewNotes: String = "",
    val rejectionReason: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val appVersion: String = "1.0.0",
    val editCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

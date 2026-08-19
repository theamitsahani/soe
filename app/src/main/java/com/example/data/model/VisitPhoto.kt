package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PhotoUploadStatus {
    CAPTURED,
    LOCAL_SAVED,
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}

@Entity(tableName = "visit_photos")
data class VisitPhoto(
    @PrimaryKey val photoId: String,
    val visitId: String,
    val taskId: String = "",
    val schoolId: String = "",
    val employeeId: String = "",
    val category: String,
    val localUri: String = "",
    val cloudinaryPublicId: String = "",
    val cloudinaryUrl: String = "",
    val uploadStatus: PhotoUploadStatus = PhotoUploadStatus.PENDING,
    val uploadAttemptCount: Int = 0,
    val fileSize: Long = 0L,
    val mimeType: String = "image/jpeg",
    val imageHash: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
    val uploadedAt: Long? = null,
    val lastError: String = ""
)

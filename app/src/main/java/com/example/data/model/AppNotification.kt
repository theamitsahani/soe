package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipientUserId: String = "", // Specific employee ID, "ADMIN", or "ALL"
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // TASK_ASSIGNED, REPORT_SUBMITTED, INFO
    val relatedId: String = "", // taskId or visitId
    val schoolName: String = "",
    val employeeName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

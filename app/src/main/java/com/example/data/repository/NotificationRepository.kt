package com.example.data.repository

import android.content.Context
import com.example.data.local.AppNotificationDao
import com.example.data.model.AppNotification
import com.example.util.AppNotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val notificationDao: AppNotificationDao,
    private val firestore: FirebaseFirestore
) {
    fun getNotificationsForUserFlow(userId: String): Flow<List<AppNotification>> {
        return notificationDao.getNotificationsForUserFlow(userId)
    }

    fun getUnreadCountFlow(userId: String): Flow<Int> {
        return notificationDao.getUnreadCountFlow(userId)
    }

    suspend fun createAndSendNotification(
        context: Context,
        recipientUserId: String,
        title: String,
        message: String,
        type: String,
        relatedId: String,
        schoolName: String = "",
        employeeName: String = ""
    ) {
        val notification = AppNotification(
            recipientUserId = recipientUserId,
            title = title,
            message = message,
            type = type,
            relatedId = relatedId,
            schoolName = schoolName,
            employeeName = employeeName,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        withContext(Dispatchers.IO) {
            // Save locally
            notificationDao.insertNotification(notification)

            // Save to Firestore
            try {
                val data = mapOf(
                    "id" to notification.id,
                    "recipientUserId" to notification.recipientUserId,
                    "title" to notification.title,
                    "message" to notification.message,
                    "type" to notification.type,
                    "relatedId" to notification.relatedId,
                    "schoolName" to notification.schoolName,
                    "employeeName" to notification.employeeName,
                    "timestamp" to notification.timestamp,
                    "isRead" to notification.isRead
                )
                firestore.collection("notifications").document(notification.id).set(data).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Post Android System Notification
            if (type == "TASK_ASSIGNED") {
                AppNotificationHelper.showTaskAssignedNotification(
                    context = context,
                    schoolName = schoolName,
                    visitDate = if (message.contains("on ")) message.substringAfter("on ").trim() else "",
                    employeeName = employeeName
                )
            } else if (type == "REPORT_SUBMITTED") {
                AppNotificationHelper.showReportSubmittedNotification(
                    context = context,
                    schoolName = schoolName,
                    employeeName = employeeName
                )
            }
        }
    }

    suspend fun syncNotificationsFromFirestore(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // BUG FIX: this used to run collection("notifications").get() with NO filter,
                // pulling every user's notifications and filtering client-side. Once the
                // Firestore security rules for "notifications" are correctly scoped per
                // recipient (see firestore.rules), Firestore rejects an unfiltered list query
                // outright for non-admin users, because it can't prove every possible result
                // satisfies the read rule — so this call would always fail with
                // PERMISSION_DENIED for employees. Querying only for the ids this user is
                // actually allowed to see keeps it both secure and working.
                val recipientIds = listOf(userId, "ADMIN", "ALL").distinct()
                val snapshot = firestore.collection("notifications")
                    .whereIn("recipientUserId", recipientIds)
                    .get()
                    .await()

                val remoteList = snapshot.documents.mapNotNull { doc ->
                    val recipient = doc.getString("recipientUserId") ?: ""
                    AppNotification(
                        id = doc.getString("id") ?: doc.id,
                        recipientUserId = recipient,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        type = doc.getString("type") ?: "INFO",
                        relatedId = doc.getString("relatedId") ?: "",
                        schoolName = doc.getString("schoolName") ?: "",
                        employeeName = doc.getString("employeeName") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                }

                if (remoteList.isNotEmpty()) {
                    notificationDao.insertNotifications(remoteList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun markAllAsRead(userId: String) {
        withContext(Dispatchers.IO) {
            notificationDao.markAllAsRead(userId)

            // BUG FIX: this used to only flip the local Room flag. The read/unread state never
            // reached Firestore, so a fresh install or a second device for the same account
            // would show every notification as unread again forever. Now the matching remote
            // docs are patched too (best-effort — local state already updated either way).
            try {
                val recipientIds = listOf(userId, "ADMIN", "ALL").distinct()
                val snapshot = firestore.collection("notifications")
                    .whereIn("recipientUserId", recipientIds)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.update(doc.reference, "isRead", true)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun clearAllForUser(userId: String) {
        withContext(Dispatchers.IO) {
            notificationDao.clearAllForUser(userId)

            // BUG FIX: same gap as markAllAsRead — "clear all" only cleared the local copy,
            // so the notifications would just come back on the next syncNotificationsFromFirestore().
            try {
                val recipientIds = listOf(userId, "ADMIN", "ALL").distinct()
                val snapshot = firestore.collection("notifications")
                    .whereIn("recipientUserId", recipientIds)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

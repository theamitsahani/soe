package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications WHERE recipientUserId = :userId OR recipientUserId = 'ADMIN' OR recipientUserId = 'ALL' ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: String): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications WHERE recipientUserId = :userId OR recipientUserId = 'ADMIN' OR recipientUserId = 'ALL' ORDER BY timestamp DESC")
    suspend fun getNotificationsForUser(userId: String): List<AppNotification>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE (recipientUserId = :userId OR recipientUserId = 'ADMIN' OR recipientUserId = 'ALL') AND isRead = 0")
    fun getUnreadCountFlow(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotification>)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE recipientUserId = :userId OR recipientUserId = 'ADMIN' OR recipientUserId = 'ALL'")
    suspend fun markAllAsRead(userId: String)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM app_notifications WHERE recipientUserId = :userId OR recipientUserId = 'ADMIN' OR recipientUserId = 'ALL'")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)
}

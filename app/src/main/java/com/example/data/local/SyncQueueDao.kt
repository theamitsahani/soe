package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncQueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun getAllQueueItems(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getItemsByStatus(status: SyncQueueStatus): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE operationId = :operationId LIMIT 1")
    suspend fun getItemById(operationId: String): SyncQueueItem?

    @Query("SELECT * FROM sync_queue WHERE entityId = :entityId LIMIT 1")
    suspend fun getItemByEntityId(entityId: String): SyncQueueItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SyncQueueItem)

    @Update
    suspend fun updateItem(item: SyncQueueItem)

    @Query("DELETE FROM sync_queue WHERE operationId = :operationId")
    suspend fun deleteItem(operationId: String)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED' AND lastAttemptAt < :timestamp")
    suspend fun purgeOldSynced(timestamp: Long)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    fun getPendingQueueCount(): Flow<Int>
}

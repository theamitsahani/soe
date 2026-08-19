package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ImportBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches ORDER BY startedAt DESC")
    fun getAllImportBatches(): Flow<List<ImportBatch>>

    @Query("SELECT * FROM import_batches WHERE importBatchId = :batchId LIMIT 1")
    suspend fun getImportBatchById(batchId: String): ImportBatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportBatch(batch: ImportBatch)

    @Update
    suspend fun updateImportBatch(batch: ImportBatch)
}

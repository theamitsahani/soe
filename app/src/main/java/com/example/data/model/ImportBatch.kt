package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_batches")
data class ImportBatch(
    @PrimaryKey val importBatchId: String,
    val startedBy: String,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalRows: Int = 0,
    val createdCount: Int = 0,
    val updatedCount: Int = 0,
    val duplicateCount: Int = 0,
    val failedCount: Int = 0,
    val errorCount: Int = 0,
    val errorsJson: String = "[]",
    val status: String = "COMPLETED" // IN_PROGRESS, COMPLETED, FAILED
)

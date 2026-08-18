package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncStatus
import com.example.data.model.VisitEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitEventDao {
    @Query("SELECT * FROM visit_events WHERE visitId = :visitId ORDER BY timestamp ASC")
    fun getEventsForVisit(visitId: String): Flow<List<VisitEvent>>

    @Query("SELECT * FROM visit_events WHERE visitId = :visitId ORDER BY timestamp ASC")
    suspend fun getEventsListForVisit(visitId: String): List<VisitEvent>

    @Query("SELECT * FROM visit_events WHERE syncStatus = :syncStatus")
    suspend fun getEventsBySyncStatus(syncStatus: SyncStatus): List<VisitEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: VisitEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<VisitEvent>)

    @Query("DELETE FROM visit_events WHERE eventId = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("DELETE FROM visit_events WHERE visitId = :visitId")
    suspend fun deleteEventsForVisit(visitId: String)
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Visit
import com.example.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY createdAt DESC")
    fun getAllVisits(): Flow<List<Visit>>

    @Query("SELECT * FROM visits")
    suspend fun getAllVisitsList(): List<Visit>

    @Query("SELECT * FROM visits WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun getVisitsBySchool(schoolId: String): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE schoolId = :schoolId")
    suspend fun getVisitsListBySchool(schoolId: String): List<Visit>

    @Query("SELECT * FROM visits WHERE employeeId = :employeeId ORDER BY createdAt DESC")
    fun getVisitsByEmployee(employeeId: String): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE visitId = :visitId LIMIT 1")
    suspend fun getVisitById(visitId: String): Visit?

    @Query("SELECT * FROM visits WHERE syncStatus = :syncStatus")
    suspend fun getVisitsBySyncStatus(syncStatus: SyncStatus): List<Visit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<Visit>)

    @Update
    suspend fun updateVisit(visit: Visit)

    @Query("DELETE FROM visits WHERE visitId = :visitId")
    suspend fun deleteVisitById(visitId: String)

    @Query("DELETE FROM visits WHERE schoolId = :schoolId")
    suspend fun deleteVisitsBySchool(schoolId: String)

    @Query("DELETE FROM visits WHERE visitId NOT IN (:validVisitIds)")
    suspend fun deleteVisitsNotIn(validVisitIds: List<String>)

    @Query("DELETE FROM visits")
    suspend fun deleteAllVisits()

    @Query("DELETE FROM visits WHERE employeeId = :employeeId")
    suspend fun deleteVisitsForEmployee(employeeId: String)

}

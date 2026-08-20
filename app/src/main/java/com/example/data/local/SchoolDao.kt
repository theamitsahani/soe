package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.School
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools ORDER BY schoolName ASC")
    fun getAllSchools(): Flow<List<School>>

    @Query("SELECT * FROM schools")
    suspend fun getAllSchoolsList(): List<School>

    @Query("SELECT * FROM schools WHERE schoolId = :schoolId LIMIT 1")
    suspend fun getSchoolById(schoolId: String): School?

    @Query("SELECT * FROM schools WHERE schoolName LIKE '%' || :query || '%' OR districtName LIKE '%' || :query || '%' OR blockName LIKE '%' || :query || '%' OR villageName LIKE '%' || :query || '%' OR principalName LIKE '%' || :query || '%'")
    fun searchSchools(query: String): Flow<List<School>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: School)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchools(schools: List<School>)

    @Update
    suspend fun updateSchool(school: School)

    @Query("UPDATE schools SET visitDate = :visitDate, updatedAt = :updatedAt WHERE schoolId = :schoolId")
    suspend fun updateSchoolVisitDate(schoolId: String, visitDate: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE schools SET isDeleted = 1, deletedAt = :deletedAt WHERE schoolId = :schoolId")
    suspend fun softDeleteSchool(schoolId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE schools SET isDeleted = 0, deletedAt = 0 WHERE schoolId = :schoolId")
    suspend fun restoreSchool(schoolId: String)

    @Query("DELETE FROM schools WHERE schoolId = :schoolId")
    suspend fun deleteSchoolById(schoolId: String)

    @Query("DELETE FROM schools")
    suspend fun clearSchools()
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE LOWER(TRIM(employeeId)) = LOWER(TRIM(:employeeId)) OR (:userEmail != '' AND LOWER(TRIM(employeeEmail)) = LOWER(TRIM(:userEmail))) OR (:userEmail != '' AND LOWER(TRIM(employeeId)) = LOWER(TRIM(:userEmail))) OR (:employeeId != '' AND LOWER(TRIM(employeeEmail)) = LOWER(TRIM(:employeeId))) ORDER BY createdAt DESC")
    fun getTasksByEmployee(employeeId: String, userEmail: String = ""): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE schoolId = :schoolId")
    suspend fun deleteTasksBySchool(schoolId: String)

    @Query("UPDATE tasks SET status = 'SUBMITTED' WHERE (employeeId = :employeeId OR employeeEmail = :employeeId) AND schoolId = :schoolId")
    suspend fun markTaskSubmittedForEmployeeAndSchool(employeeId: String, schoolId: String)
}

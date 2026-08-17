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

    @Query("SELECT * FROM tasks WHERE LOWER(TRIM(employeeId)) = LOWER(TRIM(:employeeId)) ORDER BY createdAt DESC")
    fun getTasksByEmployee(employeeId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE LOWER(TRIM(employeeId)) = LOWER(TRIM(:employeeId)) AND schoolId = :schoolId AND visitDate = :visitDate AND status != 'SUBMITTED' AND status != 'REVIEWED' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveTask(employeeId: String, schoolId: String, visitDate: String): Task?

    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): Task?

    @Query("SELECT * FROM tasks WHERE visitId = :visitId LIMIT 1")
    suspend fun getTaskByVisitId(visitId: String): Task?

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

    @Query("DELETE FROM tasks WHERE taskId NOT IN (:validTaskIds)")
    suspend fun deleteTasksNotIn(validTaskIds: List<String>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM tasks WHERE LOWER(TRIM(employeeId)) = LOWER(TRIM(:employeeId))")
    suspend fun deleteTasksForEmployee(employeeId: String)

    @Query("DELETE FROM tasks WHERE LOWER(TRIM(employeeId)) = LOWER(TRIM(:employeeId)) AND taskId NOT IN (:validTaskIds)")
    suspend fun deleteTasksForEmployeeNotIn(employeeId: String, validTaskIds: List<String>)

    @Query("UPDATE tasks SET status = 'SUBMITTED', visitId = :visitId WHERE taskId = :taskId")
    suspend fun markTaskSubmittedById(taskId: String, visitId: String)

    @Query("UPDATE tasks SET status = 'SUBMITTED' WHERE visitId = :visitId")
    suspend fun markTaskSubmittedByVisitId(visitId: String)

    @Query("UPDATE tasks SET status = 'SUBMITTED' WHERE LOWER(TRIM(employeeId)) = LOWER(TRIM(:employeeId)) AND schoolId = :schoolId")
    suspend fun markTaskSubmittedForEmployeeAndSchool(employeeId: String, schoolId: String)
}

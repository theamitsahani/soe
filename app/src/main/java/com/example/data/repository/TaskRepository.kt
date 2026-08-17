package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Task
import com.example.data.model.UserRole
import com.example.data.model.VisitStatus
import com.example.util.FirebaseUtils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TaskRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    private var tasksListenerRegistration: ListenerRegistration? = null

    fun getAllTasks(): Flow<List<Task>> = db.taskDao().getAllTasks()

    fun getTasksByEmployee(employeeId: String): Flow<List<Task>> = db.taskDao().getTasksByEmployee(employeeId)

    fun startTasksRealtimeListener(role: UserRole? = null, userId: String? = null) {
        if (tasksListenerRegistration != null) return
        val fAuth = FirebaseUtils.auth ?: return
        val currentFbUser = fAuth.currentUser ?: return
        val fStore = firestore ?: return
        val currentUid = userId?.ifBlank { currentFbUser.uid } ?: currentFbUser.uid
        val isEmployee = (role == UserRole.EMPLOYEE)

        try {
            val query = if (isEmployee && currentUid.isNotBlank()) {
                fStore.collection("tasks").whereEqualTo("employeeId", currentUid)
            } else {
                fStore.collection("tasks")
            }

            tasksListenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TaskRepository", "Tasks snapshot listener notice: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val tasks = snapshot.documents.mapNotNull { doc ->
                                parseDocToTask(doc)
                            }
                            if (tasks.isNotEmpty()) {
                                db.taskDao().insertTasks(tasks)
                            }
                        } catch (e: Exception) {
                            Log.e("TaskRepository", "Failed to cache tasks from listener", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TaskRepository", "Error starting tasks realtime listener", e)
        }
    }

    fun stopTasksRealtimeListener() {
        try {
            tasksListenerRegistration?.remove()
        } catch (e: Exception) {
            Log.w("TaskRepository", "Error stopping tasks listener", e)
        }
        tasksListenerRegistration = null
    }

    private suspend fun parseDocToTask(doc: DocumentSnapshot): Task? {
        val taskId = doc.getString("taskId")?.ifBlank { doc.id } ?: doc.id
        if (taskId.isBlank()) return null
        val schoolId = doc.getString("schoolId") ?: ""
        val employeeId = doc.getString("employeeId") ?: ""
        var schoolName = doc.getString("schoolName") ?: ""
        if (schoolName.isBlank() && schoolId.isNotBlank()) {
            schoolName = db.schoolDao().getSchoolById(schoolId)?.schoolName ?: "School ($schoolId)"
        }
        if (schoolName.isBlank()) {
            schoolName = "School Visit Task"
        }

        val visitId = doc.getString("visitId") ?: ""
        val statusStr = doc.getString("status") ?: VisitStatus.ASSIGNED.name
        var status = try { VisitStatus.valueOf(statusStr) } catch (e: Exception) { VisitStatus.ASSIGNED }

        // If local database has this task marked as SUBMITTED or REVIEWED, or if matching visit exists, preserve it
        if (status == VisitStatus.ASSIGNED) {
            val localTask = db.taskDao().getTaskById(taskId)
            if (localTask != null && (localTask.status == VisitStatus.SUBMITTED || localTask.status == VisitStatus.REVIEWED)) {
                status = localTask.status
            } else if (visitId.isNotBlank() && db.visitDao().getVisitById(visitId) != null) {
                status = VisitStatus.SUBMITTED
            } else if (schoolId.isNotBlank() && db.visitDao().getVisitsListBySchool(schoolId).isNotEmpty()) {
                status = VisitStatus.SUBMITTED
            }
        }

        return Task(
            taskId = taskId,
            visitId = visitId,
            schoolId = schoolId,
            employeeId = employeeId,
            employeeName = doc.getString("employeeName") ?: "",
            schoolName = schoolName,
            district = doc.getString("district") ?: "",
            block = doc.getString("block") ?: "",
            assignedBy = doc.getString("assignedBy") ?: "Admin",
            visitDate = doc.getString("visitDate") ?: "",
            status = status,
            notes = doc.getString("notes") ?: "",
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    suspend fun syncTasksFromFirestore(role: com.example.data.model.UserRole? = null, userId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fAuth = FirebaseUtils.auth
            val currentFbUser = fAuth?.currentUser
            val currentUid = userId ?: currentFbUser?.uid ?: ""

            val fStore = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))

            var isEmployee = (role == com.example.data.model.UserRole.EMPLOYEE)

            if (role == null && currentUid.isNotBlank()) {
                try {
                    val userDocTask = fStore.collection("users").document(currentUid).get()
                    val userDoc = com.google.android.gms.tasks.Tasks.await(userDocTask)
                    val r = userDoc.getString("role")?.trim()?.uppercase()
                    if (r == "EMPLOYEE") isEmployee = true
                } catch (e: Exception) {
                    android.util.Log.w("TaskRepository", "Could not check current user role, default behavior", e)
                }
            }

            val query = if (isEmployee && currentUid.isNotBlank()) {
                fStore.collection("tasks").whereEqualTo("employeeId", currentUid)
            } else {
                fStore.collection("tasks")
            }

            val snapshotTask = query.get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

            val tasks = snapshot.documents.mapNotNull { doc ->
                parseDocToTask(doc)
            }

            if (tasks.isNotEmpty()) {
                db.taskDao().insertTasks(tasks)
            }
            Result.success(tasks.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignTask(
        schoolId: String,
        schoolName: String,
        district: String,
        block: String,
        employeeId: String,
        employeeName: String,
        visitDate: String,
        notes: String
    ): Result<Task> = withContext(Dispatchers.IO) {
        try {
            val taskId = "tsk_" + UUID.randomUUID().toString().take(8)
            val visitId = "vst_" + UUID.randomUUID().toString().take(8)

            val task = Task(
                taskId = taskId,
                visitId = visitId,
                schoolId = schoolId,
                employeeId = employeeId,
                employeeName = employeeName,
                schoolName = schoolName,
                district = district,
                block = block,
                assignedBy = "Admin",
                visitDate = visitDate,
                status = VisitStatus.ASSIGNED,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            db.taskDao().insertTask(task)

            // Sync to Firestore
            val fStore = firestore
            if (fStore != null) {
                val setTask = fStore.collection("tasks").document(taskId).set(
                    mapOf(
                        "taskId" to taskId,
                        "visitId" to visitId,
                        "schoolId" to schoolId,
                        "employeeId" to employeeId,
                        "employeeName" to employeeName,
                        "schoolName" to schoolName,
                        "district" to district,
                        "block" to block,
                        "assignedBy" to "Admin",
                        "visitDate" to visitDate,
                        "status" to VisitStatus.ASSIGNED.name,
                        "notes" to notes,
                        "createdAt" to task.createdAt
                    )
                )
                com.google.android.gms.tasks.Tasks.await(setTask)
            }

            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(taskId: String, status: VisitStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
                val updated = task.copy(status = status)
                db.taskDao().updateTask(updated)
                val fStore = firestore
                if (fStore != null) {
                    val updateTask = fStore.collection("tasks").document(taskId).update("status", status.name)
                    com.google.android.gms.tasks.Tasks.await(updateTask)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

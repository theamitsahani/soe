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

    fun getTasksByEmployee(employeeId: String, userEmail: String = ""): Flow<List<Task>> = db.taskDao().getTasksByEmployee(employeeId, userEmail)

    fun startTasksRealtimeListener(
        role: UserRole? = null,
        userId: String? = null,
        userEmail: String? = null,
        userName: String? = null
    ) {
        stopTasksRealtimeListener()
        val fAuth = FirebaseUtils.auth ?: return
        val currentFbUser = fAuth.currentUser ?: return
        val fStore = firestore ?: return

        val currentUid = userId?.ifBlank { currentFbUser.uid } ?: currentFbUser.uid
        val isEmployee = (role == UserRole.EMPLOYEE)

        try {
            val query = if (isEmployee) {
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
        val employeeId = (doc.getString("employeeId") ?: doc.getString("empId") ?: doc.getString("userId") ?: "").trim()
        val employeeEmail = (doc.getString("employeeEmail") ?: doc.getString("email") ?: doc.getString("userEmail") ?: "").trim()
        val employeeName = (doc.getString("employeeName") ?: doc.getString("name") ?: doc.getString("userName") ?: "").trim()
        
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

        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

        // If local database has this task marked as SUBMITTED or REVIEWED, or if matching visit exists, preserve it
        if (status == VisitStatus.ASSIGNED) {
            val localTask = db.taskDao().getTaskById(taskId)
            if (localTask != null && (localTask.status == VisitStatus.SUBMITTED || localTask.status == VisitStatus.REVIEWED)) {
                status = localTask.status
            } else if (visitId.isNotBlank() && db.visitDao().getVisitById(visitId) != null) {
                status = VisitStatus.SUBMITTED
            } else if (schoolId.isNotBlank()) {
                val matchingVisits = db.visitDao().getVisitsListBySchool(schoolId)
                if (matchingVisits.any { (employeeId.isNotBlank() && it.employeeId.trim().equals(employeeId, ignoreCase = true)) && (it.createdAt >= createdAt || it.updatedAt >= createdAt || (visitId.isNotBlank() && it.visitId == visitId)) }) {
                    status = VisitStatus.SUBMITTED
                }
            }
        }

        return Task(
            taskId = taskId,
            visitId = visitId,
            schoolId = schoolId,
            employeeId = employeeId,
            employeeEmail = employeeEmail,
            employeeName = employeeName,
            schoolName = schoolName,
            state = doc.getString("state") ?: "Rajasthan",
            district = doc.getString("district") ?: "",
            block = doc.getString("block") ?: "",
            assignedBy = doc.getString("assignedBy") ?: "Admin",
            visitDate = doc.getString("visitDate") ?: "",
            status = status,
            notes = doc.getString("notes") ?: "",
            createdAt = createdAt
        )
    }

    suspend fun syncTasksFromFirestore(
        role: UserRole? = null,
        userId: String? = null,
        userEmail: String? = null,
        userName: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fAuth = FirebaseUtils.auth
            val currentFbUser = fAuth?.currentUser
            val fStore = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))

            val currentUid = (userId ?: currentFbUser?.uid ?: "").trim()
            val cleanEmail = (userEmail ?: currentFbUser?.email ?: "").trim().lowercase()
            val cleanName = (userName ?: currentFbUser?.displayName ?: "").trim()

            var isEmployee = (role == UserRole.EMPLOYEE)

            if (role == null && currentUid.isNotBlank()) {
                try {
                    val userDocTask = fStore.collection("users").document(currentUid).get()
                    val userDoc = com.google.android.gms.tasks.Tasks.await(userDocTask)
                    val r = userDoc.getString("role")?.trim()?.uppercase()
                    if (r == "EMPLOYEE") isEmployee = true
                } catch (e: Exception) {
                    Log.w("TaskRepository", "Could not check current user role", e)
                }
            }

            if (!isEmployee) {
                // ADMIN ROLE: Sync all tasks
                val query = fStore.collection("tasks")
                val snapshotTask = query.get()
                val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

                val tasks = snapshot.documents.mapNotNull { doc -> parseDocToTask(doc) }
                if (tasks.isNotEmpty()) {
                    db.taskDao().insertTasks(tasks)
                }
                Result.success(tasks.size)
            } else {
                // EMPLOYEE ROLE: Fetch tasks assigned specifically to this employee
                val allEmployeeDocs = mutableMapOf<String, DocumentSnapshot>()

                // 1. Primary Query: employeeId == currentUid
                if (currentUid.isNotBlank()) {
                    try {
                        val uidQuery = fStore.collection("tasks").whereEqualTo("employeeId", currentUid).get()
                        val uidSnap = com.google.android.gms.tasks.Tasks.await(uidQuery)
                        uidSnap.documents.forEach { allEmployeeDocs[it.id] = it }
                    } catch (e: Exception) {
                        Log.w("TaskRepository", "Query tasks by employeeId notice: ${e.message}")
                    }
                }

                // 2. Legacy Fallback Query: employeeEmail == cleanEmail
                if (cleanEmail.isNotBlank()) {
                    try {
                        val emailQuery = fStore.collection("tasks").whereEqualTo("employeeEmail", cleanEmail).get()
                        val emailSnap = com.google.android.gms.tasks.Tasks.await(emailQuery)
                        emailSnap.documents.forEach { doc ->
                            allEmployeeDocs[doc.id] = doc
                            val docEmpId = doc.getString("employeeId") ?: ""
                            if (currentUid.isNotBlank() && docEmpId != currentUid) {
                                try {
                                    doc.reference.update(
                                        mapOf(
                                            "employeeId" to currentUid,
                                            "employeeEmail" to cleanEmail
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.w("TaskRepository", "Legacy task migration notice: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("TaskRepository", "Query tasks by employeeEmail notice: ${e.message}")
                    }
                }

                // 3. Legacy Fallback Query: employeeName == cleanName
                if (cleanName.isNotBlank()) {
                    try {
                        val nameQuery = fStore.collection("tasks").whereEqualTo("employeeName", cleanName).get()
                        val nameSnap = com.google.android.gms.tasks.Tasks.await(nameQuery)
                        nameSnap.documents.forEach { doc ->
                            allEmployeeDocs[doc.id] = doc
                            val docEmpId = doc.getString("employeeId") ?: ""
                            if (currentUid.isNotBlank() && docEmpId != currentUid) {
                                try {
                                    doc.reference.update(
                                        mapOf(
                                            "employeeId" to currentUid,
                                            "employeeEmail" to cleanEmail
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.w("TaskRepository", "Legacy task migration notice: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("TaskRepository", "Query tasks by employeeName notice: ${e.message}")
                    }
                }

                val tasks = allEmployeeDocs.values.mapNotNull { doc ->
                    val task = parseDocToTask(doc)
                    if (task != null && currentUid.isNotBlank() && task.employeeId != currentUid) {
                        task.copy(employeeId = currentUid, employeeEmail = cleanEmail)
                    } else task
                }

                if (tasks.isNotEmpty()) {
                    db.taskDao().insertTasks(tasks)
                }
                Result.success(tasks.size)
            }
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
        notes: String,
        employeeEmail: String = "",
        state: String = "Rajasthan"
    ): Result<Task> = withContext(Dispatchers.IO) {
        try {
            var resolvedEmployeeUid = employeeId.trim()
            val cleanEmail = employeeEmail.trim().lowercase()
            val cleanName = employeeName.trim()

            // Resolve Firebase Auth UID from Firestore user doc if employeeId is non-standard
            val fStore = firestore
            if (fStore != null && (resolvedEmployeeUid.startsWith("emp_") || resolvedEmployeeUid.isBlank())) {
                try {
                    if (cleanEmail.isNotBlank()) {
                        val userQuery = fStore.collection("users").whereEqualTo("email", cleanEmail).limit(1).get()
                        val userSnap = com.google.android.gms.tasks.Tasks.await(userQuery)
                        val userDoc = userSnap.documents.firstOrNull()
                        if (userDoc != null) {
                            val realUid = userDoc.getString("userId")?.trim()?.ifBlank { userDoc.id } ?: userDoc.id
                            if (realUid.isNotBlank() && !realUid.startsWith("emp_")) {
                                resolvedEmployeeUid = realUid
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("TaskRepository", "Could not resolve employee Firebase UID: ${e.message}")
                }
            }

            val taskId = "tsk_" + UUID.randomUUID().toString().replace("-", "").take(10)
            val visitId = "vst_" + UUID.randomUUID().toString().replace("-", "").take(10)

            val task = Task(
                taskId = taskId,
                visitId = visitId,
                schoolId = schoolId,
                employeeId = resolvedEmployeeUid,
                employeeEmail = cleanEmail,
                employeeName = cleanName,
                schoolName = schoolName,
                state = state,
                district = district,
                block = block,
                assignedBy = "Admin",
                visitDate = visitDate,
                status = VisitStatus.ASSIGNED,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            db.taskDao().insertTask(task)

            // Sync to Firestore task document
            if (fStore != null) {
                val setTask = fStore.collection("tasks").document(taskId).set(
                    mapOf(
                        "taskId" to taskId,
                        "visitId" to visitId,
                        "schoolId" to schoolId,
                        "schoolName" to schoolName,
                        "state" to state,
                        "district" to district,
                        "block" to block,
                        "employeeId" to resolvedEmployeeUid,
                        "employeeEmail" to cleanEmail,
                        "employeeName" to cleanName,
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

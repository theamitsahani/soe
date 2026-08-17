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
                                db.taskDao().deleteTasksNotIn(tasks.map { it.taskId })
                            } else {
                                db.taskDao().deleteAllTasks()
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
        var principalName = doc.getString("principalName") ?: ""
        var principalMobile = doc.getString("principalMobile") ?: doc.getString("mobile") ?: ""
        var villageName = doc.getString("villageName") ?: doc.getString("village") ?: ""
        var schoolType = doc.getString("schoolType") ?: doc.getString("type") ?: ""
        var state = doc.getString("state") ?: doc.getString("stateName") ?: "Rajasthan"
        var district = doc.getString("district") ?: doc.getString("districtName") ?: ""
        var block = doc.getString("block") ?: doc.getString("blockName") ?: ""

        // Backward compatibility: Look up missing principal/school details from local Room School
        if (schoolId.isNotBlank()) {
            val localSchool = db.schoolDao().getSchoolById(schoolId)
            if (localSchool != null) {
                if (schoolName.isBlank()) schoolName = localSchool.schoolName
                if (principalName.isBlank()) principalName = localSchool.principalName
                if (principalMobile.isBlank()) principalMobile = localSchool.principalMobile
                if (villageName.isBlank()) villageName = localSchool.villageName
                if (schoolType.isBlank()) schoolType = localSchool.schoolType
                if (district.isBlank()) district = localSchool.districtName
                if (block.isBlank()) block = localSchool.blockName
                if (state.isBlank()) state = localSchool.stateName
            }
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
            principalName = principalName,
            principalMobile = principalMobile,
            villageName = villageName,
            schoolType = schoolType,
            state = state,
            district = district,
            block = block,
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
                    db.taskDao().deleteTasksNotIn(tasks.map { it.taskId })
                } else {
                    db.taskDao().deleteAllTasks()
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
                    db.taskDao().deleteTasksNotIn(tasks.map { it.taskId })
                } else {
                    db.taskDao().deleteTasksForEmployee(currentUid, cleanEmail)
                }
                Result.success(tasks.size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveFirebaseUidForEmployee(
        rawEmployeeId: String,
        email: String,
        name: String
    ): String = withContext(Dispatchers.IO) {
        val cleanRaw = rawEmployeeId.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        val fStore = firestore

        // 1. If rawEmployeeId is already an actual Firebase Auth UID (not starting with "emp_")
        if (cleanRaw.isNotBlank() && !cleanRaw.startsWith("emp_") && cleanRaw.length >= 15) {
            return@withContext cleanRaw
        }

        if (fStore != null) {
            // 2. Search "users" collection by email
            if (cleanEmail.isNotBlank()) {
                try {
                    val emailQuery = fStore.collection("users").whereEqualTo("email", cleanEmail).limit(1).get()
                    val emailSnap = com.google.android.gms.tasks.Tasks.await(emailQuery)
                    val userDoc = emailSnap.documents.firstOrNull()
                    if (userDoc != null) {
                        val docId = userDoc.id.trim()
                        val uidInDoc = userDoc.getString("userId")?.trim() ?: ""
                        val resolved = if (docId.isNotBlank() && !docId.startsWith("emp_")) docId
                                       else if (uidInDoc.isNotBlank() && !uidInDoc.startsWith("emp_")) uidInDoc
                                       else ""
                        if (resolved.isNotBlank()) return@withContext resolved
                    }
                } catch (e: Exception) {
                    Log.w("TaskRepository", "Email resolution notice: ${e.message}")
                }
            }

            // 3. Search "users" collection by name
            if (cleanName.isNotBlank()) {
                try {
                    val nameQuery = fStore.collection("users").whereEqualTo("name", cleanName).limit(1).get()
                    val nameSnap = com.google.android.gms.tasks.Tasks.await(nameQuery)
                    val userDoc = nameSnap.documents.firstOrNull()
                    if (userDoc != null) {
                        val docId = userDoc.id.trim()
                        val uidInDoc = userDoc.getString("userId")?.trim() ?: ""
                        val resolved = if (docId.isNotBlank() && !docId.startsWith("emp_")) docId
                                       else if (uidInDoc.isNotBlank() && !uidInDoc.startsWith("emp_")) uidInDoc
                                       else ""
                        if (resolved.isNotBlank()) return@withContext resolved
                    }
                } catch (e: Exception) {
                    Log.w("TaskRepository", "Name resolution notice: ${e.message}")
                }
            }
        }

        // 4. Local Room user search
        try {
            val localUsers = db.userDao().getAllUsersList()
            val match = localUsers.find { 
                (cleanEmail.isNotBlank() && it.email.trim().equals(cleanEmail, ignoreCase = true)) ||
                (cleanName.isNotBlank() && it.name.trim().equals(cleanName, ignoreCase = true)) ||
                (cleanRaw.isNotBlank() && it.userId.trim() == cleanRaw)
            }
            if (match != null) {
                val uid = match.userId.trim()
                if (uid.isNotBlank() && !uid.startsWith("emp_")) {
                    return@withContext uid
                }
            }
        } catch (e: Exception) {
            Log.w("TaskRepository", "Local user resolution notice: ${e.message}")
        }

        return@withContext cleanRaw
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
            val cleanEmail = employeeEmail.trim().lowercase()
            val cleanName = employeeName.trim()

            val resolvedEmployeeUid = resolveFirebaseUidForEmployee(
                rawEmployeeId = employeeId,
                email = cleanEmail,
                name = cleanName
            )

            // Fetch school from Room database as source of truth
            val localSchool = try {
                db.schoolDao().getSchoolById(schoolId)
            } catch (e: Exception) {
                null
            }

            val finalSchoolName = localSchool?.schoolName?.ifBlank { schoolName } ?: schoolName
            val finalPrincipalName = localSchool?.principalName ?: ""
            val finalPrincipalMobile = localSchool?.principalMobile ?: ""
            val finalVillageName = localSchool?.villageName ?: ""
            val finalSchoolType = localSchool?.schoolType ?: ""
            val finalState = localSchool?.stateName?.ifBlank { state } ?: state
            val finalDistrict = localSchool?.districtName?.ifBlank { district } ?: district
            val finalBlock = localSchool?.blockName?.ifBlank { block } ?: block

            val taskId = "tsk_" + UUID.randomUUID().toString().replace("-", "").take(10)
            val visitId = "vst_" + UUID.randomUUID().toString().replace("-", "").take(10)
            val createdAt = System.currentTimeMillis()

            val task = Task(
                taskId = taskId,
                visitId = visitId,
                schoolId = schoolId,
                employeeId = resolvedEmployeeUid,
                employeeEmail = cleanEmail,
                employeeName = cleanName,
                schoolName = finalSchoolName,
                principalName = finalPrincipalName,
                principalMobile = finalPrincipalMobile,
                villageName = finalVillageName,
                schoolType = finalSchoolType,
                state = finalState,
                district = finalDistrict,
                block = finalBlock,
                assignedBy = "Admin",
                visitDate = visitDate,
                status = VisitStatus.ASSIGNED,
                notes = notes,
                createdAt = createdAt
            )

            db.taskDao().insertTask(task)

            // Sync to Firestore task document
            val fStore = firestore
            if (fStore != null) {
                val setTask = fStore.collection("tasks").document(taskId).set(
                    mapOf(
                        "taskId" to taskId,
                        "visitId" to visitId,
                        "schoolId" to schoolId,
                        "schoolName" to finalSchoolName,
                        "principalName" to finalPrincipalName,
                        "principalMobile" to finalPrincipalMobile,
                        "villageName" to finalVillageName,
                        "schoolType" to finalSchoolType,
                        "state" to finalState,
                        "district" to finalDistrict,
                        "block" to finalBlock,
                        "employeeId" to resolvedEmployeeUid,
                        "employeeEmail" to cleanEmail,
                        "employeeName" to cleanName,
                        "assignedBy" to "Admin",
                        "visitDate" to visitDate,
                        "status" to VisitStatus.ASSIGNED.name,
                        "notes" to notes,
                        "createdAt" to createdAt
                    )
                )
                com.google.android.gms.tasks.Tasks.await(setTask)
            }

            // Trigger notification for assigned employee
            try {
                NotificationRepository(
                    db.appNotificationDao(),
                    fStore ?: com.google.firebase.firestore.FirebaseFirestore.getInstance()
                ).createAndSendNotification(
                    context = context,
                    recipientUserId = resolvedEmployeeUid,
                    title = "New Task Assigned! (नया कार्य असाइन हुआ)",
                    message = "You have been assigned a new visit for $finalSchoolName on $visitDate.",
                    type = "TASK_ASSIGNED",
                    relatedId = taskId,
                    schoolName = finalSchoolName,
                    employeeName = cleanName
                )
            } catch (notifErr: Exception) {
                Log.w("TaskRepository", "Failed to send notification: ${notifErr.message}")
            }

            Result.success(task)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error assigning task", e)
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

    suspend fun deleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = db.taskDao().getTaskById(taskId)
            db.taskDao().deleteTask(taskId)

            if (task != null) {
                if (task.visitId.isNotBlank()) {
                    db.visitDao().deleteVisitById(task.visitId)
                }
                if (task.schoolId.isNotBlank()) {
                    db.visitDao().deleteVisitsBySchool(task.schoolId)
                }
            }

            val fStore = firestore
            if (fStore != null) {
                val deleteTaskTask = fStore.collection("tasks").document(taskId).delete()
                com.google.android.gms.tasks.Tasks.await(deleteTaskTask)

                if (task != null) {
                    try {
                        if (task.visitId.isNotBlank()) {
                            val vstDocs1 = com.google.android.gms.tasks.Tasks.await(
                                fStore.collection("visits").whereEqualTo("visitId", task.visitId).get()
                            )
                            for (doc in vstDocs1.documents) {
                                fStore.collection("visits").document(doc.id).delete()
                            }
                        }
                        if (task.schoolId.isNotBlank()) {
                            val vstDocs2 = com.google.android.gms.tasks.Tasks.await(
                                fStore.collection("visits").whereEqualTo("schoolId", task.schoolId).get()
                            )
                            for (doc in vstDocs2.documents) {
                                fStore.collection("visits").document(doc.id).delete()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("TaskRepository", "Notice deleting associated visits in Firestore: ${e.message}")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error deleting task $taskId", e)
            Result.failure(e)
        }
    }
}

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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TaskRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    private var tasksListenerRegistration: ListenerRegistration? = null

    fun getAllTasks(): Flow<List<Task>> = db.taskDao().getAllTasks()

    fun getTasksByEmployee(employeeId: String, userEmail: String = ""): Flow<List<Task>> =
        db.taskDao().getTasksByEmployee(employeeId)

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
                            if (isEmployee) {
                                val taskIds = tasks.map { it.taskId }
                                if (tasks.isNotEmpty()) {
                                    db.taskDao().insertTasks(tasks)
                                    db.taskDao().deleteTasksForEmployeeNotIn(currentUid, taskIds)
                                } else {
                                    db.taskDao().deleteTasksForEmployee(currentUid)
                                }
                            } else {
                                val taskIds = tasks.map { it.taskId }
                                if (tasks.isNotEmpty()) {
                                    db.taskDao().insertTasks(tasks)
                                    db.taskDao().deleteTasksNotIn(taskIds)
                                } else {
                                    db.taskDao().deleteAllTasks()
                                }
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
        val status = try { VisitStatus.valueOf(statusStr) } catch (_: Exception) { VisitStatus.ASSIGNED }
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

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

            Log.d("TASK_SYNC_START", "Starting task sync for employeeId=$currentUid, role=${if (isEmployee) "EMPLOYEE" else "ADMIN"}")

            if (!isEmployee) {
                // ADMIN ROLE: Sync all tasks
                val snapshotTask = fStore.collection("tasks").get()
                val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

                Log.d("TASK_SYNC_FIRESTORE_COUNT", "Fetched ${snapshot.documents.size} tasks from Firestore for ADMIN")

                val tasks = snapshot.documents.mapNotNull { doc -> parseDocToTask(doc) }
                val localTasksBefore = db.taskDao().getAllTasks().firstOrNull()?.size ?: 0
                Log.d("TASK_SYNC_LOCAL_BEFORE", "Local task count before sync: $localTasksBefore")

                val firestoreTaskIds = tasks.map { it.taskId }
                if (tasks.isNotEmpty()) {
                    db.taskDao().insertTasks(tasks)
                    db.taskDao().deleteTasksNotIn(firestoreTaskIds)
                } else {
                    db.taskDao().deleteAllTasks()
                }

                val localTasksAfter = db.taskDao().getAllTasks().firstOrNull()?.size ?: 0
                Log.d("TASK_SYNC_LOCAL_AFTER", "Local task count after sync: $localTasksAfter")

                Result.success(tasks.size)
            } else {
                // EMPLOYEE ROLE: Fetch tasks assigned specifically to currentUid ONLY
                if (currentUid.isBlank()) return@withContext Result.success(0)

                val uidQuery = fStore.collection("tasks").whereEqualTo("employeeId", currentUid).get()
                val uidSnap = com.google.android.gms.tasks.Tasks.await(uidQuery)

                Log.d("TASK_SYNC_FIRESTORE_COUNT", "Fetched ${uidSnap.documents.size} tasks from Firestore for employeeId=$currentUid")

                val tasks = uidSnap.documents.mapNotNull { doc -> parseDocToTask(doc) }
                val localTasksBefore = db.taskDao().getTasksByEmployee(currentUid).firstOrNull()?.size ?: 0
                Log.d("TASK_SYNC_LOCAL_BEFORE", "Local task count before sync for $currentUid: $localTasksBefore")

                val firestoreTaskIds = tasks.map { it.taskId }
                if (tasks.isNotEmpty()) {
                    db.taskDao().insertTasks(tasks)
                    db.taskDao().deleteTasksForEmployeeNotIn(currentUid, firestoreTaskIds)
                } else {
                    db.taskDao().deleteTasksForEmployee(currentUid)
                }

                val localTasksAfter = db.taskDao().getTasksByEmployee(currentUid).firstOrNull()?.size ?: 0
                Log.d("TASK_SYNC_LOCAL_AFTER", "Local task count after sync for $currentUid: $localTasksAfter")

                Result.success(tasks.size)
            }
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error syncing tasks from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun resolveFirebaseUidForEmployee(
        rawEmployeeId: String,
        email: String,
        name: String
    ): String = withContext(Dispatchers.IO) {
        val cleanRaw = rawEmployeeId.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        val fStore = firestore

        if (cleanRaw.isNotBlank() && !cleanRaw.startsWith("emp_") && cleanRaw.length >= 15) {
            return@withContext cleanRaw
        }

        if (fStore != null) {
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

            val fStore = firestore

            // Check for existing active task using employeeId + schoolId + visitDate
            var existingTask: Task? = db.taskDao().getActiveTask(resolvedEmployeeUid, schoolId, visitDate)

            if (existingTask == null && fStore != null) {
                try {
                    val checkQuery = fStore.collection("tasks")
                        .whereEqualTo("employeeId", resolvedEmployeeUid)
                        .whereEqualTo("schoolId", schoolId)
                        .whereEqualTo("visitDate", visitDate)
                        .get()
                    val checkSnap = com.google.android.gms.tasks.Tasks.await(checkQuery)
                    val existingDoc = checkSnap.documents.firstOrNull { doc ->
                        val st = doc.getString("status") ?: ""
                        st != "SUBMITTED" && st != "REVIEWED"
                    }
                    if (existingDoc != null) {
                        existingTask = parseDocToTask(existingDoc)
                    }
                } catch (e: Exception) {
                    Log.w("TaskRepository", "Check existing task notice: ${e.message}")
                }
            }

            if (existingTask != null) {
                Log.w(
                    "TASK_ASSIGN_DUPLICATE_BLOCKED",
                    "Duplicate task blocked for employee $resolvedEmployeeUid, school $schoolId, date $visitDate, existingTaskId ${existingTask.taskId}"
                )
                db.taskDao().insertTask(existingTask)
                return@withContext Result.success(existingTask)
            }

            val localSchool = try {
                db.schoolDao().getSchoolById(schoolId)
            } catch (e: Exception) {
                null
            }

            if (localSchool != null && localSchool.visitDate.isNotBlank()) {
                return@withContext Result.failure(
                    Exception("यह स्कूल (${localSchool.schoolName}) पहले ही पूर्ण (Completed on ${localSchool.visitDate}) हो चुका है। इसे दोबारा असाइन नहीं किया जा सकता।")
                )
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

            val fStore = firestore
            if (fStore != null) {
                val deleteTaskDoc = fStore.collection("tasks").document(taskId).delete()
                com.google.android.gms.tasks.Tasks.await(deleteTaskDoc)
            }

            if (task != null) {
                if (task.visitId.isNotBlank()) {
                    val v = db.visitDao().getVisitById(task.visitId)
                    if (v != null) {
                        if (v.status == VisitStatus.ASSIGNED || v.status == VisitStatus.STARTED || v.status == VisitStatus.IN_PROGRESS) {
                            db.visitDao().deleteVisitById(task.visitId)
                            if (fStore != null) {
                                try {
                                    val deleteVisitDoc = fStore.collection("visits").document(task.visitId).delete()
                                    com.google.android.gms.tasks.Tasks.await(deleteVisitDoc)
                                } catch (e: Exception) {
                                    Log.w("TaskRepository", "Notice deleting draft visit in Firestore: ${e.message}")
                                }
                            }
                        }
                    }
                }
                Log.d("TASK_DELETED", "Deleted task $taskId for school ${task.schoolId}, employee ${task.employeeId}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error deleting task $taskId", e)
            Result.failure(e)
        }
    }

    suspend fun cleanupDuplicateTasks(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fStore = firestore ?: return@withContext Result.success(0)
            val snapshotTask = fStore.collection("tasks").get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

            val allTasks = snapshot.documents.mapNotNull { parseDocToTask(it) }
            val grouped = allTasks.groupBy { "${it.employeeId}_${it.schoolId}_${it.visitDate}" }

            var removedCount = 0
            for ((_, list) in grouped) {
                if (list.size > 1) {
                    val sorted = list.sortedByDescending { it.createdAt }
                    val winner = sorted.first()
                    val duplicates = sorted.drop(1)

                    for (dup in duplicates) {
                        if (dup.status != VisitStatus.SUBMITTED && dup.status != VisitStatus.REVIEWED) {
                            try {
                                fStore.collection("tasks").document(dup.taskId).delete()
                                db.taskDao().deleteTask(dup.taskId)
                                removedCount++
                                Log.d("TaskRepository", "Cleaned up duplicate task ${dup.taskId}")
                            } catch (e: Exception) {
                                Log.w("TaskRepository", "Error cleaning up duplicate task ${dup.taskId}: ${e.message}")
                            }
                        }
                    }
                }
            }
            Result.success(removedCount)
        } catch (e: Exception) {
            Log.w("TaskRepository", "Error running duplicate task cleanup: ${e.message}")
            Result.failure(e)
        }
    }
}

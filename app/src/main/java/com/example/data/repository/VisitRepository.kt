package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.School
import com.example.data.model.SyncStatus
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.Visit
import com.example.data.model.VisitEvent
import com.example.data.model.VisitStatus
import com.example.util.FirebaseUtils
import com.example.util.SyncManager
import com.example.util.ValidationResult
import com.example.util.VisitValidator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class VisitRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    private val syncManager = SyncManager.getInstance(context)

    fun getAllVisits(): Flow<List<Visit>> = db.visitDao().getAllVisits()

    fun getVisitsBySchool(schoolId: String): Flow<List<Visit>> = db.visitDao().getVisitsBySchool(schoolId)

    suspend fun getVisitsListBySchool(schoolId: String): List<Visit> = withContext(Dispatchers.IO) {
        db.visitDao().getVisitsListBySchool(schoolId)
    }

    fun getVisitsByEmployee(employeeId: String): Flow<List<Visit>> = db.visitDao().getVisitsByEmployee(employeeId)

    suspend fun getVisitById(visitId: String): Visit? = withContext(Dispatchers.IO) {
        db.visitDao().getVisitById(visitId)
    }

    fun getVisitEvents(visitId: String): Flow<List<VisitEvent>> = db.visitEventDao().getEventsForVisit(visitId)

    suspend fun getVisitEventsList(visitId: String): List<VisitEvent> = withContext(Dispatchers.IO) {
        db.visitEventDao().getEventsListForVisit(visitId)
    }

    /**
     * Records a permanent audit event locally and syncs to Firestore.
     */
    suspend fun recordEvent(
        visitId: String,
        taskId: String = "",
        eventType: String,
        actorId: String = "",
        actorName: String = "",
        actorRole: String = "",
        statusFrom: String = "",
        statusTo: String = "",
        details: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val eventId = "evt_${now}_${UUID.randomUUID().toString().take(6)}"
            val event = VisitEvent(
                eventId = eventId,
                visitId = visitId,
                taskId = taskId,
                eventType = eventType,
                actorId = actorId,
                actorName = actorName,
                actorRole = actorRole,
                statusFrom = statusFrom,
                statusTo = statusTo,
                details = details,
                timestamp = now,
                syncStatus = if (syncManager.isNetworkAvailable()) SyncStatus.SYNCED else SyncStatus.PENDING
            )
            db.visitEventDao().insertEvent(event)

            val fStore = firestore
            if (fStore != null && syncManager.isNetworkAvailable() && visitId.isNotBlank()) {
                try {
                    val eventMap = hashMapOf(
                        "eventId" to event.eventId,
                        "visitId" to event.visitId,
                        "taskId" to event.taskId,
                        "eventType" to event.eventType,
                        "actorId" to event.actorId,
                        "actorName" to event.actorName,
                        "actorRole" to event.actorRole,
                        "statusFrom" to event.statusFrom,
                        "statusTo" to event.statusTo,
                        "details" to event.details,
                        "timestamp" to event.timestamp
                    )
                    fStore.collection("visits")
                        .document(visitId)
                        .collection("events")
                        .document(eventId)
                        .set(eventMap)
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice saving audit event to Firestore: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VisitRepository", "Error recording audit event: ${e.message}")
        }
    }

    /**
     * Starts a visit session idempotently for a task/school and captures the initial historical snapshot.
     */
    suspend fun startVisit(
        task: Task?,
        school: School?,
        employee: User
    ): Result<Visit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = task?.schoolId ?: school?.schoolId ?: return@withContext Result.failure(Exception("School ID is required"))
            val validation = VisitValidator.validateStartVisit(schoolId, employee.userId)
            if (validation is ValidationResult.Error) {
                return@withContext Result.failure(Exception(validation.message))
            }

            // Check if an existing visit session already exists for this task/employee/school
            val existing = if (!task?.visitId.isNullOrBlank()) {
                db.visitDao().getVisitById(task!!.visitId)
            } else if (task != null) {
                db.visitDao().getAllVisitsList().find { it.taskId == task.taskId }
            } else {
                db.visitDao().getAllVisitsList().find {
                    it.schoolId == schoolId && it.employeeId == employee.userId &&
                            (it.status == VisitStatus.STARTED || it.status == VisitStatus.IN_PROGRESS || it.status == VisitStatus.ASSIGNED)
                }
            }

            if (existing != null) {
                return@withContext Result.success(existing)
            }

            val now = System.currentTimeMillis()
            val visitId = if (!task?.visitId.isNullOrBlank()) task!!.visitId
            else if (task != null) "vst_${task.taskId}_${employee.userId}"
            else "vst_${schoolId}_${now}"

            val initialVisit = Visit(
                visitId = visitId,
                taskId = task?.taskId ?: "",
                schoolId = schoolId,
                employeeId = employee.userId,
                employeeName = employee.name,
                schoolName = school?.schoolName ?: task?.schoolName ?: "",
                state = school?.stateName ?: task?.state ?: "Rajasthan",
                district = school?.districtName ?: task?.district ?: "",
                block = school?.blockName ?: task?.block ?: "",
                villageName = school?.villageName ?: task?.villageName ?: "",
                schoolType = school?.schoolType ?: task?.schoolType ?: "Government School",
                udiseCode = school?.referenceCode ?: "",
                principalName = school?.principalName ?: task?.principalName ?: "",
                principalMobile = school?.principalMobile ?: task?.principalMobile ?: school?.mobile ?: "",
                visitDate = task?.visitDate ?: "",
                status = VisitStatus.STARTED,
                startedAt = now,
                syncStatus = SyncStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )

            db.visitDao().insertVisit(initialVisit)

            if (task != null) {
                db.taskDao().updateTask(task.copy(status = VisitStatus.STARTED, visitId = visitId))
            }

            recordEvent(
                visitId = visitId,
                taskId = initialVisit.taskId,
                eventType = "STARTED",
                actorId = employee.userId,
                actorName = employee.name,
                actorRole = "EMPLOYEE",
                statusFrom = "ASSIGNED",
                statusTo = "STARTED",
                details = "Inspection started at ${initialVisit.schoolName}"
            )

            Result.success(initialVisit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves draft form answers and photos in the background without submitting.
     */
    suspend fun saveDraftVisit(
        visit: Visit,
        answersJson: String,
        photosJson: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val updated = visit.copy(
                status = VisitStatus.IN_PROGRESS,
                answersJson = answersJson,
                photosJson = photosJson,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
            db.visitDao().updateVisit(updated)

            recordEvent(
                visitId = visit.visitId,
                taskId = visit.taskId,
                eventType = "AUTOSAVE",
                actorId = visit.employeeId,
                actorName = visit.employeeName,
                actorRole = "EMPLOYEE",
                statusFrom = visit.status.name,
                statusTo = VisitStatus.IN_PROGRESS.name,
                details = "Form draft auto-saved locally"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncVisitsFromFirestore(role: UserRole? = null, userId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fAuth = FirebaseUtils.auth
            val currentFbUser = fAuth?.currentUser
            val currentUid = userId ?: currentFbUser?.uid ?: ""

            val fStore = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))

            var isEmployee = (role == UserRole.EMPLOYEE)

            if (role == null && currentUid.isNotBlank()) {
                try {
                    val userDocTask = fStore.collection("users").document(currentUid).get()
                    val userDoc = com.google.android.gms.tasks.Tasks.await(userDocTask)
                    val r = userDoc.getString("role")?.trim()?.uppercase()
                    if (r == "EMPLOYEE") isEmployee = true
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Could not check current user role", e)
                }
            }

            val query = if (isEmployee && currentUid.isNotBlank()) {
                fStore.collection("visits").whereEqualTo("employeeId", currentUid)
            } else {
                fStore.collection("visits")
            }

            val snapshotTask = query.get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

            val rawVisits = snapshot.documents.mapNotNull { doc ->
                val visitId = doc.getString("visitId") ?: doc.id
                val taskId = doc.getString("taskId") ?: ""
                val schoolId = doc.getString("schoolId") ?: ""
                val employeeId = doc.getString("employeeId") ?: ""
                val schoolName = doc.getString("schoolName") ?: ""
                if (schoolName.isBlank()) return@mapNotNull null

                val statusStr = doc.getString("status") ?: VisitStatus.SUBMITTED.name
                val status = try { VisitStatus.valueOf(statusStr) } catch (e: Exception) { VisitStatus.SUBMITTED }

                Visit(
                    visitId = visitId,
                    taskId = taskId,
                    schoolId = schoolId,
                    employeeId = employeeId,
                    employeeName = doc.getString("employeeName") ?: "",
                    schoolName = schoolName,
                    state = doc.getString("state") ?: "Rajasthan",
                    district = doc.getString("district") ?: "",
                    block = doc.getString("block") ?: "",
                    villageName = doc.getString("villageName") ?: "",
                    schoolType = doc.getString("schoolType") ?: "",
                    udiseCode = doc.getString("udiseCode") ?: "",
                    principalName = doc.getString("principalName") ?: "",
                    principalMobile = doc.getString("principalMobile") ?: "",
                    visitDate = doc.getString("visitDate") ?: "",
                    status = status,
                    answersJson = doc.getString("answersJson") ?: "{}",
                    photosJson = doc.getString("photosJson") ?: "{}",
                    startedAt = doc.getLong("startedAt"),
                    completedAt = doc.getLong("completedAt"),
                    submittedAt = doc.getLong("submittedAt"),
                    reviewedAt = doc.getLong("reviewedAt"),
                    reviewedBy = doc.getString("reviewedBy") ?: "",
                    reviewNotes = doc.getString("reviewNotes") ?: "",
                    rejectionReason = doc.getString("rejectionReason") ?: "",
                    latitude = doc.getDouble("latitude"),
                    longitude = doc.getDouble("longitude"),
                    appVersion = doc.getString("appVersion") ?: "1.0.0",
                    editCount = (doc.getLong("editCount") ?: 0L).toInt(),
                    syncStatus = SyncStatus.SYNCED,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }

            val cleanVisits = mutableListOf<Visit>()
            val groupedBySchoolEmp = rawVisits.groupBy {
                if (it.taskId.isNotBlank()) "task_${it.taskId}"
                else "${it.schoolId}_${it.employeeId}_${it.visitDate}"
            }

            for ((_, group) in groupedBySchoolEmp) {
                if (group.size == 1) {
                    cleanVisits.add(group.first())
                } else {
                    val winner = group.maxByOrNull { it.updatedAt } ?: group.first()
                    cleanVisits.add(winner)

                    for (duplicate in group) {
                        if (duplicate.visitId != winner.visitId) {
                            try {
                                fStore.collection("visits").document(duplicate.visitId).delete()
                                db.visitDao().deleteVisitById(duplicate.visitId)
                            } catch (e: Exception) {
                                android.util.Log.w("VisitRepository", "Could not delete duplicate visit ${duplicate.visitId}: ${e.message}")
                            }
                        }
                    }
                }
            }

            // Auto-reconcile completed schools: if a school has a visitDate set, ensure it has a completed Visit record
            val allSchools = db.schoolDao().getAllSchoolsList()
            val deletedSchoolIds = allSchools.filter { it.isDeleted }.map { it.schoolId }.toSet()
            val completedSchools = allSchools.filter { it.visitDate.isNotBlank() && !it.isDeleted }

            // Purge local visits for any deleted schools
            for (delSchId in deletedSchoolIds) {
                db.visitDao().deleteVisitsBySchool(delSchId)
            }

            val nonDeletedCleanVisits = cleanVisits.filter { !deletedSchoolIds.contains(it.schoolId) }
            val existingSchoolIdsWithVisits = (nonDeletedCleanVisits.map { it.schoolId } + db.visitDao().getAllVisitsList().map { it.schoolId }).toSet()
            val missingCompletedVisits = mutableListOf<Visit>()

            for (sch in completedSchools) {
                if (!existingSchoolIdsWithVisits.contains(sch.schoolId)) {
                    val actualVisitDate = sch.visitDate
                    val answers = com.example.data.model.VisitAnswers(
                        q1_soeName = "Admin (Prior Completion)",
                        q2_visitDate = actualVisitDate,
                        q3_schoolName = sch.schoolName,
                        q4_udiseCode = "",
                        q5_district = sch.districtName,
                        q6_block = sch.blockName,
                        q7_principalName = sch.principalName,
                        q8_principalMobile = sch.principalMobile,
                        q9_metPrincipal = "हाँ",
                        q10_missionGyanAwareness = "हाँ",
                        q11_studentCount = "Verified",
                        q12_schoolResponse = "Completed (Previous Visit)",
                        q20_finalRemarks = "Completed prior to app launch / Verified by Admin (Date: $actualVisitDate)"
                    )
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val answersAdapter = moshi.adapter(com.example.data.model.VisitAnswers::class.java)
                    val now = System.currentTimeMillis()
                    val legacyVisit = Visit(
                        visitId = "vst_" + sch.schoolId.removePrefix("sch_") + "_legacy",
                        schoolId = sch.schoolId,
                        employeeId = "emp_admin",
                        employeeName = "Admin (Prior Completion)",
                        schoolName = sch.schoolName,
                        district = sch.districtName,
                        block = sch.blockName,
                        villageName = sch.villageName,
                        schoolType = sch.schoolType,
                        principalName = sch.principalName,
                        principalMobile = sch.principalMobile,
                        state = sch.stateName,
                        visitDate = actualVisitDate,
                        status = VisitStatus.SUBMITTED,
                        answersJson = answersAdapter.toJson(answers),
                        photosJson = "{}",
                        startedAt = now - 30 * 60 * 1000L,
                        completedAt = now,
                        submittedAt = now,
                        syncStatus = SyncStatus.SYNCED,
                        createdAt = sch.createdAt,
                        updatedAt = now
                    )
                    missingCompletedVisits.add(legacyVisit)

                    // Also push to Firestore visits collection so server stays up to date
                    try {
                        val visitDocRef = fStore.collection("visits").document(legacyVisit.visitId)
                        val visitData = mapOf(
                            "visitId" to legacyVisit.visitId,
                            "schoolId" to legacyVisit.schoolId,
                            "employeeId" to legacyVisit.employeeId,
                            "employeeName" to legacyVisit.employeeName,
                            "schoolName" to legacyVisit.schoolName,
                            "state" to legacyVisit.state,
                            "district" to legacyVisit.district,
                            "block" to legacyVisit.block,
                            "villageName" to legacyVisit.villageName,
                            "schoolType" to legacyVisit.schoolType,
                            "principalName" to legacyVisit.principalName,
                            "principalMobile" to legacyVisit.principalMobile,
                            "visitDate" to legacyVisit.visitDate,
                            "status" to legacyVisit.status.name,
                            "answersJson" to legacyVisit.answersJson,
                            "photosJson" to legacyVisit.photosJson,
                            "startedAt" to legacyVisit.startedAt,
                            "completedAt" to legacyVisit.completedAt,
                            "submittedAt" to legacyVisit.submittedAt,
                            "syncStatus" to SyncStatus.SYNCED.name,
                            "createdAt" to legacyVisit.createdAt,
                            "updatedAt" to legacyVisit.updatedAt
                        )
                        visitDocRef.set(visitData, com.google.firebase.firestore.SetOptions.merge())
                    } catch (e: Exception) {
                        android.util.Log.w("VisitRepository", "Could not sync legacy visit to Firestore: ${e.message}")
                    }
                }
            }

            val finalVisitsToKeep = (nonDeletedCleanVisits + missingCompletedVisits).distinctBy { it.visitId }

            if (finalVisitsToKeep.isNotEmpty()) {
                db.visitDao().insertVisits(finalVisitsToKeep)

                val stillUnsyncedLocalIds = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING).map { it.visitId } +
                        db.visitDao().getVisitsBySyncStatus(SyncStatus.FAILED).map { it.visitId }
                val idsToKeep = (finalVisitsToKeep.map { it.visitId } + stillUnsyncedLocalIds).distinct()
                db.visitDao().deleteVisitsNotIn(idsToKeep)

                for (v in finalVisitsToKeep) {
                    if (v.status == VisitStatus.SUBMITTED || v.status == VisitStatus.REVIEWED) {
                        if (v.taskId.isNotBlank()) {
                            db.taskDao().markTaskSubmittedById(v.taskId, v.visitId)
                        } else {
                            db.taskDao().markTaskSubmittedByVisitId(v.visitId)
                        }
                    }
                }
            } else {
                if (!isEmployee) {
                    db.visitDao().deleteAllSyncedVisits()
                } else if (currentUid.isNotBlank()) {
                    db.visitDao().deleteSyncedVisitsForEmployee(currentUid)
                }
            }
            Result.success(finalVisitsToKeep.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits a visit report safely with offline persistence and slow network protection.
     */
    suspend fun submitVisit(visit: Visit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isOnline = syncManager.isNetworkAvailable()
            val initialSyncStatus = SyncStatus.PENDING
            val now = System.currentTimeMillis()

            var resolvedTaskId = visit.taskId
            if (resolvedTaskId.isBlank()) {
                val activeTask = db.taskDao().getActiveTask(visit.employeeId, visit.schoolId, visit.visitDate)
                if (activeTask != null) {
                    resolvedTaskId = activeTask.taskId
                }
            }

            val finalVisit = visit.copy(
                taskId = resolvedTaskId,
                status = VisitStatus.SUBMITTED,
                completedAt = visit.completedAt ?: now,
                submittedAt = visit.submittedAt ?: now,
                syncStatus = initialSyncStatus,
                updatedAt = now
            )

            // Step 1: Save to Room DB locally first (ensures 100% offline durability)
            db.visitDao().insertVisit(finalVisit)

            if (finalVisit.schoolId.isNotBlank()) {
                db.schoolDao().updateSchoolVisitDate(finalVisit.schoolId, finalVisit.visitDate)
            }

            if (finalVisit.taskId.isNotBlank()) {
                db.taskDao().markTaskSubmittedById(finalVisit.taskId, finalVisit.visitId)
            } else {
                db.taskDao().markTaskSubmittedByVisitId(finalVisit.visitId)
            }

            // Step 2: Record audit event for SUBMITTED
            recordEvent(
                visitId = finalVisit.visitId,
                taskId = finalVisit.taskId,
                eventType = "SUBMITTED",
                actorId = finalVisit.employeeId,
                actorName = finalVisit.employeeName,
                actorRole = "EMPLOYEE",
                statusFrom = visit.status.name,
                statusTo = VisitStatus.SUBMITTED.name,
                details = "Inspection report submitted by ${finalVisit.employeeName}"
            )

            val fStore = firestore
            if (fStore != null && isOnline) {
                try {
                    if (finalVisit.schoolId.isNotBlank()) {
                        fStore.collection("schools").document(finalVisit.schoolId).set(
                            mapOf(
                                "visitDate" to finalVisit.visitDate,
                                "status" to "COMPLETED",
                                "isCompleted" to true,
                                "completedAt" to finalVisit.visitDate,
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                    }

                    if (finalVisit.taskId.isNotBlank()) {
                        fStore.collection("tasks").document(finalVisit.taskId).update(
                            mapOf(
                                "status" to VisitStatus.SUBMITTED.name,
                                "visitId" to finalVisit.visitId,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    } else {
                        val taskQuery = fStore.collection("tasks")
                            .whereEqualTo("visitId", finalVisit.visitId)
                            .get()
                        val taskDocs = com.google.android.gms.tasks.Tasks.await(taskQuery)
                        for (doc in taskDocs.documents) {
                            doc.reference.update("status", VisitStatus.SUBMITTED.name)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice updating task status in Firestore: ${e.message}")
                }
            }

            // Step 3: If online, attempt immediate background upload with timeout protection
            if (isOnline) {
                val uploaded = syncManager.uploadSingleVisitToServer(finalVisit)
                if (uploaded) {
                    // NOTE: syncManager.uploadSingleVisitToServer already updated the Room DB record
                    // with the permanent Cloudinary photo URLs and SYNCED status. Do NOT overwrite with
                    // finalVisit (which still holds local file URIs), or checkPendingCount will see local
                    // media and re-upload the same photos 2-3 times!
                    recordEvent(
                        visitId = finalVisit.visitId,
                        taskId = finalVisit.taskId,
                        eventType = "SYNCED",
                        actorId = finalVisit.employeeId,
                        actorName = finalVisit.employeeName,
                        actorRole = "EMPLOYEE",
                        details = "All inspection data and media uploaded to cloud"
                    )
                }
            }

            syncManager.checkPendingCount()

            try {
                NotificationRepository(
                    db.appNotificationDao(),
                    firestore ?: com.google.firebase.firestore.FirebaseFirestore.getInstance()
                ).createAndSendNotification(
                    context = context,
                    recipientUserId = "ADMIN",
                    title = "Visit Report Submitted! (रिपोर्ट सबमिट हुई)",
                    message = "${finalVisit.employeeName} submitted visit report for ${finalVisit.schoolName}.",
                    type = "REPORT_SUBMITTED",
                    relatedId = finalVisit.visitId,
                    schoolName = finalVisit.schoolName,
                    employeeName = finalVisit.employeeName
                )
            } catch (notifErr: Exception) {
                android.util.Log.w("VisitRepository", "Failed to send report notification: ${notifErr.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                db.visitDao().insertVisit(visit.copy(status = VisitStatus.SUBMITTED, syncStatus = SyncStatus.PENDING, updatedAt = System.currentTimeMillis()))
                syncManager.checkPendingCount()
                Result.success(Unit)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
        }
    }

    /**
     * Admin review action: Marks report as REVIEWED or REJECTED with notes.
     */
    suspend fun reviewVisit(
        visitId: String,
        adminUser: User,
        isApproved: Boolean,
        reviewNotes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = db.visitDao().getVisitById(visitId) ?: return@withContext Result.failure(Exception("Visit record not found"))
            val now = System.currentTimeMillis()
            val newStatus = if (isApproved) VisitStatus.REVIEWED else VisitStatus.REJECTED

            val updated = existing.copy(
                status = newStatus,
                reviewedAt = now,
                reviewedBy = adminUser.name.ifBlank { adminUser.email },
                reviewNotes = reviewNotes,
                rejectionReason = if (!isApproved) reviewNotes else "",
                updatedAt = now
            )
            db.visitDao().updateVisit(updated)

            if (updated.taskId.isNotBlank()) {
                db.taskDao().updateTaskStatus(updated.taskId, newStatus)
            }

            recordEvent(
                visitId = visitId,
                taskId = updated.taskId,
                eventType = if (isApproved) "REVIEWED" else "REJECTED",
                actorId = adminUser.userId,
                actorName = adminUser.name,
                actorRole = "ADMIN",
                statusFrom = existing.status.name,
                statusTo = newStatus.name,
                details = reviewNotes.ifBlank { if (isApproved) "Report approved by Admin" else "Report rejected by Admin" }
            )

            val fStore = firestore
            if (fStore != null && syncManager.isNetworkAvailable()) {
                try {
                    val updateMap = hashMapOf<String, Any>(
                        "status" to newStatus.name,
                        "reviewedAt" to now,
                        "reviewedBy" to updated.reviewedBy,
                        "reviewNotes" to reviewNotes,
                        "updatedAt" to now
                    )
                    if (!isApproved) {
                        updateMap["rejectionReason"] = reviewNotes
                    }
                    fStore.collection("visits").document(visitId).update(updateMap)

                    if (updated.taskId.isNotBlank()) {
                        fStore.collection("tasks").document(updated.taskId).update(
                            mapOf("status" to newStatus.name, "updatedAt" to now)
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice updating review in Firestore: ${e.message}")
                }
            }

            // Notify employee if report was reviewed/rejected
            if (existing.employeeId.isNotBlank()) {
                try {
                    NotificationRepository(
                        db.appNotificationDao(),
                        firestore ?: com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    ).createAndSendNotification(
                        context = context,
                        recipientUserId = existing.employeeId,
                        title = if (isApproved) "Report Reviewed (रिपोर्ट जाँची गई)" else "Report Needs Attention (संशोधन आवश्यक)",
                        message = if (isApproved) "Your visit report for ${existing.schoolName} has been approved." else "Feedback: $reviewNotes",
                        type = if (isApproved) "REPORT_REVIEWED" else "REPORT_REJECTED",
                        relatedId = visitId,
                        schoolName = existing.schoolName,
                        employeeName = existing.employeeName
                    )
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice sending review alert: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVisitAnswers(visitId: String, updatedAnswers: com.example.data.model.VisitAnswers): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = db.visitDao().getVisitById(visitId) ?: return@withContext Result.failure(Exception("Visit not found"))
            val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val answersJson = moshi.adapter(com.example.data.model.VisitAnswers::class.java).toJson(updatedAnswers)
            val updated = existing.copy(
                answersJson = answersJson,
                updatedAt = System.currentTimeMillis()
            )
            db.visitDao().updateVisit(updated)

            val fStore = firestore
            if (fStore != null && syncManager.isNetworkAvailable()) {
                try {
                    val task = fStore.collection("visits").document(visitId).update(
                        mapOf(
                            "answersJson" to answersJson,
                            "updatedAt" to updated.updatedAt
                        )
                    )
                    com.google.android.gms.tasks.Tasks.await(task)
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice updating answers in Firestore: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhotoFromVisit(visitId: String, categoryId: String, photoUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = db.visitDao().getVisitById(visitId) ?: return@withContext Result.failure(Exception("Visit not found"))
            val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
            val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
            val currentMap = try { adapter.fromJson(existing.photosJson) ?: emptyMap() } catch (e: Exception) { emptyMap() }

            val updatedMap = currentMap.toMutableMap()
            val currentList = updatedMap[categoryId]?.toMutableList() ?: mutableListOf()
            currentList.remove(photoUrl)
            if (currentList.isEmpty()) {
                updatedMap.remove(categoryId)
            } else {
                updatedMap[categoryId] = currentList
            }

            val updatedPhotosJson = adapter.toJson(updatedMap)
            val updatedVisit = existing.copy(
                photosJson = updatedPhotosJson,
                updatedAt = System.currentTimeMillis()
            )
            db.visitDao().updateVisit(updatedVisit)

            recordEvent(
                visitId = visitId,
                taskId = existing.taskId,
                eventType = "PHOTO_DELETED",
                actorId = existing.employeeId,
                details = "Photo removed from category: $categoryId"
            )

            val fStore = firestore
            if (fStore != null && syncManager.isNetworkAvailable()) {
                try {
                    val task = fStore.collection("visits").document(visitId).update(
                        mapOf(
                            "photosJson" to updatedPhotosJson,
                            "updatedAt" to updatedVisit.updatedAt
                        )
                    )
                    com.google.android.gms.tasks.Tasks.await(task)
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice updating deleted photo in Firestore: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPhotoToVisit(
        visitId: String,
        categoryId: String,
        photoUriOrUrl: String,
        actorUser: User? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = db.visitDao().getVisitById(visitId) ?: return@withContext Result.failure(Exception("Visit not found"))
            val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
            val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
            val currentMap = try { adapter.fromJson(existing.photosJson) ?: emptyMap() } catch (e: Exception) { emptyMap() }

            val updatedMap = currentMap.toMutableMap()
            val currentList = updatedMap[categoryId]?.toMutableList() ?: mutableListOf()
            if (!currentList.contains(photoUriOrUrl)) {
                currentList.add(photoUriOrUrl)
            }
            updatedMap[categoryId] = currentList

            val updatedPhotosJson = adapter.toJson(updatedMap)
            val updatedVisit = existing.copy(
                photosJson = updatedPhotosJson,
                updatedAt = System.currentTimeMillis()
            )
            db.visitDao().updateVisit(updatedVisit)

            recordEvent(
                visitId = visitId,
                taskId = existing.taskId,
                eventType = "PHOTO_ADDED",
                actorId = actorUser?.userId ?: existing.employeeId,
                actorName = actorUser?.name ?: existing.employeeName,
                actorRole = actorUser?.role?.name ?: "ADMIN",
                details = "Photo added to category: $categoryId"
            )

            val fStore = firestore
            if (fStore != null && syncManager.isNetworkAvailable()) {
                try {
                    val task = fStore.collection("visits").document(visitId).update(
                        mapOf(
                            "photosJson" to updatedPhotosJson,
                            "updatedAt" to updatedVisit.updatedAt
                        )
                    )
                    com.google.android.gms.tasks.Tasks.await(task)
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice updating added photo in Firestore: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVisit(visit: Visit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = visit.copy(updatedAt = System.currentTimeMillis())
            db.visitDao().updateVisit(updated)

            val fStore = firestore
            if (fStore != null && syncManager.isNetworkAvailable()) {
                try {
                    val currentUid = FirebaseUtils.auth?.currentUser?.uid ?: ""
                    val isEmployeeCaller = (currentUid == visit.employeeId)

                    if (isEmployeeCaller) {
                        val task = fStore.collection("visits").document(visit.visitId).update(
                            mapOf(
                                "status" to updated.status.name,
                                "answersJson" to updated.answersJson,
                                "photosJson" to updated.photosJson,
                                "syncStatus" to updated.syncStatus.name,
                                "updatedAt" to updated.updatedAt
                            )
                        )
                        com.google.android.gms.tasks.Tasks.await(task)
                    } else {
                        val task = fStore.collection("visits").document(visit.visitId).set(
                            mapOf(
                                "visitId" to updated.visitId,
                                "taskId" to updated.taskId,
                                "schoolId" to updated.schoolId,
                                "employeeId" to updated.employeeId,
                                "employeeName" to updated.employeeName,
                                "schoolName" to updated.schoolName,
                                "state" to updated.state,
                                "district" to updated.district,
                                "block" to updated.block,
                                "villageName" to updated.villageName,
                                "schoolType" to updated.schoolType,
                                "udiseCode" to updated.udiseCode,
                                "principalName" to updated.principalName,
                                "principalMobile" to updated.principalMobile,
                                "visitDate" to updated.visitDate,
                                "status" to updated.status.name,
                                "answersJson" to updated.answersJson,
                                "photosJson" to updated.photosJson,
                                "startedAt" to (updated.startedAt ?: 0L),
                                "completedAt" to (updated.completedAt ?: 0L),
                                "submittedAt" to (updated.submittedAt ?: 0L),
                                "reviewedAt" to (updated.reviewedAt ?: 0L),
                                "reviewedBy" to updated.reviewedBy,
                                "reviewNotes" to updated.reviewNotes,
                                "rejectionReason" to updated.rejectionReason,
                                "syncStatus" to updated.syncStatus.name,
                                "updatedAt" to updated.updatedAt
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        com.google.android.gms.tasks.Tasks.await(task)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("VisitRepository", "Notice updating visit in Firestore: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

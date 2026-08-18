package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.SyncStatus
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.util.FirebaseUtils
import com.example.util.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VisitRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    // BUG FIX: was `SyncManager(context)` which created a *second*, independent SyncManager
    // instance separate from the one MainActivity observes for isOnline/pendingSyncCount/
    // isSyncing. That desynced the UI badges from the real upload state and ran duplicate
    // network listeners. Now uses the shared app-wide singleton instead.
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

    suspend fun syncVisitsFromFirestore(role: com.example.data.model.UserRole? = null, userId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
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
                    district = doc.getString("district") ?: "",
                    block = doc.getString("block") ?: "",
                    visitDate = doc.getString("visitDate") ?: "",
                    status = status,
                    answersJson = doc.getString("answersJson") ?: "{}",
                    photosJson = doc.getString("photosJson") ?: "{}",
                    editCount = (doc.getLong("editCount") ?: 0L).toInt(),
                    syncStatus = SyncStatus.SYNCED,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }

            // Group visits to remove true duplicates created by multiple/double-tap submissions.
            // BUG FIX: this used to key ONLY by "schoolId_employeeId". That silently deleted
            // legitimate separate visits — e.g. the same employee visiting the same school on
            // two different dates/tasks — because they'd collapse into "one duplicate group"
            // and every visit except the latest was PERMANENTLY DELETED from Firestore + Room.
            // This was the main cause of visit reports randomly disappearing / one panel
            // showing fewer visits than another. A real accidental duplicate is the same task
            // submitted twice, so the key must include taskId (or visitDate as a fallback when
            // taskId is missing) to avoid merging genuinely different visits.
            val cleanVisits = mutableListOf<Visit>()
            val groupedBySchoolEmp = rawVisits.groupBy {
                if (it.taskId.isNotBlank()) "task_${it.taskId}"
                else "${it.schoolId}_${it.employeeId}_${it.visitDate}"
            }

            for ((_, group) in groupedBySchoolEmp) {
                if (group.size == 1) {
                    cleanVisits.add(group.first())
                } else {
                    // Pick the most recent visit
                    val winner = group.maxByOrNull { it.updatedAt } ?: group.first()
                    cleanVisits.add(winner)

                    // Clean up extra duplicate documents from Firestore and local DB
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

            if (cleanVisits.isNotEmpty()) {
                db.visitDao().insertVisits(cleanVisits)

                // CRITICAL BUG FIX: this used to run
                //   db.visitDao().deleteVisitsNotIn(cleanVisits.map { it.visitId })
                // which deletes EVERY local visit whose id isn't in the just-fetched remote
                // set. That includes visits saved locally while offline (syncStatus = PENDING)
                // that haven't reached Firestore yet — the moment this pull ran (tab switch,
                // login, network-back callback, etc.) before the pending upload finished, the
                // employee's not-yet-synced report was permanently wiped from the device with
                // no way to recover it. A remote pull must never delete data the server hasn't
                // seen yet. Now local PENDING/FAILED (not-yet-synced) visits are always kept,
                // no matter what the server returned.
                val stillUnsyncedLocalIds = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING).map { it.visitId } +
                        db.visitDao().getVisitsBySyncStatus(SyncStatus.FAILED).map { it.visitId }
                val idsToKeep = (cleanVisits.map { it.visitId } + stillUnsyncedLocalIds).distinct()
                db.visitDao().deleteVisitsNotIn(idsToKeep)
                for (v in cleanVisits) {
                    if (v.status == VisitStatus.SUBMITTED || v.status == VisitStatus.REVIEWED) {
                        if (v.taskId.isNotBlank()) {
                            db.taskDao().markTaskSubmittedById(v.taskId, v.visitId)
                        } else {
                            db.taskDao().markTaskSubmittedByVisitId(v.visitId)
                        }
                    }
                }
            } else {
                // BUG FIX: same data-loss issue as above — deleteAllVisits()/deleteVisitsForEmployee()
                // would also wipe local PENDING/FAILED (not-yet-synced) visits whenever the
                // remote query legitimately returned zero results. Only clear out already-SYNCED
                // local rows so unsynced offline work is never lost.
                if (!isEmployee) {
                    db.visitDao().deleteAllSyncedVisits()
                } else if (currentUid.isNotBlank()) {
                    db.visitDao().deleteSyncedVisitsForEmployee(currentUid)
                }
            }
            Result.success(cleanVisits.size)
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

            // If taskId is blank, attempt to resolve matching active task
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
                syncStatus = initialSyncStatus,
                updatedAt = System.currentTimeMillis()
            )

            // Step 1: Save to Room DB locally first (ensures 100% offline durability)
            db.visitDao().insertVisit(finalVisit)
            
            // Step 2: Mark ONLY the exact assigned task as SUBMITTED
            if (finalVisit.taskId.isNotBlank()) {
                db.taskDao().markTaskSubmittedById(finalVisit.taskId, finalVisit.visitId)
            } else {
                db.taskDao().markTaskSubmittedByVisitId(finalVisit.visitId)
            }

            val fStore = firestore
            if (fStore != null && isOnline) {
                try {
                    // Update exact task in Firestore
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
                    db.visitDao().updateVisit(finalVisit.copy(syncStatus = SyncStatus.SYNCED))
                } else {
                    // Stays PENDING; SyncManager auto-sync will upload as soon as network stabilizes
                    db.visitDao().updateVisit(finalVisit.copy(syncStatus = SyncStatus.PENDING))
                }
            }

            syncManager.checkPendingCount()

            // Trigger notification for Admin on report submission
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
            // Even on unexpected exception, try local save fallback
            try {
                db.visitDao().insertVisit(visit.copy(status = VisitStatus.SUBMITTED, syncStatus = SyncStatus.PENDING, updatedAt = System.currentTimeMillis()))
                syncManager.checkPendingCount()
                Result.success(Unit)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
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
                                "schoolId" to updated.schoolId,
                                "employeeId" to updated.employeeId,
                                "employeeName" to updated.employeeName,
                                "schoolName" to updated.schoolName,
                                "state" to updated.state,
                                "district" to updated.district,
                                "block" to updated.block,
                                "visitDate" to updated.visitDate,
                                "status" to updated.status.name,
                                "answersJson" to updated.answersJson,
                                "photosJson" to updated.photosJson,
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

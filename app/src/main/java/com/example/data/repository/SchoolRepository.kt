package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.School
import com.example.util.FirebaseUtils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SchoolRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore

    private var schoolsListenerRegistration: ListenerRegistration? = null

    fun getAllSchools(): Flow<List<School>> = db.schoolDao().getAllSchools()

    fun searchSchools(query: String): Flow<List<School>> = db.schoolDao().searchSchools(query)

    suspend fun getSchoolById(schoolId: String): School? = withContext(Dispatchers.IO) {
        db.schoolDao().getSchoolById(schoolId)
    }

    fun startSchoolsRealtimeListener() {
        if (schoolsListenerRegistration != null) return
        val fStore = firestore ?: return
        val auth = FirebaseUtils.auth
        val currentUser = auth?.currentUser
        val uid = currentUser?.uid ?: "Unauthenticated"
        val projectId = try { fStore.app.options.projectId } catch (e: Exception) { "Unknown" }

        try {
            schoolsListenerRegistration = fStore.collection("schools").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val errCode = error.code.name.lowercase().replace('_', '-')
                    val logDetails = """
                        ================ REALTIME LISTENER EXCEPTION ================
                        Collection Path: schools
                        Firebase Error Code: ${error.code} ($errCode)
                        Error Message: ${error.message}
                        Project ID: $projectId
                        Authenticated Firebase UID: $uid
                        =============================================================
                    """.trimIndent()
                    Log.e("SchoolRepository", logDetails, error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val docCount = snapshot.documents.size
                    Log.d("SchoolRepository", "Schools Firestore documents fetched via listener: $docCount")
                    val parsedSchools = snapshot.documents.mapNotNull { doc ->
                        parseDocToSchool(doc)
                    }
                    Log.d("SchoolRepository", "Schools parsed via listener: ${parsedSchools.size}")
                    if (parsedSchools.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                db.schoolDao().insertSchools(parsedSchools)
                                Log.d("SchoolRepository", "Schools cached in Room via listener: ${parsedSchools.size}")
                            } catch (e: Exception) {
                                Log.e("SchoolRepository", "Failed to insert schools into Room cache", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Failed to start schools realtime listener", e)
        }
    }

    fun stopSchoolsRealtimeListener() {
        try {
            schoolsListenerRegistration?.remove()
        } catch (e: Exception) {
            Log.w("SchoolRepository", "Error removing schools realtime listener", e)
        }
        schoolsListenerRegistration = null
    }

    private fun parseDocToSchool(doc: DocumentSnapshot): School? {
        // Authoritative School ID: Always use Firestore document ID
        val schoolId = doc.id.trim()
        if (schoolId.isBlank()) return null

        // Field aliases support for schoolName
        val schoolName = (doc.getString("schoolName")
            ?: doc.getString("SCHOOL NAME")
            ?: doc.getString("school_name")
            ?: "").trim()
        if (schoolName.isBlank()) return null

        val stateName = (doc.getString("state")
            ?: doc.getString("stateName")
            ?: doc.getString("STATE")
            ?: "Rajasthan").trim().ifBlank { "Rajasthan" }

        val districtName = (doc.getString("district")
            ?: doc.getString("districtName")
            ?: doc.getString("DISTRICT")
            ?: "").trim()

        val schoolType = (doc.getString("type")
            ?: doc.getString("schoolType")
            ?: doc.getString("TYPE")
            ?: "").trim()

        val villageName = (doc.getString("village")
            ?: doc.getString("villageName")
            ?: doc.getString("VILLAGE")
            ?: "").trim()

        val principalName = (doc.getString("principalName")
            ?: doc.getString("PRINCIPAL NAME")
            ?: doc.getString("principal_name")
            ?: "").trim()

        val blockName = (doc.getString("block")
            ?: doc.getString("blockName")
            ?: doc.getString("BLOCK")
            ?: "").trim()

        val principalMobile = (doc.getString("mobile")
            ?: doc.getString("principalMobile")
            ?: doc.getString("MOB")
            ?: doc.getString("principalPhone")
            ?: "").trim()

        val visitDate = (doc.getString("visitDate")
            ?: doc.getString("originalVisitDate")
            ?: doc.getString("completedAt")
            ?: doc.getString("Visit Date")
            ?: "").trim()

        val isDeleted = doc.getBoolean("isDeleted") ?: false
        val deletedAt = doc.getLong("deletedAt") ?: 0L

        return School(
            schoolId = schoolId,
            sr = doc.getString("sr") ?: "",
            stateName = stateName,
            districtName = districtName,
            schoolName = schoolName,
            schoolType = schoolType,
            villageName = villageName,
            principalName = principalName,
            blockName = blockName,
            principalMobile = principalMobile,
            visitDate = visitDate,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    suspend fun syncSchoolsFromFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        val fStore = firestore ?: run {
            val msg = "Firestore instance is null"
            Log.e("SchoolRepository", "Sync failed: $msg")
            return@withContext Result.failure(Exception(msg))
        }

        val auth = FirebaseUtils.auth
        val currentUser = auth?.currentUser
        val uid = currentUser?.uid ?: "Unauthenticated"
        val projectId = try { fStore.app.options.projectId } catch (e: Exception) { "Unknown" }

        var roleStr = "Unknown"
        if (currentUser != null) {
            try {
                val userDocTask = fStore.collection("users").document(uid).get()
                val userDoc = com.google.android.gms.tasks.Tasks.await(userDocTask)
                roleStr = userDoc.getString("role") ?: "No role field"
            } catch (e: Exception) {
                roleStr = "Failed to fetch role: ${e.message}"
            }
        }

        try {
            Log.d("SchoolRepository", "Executing query: FirebaseFirestore.getInstance().collection(\"schools\").get()")
            val snapshotTask = fStore.collection("schools").get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)
            val docCount = snapshot.documents.size
            Log.d("SchoolRepository", "Schools Firestore documents fetched: $docCount for project: $projectId, path: schools, uid: $uid, role: $roleStr")

            val schools = snapshot.documents.mapNotNull { doc ->
                parseDocToSchool(doc)
            }
            Log.d("SchoolRepository", "Schools parsed: ${schools.size}")

            if (schools.isNotEmpty()) {
                db.schoolDao().insertSchools(schools)
                Log.d("SchoolRepository", "Schools cached in Room: ${schools.size}")
            }

            startSchoolsRealtimeListener()

            Result.success(schools.size)
        } catch (e: FirebaseFirestoreException) {
            val errCode = e.code.name.lowercase().replace('_', '-')
            val logDetails = """
                ================ FIRESTORE EXCEPTION DETECTED ================
                Collection Path: schools
                Firebase Error Code: ${e.code} ($errCode)
                Error Message: ${e.message}
                Project ID: $projectId
                Authenticated Firebase UID: $uid
                Authenticated User Role: $roleStr
                ==============================================================
            """.trimIndent()
            Log.e("SchoolRepository", logDetails, e)
            Result.failure(e)
        } catch (e: Exception) {
            val logDetails = """
                ================ EXCEPTION DETECTED ================
                Collection Path: schools
                Exception Type: ${e.javaClass.name}
                Error Message: ${e.message}
                Project ID: $projectId
                Authenticated Firebase UID: $uid
                Authenticated User Role: $roleStr
                ====================================================
            """.trimIndent()
            Log.e("SchoolRepository", logDetails, e)
            Result.failure(e)
        }
    }

    suspend fun importSchools(
        schools: List<School>,
        completedVisits: List<com.example.data.model.Visit> = emptyList()
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (schools.isEmpty()) return@withContext Result.success(0)

            // 1. Save schools and completed visits to Room database cache immediately
            db.schoolDao().insertSchools(schools)
            if (completedVisits.isNotEmpty()) {
                db.visitDao().insertVisits(completedVisits)
            }

            // 2. Sync schools and completed visits to Firestore in atomic batches
            val fStore = firestore
            if (fStore != null) {
                // Firestore batches can hold up to 500 operations
                val batchSize = 400
                schools.chunked(batchSize).forEach { chunk ->
                    val batch = fStore.batch()
                    for (sch in chunk) {
                        val docRef = fStore.collection("schools").document(sch.schoolId)
                        val isCompleted = sch.visitDate.isNotBlank()
                        val data = mapOf(
                            "schoolId" to sch.schoolId,
                            "sr" to sch.sr,
                            "state" to sch.stateName,
                            "stateName" to sch.stateName,
                            "district" to sch.districtName,
                            "districtName" to sch.districtName,
                            "schoolName" to sch.schoolName,
                            "type" to sch.schoolType,
                            "schoolType" to sch.schoolType,
                            "village" to sch.villageName,
                            "villageName" to sch.villageName,
                            "principalName" to sch.principalName,
                            "block" to sch.blockName,
                            "blockName" to sch.blockName,
                            "mobile" to sch.principalMobile,
                            "principalMobile" to sch.principalMobile,
                            "visitDate" to sch.visitDate,
                            "status" to if (isCompleted) "COMPLETED" else "PENDING",
                            "isCompleted" to isCompleted,
                            "completedAt" to if (isCompleted) sch.visitDate else "",
                            "isDeleted" to sch.isDeleted,
                            "deletedAt" to sch.deletedAt,
                            "createdAt" to sch.createdAt,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                    }
                    com.google.android.gms.tasks.Tasks.await(batch.commit())
                }

                // Batch upload the generated completed visits to Firestore visits collection
                if (completedVisits.isNotEmpty()) {
                    completedVisits.chunked(batchSize).forEach { visitChunk ->
                        val visitBatch = fStore.batch()
                        for (v in visitChunk) {
                            val visitDocRef = fStore.collection("visits").document(v.visitId)
                            val visitData = mapOf(
                                "visitId" to v.visitId,
                                "schoolId" to v.schoolId,
                                "employeeId" to v.employeeId,
                                "employeeName" to v.employeeName,
                                "schoolName" to v.schoolName,
                                "district" to v.district,
                                "block" to v.block,
                                "visitDate" to v.visitDate,
                                "status" to v.status.name,
                                "answersJson" to v.answersJson,
                                "photosJson" to v.photosJson,
                                "syncStatus" to com.example.data.model.SyncStatus.SYNCED.name,
                                "createdAt" to v.createdAt,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            visitBatch.set(visitDocRef, visitData, com.google.firebase.firestore.SetOptions.merge())
                        }
                        com.google.android.gms.tasks.Tasks.await(visitBatch.commit())
                    }
                }
            }

            Result.success(schools.size)
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Import failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addSchool(school: School): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val isCompleted = school.visitDate.isNotBlank()
            val finalSchool = school.copy(createdAt = now, updatedAt = now)

            db.schoolDao().insertSchool(finalSchool)

            val fStore = firestore
            if (fStore != null) {
                val data = mapOf(
                    "schoolId" to finalSchool.schoolId,
                    "sr" to finalSchool.sr,
                    "state" to finalSchool.stateName,
                    "stateName" to finalSchool.stateName,
                    "district" to finalSchool.districtName,
                    "districtName" to finalSchool.districtName,
                    "schoolName" to finalSchool.schoolName,
                    "type" to finalSchool.schoolType,
                    "schoolType" to finalSchool.schoolType,
                    "village" to finalSchool.villageName,
                    "villageName" to finalSchool.villageName,
                    "principalName" to finalSchool.principalName,
                    "block" to finalSchool.blockName,
                    "blockName" to finalSchool.blockName,
                    "mobile" to finalSchool.principalMobile,
                    "principalMobile" to finalSchool.principalMobile,
                    "visitDate" to finalSchool.visitDate,
                    "status" to if (isCompleted) "COMPLETED" else "PENDING",
                    "isCompleted" to isCompleted,
                    "completedAt" to if (isCompleted) finalSchool.visitDate else "",
                    "isDeleted" to false,
                    "deletedAt" to 0L,
                    "createdAt" to finalSchool.createdAt,
                    "updatedAt" to finalSchool.updatedAt
                )
                val task = fStore.collection("schools").document(finalSchool.schoolId).set(
                    data,
                    com.google.firebase.firestore.SetOptions.merge()
                )
                com.google.android.gms.tasks.Tasks.await(task)

                if (isCompleted) {
                    val visitId = "vst_" + finalSchool.schoolId.removePrefix("sch_") + "_manual"
                    val visitData = mapOf(
                        "visitId" to visitId,
                        "schoolId" to finalSchool.schoolId,
                        "employeeId" to "emp_admin",
                        "employeeName" to "Admin (Direct Entry)",
                        "schoolName" to finalSchool.schoolName,
                        "district" to finalSchool.districtName,
                        "block" to finalSchool.blockName,
                        "visitDate" to finalSchool.visitDate,
                        "status" to com.example.data.model.VisitStatus.SUBMITTED.name,
                        "answersJson" to "{}",
                        "photosJson" to "{}",
                        "syncStatus" to com.example.data.model.SyncStatus.SYNCED.name,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                    fStore.collection("visits").document(visitId).set(
                        visitData,
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Failed to add school: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateSchoolRecord(school: School): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = school.copy(updatedAt = System.currentTimeMillis())
            val isCompleted = updated.visitDate.isNotBlank()

            val fStore = firestore
            if (fStore != null) {
                val task = fStore.collection("schools").document(school.schoolId).set(
                    mapOf(
                        "schoolId" to updated.schoolId,
                        "sr" to updated.sr,
                        "state" to updated.stateName,
                        "stateName" to updated.stateName,
                        "district" to updated.districtName,
                        "districtName" to updated.districtName,
                        "schoolName" to updated.schoolName,
                        "type" to updated.schoolType,
                        "schoolType" to updated.schoolType,
                        "village" to updated.villageName,
                        "villageName" to updated.villageName,
                        "principalName" to updated.principalName,
                        "block" to updated.blockName,
                        "blockName" to updated.blockName,
                        "mobile" to updated.principalMobile,
                        "principalMobile" to updated.principalMobile,
                        "visitDate" to updated.visitDate,
                        "status" to if (isCompleted) "COMPLETED" else "PENDING",
                        "isCompleted" to isCompleted,
                        "completedAt" to if (isCompleted) updated.visitDate else "",
                        "updatedAt" to updated.updatedAt
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                com.google.android.gms.tasks.Tasks.await(task)

                if (isCompleted) {
                    val visitId = "vst_" + updated.schoolId.removePrefix("sch_") + "_manual"
                    val visitData = mapOf(
                        "visitId" to visitId,
                        "schoolId" to updated.schoolId,
                        "employeeId" to "emp_admin",
                        "employeeName" to "Admin (Updated)",
                        "schoolName" to updated.schoolName,
                        "district" to updated.districtName,
                        "block" to updated.blockName,
                        "visitDate" to updated.visitDate,
                        "status" to com.example.data.model.VisitStatus.SUBMITTED.name,
                        "answersJson" to "{}",
                        "photosJson" to "{}",
                        "syncStatus" to com.example.data.model.SyncStatus.SYNCED.name,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    fStore.collection("visits").document(visitId).set(
                        visitData,
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
            }

            db.schoolDao().updateSchool(updated)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun softDeleteSchool(schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            db.schoolDao().softDeleteSchool(schoolId, now)
            
            // Delete associated visits and tasks from local Room database
            db.visitDao().deleteVisitsBySchool(schoolId)
            db.taskDao().deleteTasksBySchool(schoolId)

            val fStore = firestore
            if (fStore != null) {
                val task = fStore.collection("schools").document(schoolId).update(
                    mapOf(
                        "isDeleted" to true,
                        "deletedAt" to now,
                        "updatedAt" to now
                    )
                )
                com.google.android.gms.tasks.Tasks.await(task)

                // Also delete associated visits & tasks from Firestore
                try {
                    val visitDocs = com.google.android.gms.tasks.Tasks.await(
                        fStore.collection("visits").whereEqualTo("schoolId", schoolId).get()
                    )
                    for (doc in visitDocs.documents) {
                        fStore.collection("visits").document(doc.id).delete()
                    }
                    val taskDocs = com.google.android.gms.tasks.Tasks.await(
                        fStore.collection("tasks").whereEqualTo("schoolId", schoolId).get()
                    )
                    for (doc in taskDocs.documents) {
                        fStore.collection("tasks").document(doc.id).delete()
                    }
                } catch (e: Exception) {
                    Log.w("SchoolRepository", "Notice deleting visits/tasks from Firestore: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Failed to soft delete school: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun restoreSchool(schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            db.schoolDao().restoreSchool(schoolId)

            val fStore = firestore
            if (fStore != null) {
                val task = fStore.collection("schools").document(schoolId).update(
                    mapOf(
                        "isDeleted" to false,
                        "deletedAt" to 0L,
                        "updatedAt" to now
                    )
                )
                com.google.android.gms.tasks.Tasks.await(task)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Failed to restore school: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun permanentDeleteSchool(schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("schools")?.document(schoolId)?.delete()
            db.schoolDao().deleteSchoolById(schoolId)
            
            // Delete associated visits and tasks from local Room database
            db.visitDao().deleteVisitsBySchool(schoolId)
            db.taskDao().deleteTasksBySchool(schoolId)

            val fStore = firestore
            if (fStore != null) {
                try {
                    val visitDocs = com.google.android.gms.tasks.Tasks.await(
                        fStore.collection("visits").whereEqualTo("schoolId", schoolId).get()
                    )
                    for (doc in visitDocs.documents) {
                        fStore.collection("visits").document(doc.id).delete()
                    }
                    val taskDocs = com.google.android.gms.tasks.Tasks.await(
                        fStore.collection("tasks").whereEqualTo("schoolId", schoolId).get()
                    )
                    for (doc in taskDocs.documents) {
                        fStore.collection("tasks").document(doc.id).delete()
                    }
                } catch (e: Exception) {
                    Log.w("SchoolRepository", "Notice permanently deleting visits/tasks from Firestore: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Failed to permanently delete school: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSchool(schoolId: String): Result<Unit> = softDeleteSchool(schoolId)
}

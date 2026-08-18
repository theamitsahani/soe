package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.AppDatabase
import com.example.data.model.SyncStatus
import com.example.data.model.Visit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SyncManager private constructor(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val syncMutex = Mutex()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        // BUG FIX (data-loss safety net): AppDatabase declares Room migrations 6->7 and 8->9
        // but no 7->8, while using fallbackToDestructiveMigration(true). A device that updates
        // straight across that version gap (very common — most people don't install every
        // single release) has its ENTIRE local Room database silently wiped and recreated the
        // next time it's opened. Every other table (schools/tasks/visits cache) is harmless to
        // lose because it's just a Firestore mirror and gets re-synced on next login — but a
        // visit still sitting as PENDING/FAILED (collected offline, never yet reached the
        // server) only exists in this local database. A destructive migration would silently
        // and permanently destroy that employee's unsynced field report with no error shown
        // anywhere. restorePendingVisitsFromBackup() runs first, before any sync activity, and
        // recovers any pending visit that a prior session's backupPendingVisits() call
        // persisted to SharedPreferences (a store Room's migration path never touches) but that
        // is now missing from Room — i.e. exactly the signature of a destructive wipe.
        syncScope.launch {
            try {
                restorePendingVisitsFromBackup()
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "Error restoring pending visits from backup", e)
            }
        }
        registerNetworkCallback()
        syncScope.launch {
            checkPendingCount()
            if (isNetworkAvailable()) {
                syncPendingData()
            }
        }
    }

    private fun backupPrefs() = context.applicationContext.getSharedPreferences("soe_sync_backup", Context.MODE_PRIVATE)

    /**
     * Persists every currently PENDING/FAILED (not-yet-synced) visit to SharedPreferences,
     * a storage location completely independent of the Room database file, so it survives
     * even a destructive Room migration or an accidental local DB corruption/wipe.
     */
    private suspend fun backupPendingVisits() {
        try {
            val pending = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING) +
                    db.visitDao().getVisitsBySyncStatus(SyncStatus.FAILED)
            val arr = org.json.JSONArray()
            for (v in pending) {
                val obj = org.json.JSONObject()
                obj.put("visitId", v.visitId)
                obj.put("taskId", v.taskId)
                obj.put("schoolId", v.schoolId)
                obj.put("employeeId", v.employeeId)
                obj.put("employeeName", v.employeeName)
                obj.put("schoolName", v.schoolName)
                obj.put("state", v.state)
                obj.put("district", v.district)
                obj.put("block", v.block)
                obj.put("visitDate", v.visitDate)
                obj.put("answersJson", v.answersJson)
                obj.put("photosJson", v.photosJson)
                obj.put("editCount", v.editCount)
                obj.put("createdAt", v.createdAt)
                obj.put("updatedAt", v.updatedAt)
                arr.put(obj)
            }
            backupPrefs().edit().putString("pending_visits_json", arr.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w("SyncManager", "Error backing up pending visits: ${e.message}")
        }
    }

    /**
     * Recovers any visit present in the SharedPreferences backup but missing from Room —
     * the signature of a destructive migration/local-DB wipe having just happened. Restored
     * visits are marked PENDING so the normal sync flow uploads them on the next opportunity.
     */
    private suspend fun restorePendingVisitsFromBackup() {
        val raw = backupPrefs().getString("pending_visits_json", null) ?: return
        try {
            val arr = org.json.JSONArray(raw)
            var restoredCount = 0
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val visitId = obj.optString("visitId")
                if (visitId.isBlank()) continue
                val existing = db.visitDao().getVisitById(visitId)
                if (existing != null) continue
                val restored = Visit(
                    visitId = visitId,
                    taskId = obj.optString("taskId", ""),
                    schoolId = obj.optString("schoolId", ""),
                    employeeId = obj.optString("employeeId", ""),
                    employeeName = obj.optString("employeeName", ""),
                    schoolName = obj.optString("schoolName", ""),
                    state = obj.optString("state", "Rajasthan"),
                    district = obj.optString("district", ""),
                    block = obj.optString("block", ""),
                    visitDate = obj.optString("visitDate", ""),
                    status = com.example.data.model.VisitStatus.SUBMITTED,
                    answersJson = obj.optString("answersJson", ""),
                    photosJson = obj.optString("photosJson", ""),
                    editCount = obj.optInt("editCount", 0),
                    syncStatus = SyncStatus.PENDING,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                db.visitDao().insertVisit(restored)
                restoredCount++
            }
            if (restoredCount > 0) {
                android.util.Log.w("SyncManager", "Recovered $restoredCount pending visit(s) from backup after local DB was missing them (likely a destructive migration).")
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncManager", "Error restoring pending visits from backup: ${e.message}")
        }
    }

    private fun registerNetworkCallback() {
        try {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connectivityManager.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    // Auto-sync as soon as network is detected!
                    syncScope.launch {
                        checkPendingCount()
                        syncPendingData()
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = isNetworkAvailable()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isOnline.value = hasInternet
                    if (hasInternet) {
                        syncScope.launch {
                            checkPendingCount()
                            syncPendingData()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateNetworkState() {
        _isOnline.value = isNetworkAvailable()
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
            actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkPendingCount() {
        withContext(Dispatchers.IO) {
            val allVisits = db.visitDao().getAllVisitsList()
            for (v in allVisits) {
                if (v.syncStatus == SyncStatus.SYNCED && !MediaStorageHelper.isAllMediaUploaded(v.photosJson)) {
                    android.util.Log.w("SyncManager", "Visit ${v.visitId} was marked SYNCED but has unuploaded local media! Marking PENDING for Cloudinary upload.")
                    db.visitDao().updateVisit(v.copy(syncStatus = SyncStatus.PENDING))
                }
            }
            val pendingVisits = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING)
            _pendingSyncCount.value = pendingVisits.size
            // Keep the SharedPreferences safety-net backup current every time pending state
            // is recalculated, so it always reflects the latest not-yet-synced work.
            backupPendingVisits()
        }
    }

    /**
     * Uploads all pending offline/slow-network saved visits to the server safely.
     */
    suspend fun syncPendingData(): Boolean = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            android.util.Log.d("SyncManager", "Sync is already in progress, skipping duplicate call.")
            return@withContext false
        }
        try {
            updateNetworkState()
            if (!_isOnline.value) return@withContext false

            _isSyncing.value = true
            val pendingVisits = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING)
            if (pendingVisits.isEmpty()) {
                _pendingSyncCount.value = 0
                return@withContext true
            }

            // BUG FIX: was grouping only by "schoolId_employeeId", which deleted genuinely
            // different pending visits (same employee/school, different task or date) before
            // they ever reached the server — permanent data loss for offline-collected reports.
            // Key by taskId (falls back to schoolId+employeeId+visitDate) so only true repeated
            // submissions of the same task are treated as duplicates.
            val uniqueVisitsMap = pendingVisits.groupBy {
                if (it.taskId.isNotBlank()) "task_${it.taskId}"
                else "${it.schoolId}_${it.employeeId}_${it.visitDate}"
            }
            val cleanPendingVisits = uniqueVisitsMap.values.map { list -> list.maxByOrNull { it.updatedAt }!! }

            // Delete extra local duplicate pending records if any
            for (p in pendingVisits) {
                if (!cleanPendingVisits.contains(p)) {
                    db.visitDao().deleteVisitById(p.visitId)
                }
            }

            var anySuccess = false
            for (visit in cleanPendingVisits) {
                val uploadSuccess = uploadSingleVisitToServer(visit)
                if (uploadSuccess) {
                    db.visitDao().updateVisit(visit.copy(syncStatus = SyncStatus.SYNCED))
                    anySuccess = true
                }
            }

            checkPendingCount()
            anySuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
        }
    }

    suspend fun uploadSingleVisitToServer(visit: Visit): Boolean = withContext(Dispatchers.IO) {
        val fStore = firestore ?: return@withContext false
        try {
            // 1. Upload photos/videos directly to Cloudinary and get permanent download URLs
            val updatedPhotosJson = try {
                MediaStorageHelper.uploadPhotosJsonToFirebaseStorage(
                    context = context,
                    visitId = visit.visitId,
                    schoolId = visit.schoolId,
                    employeeId = visit.employeeId,
                    photosJson = visit.photosJson,
                    schoolName = visit.schoolName,
                    visitDate = visit.visitDate
                )
            } catch (e: Exception) {
                visit.photosJson
            }

            // 2. Check if 100% of photos and videos are now Cloudinary remote URLs
            val isAllMediaUploaded = MediaStorageHelper.isAllMediaUploaded(updatedPhotosJson)
            val finalSyncStatus = if (isAllMediaUploaded) SyncStatus.SYNCED else SyncStatus.PENDING

            android.util.Log.d("VISIT_SYNC_START", "Starting visit sync for visitId=${visit.visitId}, schoolId=${visit.schoolId}, employeeId=${visit.employeeId}")

            // 3. Set timeout for Firestore write
            val success = withTimeoutOrNull(120000L) {
                val visitMap = hashMapOf(
                    "visitId" to visit.visitId,
                    "taskId" to visit.taskId,
                    "schoolId" to visit.schoolId,
                    "employeeId" to visit.employeeId,
                    "employeeName" to visit.employeeName,
                    "schoolName" to visit.schoolName,
                    "district" to visit.district,
                    "block" to visit.block,
                    "visitDate" to visit.visitDate,
                    "status" to visit.status.name,
                    "answersJson" to visit.answersJson,
                    "photosJson" to updatedPhotosJson,
                    "syncStatus" to finalSyncStatus.name,
                    "editCount" to visit.editCount,
                    "createdAt" to visit.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )

                val setTask = fStore.collection("visits")
                    .document(visit.visitId)
                    .set(visitMap, com.google.firebase.firestore.SetOptions.merge())
                com.google.android.gms.tasks.Tasks.await(setTask)

                // Update local Room database with permanent photo URLs and current sync status
                db.visitDao().updateVisit(visit.copy(photosJson = updatedPhotosJson, syncStatus = finalSyncStatus))

                // Update exact assigned task in Firestore
                try {
                    if (visit.taskId.isNotBlank()) {
                        fStore.collection("tasks").document(visit.taskId).update(
                            mapOf(
                                "status" to "SUBMITTED",
                                "visitId" to visit.visitId,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    } else {
                        val taskQuery = fStore.collection("tasks")
                            .whereEqualTo("visitId", visit.visitId)
                            .get()
                        val taskSnap = com.google.android.gms.tasks.Tasks.await(taskQuery)
                        for (taskDoc in taskSnap.documents) {
                            taskDoc.reference.update(
                                mapOf(
                                    "status" to "SUBMITTED",
                                    "visitId" to visit.visitId,
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                if (finalSyncStatus == SyncStatus.SYNCED) {
                    android.util.Log.d("VISIT_SYNC_SUCCESS", "Visit ${visit.visitId} synced successfully with all Cloudinary media uploaded.")
                } else {
                    android.util.Log.w("VISIT_SYNC_PENDING", "Visit ${visit.visitId} written to Firestore but media is pending Cloudinary upload.")
                }

                true
            }
            (success == true) && isAllMediaUploaded
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SyncManager? = null

        /**
         * BUG FIX: SyncManager used to be instantiated separately in MainActivity AND inside
         * VisitRepository. That created two independent instances, each with its own
         * isOnline / pendingSyncCount / isSyncing StateFlows and its own network callback +
         * sync mutex. Result: the UI (observing MainActivity's instance) never reflected the
         * uploads that VisitRepository's own private instance was doing, panels showed stale/
         * mismatched pending-sync counts, and two parallel network listeners could trigger
         * duplicate concurrent uploads of the same visit.
         *
         * Fixing this by making SyncManager a true app-wide singleton, exactly like
         * AppDatabase, so every screen/repository shares one source of truth.
         */
        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

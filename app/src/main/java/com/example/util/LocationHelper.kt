package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val time: Long = System.currentTimeMillis()
)

data class SchoolMapItem(
    val schoolId: String,
    val schoolName: String,
    val state: String,
    val district: String,
    val block: String,
    val village: String,
    val schoolType: String,
    val principalName: String,
    val principalMobile: String,
    val mapLink: String,
    val latitude: Double,
    val longitude: Double,
    val isExactCoordinate: Boolean,
    val status: VisitStatus,
    val assignedEmployeeName: String = "",
    val assignedDate: String = "",
    val completedDate: String = "",
    val visitId: String = "",
    val taskId: String = "",
    var distanceFromUserMeters: Float? = null
)

object LocationHelper {

    private const val TAG = "LocationHelper"

    // Centroids for major districts of Rajasthan and surrounding states
    private val DISTRICT_COORDINATES = mapOf(
        "Jaipur" to Pair(26.9124, 75.7873),
        "Jaipur Rural" to Pair(26.9800, 75.8200),
        "Jodhpur" to Pair(26.2389, 73.0243),
        "Jodhpur Rural" to Pair(26.3500, 72.9500),
        "Kota" to Pair(25.2138, 75.8648),
        "Bikaner" to Pair(28.0229, 73.3119),
        "Ajmer" to Pair(26.4499, 74.6399),
        "Udaipur" to Pair(24.5854, 73.7125),
        "Alwar" to Pair(27.5530, 76.6346),
        "Bharatpur" to Pair(27.2152, 77.5030),
        "Bhilwara" to Pair(25.3407, 74.6313),
        "Sri Ganganagar" to Pair(29.9038, 73.8772),
        "Sikar" to Pair(27.6094, 75.1398),
        "Pali" to Pair(25.7711, 73.3234),
        "Tonk" to Pair(26.1664, 75.7885),
        "Churu" to Pair(28.2900, 74.9600),
        "Nagaur" to Pair(27.2000, 73.7400),
        "Jhunjhunu" to Pair(28.1289, 75.3995),
        "Barmer" to Pair(25.7521, 71.3967),
        "Jaisalmer" to Pair(26.9157, 70.9083),
        "Chittorgarh" to Pair(24.8887, 74.6269),
        "Hanumangarh" to Pair(29.5800, 74.3200),
        "Dausa" to Pair(26.8900, 76.3300),
        "Sawai Madhopur" to Pair(25.9928, 76.3711),
        "Jhalawar" to Pair(24.5973, 76.1610),
        "Bundi" to Pair(25.4400, 75.6400),
        "Baran" to Pair(25.1000, 76.5100),
        "Banswara" to Pair(23.5461, 74.4373),
        "Dungarpur" to Pair(23.8365, 73.7147),
        "Rajsamand" to Pair(25.0440, 73.8820),
        "Sirohi" to Pair(24.8826, 72.8625),
        "Jalore" to Pair(25.3450, 72.6150),
        "Dholpur" to Pair(26.7020, 77.8930),
        "Karauli" to Pair(26.4950, 77.0200),
        "Pratapgarh" to Pair(24.0320, 74.7810),
        "Balotra" to Pair(25.8300, 72.2400),
        "Beawar" to Pair(26.1000, 74.3200),
        "Didwana-Kuchaman" to Pair(27.4000, 74.5800),
        "Deeg" to Pair(27.4700, 77.3200),
        "Dudu" to Pair(26.6800, 75.2300),
        "Gangapur City" to Pair(26.4700, 76.7200),
        "Kekri" to Pair(25.9700, 75.1500),
        "Khairthal-Tijara" to Pair(27.7900, 76.6200),
        "Kotputli-Behror" to Pair(27.7000, 76.2000),
        "Neem Ka Thana" to Pair(27.7400, 75.7800),
        "Phalodi" to Pair(27.1300, 72.3600),
        "Salumber" to Pair(24.1300, 74.0400),
        "Sanchore" to Pair(24.7500, 71.7700),
        "Shahpura" to Pair(25.6300, 74.9300),
        "Anupgarh" to Pair(29.1900, 73.2100)
    )

    /**
     * Resolves map coordinates for a school with high accuracy.
     * 1. Uses school.latitude and school.longitude if available.
     * 2. Extracts from school.mapLink if present.
     * 3. Uses district/block centroid with deterministic jitter based on schoolId so pins don't overlap.
     */
    fun resolveCoordinates(
        schoolId: String,
        latitude: Double?,
        longitude: Double?,
        mapLink: String,
        district: String,
        state: String
    ): Triple<Double, Double, Boolean> {
        if (latitude != null && longitude != null && latitude in 6.0..38.0 && longitude in 68.0..98.0) {
            return Triple(latitude, longitude, true)
        }

        val extracted = GoogleMapHelper.extractCoordinates(mapLink)
        if (extracted != null && extracted.first in 6.0..38.0 && extracted.second in 68.0..98.0) {
            return Triple(extracted.first, extracted.second, true)
        }

        // District Fallback with hash-based spread (within ~4-8 km of district centroid)
        val normalizedDistrict = district.trim()
        val centroid = DISTRICT_COORDINATES[normalizedDistrict]
            ?: DISTRICT_COORDINATES.entries.find { it.key.equals(normalizedDistrict, ignoreCase = true) }?.value
            ?: Pair(26.9124, 75.7873) // Default to Jaipur centroid

        val hash = (schoolId.hashCode().toLong() and 0x7FFFFFFF).toDouble()
        val angle = (hash % 360) * (Math.PI / 180.0)
        val distanceDegrees = 0.015 + ((hash % 100) / 100.0) * 0.05 // ~1.5km to 7km spread

        val spreadLat = centroid.first + (sin(angle) * distanceDegrees)
        val spreadLng = centroid.second + (cos(angle) * distanceDegrees)

        return Triple(spreadLat, spreadLng, false)
    }

    /**
     * Converts a collection of Schools, Visits, and Tasks into unified Map Items
     */
    fun buildMapItems(
        schools: List<School>,
        tasks: List<Task>,
        visits: List<Visit>
    ): List<SchoolMapItem> {
        val completedVisitMap = visits.groupBy { it.schoolId }
        val taskMap = tasks.groupBy { it.schoolId }

        return schools.filter { !it.isDeleted }.map { school ->
            val schoolVisits = completedVisitMap[school.schoolId] ?: emptyList()
            val schoolTasks = taskMap[school.schoolId] ?: emptyList()

            val latestVisit = schoolVisits.maxByOrNull { it.submittedAt ?: it.completedAt ?: it.updatedAt }
            val activeTask = schoolTasks.firstOrNull { it.status != VisitStatus.REVIEWED }

            val hasCompleted = latestVisit != null || 
                               school.visitDate.isNotBlank() || 
                               schoolTasks.any { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }

            val status = when {
                hasCompleted -> VisitStatus.SUBMITTED
                activeTask != null -> VisitStatus.ASSIGNED
                else -> VisitStatus.CREATED
            }

            val coords = resolveCoordinates(
                schoolId = school.schoolId,
                latitude = school.latitude ?: activeTask?.latitude,
                longitude = school.longitude ?: activeTask?.longitude,
                mapLink = school.mapLink.ifBlank { activeTask?.mapLink ?: "" },
                district = school.districtName,
                state = school.stateName
            )

            val assignedEmpName = activeTask?.employeeName ?: ""
            val assignedDate = activeTask?.visitDate ?: ""
            val compDate = latestVisit?.visitDate?.ifBlank { school.visitDate } ?: school.visitDate

            SchoolMapItem(
                schoolId = school.schoolId,
                schoolName = school.schoolName,
                state = school.stateName,
                district = school.districtName,
                block = school.blockName,
                village = school.villageName,
                schoolType = school.schoolType,
                principalName = school.principalName,
                principalMobile = school.principalMobile,
                mapLink = school.mapLink,
                latitude = coords.first,
                longitude = coords.second,
                isExactCoordinate = coords.third,
                status = status,
                assignedEmployeeName = assignedEmpName,
                assignedDate = assignedDate,
                completedDate = compDate,
                visitId = latestVisit?.visitId ?: "",
                taskId = activeTask?.taskId ?: ""
            )
        }
    }

    /**
     * Calculates great-circle distance between two GPS points using Haversine formula (in meters)
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }

    /**
     * Formats distance into a human-readable Indian / Metric string
     */
    fun formatDistance(meters: Float?): String {
        if (meters == null) return "Distance unknown"
        return if (meters < 1000) {
            "${meters.toInt()} m"
        } else {
            String.format("%.1f km", meters / 1000.0)
        }
    }

    /**
     * Calculates compass cardinal direction from point A to point B
     */
    fun getCardinalDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        var brng = Math.toDegrees(atan2(y, x))
        brng = (brng + 360) % 360

        return when {
            brng in 337.5..360.0 || brng in 0.0..22.5 -> "North (उत्तर)"
            brng in 22.5..67.5 -> "North-East (उत्तर-पूर्व)"
            brng in 67.5..112.5 -> "East (पूर्व)"
            brng in 112.5..157.5 -> "South-East (दक्षिण-पूर्व)"
            brng in 157.5..202.5 -> "South (दक्षिण)"
            brng in 202.5..247.5 -> "South-West (दक्षिण-पश्चिम)"
            brng in 247.5..292.5 -> "West (पश्चिम)"
            else -> "North-West (उत्तर-पश्चिम)"
        }
    }

    /**
     * Live Location Flow from FusedLocationProviderClient
     */
    @SuppressLint("MissingPermission")
    fun getLocationFlow(context: Context): Flow<UserLocation?> = callbackFlow {
        val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        // Try getting last known location first for instantaneous startup
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    trySend(UserLocation(loc.latitude, loc.longitude, loc.accuracy, loc.time))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch last known location: ${e.message}")
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2500L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(UserLocation(loc.latitude, loc.longitude, loc.accuracy, loc.time))
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing", e)
            trySend(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed requesting location updates", e)
            trySend(null)
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(callback)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

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

    // Centroids for all districts of Rajasthan (English & Hindi) and other major Indian states
    private val DISTRICT_COORDINATES = mapOf(
        // Rajasthan Districts (English & Hindi)
        "Jaipur" to Pair(26.9124, 75.7873),
        "जयपुर" to Pair(26.9124, 75.7873),
        "Jaipur Rural" to Pair(26.9800, 75.8200),
        "जयपुर ग्रामीण" to Pair(26.9800, 75.8200),
        "Jodhpur" to Pair(26.2389, 73.0243),
        "जोधपुर" to Pair(26.2389, 73.0243),
        "Jodhpur Rural" to Pair(26.3500, 72.9500),
        "जोधपुर ग्रामीण" to Pair(26.3500, 72.9500),
        "Kota" to Pair(25.2138, 75.8648),
        "कोटा" to Pair(25.2138, 75.8648),
        "Bikaner" to Pair(28.0229, 73.3119),
        "बीकानेर" to Pair(28.0229, 73.3119),
        "Ajmer" to Pair(26.4499, 74.6399),
        "अजमेर" to Pair(26.4499, 74.6399),
        "Udaipur" to Pair(24.5854, 73.7125),
        "उदयपुर" to Pair(24.5854, 73.7125),
        "Alwar" to Pair(27.5530, 76.6346),
        "अलवर" to Pair(27.5530, 76.6346),
        "Bharatpur" to Pair(27.2152, 77.5030),
        "भरतपुर" to Pair(27.2152, 77.5030),
        "Bhilwara" to Pair(25.3407, 74.6313),
        "भीलवाड़ा" to Pair(25.3407, 74.6313),
        "Sri Ganganagar" to Pair(29.9038, 73.8772),
        "श्रीगंगानगर" to Pair(29.9038, 73.8772),
        "Sikar" to Pair(27.6094, 75.1398),
        "सीकर" to Pair(27.6094, 75.1398),
        "Pali" to Pair(25.7711, 73.3234),
        "पाली" to Pair(25.7711, 73.3234),
        "Tonk" to Pair(26.1664, 75.7885),
        "टोंक" to Pair(26.1664, 75.7885),
        "Churu" to Pair(28.2900, 74.9600),
        "चूरू" to Pair(28.2900, 74.9600),
        "Nagaur" to Pair(27.2000, 73.7400),
        "नागौर" to Pair(27.2000, 73.7400),
        "Jhunjhunu" to Pair(28.1289, 75.3995),
        "झुंझुनूं" to Pair(28.1289, 75.3995),
        "Barmer" to Pair(25.7521, 71.3967),
        "बाड़मेर" to Pair(25.7521, 71.3967),
        "Jaisalmer" to Pair(26.9157, 70.9083),
        "जैसलमेर" to Pair(26.9157, 70.9083),
        "Chittorgarh" to Pair(24.8887, 74.6269),
        "चित्तौड़गढ़" to Pair(24.8887, 74.6269),
        "Hanumangarh" to Pair(29.5800, 74.3200),
        "हनुमानगढ़" to Pair(29.5800, 74.3200),
        "Dausa" to Pair(26.8900, 76.3300),
        "दौसा" to Pair(26.8900, 76.3300),
        "Sawai Madhopur" to Pair(25.9928, 76.3711),
        "सवाई माधोपुर" to Pair(25.9928, 76.3711),
        "Jhalawar" to Pair(24.5973, 76.1610),
        "झालावाड़" to Pair(24.5973, 76.1610),
        "Bundi" to Pair(25.4400, 75.6400),
        "बूंदी" to Pair(25.4400, 75.6400),
        "Baran" to Pair(25.1000, 76.5100),
        "बारां" to Pair(25.1000, 76.5100),
        "Banswara" to Pair(23.5461, 74.4373),
        "बांसवाड़ा" to Pair(23.5461, 74.4373),
        "Dungarpur" to Pair(23.8365, 73.7147),
        "डूंगरपुर" to Pair(23.8365, 73.7147),
        "Rajsamand" to Pair(25.0440, 73.8820),
        "राजसमंद" to Pair(25.0440, 73.8820),
        "Sirohi" to Pair(24.8826, 72.8625),
        "सिरोही" to Pair(24.8826, 72.8625),
        "Jalore" to Pair(25.3450, 72.6150),
        "जालौर" to Pair(25.3450, 72.6150),
        "Dholpur" to Pair(26.7020, 77.8930),
        "धौलपुर" to Pair(26.7020, 77.8930),
        "Karauli" to Pair(26.4950, 77.0200),
        "करौली" to Pair(26.4950, 77.0200),
        "Pratapgarh" to Pair(24.0320, 74.7810),
        "प्रतापगढ़" to Pair(24.0320, 74.7810),
        "Balotra" to Pair(25.8300, 72.2400),
        "बालोतरा" to Pair(25.8300, 72.2400),
        "Beawar" to Pair(26.1000, 74.3200),
        "ब्यावर" to Pair(26.1000, 74.3200),
        "Didwana-Kuchaman" to Pair(27.4000, 74.5800),
        "डीडवाना-कुचामन" to Pair(27.4000, 74.5800),
        "Deeg" to Pair(27.4700, 77.3200),
        "डीग" to Pair(27.4700, 77.3200),
        "Dudu" to Pair(26.6800, 75.2300),
        "दूदू" to Pair(26.6800, 75.2300),
        "Gangapur City" to Pair(26.4700, 76.7200),
        "गंगापुर सिटी" to Pair(26.4700, 76.7200),
        "Kekri" to Pair(25.9700, 75.1500),
        "केकड़ी" to Pair(25.9700, 75.1500),
        "Khairthal-Tijara" to Pair(27.7900, 76.6200),
        "खैरथल-तिजारा" to Pair(27.7900, 76.6200),
        "Kotputli-Behror" to Pair(27.7000, 76.2000),
        "कोटपूतली-बहरोड़" to Pair(27.7000, 76.2000),
        "Neem Ka Thana" to Pair(27.7400, 75.7800),
        "नीम का थाना" to Pair(27.7400, 75.7800),
        "Phalodi" to Pair(27.1300, 72.3600),
        "फलौदी" to Pair(27.1300, 72.3600),
        "Salumber" to Pair(24.1300, 74.0400),
        "सलूम्बर" to Pair(24.1300, 74.0400),
        "Sanchore" to Pair(24.7500, 71.7700),
        "सांचौर" to Pair(24.7500, 71.7700),
        "Shahpura" to Pair(25.6300, 74.9300),
        "शाहपुरा" to Pair(25.6300, 74.9300),
        "Anupgarh" to Pair(29.1900, 73.2100),
        "अनूपगढ़" to Pair(29.1900, 73.2100),

        // Uttar Pradesh Major Districts
        "Lucknow" to Pair(26.8467, 80.9462),
        "लखनऊ" to Pair(26.8467, 80.9462),
        "Kanpur" to Pair(26.4499, 80.3319),
        "Kanpur Nagar" to Pair(26.4499, 80.3319),
        "कानपुर" to Pair(26.4499, 80.3319),
        "Agra" to Pair(27.1767, 78.0081),
        "आगरा" to Pair(27.1767, 78.0081),
        "Varanasi" to Pair(25.3176, 82.9739),
        "वाराणसी" to Pair(25.3176, 82.9739),
        "Prayagraj" to Pair(25.4358, 81.8463),
        "प्रयागराज" to Pair(25.4358, 81.8463),
        "Gorakhpur" to Pair(26.7606, 83.3732),
        "गोरखपुर" to Pair(26.7606, 83.3732),
        "Ghaziabad" to Pair(28.6692, 77.4538),
        "गाजियाबाद" to Pair(28.6692, 77.4538),
        "Gautam Buddha Nagar" to Pair(28.5355, 77.3910),
        "Noida" to Pair(28.5355, 77.3910),
        "Meerut" to Pair(28.9845, 77.7064),
        "मेरठ" to Pair(28.9845, 77.7064),
        "Bareilly" to Pair(28.3670, 79.4304),
        "बरेली" to Pair(28.3670, 79.4304),
        "Aligarh" to Pair(27.8974, 78.0880),
        "अलीगढ़" to Pair(27.8974, 78.0880),
        "Moradabad" to Pair(28.8386, 78.7733),
        "मुरादाबाद" to Pair(28.8386, 78.7733),
        "Saharanpur" to Pair(29.9671, 77.5510),
        "सहारनपुर" to Pair(29.9671, 77.5510),
        "Ayodhya" to Pair(26.7922, 82.1998),
        "अयोध्या" to Pair(26.7922, 82.1998),
        "Mathura" to Pair(27.4924, 77.6737),
        "मथुरा" to Pair(27.4924, 77.6737),
        "Jhansi" to Pair(25.4484, 78.5685),
        "झांसी" to Pair(25.4484, 78.5685),

        // Madhya Pradesh Major Districts
        "Bhopal" to Pair(23.2599, 77.4126),
        "भोपाल" to Pair(23.2599, 77.4126),
        "Indore" to Pair(22.7196, 75.8577),
        "इंदौर" to Pair(22.7196, 75.8577),
        "Gwalior" to Pair(26.2183, 78.1828),
        "ग्वालियर" to Pair(26.2183, 78.1828),
        "Jabalpur" to Pair(23.1815, 79.9864),
        "जबलपुर" to Pair(23.1815, 79.9864),
        "Ujjain" to Pair(23.1765, 75.7885),
        "उज्जैन" to Pair(23.1765, 75.7885),
        "Sagar" to Pair(23.8388, 78.7378),
        "सागर" to Pair(23.8388, 78.7378),
        "Rewa" to Pair(24.5362, 81.3037),
        "रीवा" to Pair(24.5362, 81.3037),
        "Satna" to Pair(24.6005, 80.8322),
        "सतना" to Pair(24.6005, 80.8322),
        "Ratlam" to Pair(23.3315, 75.0367),
        "रतलाम" to Pair(23.3315, 75.0367),
        "Mandsaur" to Pair(24.0722, 75.0688),
        "मंदसौर" to Pair(24.0722, 75.0688),
        "Neemuch" to Pair(24.4764, 74.8732),
        "नीमच" to Pair(24.4764, 74.8732),

        // Delhi & Haryana Major Districts
        "Delhi" to Pair(28.6139, 77.2090),
        "New Delhi" to Pair(28.6139, 77.2090),
        "दिल्ली" to Pair(28.6139, 77.2090),
        "Gurugram" to Pair(28.4595, 77.0266),
        "Gurgaon" to Pair(28.4595, 77.0266),
        "गुरुग्राम" to Pair(28.4595, 77.0266),
        "Faridabad" to Pair(28.4089, 77.3178),
        "फरीदाबाद" to Pair(28.4089, 77.3178),
        "Panipat" to Pair(29.3909, 76.9635),
        "पानीपत" to Pair(29.3909, 76.9635),
        "Ambala" to Pair(30.3782, 76.7767),
        "अंबाला" to Pair(30.3782, 76.7767),
        "Hisar" to Pair(29.1492, 75.7217),
        "हिसार" to Pair(29.1492, 75.7217),
        "Karnal" to Pair(29.6857, 76.9905),
        "करनाल" to Pair(29.6857, 76.9905),
        "Rohtak" to Pair(28.8955, 76.6066),
        "रोहतक" to Pair(28.8955, 76.6066),
        "Rewari" to Pair(28.1828, 76.6191),
        "रेवाड़ी" to Pair(28.1828, 76.6191),

        // Gujarat Major Districts
        "Ahmedabad" to Pair(23.0225, 72.5714),
        "अहमदाबाद" to Pair(23.0225, 72.5714),
        "Surat" to Pair(21.1702, 72.8311),
        "सूरत" to Pair(21.1702, 72.8311),
        "Vadodara" to Pair(22.3072, 73.1812),
        "वडोदरा" to Pair(22.3072, 73.1812),
        "Rajkot" to Pair(22.3039, 70.8022),
        "राजकोट" to Pair(22.3039, 70.8022),
        "Gandhinagar" to Pair(23.2156, 72.6369),
        "गांधीनगर" to Pair(23.2156, 72.6369),

        // Bihar Major Districts
        "Patna" to Pair(25.5941, 85.1376),
        "पटना" to Pair(25.5941, 85.1376),
        "Gaya" to Pair(24.7914, 85.0002),
        "गया" to Pair(24.7914, 85.0002),
        "Muzaffarpur" to Pair(26.1209, 85.3647),
        "मुजफ्फरपुर" to Pair(26.1209, 85.3647),
        "Bhagalpur" to Pair(25.2425, 86.9842),
        "भागलपुर" to Pair(25.2425, 86.9842),

        // Maharashtra & Punjab
        "Mumbai" to Pair(19.0760, 72.8777),
        "मुंबई" to Pair(19.0760, 72.8777),
        "Pune" to Pair(18.5204, 73.8567),
        "पुणे" to Pair(18.5204, 73.8567),
        "Nagpur" to Pair(21.1458, 79.0882),
        "नागपुर" to Pair(21.1458, 79.0882),
        "Chandigarh" to Pair(30.7333, 76.7794),
        "चंडीगढ़" to Pair(30.7333, 76.7794),
        "Ludhiana" to Pair(30.9010, 75.8573),
        "लुधियाना" to Pair(30.9010, 75.8573),
        "Amritsar" to Pair(31.6340, 74.8723),
        "अमृतसर" to Pair(31.6340, 74.8723)
    )

    private val STATE_COORDINATES = mapOf(
        "Rajasthan" to Pair(26.9124, 75.7873),
        "राजस्थान" to Pair(26.9124, 75.7873),
        "Uttar Pradesh" to Pair(26.8467, 80.9462),
        "उत्तर प्रदेश" to Pair(26.8467, 80.9462),
        "Madhya Pradesh" to Pair(23.2599, 77.4126),
        "मध्य प्रदेश" to Pair(23.2599, 77.4126),
        "Delhi" to Pair(28.6139, 77.2090),
        "दिल्ली" to Pair(28.6139, 77.2090),
        "Haryana" to Pair(29.0588, 76.0856),
        "हरियाणा" to Pair(29.0588, 76.0856),
        "Gujarat" to Pair(22.2587, 71.1924),
        "गुजरात" to Pair(22.2587, 71.1924),
        "Bihar" to Pair(25.0961, 85.3131),
        "बिहार" to Pair(25.0961, 85.3131),
        "Punjab" to Pair(31.1471, 75.3412),
        "पंजाब" to Pair(31.1471, 75.3412),
        "Maharashtra" to Pair(19.7515, 75.7139),
        "महाराष्ट्र" to Pair(19.7515, 75.7139)
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

        // District Fallback with normalized lookup and hash-based spread within that specific district
        val normState = IndiaLocationData.normalizeState(state)
        val normDistrict = IndiaLocationData.normalizeDistrict(normState, district).trim()

        val centroid = DISTRICT_COORDINATES[normDistrict]
            ?: DISTRICT_COORDINATES[district.trim()]
            ?: DISTRICT_COORDINATES.entries.find { it.key.equals(normDistrict, ignoreCase = true) }?.value
            ?: DISTRICT_COORDINATES.entries.find { it.key.equals(district.trim(), ignoreCase = true) }?.value
            ?: STATE_COORDINATES[normState]
            ?: STATE_COORDINATES[state.trim()]
            ?: STATE_COORDINATES.entries.find { it.key.equals(normState, ignoreCase = true) }?.value
            ?: Pair(26.9124, 75.7873) // Jaipur centroid default

        val hash = (schoolId.hashCode().toLong() and 0x7FFFFFFF).toDouble()
        val angle = (hash % 360) * (Math.PI / 180.0)
        val distanceDegrees = 0.012 + ((hash % 100) / 100.0) * 0.035 // ~1.2km to 4.5km spread around district center

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

            val effectiveMapLink = if (school.mapLink.isNotBlank()) school.mapLink else (activeTask?.mapLink ?: "")
            val effectiveDistrict = if (school.districtName.isNotBlank()) school.districtName else (activeTask?.district ?: "")
            val effectiveState = if (school.stateName.isNotBlank()) school.stateName else (activeTask?.state ?: "Rajasthan")

            val coords = resolveCoordinates(
                schoolId = school.schoolId,
                latitude = school.latitude ?: activeTask?.latitude,
                longitude = school.longitude ?: activeTask?.longitude,
                mapLink = effectiveMapLink,
                district = effectiveDistrict,
                state = effectiveState
            )

            val assignedEmpName = activeTask?.employeeName ?: ""
            val assignedDate = activeTask?.visitDate ?: ""
            val compDate = latestVisit?.visitDate?.ifBlank { school.visitDate } ?: school.visitDate

            SchoolMapItem(
                schoolId = school.schoolId,
                schoolName = school.schoolName,
                state = effectiveState,
                district = effectiveDistrict,
                block = school.blockName.ifBlank { activeTask?.block ?: "" },
                village = school.villageName.ifBlank { activeTask?.villageName ?: "" },
                schoolType = school.schoolType.ifBlank { activeTask?.schoolType ?: "" },
                principalName = school.principalName.ifBlank { activeTask?.principalName ?: "" },
                principalMobile = school.principalMobile.ifBlank { activeTask?.principalMobile ?: "" },
                mapLink = effectiveMapLink,
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

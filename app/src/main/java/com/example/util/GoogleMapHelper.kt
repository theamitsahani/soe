package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.data.model.Task
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object GoogleMapHelper {

    private const val TAG = "GoogleMapHelper"

    /**
     * Attempts to parse latitude and longitude from various Google Maps link formats,
     * place URLs, URL-encoded strings, DMS strings, or raw coordinate strings.
     */
    fun extractCoordinates(rawInput: String?): Pair<Double, Double>? {
        if (rawInput.isNullOrBlank()) return null
        val decoded = try {
            java.net.URLDecoder.decode(rawInput.trim(), StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            rawInput.trim()
        }

        // 1. Check for @lat,lng format (e.g., @26.912433,75.787271 or @26.912433,75.787271,17z)
        val atRegex = Regex("""@([+-]?\d{1,2}(?:\.\d+)?),([+-]?\d{1,3}(?:\.\d+)?)""")
        atRegex.find(decoded)?.let {
            val lat = it.groupValues[1].toDoubleOrNull()
            val lng = it.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                return Pair(lat, lng)
            }
        }

        // 2. Check for Google Maps place data (!3dlat!4dlng or !8m2!3dlat!4dlng)
        val placeDataRegex = Regex("""!3d([+-]?\d{1,2}(?:\.\d+)?)(?:.*?)[!&]4d([+-]?\d{1,3}(?:\.\d+)?)""")
        placeDataRegex.find(decoded)?.let {
            val lat = it.groupValues[1].toDoubleOrNull()
            val lng = it.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                return Pair(lat, lng)
            }
        }

        // 3. Check for query parameters: q=lat,lng or query=lat,lng or daddr=lat,lng or destination=lat,lng or ll=lat,lng or sll=lat,lng
        val paramRegex = Regex("""(?:q|query|daddr|destination|ll|sll|center|loc|geo)[:=]([+-]?\d{1,2}(?:\.\d+)?)[,\s/+]+([+-]?\d{1,3}(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        paramRegex.find(decoded)?.let {
            val lat = it.groupValues[1].toDoubleOrNull()
            val lng = it.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                return Pair(lat, lng)
            }
        }

        // 4. Check for /place/lat,lng or /dir/lat,lng or /search/lat,lng
        val pathCoordRegex = Regex("""/(?:place|dir|search|maps)/([+-]?\d{1,2}(?:\.\d+)?)[,\s/+]+([+-]?\d{1,3}(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        pathCoordRegex.find(decoded)?.let {
            val lat = it.groupValues[1].toDoubleOrNull()
            val lng = it.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                return Pair(lat, lng)
            }
        }

        // 5. Check for Degree Minute Second (DMS) format: e.g. 26°54'44.6"N 75°48'00.5"E
        val dmsResult = parseDmsCoordinates(decoded)
        if (dmsResult != null && isValidLatLng(dmsResult.first, dmsResult.second)) {
            return dmsResult
        }

        // 6. Check for plain raw coordinate string: e.g. "26.912433, 75.787271" or "26.912433,75.787271" or "26.912433 75.787271"
        val generalRegex = Regex("""([+-]?\d{1,2}(?:\.\d+)?)[,\s/+]+([+-]?\d{1,3}(?:\.\d+)?)""")
        generalRegex.findAll(decoded).forEach { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lng = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                return Pair(lat, lng)
            }
        }

        return null
    }

    private fun isValidLatLng(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0 && (lat != 0.0 || lng != 0.0)
    }

    /**
     * Parses DMS coordinates like 26°54'44.6"N 75°48'00.5"E into decimal degrees
     */
    private fun parseDmsCoordinates(text: String): Pair<Double, Double>? {
        try {
            val dmsPattern = Regex("""(\d{1,2})[°\s]+(\d{1,2})['\s]+([\d.]+)?["\s]*([NSEWnsew])[,\s]+(\d{1,3})[°\s]+(\d{1,2})['\s]+([\d.]+)?["\s]*([NSEWnsew])""")
            val match = dmsPattern.find(text) ?: return null
            val (d1, m1, s1, dir1, d2, m2, s2, dir2) = match.destructured

            fun convert(d: String, m: String, s: String?, dir: String): Double {
                val deg = d.toDoubleOrNull() ?: 0.0
                val min = m.toDoubleOrNull() ?: 0.0
                val sec = s?.toDoubleOrNull() ?: 0.0
                var result = deg + (min / 60.0) + (sec / 3600.0)
                if (dir.equals("S", ignoreCase = true) || dir.equals("W", ignoreCase = true)) {
                    result = -result
                }
                return result
            }

            val val1 = convert(d1, m1, s1, dir1)
            val val2 = convert(d2, m2, s2, dir2)

            return if (dir1.equals("N", true) || dir1.equals("S", true)) {
                Pair(val1, val2)
            } else {
                Pair(val2, val1)
            }
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Follows HTTP redirects for shortened URLs (maps.app.goo.gl, goo.gl/maps, bit.ly)
     * and extracts coordinates from the final resolved URL.
     */
    fun extractCoordinatesWithNetwork(rawInput: String?): Pair<Double, Double>? {
        if (rawInput.isNullOrBlank()) return null
        val direct = extractCoordinates(rawInput)
        if (direct != null) return direct

        val trimmed = rawInput.trim()
        if (trimmed.contains("goo.gl") || trimmed.contains("maps.app") || trimmed.contains("bit.ly")) {
            val resolvedUrl = resolveShortUrlSync(trimmed)
            if (!resolvedUrl.isNullOrBlank()) {
                return extractCoordinates(resolvedUrl)
            }
        }
        return null
    }

    private fun resolveShortUrlSync(shortUrl: String): String? {
        var currentUrl = if (!shortUrl.startsWith("http://") && !shortUrl.startsWith("https://")) "https://$shortUrl" else shortUrl
        try {
            for (i in 0..3) {
                val urlObj = java.net.URL(currentUrl)
                val conn = urlObj.openConnection() as java.net.HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 3500
                conn.readTimeout = 3500
                conn.requestMethod = "HEAD"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) break
                    currentUrl = loc
                    if (!currentUrl.contains("goo.gl") && !currentUrl.contains("maps.app")) {
                        break
                    }
                } else {
                    conn.disconnect()
                    break
                }
            }
            return currentUrl
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve short URL: ${e.message}")
            return null
        }
    }

    /**
     * Formats a clean search query or destination address for a school
     */
    fun formatSchoolAddress(schoolName: String, village: String = "", block: String = "", district: String = "", state: String = "Rajasthan"): String {
        val parts = listOf(schoolName, village, block, district, state)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return parts.joinToString(", ")
    }

    /**
     * Convenience method to start navigation for a Task with fallback to School data
     */
    fun startNavigation(context: Context, task: Task, school: com.example.data.model.School? = null) {
        val mapLink = task.mapLink.ifBlank { school?.mapLink ?: "" }
        val lat = task.latitude ?: school?.latitude
        val lng = task.longitude ?: school?.longitude
        val sName = task.schoolName.ifBlank { school?.schoolName ?: "" }
        val village = task.villageName.ifBlank { school?.villageName ?: "" }
        val block = task.block.ifBlank { school?.blockName ?: "" }
        val district = task.district.ifBlank { school?.districtName ?: "" }
        val state = task.state.ifBlank { school?.stateName ?: "Rajasthan" }
        val addr = formatSchoolAddress(sName, village, block, district, state)
        startNavigation(context, mapLink, lat, lng, sName, addr)
    }

    /**
     * Convenience method to view location on map for a Task with fallback to School data
     */
    fun openLocationOnMap(context: Context, task: Task, school: com.example.data.model.School? = null) {
        val mapLink = task.mapLink.ifBlank { school?.mapLink ?: "" }
        val lat = task.latitude ?: school?.latitude
        val lng = task.longitude ?: school?.longitude
        val sName = task.schoolName.ifBlank { school?.schoolName ?: "" }
        val village = task.villageName.ifBlank { school?.villageName ?: "" }
        val block = task.block.ifBlank { school?.blockName ?: "" }
        val district = task.district.ifBlank { school?.districtName ?: "" }
        val state = task.state.ifBlank { school?.stateName ?: "Rajasthan" }
        val addr = formatSchoolAddress(sName, village, block, district, state)
        openLocationOnMap(context, mapLink, lat, lng, sName, addr)
    }

    /**
     * Starts Google Maps navigation directly using the admin-provided map link.
     * If a map link is given by the admin, that exact link/URL is opened immediately.
     */
    fun startNavigation(
        context: Context,
        mapLink: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        schoolName: String = "",
        address: String = ""
    ) {
        try {
            val cleanLink = mapLink?.trim()
            val navUri: Uri = if (!cleanLink.isNullOrBlank()) {
                if (cleanLink.startsWith("http://") || cleanLink.startsWith("https://") || cleanLink.startsWith("geo:")) {
                    Uri.parse(cleanLink)
                } else if (cleanLink.contains("maps.app.goo.gl") || cleanLink.contains("goo.gl") || cleanLink.contains("maps.google")) {
                    Uri.parse("https://$cleanLink")
                } else {
                    val coords = extractCoordinates(cleanLink)
                    if (coords != null) {
                        Uri.parse("google.navigation:q=${coords.first},${coords.second}")
                    } else {
                        val encoded = URLEncoder.encode(cleanLink, StandardCharsets.UTF_8.toString())
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
                    }
                }
            } else if (latitude != null && longitude != null) {
                Uri.parse("google.navigation:q=$latitude,$longitude")
            } else {
                val query = formatSchoolAddress(schoolName, district = address)
                val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
            }

            val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                mapIntent.setPackage("com.google.android.apps.maps")
                context.startActivity(mapIntent)
            } catch (e: ActivityNotFoundException) {
                mapIntent.setPackage(null)
                context.startActivity(mapIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch navigation", e)
            Toast.makeText(context, "Could not launch map: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the school location on Google Maps using admin provided link or location.
     */
    fun openLocationOnMap(
        context: Context,
        mapLink: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        schoolName: String = "",
        address: String = ""
    ) {
        startNavigation(context, mapLink, latitude, longitude, schoolName, address)
    }

    /**
     * Builds a Google Maps Directions URL with multiple stops/waypoints.
     * Useful when an employee is assigned 2, 3, or more visits in a day and wants
     * to navigate through all assigned schools in sequence.
     */
    fun buildMultiStopRouteUrl(tasks: List<Task>): String {
        if (tasks.isEmpty()) return "https://www.google.com/maps"
        if (tasks.size == 1) {
            val t = tasks[0]
            val coords = if (t.latitude != null && t.longitude != null) {
                "${t.latitude},${t.longitude}"
            } else {
                val c = extractCoordinates(t.mapLink)
                if (c != null) "${c.first},${c.second}"
                else formatSchoolAddress(t.schoolName, t.villageName, t.block, t.district, t.state)
            }
            val encoded = URLEncoder.encode(coords, StandardCharsets.UTF_8.toString())
            return "https://www.google.com/maps/dir/?api=1&destination=$encoded"
        }

        // Multiple tasks: Destination is the last school, intermediate are waypoints
        val destinations = tasks.map { t ->
            if (t.latitude != null && t.longitude != null) {
                "${t.latitude},${t.longitude}"
            } else {
                val c = extractCoordinates(t.mapLink)
                if (c != null) "${c.first},${c.second}"
                else formatSchoolAddress(t.schoolName, t.villageName, t.block, t.district, t.state)
            }
        }

        val lastDestination = URLEncoder.encode(destinations.last(), StandardCharsets.UTF_8.toString())
        val intermediateWaypoints = destinations.dropLast(1).joinToString("|") {
            URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
        }

        return "https://www.google.com/maps/dir/?api=1&destination=$lastDestination&waypoints=$intermediateWaypoints"
    }

    /**
     * Launches the full multi-stop navigation route in Google Maps
     */
    fun startMultiStopNavigation(context: Context, tasks: List<Task>) {
        if (tasks.isEmpty()) {
            Toast.makeText(context, "No assigned visits to route.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val routeUrl = buildMultiStopRouteUrl(tasks)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(routeUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                intent.setPackage("com.google.android.apps.maps")
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                intent.setPackage(null)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch multi-stop navigation", e)
            Toast.makeText(context, "Could not open route: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

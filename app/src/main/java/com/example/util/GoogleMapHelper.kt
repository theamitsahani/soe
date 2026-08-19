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
     * Attempts to parse latitude and longitude from various Google Maps link formats
     * or raw coordinate strings.
     */
    fun extractCoordinates(rawInput: String?): Pair<Double, Double>? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()

        try {
            // Pattern 1: "@26.912433,75.787271" or "q=26.912433,75.787271" or "ll=26.912433,75.787271" or "geo:26.912433,75.787271"
            val coordRegex = Pattern.compile("[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)[,\\s/]+[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)")
            val matcher = coordRegex.matcher(trimmed)
            if (matcher.find()) {
                val matchGroup = matcher.group(0)
                val parts = matchGroup.split(Regex("[,\\s/]+"))
                if (parts.size >= 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                        return Pair(lat, lng)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting coordinates from $rawInput", e)
        }

        return null
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

package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.GoogleMapHelper
import com.example.util.IndiaLocationData
import com.example.util.LocationHelper
import com.example.util.MapHtmlBuilder
import com.example.util.SchoolMapItem
import com.example.util.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ViewDisplayMode {
    MAP,
    LIST
}

enum class StatusFilterOption(val label: String, val color: Color) {
    ALL("All (सभी)", Indigo600),
    COMPLETED("Completed (पूर्ण)", Emerald600),
    ASSIGNED("Assigned (असाइन)", Amber600),
    PENDING("Pending (बाकी)", Rose600)
}

enum class RadiusOption(val label: String, val radiusKm: Float?) {
    ALL("Any Distance", null),
    KM_5("5 km", 5f),
    KM_10("10 km", 10f),
    KM_25("25 km", 25f),
    KM_50("50 km", 50f)
}

class WebAppInterface(private val onSchoolSelected: (String) -> Unit) {
    @JavascriptInterface
    fun onSchoolClick(schoolId: String) {
        onSchoolSelected(schoolId)
    }

    @JavascriptInterface
    fun onMapReady() {
        // Map loaded
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolInteractiveMapView(
    schools: List<School>,
    tasks: List<Task> = emptyList(),
    visits: List<Visit> = emptyList(),
    onStartVisit: ((Task) -> Unit)? = null,
    onViewDetails: ((SchoolMapItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(StatusFilterOption.ALL) }
    var selectedRadius by remember { mutableStateOf(RadiusOption.ALL) }
    var selectedDistrict by remember { mutableStateOf("All") }
    var selectedBlock by remember { mutableStateOf("All") }
    var displayMode by remember { mutableStateOf(ViewDisplayMode.MAP) }
    var isSatelliteLayer by remember { mutableStateOf(false) }

    var selectedSchoolId by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<UserLocation?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            Toast.makeText(context, "Location permission granted. Finding nearby schools...", Toast.LENGTH_SHORT).show()
        }
    }

    // Collect live GPS location when permission is active
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationHelper.getLocationFlow(context).collect { loc ->
                userLocation = loc
                if (loc != null) {
                    webViewRef?.evaluateJavascript(
                        "if (typeof updateUserLocation === 'function') { updateUserLocation(${loc.latitude}, ${loc.longitude}, ${loc.accuracy}); }",
                        null
                    )
                }
            }
        }
    }

    // Build unified map items
    val allMapItems = remember(schools, tasks, visits) {
        LocationHelper.buildMapItems(schools, tasks, visits)
    }

    // Available Districts & Blocks
    val districtList = remember(allMapItems) {
        listOf("All") + allMapItems.map { it.district }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val blockList = remember(allMapItems, selectedDistrict) {
        val filtered = if (selectedDistrict == "All") allMapItems else allMapItems.filter { it.district.equals(selectedDistrict, ignoreCase = true) }
        listOf("All") + filtered.map { it.block }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Calculate distance to each school from current user location & apply filters
    val filteredMapItems = remember(allMapItems, searchQuery, selectedStatus, selectedRadius, selectedDistrict, selectedBlock, userLocation) {
        val q = searchQuery.trim().lowercase()
        val currentLoc = userLocation

        allMapItems.map { item ->
            val dist = if (currentLoc != null) {
                LocationHelper.calculateDistanceMeters(
                    currentLoc.latitude,
                    currentLoc.longitude,
                    item.latitude,
                    item.longitude
                )
            } else null
            item.copy(distanceFromUserMeters = dist)
        }.filter { item ->
            // Status check
            val matchesStatus = when (selectedStatus) {
                StatusFilterOption.ALL -> true
                StatusFilterOption.COMPLETED -> item.status == VisitStatus.SUBMITTED || item.status == VisitStatus.REVIEWED
                StatusFilterOption.ASSIGNED -> item.status == VisitStatus.ASSIGNED
                StatusFilterOption.PENDING -> item.status != VisitStatus.SUBMITTED && item.status != VisitStatus.REVIEWED && item.status != VisitStatus.ASSIGNED
            }
            if (!matchesStatus) return@filter false

            // District & Block check
            if (selectedDistrict != "All" && !item.district.equals(selectedDistrict, ignoreCase = true)) return@filter false
            if (selectedBlock != "All" && !item.block.equals(selectedBlock, ignoreCase = true)) return@filter false

            // Radius check
            val maxRadiusKm = selectedRadius.radiusKm
            if (maxRadiusKm != null) {
                val distMeters = item.distanceFromUserMeters
                if (distMeters == null || (distMeters / 1000f) > maxRadiusKm) return@filter false
            }

            // Search query
            if (q.isNotEmpty()) {
                val match = item.schoolName.lowercase().contains(q) ||
                            item.village.lowercase().contains(q) ||
                            item.block.lowercase().contains(q) ||
                            item.district.lowercase().contains(q) ||
                            item.principalName.lowercase().contains(q) ||
                            item.assignedEmployeeName.lowercase().contains(q)
                if (!match) return@filter false
            }

            true
        }.sortedWith(
            compareBy(
                { it.distanceFromUserMeters ?: Float.MAX_VALUE },
                { it.schoolName }
            )
        )
    }

    val selectedSchoolItem = remember(selectedSchoolId, allMapItems, userLocation) {
        allMapItems.find { it.schoolId == selectedSchoolId }?.let { item ->
            val currentLoc = userLocation
            val dist = if (currentLoc != null) {
                LocationHelper.calculateDistanceMeters(
                    currentLoc.latitude,
                    currentLoc.longitude,
                    item.latitude,
                    item.longitude
                )
            } else null
            item.copy(distanceFromUserMeters = dist)
        }
    }

    // Summary counts
    val totalCount = filteredMapItems.size
    val completedCount = filteredMapItems.count { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }
    val assignedCount = filteredMapItems.count { it.status == VisitStatus.ASSIGNED }
    val pendingCount = filteredMapItems.count { it.status != VisitStatus.SUBMITTED && it.status != VisitStatus.REVIEWED && it.status != VisitStatus.ASSIGNED }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Controls Bar (Search + Radius + Status Chips)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Search Row + View Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search school, village, block...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate500, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Indigo600,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(48.dp)
                        )

                        // Toggle Mode: Map vs List
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { displayMode = ViewDisplayMode.MAP },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Map View",
                                        tint = if (displayMode == ViewDisplayMode.MAP) Indigo600 else Slate500
                                    )
                                }
                                IconButton(
                                    onClick = { displayMode = ViewDisplayMode.LIST },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "List View",
                                        tint = if (displayMode == ViewDisplayMode.LIST) Indigo600 else Slate500
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Filters Scroll Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusFilterOption.entries.forEach { opt ->
                            val isSelected = selectedStatus == opt
                            val countStr = when (opt) {
                                StatusFilterOption.ALL -> " ($totalCount)"
                                StatusFilterOption.COMPLETED -> " ($completedCount)"
                                StatusFilterOption.ASSIGNED -> " ($assignedCount)"
                                StatusFilterOption.PENDING -> " ($pendingCount)"
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedStatus = opt
                                    webViewRef?.evaluateJavascript(
                                        "if (typeof filterMarkers === 'function') { filterMarkers('${opt.name}', '${searchQuery}', '${selectedDistrict}', '${selectedBlock}'); }",
                                        null
                                    )
                                },
                                label = {
                                    Text(
                                        text = "${opt.label}$countStr",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = opt.color.copy(alpha = 0.15f),
                                    selectedLabelColor = opt.color,
                                    containerColor = Color.Transparent,
                                    labelColor = Slate700
                                )
                            )
                        }
                    }

                    // Radius / Distance Filter Row (Nearby Mode)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = Indigo600,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Radius:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        }

                        RadiusOption.entries.forEach { rad ->
                            val isSelected = selectedRadius == rad
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Indigo600 else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable {
                                    if (rad.radiusKm != null && !hasLocationPermission) {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                    selectedRadius = rad
                                }
                            ) {
                                Text(
                                    text = rad.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area: Map View OR Nearby List View
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (displayMode == ViewDisplayMode.MAP) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        userLocation?.let { loc ->
                                            view?.evaluateJavascript(
                                                "if (typeof updateUserLocation === 'function') { updateUserLocation(${loc.latitude}, ${loc.longitude}, ${loc.accuracy}); }",
                                                null
                                            )
                                        }
                                    }
                                }

                                addJavascriptInterface(WebAppInterface { schoolId ->
                                    scope.launch(Dispatchers.Main) {
                                        selectedSchoolId = schoolId
                                    }
                                }, "AndroidBridge")

                                val html = MapHtmlBuilder.buildMapHtml(
                                    items = filteredMapItems,
                                    userLocation = userLocation,
                                    initialLat = userLocation?.latitude ?: 26.9124,
                                    initialLng = userLocation?.longitude ?: 75.7873
                                )
                                loadDataWithBaseURL("https://appassets.androidplatform.net", html, "text/html", "UTF-8", null)
                                webViewRef = this
                            }
                        },
                        update = { view ->
                            val html = MapHtmlBuilder.buildMapHtml(
                                items = filteredMapItems,
                                userLocation = userLocation,
                                initialLat = userLocation?.latitude ?: 26.9124,
                                initialLng = userLocation?.longitude ?: 75.7873
                            )
                            view.loadDataWithBaseURL("https://appassets.androidplatform.net", html, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating Map Action Buttons (GPS, Satellite Layer, Reset Zoom)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // My Location FAB
                        FloatingActionButton(
                            onClick = {
                                if (!hasLocationPermission) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    val loc = userLocation
                                    if (loc != null) {
                                        webViewRef?.evaluateJavascript(
                                            "if (typeof centerOnUser === 'function') { centerOnUser(${loc.latitude}, ${loc.longitude}, 15); }",
                                            null
                                        )
                                        Toast.makeText(context, "Centered to live GPS location", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            containerColor = Color.White,
                            contentColor = Indigo600,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(22.dp))
                        }

                        // Satellite Layer Switcher FAB
                        FloatingActionButton(
                            onClick = {
                                isSatelliteLayer = !isSatelliteLayer
                                val layerType = if (isSatelliteLayer) "satellite" else "osm"
                                webViewRef?.evaluateJavascript(
                                    "if (typeof switchLayer === 'function') { switchLayer('$layerType'); }",
                                    null
                                )
                            },
                            containerColor = Color.White,
                            contentColor = if (isSatelliteLayer) Emerald600 else Slate700,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = "Layers", modifier = Modifier.size(22.dp))
                        }

                        // Reset View Bounds FAB
                        FloatingActionButton(
                            onClick = {
                                webViewRef?.evaluateJavascript(
                                    "if (typeof resetMapBounds === 'function') { resetMapBounds(); }",
                                    null
                                )
                            },
                            containerColor = Color.White,
                            contentColor = Slate700,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Bounds", modifier = Modifier.size(22.dp))
                        }
                    }
                } else {
                    // Nearby List View (Sorted by Distance)
                    NearbySchoolsListView(
                        schools = filteredMapItems,
                        userLocation = userLocation,
                        onSchoolClick = { item ->
                            selectedSchoolId = item.schoolId
                            displayMode = ViewDisplayMode.MAP
                            webViewRef?.evaluateJavascript(
                                "if (typeof centerOnSchool === 'function') { centerOnSchool('${item.schoolId}'); }",
                                null
                            )
                        },
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        hasLocationPermission = hasLocationPermission
                    )
                }

                // Bottom Selected School Card
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedSchoolItem != null && displayMode == ViewDisplayMode.MAP,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    selectedSchoolItem?.let { schoolItem ->
                        SchoolMapDetailCard(
                            item = schoolItem,
                            userLocation = userLocation,
                            onDismiss = { selectedSchoolId = null },
                            onStartVisit = onStartVisit?.let { callback ->
                                {
                                    val matchedTask = tasks.find { it.schoolId == schoolItem.schoolId }
                                    if (matchedTask != null) {
                                        callback(matchedTask)
                                    } else {
                                        // Synthetic task for ad-hoc visit
                                        val newTask = Task(
                                            taskId = "task_adhoc_${schoolItem.schoolId}_${System.currentTimeMillis()}",
                                            visitId = "",
                                            schoolId = schoolItem.schoolId,
                                            employeeId = "",
                                            employeeName = "",
                                            schoolName = schoolItem.schoolName,
                                            principalName = schoolItem.principalName,
                                            principalMobile = schoolItem.principalMobile,
                                            villageName = schoolItem.village,
                                            schoolType = schoolItem.schoolType,
                                            state = schoolItem.state,
                                            district = schoolItem.district,
                                            block = schoolItem.block,
                                            visitDate = "",
                                            status = VisitStatus.ASSIGNED,
                                            mapLink = schoolItem.mapLink,
                                            latitude = schoolItem.latitude,
                                            longitude = schoolItem.longitude,
                                            createdAt = System.currentTimeMillis()
                                        )
                                        callback(newTask)
                                    }
                                }
                            },
                            onViewDetails = {
                                onViewDetails?.invoke(schoolItem)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NearbySchoolsListView(
    schools: List<SchoolMapItem>,
    userLocation: UserLocation?,
    onSchoolClick: (SchoolMapItem) -> Unit,
    onRequestLocationPermission: () -> Unit,
    hasLocationPermission: Boolean
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hasLocationPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable GPS for Nearby Distance",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = "Allow location permission to sort schools by nearest distance and get turn-by-turn guidance.",
                                fontSize = 12.sp,
                                color = Color(0xFF3B82F6)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestLocationPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (schools.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No schools matched this filter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                        Text(
                            text = "Try increasing the radius or clearing the search query.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            }
        }

        items(schools, key = { it.schoolId }) { school ->
            val statusColor = when (school.status) {
                VisitStatus.SUBMITTED, VisitStatus.REVIEWED -> Emerald600
                VisitStatus.ASSIGNED -> Amber600
                else -> Rose600
            }
            val statusBg = when (school.status) {
                VisitStatus.SUBMITTED, VisitStatus.REVIEWED -> Emerald100
                VisitStatus.ASSIGNED -> Amber100
                else -> Rose100
            }
            val statusText = when (school.status) {
                VisitStatus.SUBMITTED, VisitStatus.REVIEWED -> "Completed (${school.completedDate.ifBlank { "Done" }})"
                VisitStatus.ASSIGNED -> "Assigned to ${school.assignedEmployeeName.ifBlank { "Staff" }}"
                else -> "Pending / Unassigned"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSchoolClick(school) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (school.distanceFromUserMeters != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = Indigo600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = LocationHelper.formatDistance(school.distanceFromUserMeters),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Indigo600
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = school.schoolName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${school.village.ifBlank { "Village" }} • ${school.block}, ${school.district}",
                        fontSize = 12.sp,
                        color = Slate500
                    )

                    if (school.principalName.isNotBlank() || school.principalMobile.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Slate500, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${school.principalName} ${if (school.principalMobile.isNotBlank()) "(${school.principalMobile})" else ""}",
                                fontSize = 12.sp,
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                GoogleMapHelper.startNavigation(
                                    context = context,
                                    mapLink = school.mapLink,
                                    latitude = school.latitude,
                                    longitude = school.longitude,
                                    schoolName = school.schoolName,
                                    address = "${school.village}, ${school.block}, ${school.district}"
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Navigate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (school.principalMobile.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${school.principalMobile.trim()}"))
                                    context.startActivity(dialIntent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald600)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Emerald600)
                            }
                        }

                        OutlinedButton(
                            onClick = { onSchoolClick(school) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp), tint = Indigo600)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Map View", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Indigo600)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolMapDetailCard(
    item: SchoolMapItem,
    userLocation: UserLocation?,
    onDismiss: () -> Unit,
    onStartVisit: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val statusColor = when (item.status) {
        VisitStatus.SUBMITTED, VisitStatus.REVIEWED -> Emerald600
        VisitStatus.ASSIGNED -> Amber600
        else -> Rose600
    }
    val statusBg = when (item.status) {
        VisitStatus.SUBMITTED, VisitStatus.REVIEWED -> Emerald100
        VisitStatus.ASSIGNED -> Amber100
        else -> Rose100
    }
    val statusLabel = when (item.status) {
        VisitStatus.SUBMITTED, VisitStatus.REVIEWED -> "🟢 Completed (विज़िट पूर्ण)"
        VisitStatus.ASSIGNED -> "🟡 Assigned to ${item.assignedEmployeeName.ifBlank { "Staff" }}"
        else -> "🔴 Pending / Unassigned (विज़िट बाकी)"
    }

    val distanceText = remember(item.distanceFromUserMeters, userLocation) {
        if (item.distanceFromUserMeters != null && userLocation != null) {
            val dist = LocationHelper.formatDistance(item.distanceFromUserMeters)
            val dir = LocationHelper.getCardinalDirection(userLocation.latitude, userLocation.longitude, item.latitude, item.longitude)
            "📍 $dist • $dir"
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Status Chip + Distance + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (distanceText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = distanceText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Indigo600,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // School Name
            Text(
                text = item.schoolName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Location Info
            Text(
                text = "Village: ${item.village.ifBlank { "N/A" }} • ${item.block}, ${item.district}",
                fontSize = 12.sp,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )

            if (item.principalName.isNotBlank() || item.principalMobile.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Indigo600, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Principal: ${item.principalName.ifBlank { "Not Specified" }}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                    if (item.principalMobile.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${item.principalMobile})",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: 1. Turn-by-Turn Navigation, 2. Call Principal, 3. Start/View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        GoogleMapHelper.startNavigation(
                            context = context,
                            mapLink = item.mapLink,
                            latitude = item.latitude,
                            longitude = item.longitude,
                            schoolName = item.schoolName,
                            address = "${item.village}, ${item.block}, ${item.district}"
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.weight(1.3f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Navigation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (item.principalMobile.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.principalMobile.trim()}"))
                            context.startActivity(dialIntent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = Emerald600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                    }
                }

                if (onStartVisit != null && (item.status != VisitStatus.SUBMITTED && item.status != VisitStatus.REVIEWED)) {
                    Button(
                        onClick = onStartVisit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Visit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (onViewDetails != null) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = Indigo600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                    }
                }
            }
        }
    }
}

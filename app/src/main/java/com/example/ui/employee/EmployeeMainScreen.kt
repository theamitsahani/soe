package com.example.ui.employee

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.ui.components.NotificationBellIcon
import com.example.ui.components.NotificationDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.SyncStatusBanner
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.GoogleMapHelper

enum class EmployeeNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAY_TASKS("Today", Icons.Default.Home),
    VISIT_MAP("Visit Map", Icons.Default.LocationOn),
    UPCOMING("Upcoming", Icons.Default.Assignment),
    COMPLETED("Reports", Icons.Default.CheckCircle),
    PROFILE("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeMainScreen(
    employeeUser: User,
    tasks: List<Task>,
    completedVisits: List<Visit>,
    schools: List<School> = emptyList(),
    isOnline: Boolean,
    pendingSyncCount: Int,
    isSyncing: Boolean = false,
    onSyncClick: () -> Unit,
    onStartVisit: (Task) -> Unit,
    onEditVisit: (Visit) -> Unit = {},
    notifications: List<AppNotification> = emptyList(),
    onMarkAllNotificationsRead: () -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onTabSelected: ((Int) -> Unit)? = null,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = EmployeeNavTab.entries
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    // BUG FIX: was distinctBy { "${it.schoolId}_${it.employeeId}" }, which hid an employee's
    // own legitimate second report (a re-visit to the same school) from their own "My Reports"
    // list — they'd submit it successfully but then be unable to find/view it again. Dedup of
    // true duplicate documents already happens upstream at sync time; visitId is the real key.
    val cleanCompletedVisits = remember(completedVisits) {
        completedVisits.distinctBy { it.visitId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Indigo600),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Mission Gyan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            Text("Field Officer Portal", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                actions = {
                    NotificationBellIcon(
                        unreadCount = notifications.count { !it.isRead },
                        onClick = { showNotificationDialog = true }
                    )
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Slate700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            onTabSelected?.invoke(index)
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Indigo600,
                            selectedTextColor = Indigo600,
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500,
                            indicatorColor = Color(0xFFEEF2FF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate100)
        ) {
            SyncStatusBanner(
                isOnline = isOnline,
                pendingCount = pendingSyncCount,
                isSyncing = isSyncing,
                onSyncClick = onSyncClick
            )

            val todayTasks = remember(tasks) {
                tasks.filter { task ->
                    isTaskForTodayOrPast(task.visitDate)
                }
            }

            val upcomingTasks = remember(tasks) {
                tasks.filter { task ->
                    isTaskUpcoming(task.visitDate)
                }
            }

            when (tabs[selectedTab]) {
                EmployeeNavTab.TODAY_TASKS -> {
                    TasksListSection(
                        title = "Today's Assigned Tasks (${todayTasks.size})",
                        tasks = todayTasks,
                        completedVisits = cleanCompletedVisits,
                        schools = schools,
                        onStartVisit = onStartVisit,
                        onOpenMapTab = {
                            val mapTabIndex = tabs.indexOf(EmployeeNavTab.VISIT_MAP)
                            if (mapTabIndex >= 0) selectedTab = mapTabIndex
                        }
                    )
                }
                EmployeeNavTab.VISIT_MAP -> {
                    EmployeeMapSection(
                        tasks = tasks,
                        completedVisits = cleanCompletedVisits,
                        schools = schools,
                        onStartVisit = onStartVisit
                    )
                }
                EmployeeNavTab.UPCOMING -> {
                    TasksListSection(
                        title = "Upcoming Field Tasks (${upcomingTasks.size})",
                        tasks = upcomingTasks,
                        completedVisits = cleanCompletedVisits,
                        schools = schools,
                        onStartVisit = onStartVisit,
                        onOpenMapTab = {
                            val mapTabIndex = tabs.indexOf(EmployeeNavTab.VISIT_MAP)
                            if (mapTabIndex >= 0) selectedTab = mapTabIndex
                        }
                    )
                }
                EmployeeNavTab.COMPLETED -> {
                    CompletedVisitsSection(
                        visits = cleanCompletedVisits,
                        schools = schools,
                        onEditVisit = onEditVisit
                    )
                }
                EmployeeNavTab.PROFILE -> {
                    EmployeeProfileSection(user = employeeUser, onLogout = { showLogoutDialog = true })
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout / लॉगआउट पुष्टि", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Do you want to logout? / क्या आप लॉगआउट करना चाहते हैं?", color = Slate700) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Yes / हाँ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("No / नहीं", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showNotificationDialog) {
        NotificationDialog(
            notifications = notifications,
            onDismiss = { showNotificationDialog = false },
            onMarkAllRead = onMarkAllNotificationsRead,
            onClearAll = onClearAllNotifications
        )
    }
}

@Composable
fun TasksListSection(
    title: String,
    tasks: List<Task>,
    completedVisits: List<Visit> = emptyList(),
    schools: List<School> = emptyList(),
    onStartVisit: (Task) -> Unit,
    onOpenMapTab: (() -> Unit)? = null
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                if (tasks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF)
                    ) {
                        Text(
                            text = "${tasks.size} Visits",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Multi-Stop Route & Navigation Card for assigned visits
        if (tasks.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Route & Google Maps Navigation",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (tasks.size > 1) "${tasks.size} assigned visits scheduled in route" else "Direct navigation to assigned school",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { GoogleMapHelper.startMultiStopNavigation(context, tasks) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1.3f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (tasks.size > 1) "Start Route Navigation" else "Start Navigation",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (onOpenMapTab != null) {
                                OutlinedButton(
                                    onClick = onOpenMapTab,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF38BDF8)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "View Map",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No tasks assigned for today", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Text("Check back later or contact your admin", fontSize = 12.sp, color = Slate500)
                    }
                }
            }
        } else {
            items(tasks) { task ->
                val matchedSchool = remember(task.schoolId, schools) {
                    schools.find { it.schoolId == task.schoolId }
                }
                TaskCardItem(
                    task = task,
                    school = matchedSchool,
                    completedVisits = completedVisits,
                    onStartVisit = { onStartVisit(task) }
                )
            }
        }
    }
}

@Composable
fun TaskCardItem(
    task: Task,
    school: School?,
    completedVisits: List<Visit> = emptyList(),
    onStartVisit: () -> Unit
) {
    val context = LocalContext.current
    var showSchoolDetailsDialog by remember { mutableStateOf(false) }

    val isSubmitted = task.status == VisitStatus.SUBMITTED || 
                      task.status == VisitStatus.REVIEWED || 
                      completedVisits.any { it.schoolId == task.schoolId || (task.visitId.isNotBlank() && it.visitId == task.visitId) }

    // Outer Preview & Details Display Priority: task.field -> school.field -> Fallback
    val principalNameDisplay = remember(task, school) {
        task.principalName.ifBlank { school?.principalName ?: "" }.ifBlank { "Not Specified" }
    }
    val principalMobileDisplay = remember(task, school) {
        task.principalMobile.ifBlank { school?.principalMobile ?: "" }
    }
    val villageNameDisplay = remember(task, school) {
        task.villageName.ifBlank { school?.villageName ?: "" }.ifBlank { "Not Specified" }
    }
    val blockDisplay = remember(task, school) {
        task.block.ifBlank { school?.blockName ?: "" }.ifBlank { "Not Specified" }
    }
    val districtDisplay = remember(task, school) {
        task.district.ifBlank { school?.districtName ?: "" }.ifBlank { "Not Specified" }
    }
    val schoolTypeDisplay = remember(task, school) {
        task.schoolType.ifBlank { school?.schoolType ?: "" }.ifBlank { "School Details" }
    }
    val mapLinkDisplay = remember(task, school) {
        task.mapLink.ifBlank { school?.mapLink ?: "" }
    }
    val hasCoordinates = task.latitude != null || school?.latitude != null || mapLinkDisplay.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSchoolDetailsDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Status Chip & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(statusName = if (isSubmitted) VisitStatus.SUBMITTED.name else task.status.name)
                Text(
                    text = "Date: ${task.visitDate}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. School Name (विद्यालय का नाम)
            Text(
                text = task.schoolName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Quick Preview: Principal Name & Village/Location Info
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Principal: $principalNameDisplay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Village: $villageNameDisplay • $blockDisplay, $districtDisplay",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    }

                    if (hasCoordinates) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Map GPS Linked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald600
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Navigate (Google Maps), View Details & Start Visit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direct Google Maps Turn-by-Turn Navigation Button
                Button(
                    onClick = { GoogleMapHelper.startNavigation(context, task, school) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F766E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Navigate",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Navigate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // View Details Button
                OutlinedButton(
                    onClick = { showSchoolDetailsDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Indigo600
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Indigo600
                    )
                }

                // Start School Visit Button
                Button(
                    onClick = onStartVisit,
                    enabled = !isSubmitted,
                    shape = RoundedCornerShape(10.dp),
                    colors = if (isSubmitted) {
                        ButtonDefaults.buttonColors(containerColor = Emerald600, disabledContainerColor = Emerald600, disabledContentColor = Color.White)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = Indigo600)
                    },
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isSubmitted) "Done" else "Start",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Detailed School Information Dialog with Call Action & Google Maps Integration
    if (showSchoolDetailsDialog) {
        val s = school
        val pMobile = principalMobileDisplay.trim()

        AlertDialog(
            onDismissRequest = { showSchoolDetailsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.schoolName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Navy900,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = schoolTypeDisplay,
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Maps Navigation Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Emerald600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Google Maps Navigation & Location",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Address: $villageNameDisplay, $blockDisplay, $districtDisplay, ${task.state.ifBlank { s?.stateName ?: "Rajasthan" }}",
                                fontSize = 11.sp,
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            )

                            if (mapLinkDisplay.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Map Link: $mapLinkDisplay",
                                    fontSize = 10.sp,
                                    color = Color(0xFF2563EB),
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { GoogleMapHelper.startNavigation(context, task, s) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    modifier = Modifier.weight(1.3f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start Navigation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { GoogleMapHelper.openLocationOnMap(context, task, s) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = Indigo600,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Pin", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Indigo600)
                                }
                            }
                        }
                    }

                    // Principal Name
                    SchoolInfoRow(
                        label = "Principal Name (प्रधानाचार्य)",
                        value = principalNameDisplay
                    )

                    // Principal Mobile with Call Option
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Principal Mobile (मोबाइल नंबर)",
                            fontSize = 11.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (pMobile.isNotBlank()) pMobile else "Not Provided",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )

                            if (pMobile.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$pMobile"))
                                        context.startActivity(dialIntent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call (कॉल)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Village Name
                    SchoolInfoRow(
                        label = "Village Name (गांव का नाम)",
                        value = villageNameDisplay
                    )

                    // Block & District
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SchoolInfoRow(
                                label = "Block (ब्लॉक)",
                                value = blockDisplay
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SchoolInfoRow(
                                label = "District (जिला)",
                                value = districtDisplay
                            )
                        }
                    }

                    // State
                    SchoolInfoRow(
                        label = "State (राज्य)",
                        value = task.state.ifBlank { s?.stateName ?: "Rajasthan" }
                    )

                    // Scheduled Visit Date
                    SchoolInfoRow(
                        label = "Scheduled Visit Date (विज़िट तिथि)",
                        value = task.visitDate.ifBlank { s?.visitDate ?: "Not scheduled" }
                    )

                    // Task Notes if any
                    if (task.notes.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Admin Instructions / Notes",
                                fontSize = 11.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.notes,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isSubmitted) {
                    Button(
                        onClick = {
                            showSchoolDetailsDialog = false
                            onStartVisit()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Visit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSchoolDetailsDialog = false }) {
                    Text("Close", fontSize = 12.sp, color = Slate700)
                }
            }
        )
    }
}

/**
 * Dedicated Visit Map & Route Optimization Section for Field Officers.
 * Displays all assigned visits as ordered stops and provides Google Maps navigation.
 */
@Composable
fun EmployeeMapSection(
    tasks: List<Task>,
    completedVisits: List<Visit> = emptyList(),
    schools: List<School> = emptyList(),
    onStartVisit: (Task) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Assigned Visits Map & Route",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = "Google Maps Navigation & Multi-Stop Itinerary",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = "${tasks.size} Visits",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Assigned Visits on Map",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When admin assigns visits with map links, all your destinations will appear here with route navigation.",
                            fontSize = 12.sp,
                            color = Slate500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Master Navigation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Full Route Navigation",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Optimized itinerary connecting all ${tasks.size} stops in Google Maps",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { GoogleMapHelper.startMultiStopNavigation(context, tasks) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Complete Route in Google Maps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // List of Ordered Stops
            items(tasks.size) { index ->
                val task = tasks[index]
                val matchedSchool = remember(task.schoolId, schools) {
                    schools.find { it.schoolId == task.schoolId }
                }
                val isSubmitted = task.status == VisitStatus.SUBMITTED || 
                                  task.status == VisitStatus.REVIEWED || 
                                  completedVisits.any { it.schoolId == task.schoolId || (task.visitId.isNotBlank() && it.visitId == task.visitId) }

                val pName = task.principalName.ifBlank { matchedSchool?.principalName ?: "" }.ifBlank { "Principal" }
                val pMobile = task.principalMobile.ifBlank { matchedSchool?.principalMobile ?: "" }.trim()
                val village = task.villageName.ifBlank { matchedSchool?.villageName ?: "" }.ifBlank { "Village Area" }
                val block = task.block.ifBlank { matchedSchool?.blockName ?: "" }
                val district = task.district.ifBlank { matchedSchool?.districtName ?: "" }
                val state = task.state.ifBlank { matchedSchool?.stateName ?: "Rajasthan" }
                val mapLink = task.mapLink.ifBlank { matchedSchool?.mapLink ?: "" }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Stop Badge & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSubmitted) Color(0xFFDCFCE7) else Color(0xFFEEF2FF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSubmitted) Emerald600 else Indigo600,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Stop #${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSubmitted) Emerald600 else Indigo600
                                    )
                                }
                            }

                            StatusChip(statusName = if (isSubmitted) VisitStatus.SUBMITTED.name else task.status.name)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // School Name
                        Text(
                            text = task.schoolName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Location Details Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Indigo600,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Address: $village, $block, $district, $state",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate700
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Indigo600,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Principal: $pName",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate700
                                    )
                                    if (pMobile.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "($pMobile)",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                if (mapLink.isNotBlank() || task.latitude != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = Emerald600,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Google Maps Coordinates Active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald600
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions Row: 1. Turn-by-Turn Navigation, 2. Open Pin, 3. Call/Start Visit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Turn-by-Turn Navigation Button
                            Button(
                                onClick = { GoogleMapHelper.startNavigation(context, task, matchedSchool) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F766E),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1.3f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Navigate",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Start Navigation",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // View Pin on Map
                            OutlinedButton(
                                onClick = { GoogleMapHelper.openLocationOnMap(context, task, matchedSchool) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = Indigo600,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Open Pin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Indigo600
                                )
                            }

                            // Start Visit Button
                            Button(
                                onClick = { onStartVisit(task) },
                                enabled = !isSubmitted,
                                shape = RoundedCornerShape(10.dp),
                                colors = if (isSubmitted) {
                                    ButtonDefaults.buttonColors(containerColor = Emerald600, disabledContainerColor = Emerald600, disabledContentColor = Color.White)
                                } else {
                                    ButtonDefaults.buttonColors(containerColor = Indigo600)
                                },
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isSubmitted) "Done" else "Visit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchoolInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Slate500,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Navy900
        )
    }
}

@Composable
fun CompletedVisitsSection(
    visits: List<Visit>,
    schools: List<School> = emptyList(),
    onEditVisit: (Visit) -> Unit = {}
) {
    var selectedVisitForDetail by remember { mutableStateOf<Visit?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your Completed Visits", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("All submitted visit reports", fontSize = 11.sp, color = Slate500)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = "${visits.size} Total",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (visits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No completed visit reports yet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Submitted visit reports will appear here", fontSize = 12.sp, color = Slate500)
                    }
                }
            }
        } else {
            items(visits) { visit ->
                val matchedSchool = remember(visit.schoolId, schools) {
                    schools.find { it.schoolId == visit.schoolId }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = visit.schoolName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                modifier = Modifier.weight(1f)
                            )
                            StatusChip(statusName = visit.status.name)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${visit.district} • ${visit.block}", fontSize = 12.sp, color = Slate500)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Visit Date: ${visit.visitDate}", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { selectedVisitForDetail = visit },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Visit Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Detail View Dialog
    selectedVisitForDetail?.let { visit ->
        val matchedSchool = remember(visit.schoolId, schools) {
            schools.find { it.schoolId == visit.schoolId }
        }

        VisitDetailDialog(
            visit = visit,
            school = matchedSchool,
            onDismiss = { selectedVisitForDetail = null },
            isEditable = false,
            onEditClick = null
        )
    }
}

@Composable
fun EmployeeProfileSection(user: User, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Indigo600),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(user.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text(user.email, fontSize = 13.sp, color = Slate500)
                if (user.mobile.isNotBlank()) {
                    Text("Mobile: ${user.mobile}", fontSize = 13.sp, color = Slate500)
                }

                Spacer(modifier = Modifier.height(12.dp))

                StatusChip(statusName = user.status.name)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun isTaskForTodayOrPast(visitDate: String): Boolean {
    if (visitDate.isBlank()) return true
    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    val dateFormats = listOf(
        "dd-MMM-yyyy",
        "dd MMM yyyy",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd",
        "d-MMM-yyyy",
        "d MMM yyyy",
        "d/M/yyyy"
    )

    for (pattern in dateFormats) {
        try {
            val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH).apply { isLenient = true }
            val parsed = sdf.parse(visitDate.trim())
            if (parsed != null) {
                val taskCal = java.util.Calendar.getInstance().apply {
                    time = parsed
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                return !taskCal.after(today)
            }
        } catch (_: Exception) {}
    }
    // Default to showing in today's tasks so no task is missed
    return true
}

private fun isTaskUpcoming(visitDate: String): Boolean {
    if (visitDate.isBlank()) return false
    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    val dateFormats = listOf(
        "dd-MMM-yyyy",
        "dd MMM yyyy",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd",
        "d-MMM-yyyy",
        "d MMM yyyy",
        "d/M/yyyy"
    )

    for (pattern in dateFormats) {
        try {
            val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH).apply { isLenient = true }
            val parsed = sdf.parse(visitDate.trim())
            if (parsed != null) {
                val taskCal = java.util.Calendar.getInstance().apply {
                    time = parsed
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                return taskCal.after(today)
            }
        } catch (_: Exception) {}
    }
    return false
}

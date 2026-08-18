package com.example.ui.employee

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassEmptyState
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassSectionHeader
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.NotificationBellIcon
import com.example.ui.components.NotificationDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.SyncStatusBanner
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryGradient
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

enum class EmployeeNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAY_TASKS("Today's Tasks", Icons.Default.Home),
    UPCOMING("Upcoming Tasks", Icons.Default.Assignment),
    COMPLETED("Completed Visits", Icons.Default.CheckCircle),
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

    val cleanCompletedVisits = remember(completedVisits) {
        completedVisits.distinctBy { it.visitId }
    }

    Scaffold(
        topBar = {
            Surface(
                color = GlassSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorderSubtle),
                shadowElevation = 2.dp
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Indigo600, Color(0xFF7C3AED))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("MG", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Mission Gyan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                Text("Field Officer Portal", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.SemiBold)
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        bottomBar = {
            Surface(
                color = GlassSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorderSubtle),
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
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
        }
    ) { innerPadding ->
        LiquidGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                            onStartVisit = onStartVisit
                        )
                    }
                    EmployeeNavTab.UPCOMING -> {
                        TasksListSection(
                            title = "Upcoming Field Tasks (${upcomingTasks.size})",
                            tasks = upcomingTasks,
                            completedVisits = cleanCompletedVisits,
                            schools = schools,
                            onStartVisit = onStartVisit
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
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = { Text("Confirm Logout / लॉगआउट पुष्टि", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Do you want to logout? / क्या आप लॉगआउट करना चाहते हैं?", color = Slate700) },
            confirmButton = {
                GlassButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    text = "Yes / हाँ",
                    gradient = Brush.linearGradient(listOf(Red600, Color(0xFFE11D48)))
                )
            },
            dismissButton = {
                GlassOutlinedButton(
                    onClick = { showLogoutDialog = false },
                    text = "No / नहीं",
                    textColor = Slate700
                )
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
    onStartVisit: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassSectionHeader(
                title = title,
                badgeText = "${tasks.size} Tasks"
            )
        }

        if (tasks.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.School,
                    title = "No tasks assigned for this period",
                    subtitle = "Check back later or contact your administrator."
                )
            }
        } else {
            items(tasks, key = { it.taskId.ifBlank { it.schoolId } }) { task ->
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

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showSchoolDetailsDialog = true },
        containerColor = GlassSurfaceElevated,
        contentPadding = PaddingValues(14.dp),
        elevation = 2.dp
    ) {
        // Top Row: Status Chip & Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(statusName = if (isSubmitted) VisitStatus.SUBMITTED.name else task.status.name)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = task.visitDate,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // School Name
        Text(
            text = task.schoolName,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Preview Glass Surface
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Slate200.copy(alpha = 0.6f)),
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Principal: $principalNameDisplay",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Village: $villageNameDisplay",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Slate500
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassOutlinedButton(
                onClick = { showSchoolDetailsDialog = true },
                text = "View Details",
                icon = Icons.Default.Visibility,
                modifier = Modifier.weight(1f)
            )

            GlassButton(
                onClick = onStartVisit,
                enabled = !isSubmitted,
                text = if (isSubmitted) "Completed" else "Start Visit",
                icon = if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                gradient = if (isSubmitted) Brush.linearGradient(listOf(Emerald600, Emerald500)) else PrimaryGradient,
                modifier = Modifier.weight(1.2f)
            )
        }
    }

    if (showSchoolDetailsDialog) {
        val s = school
        val pMobile = principalMobileDisplay.trim()

        AlertDialog(
            onDismissRequest = { showSchoolDetailsDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.schoolName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = schoolTypeDisplay,
                            fontSize = 11.5.sp,
                            color = Indigo600,
                            fontWeight = FontWeight.SemiBold
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
                    SchoolInfoRow(
                        label = "Principal Name (प्रधानाचार्य)",
                        value = principalNameDisplay
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
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
                                color = Slate900
                            )

                            if (pMobile.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Emerald600,
                                    modifier = Modifier.clickable {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$pMobile"))
                                        context.startActivity(dialIntent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    SchoolInfoRow(
                        label = "Village Name (गांव का नाम)",
                        value = villageNameDisplay
                    )

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

                    SchoolInfoRow(
                        label = "State (राज्य)",
                        value = task.state.ifBlank { s?.stateName ?: "Rajasthan" }
                    )

                    SchoolInfoRow(
                        label = "Scheduled Visit Date (विज़िट तिथि)",
                        value = task.visitDate.ifBlank { s?.visitDate ?: "Not scheduled" }
                    )

                    if (task.notes.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Slate200, RoundedCornerShape(10.dp))
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
                    GlassButton(
                        onClick = {
                            showSchoolDetailsDialog = false
                            onStartVisit()
                        },
                        text = "Start Visit",
                        icon = Icons.Default.PlayArrow
                    )
                }
            },
            dismissButton = {
                GlassOutlinedButton(
                    onClick = { showSchoolDetailsDialog = false },
                    text = "Close",
                    textColor = Slate700
                )
            }
        )
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
            color = Slate900
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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassSectionHeader(
                title = "Your Completed Visits",
                subtitle = "All submitted visit reports",
                badgeText = "${visits.size} Total"
            )
        }

        if (visits.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = "No completed visit reports yet",
                    subtitle = "Submitted visit reports will appear here."
                )
            }
        } else {
            items(visits, key = { it.visitId }) { visit ->
                val matchedSchool = remember(visit.schoolId, schools) {
                    schools.find { it.schoolId == visit.schoolId }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedVisitForDetail = visit },
                    containerColor = GlassSurfaceElevated,
                    contentPadding = PaddingValues(14.dp),
                    elevation = 1.5.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = visit.schoolName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusChip(statusName = visit.status.name)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${visit.district} • ${visit.block}", fontSize = 12.sp, color = Slate500)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Visit Date: ${visit.visitDate}", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))
                    GlassOutlinedButton(
                        onClick = { selectedVisitForDetail = visit },
                        text = "View Visit Details",
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = GlassSurfaceElevated,
            contentPadding = PaddingValues(24.dp),
            elevation = 3.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(PrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(user.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(2.dp))
                Text(user.email, fontSize = 13.sp, color = Slate500)
                if (user.mobile.isNotBlank()) {
                    Text("Mobile: ${user.mobile}", fontSize = 13.sp, color = Slate500)
                }

                Spacer(modifier = Modifier.height(12.dp))

                StatusChip(statusName = user.status.name)

                Spacer(modifier = Modifier.height(26.dp))

                GlassOutlinedButton(
                    onClick = onLogout,
                    text = "Sign Out (लॉगआउट)",
                    icon = Icons.Default.ExitToApp,
                    textColor = Red600,
                    modifier = Modifier.fillMaxWidth()
                )
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

package com.example.ui.employee

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
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
import com.example.ui.components.GlassLevel
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassOutlinedButton
import com.example.ui.components.LiquidGlassStatusBadge
import com.example.ui.components.NotificationBellIcon
import com.example.ui.components.NotificationDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.SyncStatusBanner
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassIndigoGradient
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red500
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal500

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

    LiquidGlassBackground(
        modifier = Modifier.fillMaxSize(),
        enableOrbs = true
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = Color.White.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, GlassBorderLight),
                    shadowElevation = 4.dp
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            ambientColor = Indigo600.copy(alpha = 0.25f),
                                            spotColor = Indigo600.copy(alpha = 0.35f)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GlassIndigoGradient)
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("MG", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Mission Gyan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            bottomBar = {
                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, GlassBorderLight),
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
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate500,
                                    indicatorColor = Indigo50
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
        LiquidGlassDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = "Confirm Logout / लॉगआउट पुष्टि",
            confirmButton = {
                LiquidGlassButton(
                    text = "Yes / हाँ",
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    gradient = Brush.horizontalGradient(listOf(Red500, Red600)),
                    height = 42.dp
                )
            },
            dismissButton = {
                LiquidGlassOutlinedButton(
                    text = "No / नहीं",
                    onClick = { showLogoutDialog = false },
                    height = 42.dp
                )
            }
        ) {
            Text("Do you want to logout? / क्या आप लॉगआउट करना चाहते हैं?", color = Slate700, fontSize = 14.sp)
        }
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy900,
                    letterSpacing = 0.2.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Indigo50)
                        .border(BorderStroke(1.dp, Indigo500.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${tasks.size} Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    level = GlassLevel.LEVEL_1_CARD,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Indigo50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Indigo600, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("No tasks assigned for today", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Check back later or contact your admin", fontSize = 13.sp, color = Slate500)
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

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSchoolDetailsDialog = true },
        level = GlassLevel.LEVEL_2_SURFACE,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column {
            // Top Row: Status Chip & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassStatusBadge(status = if (isSubmitted) VisitStatus.SUBMITTED.name else task.status.name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = task.visitDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // School Name
            Text(
                text = task.schoolName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Preview Surface (Principal & Village)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.65f))
                    .border(BorderStroke(1.dp, GlassBorderLight), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Indigo50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Indigo600,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Principal: $principalNameDisplay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Village: $villageNameDisplay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassOutlinedButton(
                    text = "View Details",
                    onClick = { showSchoolDetailsDialog = true },
                    icon = Icons.Default.Visibility,
                    modifier = Modifier.weight(1f),
                    height = 40.dp
                )

                LiquidGlassButton(
                    text = if (isSubmitted) "Completed" else "Start Visit",
                    onClick = onStartVisit,
                    enabled = !isSubmitted,
                    icon = if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                    gradient = if (isSubmitted) {
                        Brush.horizontalGradient(listOf(Emerald500, Emerald600))
                    } else {
                        GlassIndigoGradient
                    },
                    modifier = Modifier.weight(1.3f),
                    height = 40.dp
                )
            }
        }
    }

    if (showSchoolDetailsDialog) {
        val s = school
        val pMobile = principalMobileDisplay.trim()

        LiquidGlassDialog(
            onDismissRequest = { showSchoolDetailsDialog = false },
            title = task.schoolName,
            confirmButton = {
                if (!isSubmitted) {
                    LiquidGlassButton(
                        text = "Start Visit",
                        onClick = {
                            showSchoolDetailsDialog = false
                            onStartVisit()
                        },
                        icon = Icons.Default.PlayArrow,
                        gradient = GlassIndigoGradient,
                        height = 42.dp
                    )
                }
            },
            dismissButton = {
                LiquidGlassOutlinedButton(
                    text = "Close",
                    onClick = { showSchoolDetailsDialog = false },
                    height = 42.dp
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = schoolTypeDisplay,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Indigo600
                )

                SchoolInfoRow(
                    label = "Principal Name (प्रधानाचार्य)",
                    value = principalNameDisplay
                )

                // Principal Mobile with Instant Call Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF0FDF4).copy(alpha = 0.9f))
                        .border(BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
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
                                LiquidGlassButton(
                                    text = "Call (कॉल)",
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$pMobile"))
                                        context.startActivity(dialIntent)
                                    },
                                    icon = Icons.Default.Phone,
                                    gradient = Brush.horizontalGradient(listOf(Emerald500, Emerald600)),
                                    height = 32.dp,
                                    shape = RoundedCornerShape(10.dp)
                                )
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(BorderStroke(1.dp, GlassBorderLight), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
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
        Spacer(modifier = Modifier.height(2.dp))
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your Completed Visits", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Navy900)
                    Text("All submitted visit reports", fontSize = 12.sp, color = Slate500)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFECFDF5))
                        .border(BorderStroke(1.dp, Emerald500.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${visits.size} Total",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600
                    )
                }
            }
        }

        if (visits.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    level = GlassLevel.LEVEL_1_CARD,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("No completed visit reports yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Submitted visit reports will appear here", fontSize = 13.sp, color = Slate500)
                    }
                }
            }
        } else {
            items(visits) { visit ->
                val matchedSchool = remember(visit.schoolId, schools) {
                    schools.find { it.schoolId == visit.schoolId }
                }

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    level = GlassLevel.LEVEL_2_SURFACE,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column {
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
                            LiquidGlassStatusBadge(status = visit.status.name)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${visit.district} • ${visit.block}", fontSize = 12.sp, color = Slate500)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Visit Date: ${visit.visitDate}", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassOutlinedButton(
                            text = "View Visit Details",
                            onClick = { selectedVisitForDetail = visit },
                            icon = Icons.Default.Visibility,
                            modifier = Modifier.fillMaxWidth(),
                            height = 38.dp
                        )
                    }
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
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            level = GlassLevel.LEVEL_3_FLOATING,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            ambientColor = Indigo600.copy(alpha = 0.3f),
                            spotColor = Indigo600.copy(alpha = 0.4f)
                        )
                        .clip(CircleShape)
                        .background(GlassIndigoGradient)
                        .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)), CircleShape),
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

                Text(user.name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Navy900)
                Text(user.email, fontSize = 13.sp, color = Slate500)
                if (user.mobile.isNotBlank()) {
                    Text("Mobile: ${user.mobile}", fontSize = 13.sp, color = Slate500)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LiquidGlassStatusBadge(status = user.status.name)

                Spacer(modifier = Modifier.height(24.dp))

                LiquidGlassOutlinedButton(
                    text = "Sign Out",
                    onClick = onLogout,
                    icon = Icons.Default.ExitToApp,
                    contentColor = Red600,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp
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


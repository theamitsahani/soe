package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.ExcelExportHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUser: User,
    users: List<User>,
    schools: List<School>,
    tasks: List<Task>,
    visits: List<Visit>,
    districts: List<String>,
    onAssignNewTask: (Task) -> Unit,
    onAddNewSchool: (School) -> Unit,
    onDeleteTask: (String) -> Unit,
    onViewVisitDetail: (Visit) -> Unit,
    onSyncRemote: () -> Unit,
    onSwitchUser: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Visits, 1 = Tasks, 2 = Schools, 3 = Officers

    var showAssignTaskDialog by remember { mutableStateOf(false) }
    var showAddSchoolDialog by remember { mutableStateOf(false) }
    var selectedDistrictFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val fieldOfficers = remember(users) { users.filter { it.role == UserRole.EMPLOYEE } }

    Scaffold(
        containerColor = Slate50,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 2) {
                        showAddSchoolDialog = true
                    } else {
                        showAssignTaskDialog = true
                    }
                },
                modifier = Modifier.testTag("fab_admin_action"),
                containerColor = Indigo600,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        if (selectedTab == 2) "Add School" else "Assign Task",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Admin Top Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Navy900, Navy800)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Amber600.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Amber500,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = currentUser.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "State Coordinator • Admin Control",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate400
                                    )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onSyncRemote,
                                modifier = Modifier.testTag("btn_admin_sync")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = onSwitchUser,
                                modifier = Modifier.testTag("btn_admin_logout")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Switch Profile",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${schools.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text("Schools", style = MaterialTheme.typography.labelSmall.copy(color = Slate400))
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${visits.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald400
                                    )
                                )
                                Text("Visits", style = MaterialTheme.typography.labelSmall.copy(color = Slate400))
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${tasks.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Amber400
                                    )
                                )
                                Text("Tasks", style = MaterialTheme.typography.labelSmall.copy(color = Slate400))
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${fieldOfficers.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Indigo100
                                    )
                                )
                                Text("Officers", style = MaterialTheme.typography.labelSmall.copy(color = Slate400))
                            }
                        }
                    }
                }
            }

            // Quick Actions Bar (Export Excel)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Emerald50,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null, tint = Emerald600)
                        Column {
                            Text(
                                text = "Export Excel Report (.xlsx)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            )
                            Text(
                                text = "Export all ${visits.size} completed visit records",
                                style = MaterialTheme.typography.labelSmall.copy(color = Emerald600)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val file = ExcelExportHelper.exportVisitsToExcel(context, visits)
                            if (file != null) {
                                ExcelExportHelper.shareExcelFile(context, file)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_export_excel")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp)
                    }
                }
            }

            // Admin Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("admin_tab_visits"),
                    text = { Text("Visits (${visits.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("admin_tab_tasks"),
                    text = { Text("Tasks (${tasks.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("admin_tab_schools"),
                    text = { Text("Schools (${schools.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    modifier = Modifier.testTag("admin_tab_officers"),
                    text = { Text("Officers (${fieldOfficers.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            // Tab Contents
            when (selectedTab) {
                0 -> {
                    // Visits Tab
                    if (visits.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Assessment,
                            title = "No Visits Recorded",
                            description = "Field officers have not recorded any visits yet.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                        ) {
                            items(visits, key = { it.visitId }) { visit ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onViewVisitDetail(visit) }
                                        .testTag("admin_visit_item_${visit.visitId}"),
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
                                            SyncBadge(isSynced = visit.isSynced)
                                            Text(
                                                text = "${visit.visitDate} • ${visit.visitTime}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = visit.schoolName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "${visit.block}, ${visit.district} • Officer: ${visit.employeeName}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Tasks Tab
                    if (tasks.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Assignment,
                            title = "No Tasks Assigned",
                            description = "Tap 'Assign Task' to delegate school visits to field officers.",
                            modifier = Modifier.weight(1f),
                            actionButton = {
                                Button(
                                    onClick = { showAssignTaskDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                                ) {
                                    Text("Assign New Task")
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                        ) {
                            items(tasks, key = { it.taskId }) { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            StatusBadge(status = task.status)
                                            Text(
                                                text = "Assigned to: ${task.employeeName}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Indigo600
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = task.schoolName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "${task.block}, ${task.district} • Target: ${task.visitDate}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                        )

                                        if (task.notes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Notes: ${task.notes}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Schools Directory Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        items(schools, key = { it.schoolId }) { school ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Indigo50),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, tint = Indigo600, modifier = Modifier.size(22.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = school.schoolName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                        )
                                        Text(
                                            text = "${school.blockName}, ${school.districtName} • UDISE: ${school.udiseCode}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Field Officers Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        items(fieldOfficers, key = { it.uid }) { officer ->
                            val officerTasks = tasks.filter { it.employeeId == officer.uid }
                            val officerVisits = visits.filter { it.employeeId == officer.uid }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Indigo50),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = officer.name.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Indigo600
                                            )
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = officer.name,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                        )
                                        Text(
                                            text = "${officer.designation} • ${officer.mobile}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "${officerVisits.size} completed",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Emerald600,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                            Text(
                                                text = "• ${officerTasks.size} active tasks",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Amber700,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Assign Task Dialog
    if (showAssignTaskDialog) {
        var selectedOfficer by remember { mutableStateOf(fieldOfficers.firstOrNull()) }
        var selectedSchool by remember { mutableStateOf(schools.firstOrNull()) }
        var visitDate by remember { mutableStateOf("2026-08-18") }
        var notes by remember { mutableStateOf("Conduct digital class orientation and verify SOE poster installation.") }

        AlertDialog(
            onDismissRequest = { showAssignTaskDialog = false },
            title = { Text("Assign School Visit Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Field Officer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    var officerDropdownExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { officerDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedOfficer?.name ?: "Choose Officer")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = officerDropdownExpanded,
                        onDismissRequest = { officerDropdownExpanded = false }
                    ) {
                        fieldOfficers.forEach { off ->
                            DropdownMenuItem(
                                text = { Text(off.name) },
                                onClick = {
                                    selectedOfficer = off
                                    officerDropdownExpanded = false
                                }
                            )
                        }
                    }

                    Text("Select School", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    var schoolDropdownExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { schoolDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedSchool?.schoolName?.take(26) ?: "Choose School")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = schoolDropdownExpanded,
                        onDismissRequest = { schoolDropdownExpanded = false }
                    ) {
                        schools.forEach { sch ->
                            DropdownMenuItem(
                                text = { Text("${sch.schoolName} (${sch.districtName})") },
                                onClick = {
                                    selectedSchool = sch
                                    schoolDropdownExpanded = false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = visitDate,
                        onValueChange = { visitDate = it },
                        label = { Text("Target Visit Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Special Instructions / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val officer = selectedOfficer
                        val school = selectedSchool
                        if (officer != null && school != null) {
                            val newTask = Task(
                                taskId = "TASK_${System.currentTimeMillis()}",
                                schoolId = school.schoolId,
                                employeeId = officer.uid,
                                employeeName = officer.name,
                                schoolName = school.schoolName,
                                state = school.stateName,
                                district = school.districtName,
                                block = school.blockName,
                                assignedBy = currentUser.name,
                                visitDate = visitDate,
                                status = VisitStatus.ASSIGNED,
                                notes = notes,
                                createdAt = System.currentTimeMillis()
                            )
                            onAssignNewTask(newTask)
                            showAssignTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Assign Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add School Dialog
    if (showAddSchoolDialog) {
        var schoolName by remember { mutableStateOf("") }
        var district by remember { mutableStateOf("Jaipur") }
        var block by remember { mutableStateOf("Sanganer") }
        var udise by remember { mutableStateOf("") }
        var principal by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSchoolDialog = false },
            title = { Text("Add School to Directory", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text("Block *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = udise,
                        onValueChange = { udise = it },
                        label = { Text("UDISE Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = principal,
                        onValueChange = { principal = it },
                        label = { Text("Principal Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Principal Mobile") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (schoolName.isNotBlank() && district.isNotBlank()) {
                            val newSchool = School(
                                schoolId = "SCH_${System.currentTimeMillis()}",
                                schoolName = schoolName,
                                districtName = district,
                                blockName = block,
                                udiseCode = udise,
                                principalName = principal,
                                principalMobile = mobile
                            )
                            onAddNewSchool(newSchool)
                            showAddSchoolDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save School")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSchoolDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

val Emerald400 = Color(0xFF34D399)
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)

package com.example.ui.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.data.model.VisitStatus
import com.example.ui.components.StatusChip
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate900
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignVisitsTab(
    schools: List<School>,
    employees: List<User>,
    assignedTasks: List<Task>,
    visits: List<Visit> = emptyList(),
    onDeleteTask: (String) -> Unit = {},
    onUpdateVisitAnswers: ((String, VisitAnswers) -> Unit)? = null,
    onAssignTask: (
        school: School,
        employee: User,
        visitDate: String,
        notes: String,
        onComplete: (Result<Task>) -> Unit
    ) -> Unit
) {
    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var selectedEmployee1 by remember { mutableStateOf<User?>(null) }
    var selectedEmployee2 by remember { mutableStateOf<User?>(null) }
    var enableCoOfficer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val todayDateFormatted = remember {
        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
        sdf.format(Date())
    }
    var visitDate by remember { mutableStateOf(todayDateFormatted) }

    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember(context) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
                visitDate = sdf.format(selectedCal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    var notes by remember { mutableStateOf("") }

    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }

    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }
    var schoolDropdownExpanded by remember { mutableStateOf(false) }
    var employee1DropdownExpanded by remember { mutableStateOf(false) }
    var employee2DropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }
    var showAllTasksDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    // Identify completed schools from school visitDate, visits reports, or completed tasks
    val completedSchoolIds = remember(schools, visits, assignedTasks) {
        val fromSchools = schools.filter { it.visitDate.isNotBlank() }.map { it.schoolId }
        val fromVisits = visits.filter { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }.map { it.schoolId }
        val fromTasks = assignedTasks.filter { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }.map { it.schoolId }
        (fromSchools + fromVisits + fromTasks).toSet()
    }

    // Exclude schools that currently have an active (non-submitted) task in assignedTasks
    val currentlyAssignedSchoolIds = remember(assignedTasks) {
        assignedTasks.filter { it.status == VisitStatus.ASSIGNED || it.status == VisitStatus.STARTED || it.status == VisitStatus.IN_PROGRESS }.map { it.schoolId }.toSet()
    }

    val availableSchools = remember(schools, currentlyAssignedSchoolIds, completedSchoolIds) {
        schools.filter { school ->
            !school.isDeleted &&
            !completedSchoolIds.contains(school.schoolId) &&
            !currentlyAssignedSchoolIds.contains(school.schoolId)
        }
    }

    val stateList = remember(availableSchools) {
        listOf("All States") + availableSchools.map { if (it.state.isNotBlank()) it.state else "Rajasthan" }.distinct()
    }

    val districtList = remember(availableSchools, selectedState) {
        val base = if (selectedState == "All States") availableSchools else availableSchools.filter { (it.state.ifBlank { "Rajasthan" }) == selectedState }
        listOf("All Districts") + base.map { it.district }.filter { it.isNotBlank() }.distinct()
    }

    val blockList = remember(availableSchools, selectedState, selectedDistrict) {
        val base = availableSchools.filter {
            (selectedState == "All States" || (it.state.ifBlank { "Rajasthan" }) == selectedState) &&
            (selectedDistrict == "All Districts" || it.district == selectedDistrict)
        }
        listOf("All Blocks") + base.map { it.block }.filter { it.isNotBlank() }.distinct()
    }

    val filteredSchools = remember(availableSchools, selectedState, selectedDistrict, selectedBlock) {
        availableSchools.filter { school ->
            val sState = school.state.ifBlank { "Rajasthan" }
            (selectedState == "All States" || sState == selectedState) &&
            (selectedDistrict == "All Districts" || school.district == selectedDistrict) &&
            (selectedBlock == "All Blocks" || school.block == selectedBlock)
        }
    }

    // Only ACTIVE employees can be assigned tasks; inactive employees are excluded
    val activeEmployees = remember(employees) {
        employees.filter { it.status == UserStatus.ACTIVE }
    }

    // Determine target district for officer filtering
    val targetDistrict = remember(selectedSchool, selectedDistrict) {
        val d = selectedSchool?.district?.ifBlank { selectedDistrict } ?: selectedDistrict
        if (d == "All Districts") "" else d
    }

    val filteredEmployees = remember(activeEmployees, targetDistrict) {
        if (targetDistrict.isBlank()) {
            activeEmployees
        } else {
            val matched = activeEmployees.filter { it.district.equals(targetDistrict, ignoreCase = true) }
            if (matched.isNotEmpty()) matched else activeEmployees
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Indigo600,
        unfocusedBorderColor = Slate300
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Assign New School Visit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
        }

        // Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (message != null) {
                        Text(message!!, color = Indigo600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("Target Location Category (स्थान फ़िल्टर)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)

                    // Row 1: State & District Dropdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // State Dropdown
                        ExposedDropdownMenuBox(
                            expanded = stateExpanded,
                            onExpandedChange = { stateExpanded = !stateExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("State (राज्य)", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = stateExpanded,
                                onDismissRequest = { stateExpanded = false }
                            ) {
                                stateList.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, fontSize = 13.sp) },
                                        onClick = {
                                            selectedState = s
                                            selectedDistrict = "All Districts"
                                            selectedBlock = "All Blocks"
                                            selectedSchool = null
                                            stateExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // District Dropdown
                        ExposedDropdownMenuBox(
                            expanded = districtExpanded,
                            onExpandedChange = { districtExpanded = !districtExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedDistrict,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("District (जिला)", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = districtExpanded,
                                onDismissRequest = { districtExpanded = false }
                            ) {
                                districtList.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDistrict = d
                                            selectedBlock = "All Blocks"
                                            selectedSchool = null
                                            districtExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Block Dropdown (Full Width for complete readability)
                    ExposedDropdownMenuBox(
                        expanded = blockExpanded,
                        onExpandedChange = { blockExpanded = !blockExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBlock,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Block (ब्लॉक)", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = blockExpanded,
                            onDismissRequest = { blockExpanded = false }
                        ) {
                            blockList.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b, fontSize = 13.sp) },
                                    onClick = {
                                        selectedBlock = b
                                        selectedSchool = null
                                        blockExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Select School Dropdown
                    ExposedDropdownMenuBox(
                        expanded = schoolDropdownExpanded,
                        onExpandedChange = { schoolDropdownExpanded = !schoolDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSchool?.schoolName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target School (${filteredSchools.size} unassigned)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = schoolDropdownExpanded,
                            onDismissRequest = { schoolDropdownExpanded = false }
                        ) {
                            if (filteredSchools.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No unassigned schools available (All assigned/completed)", color = Slate500, fontSize = 12.sp) },
                                    onClick = { schoolDropdownExpanded = false }
                                )
                            } else {
                                filteredSchools.forEach { school ->
                                    DropdownMenuItem(
                                        text = { Text("${school.schoolName} (${school.block}, ${school.district})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedSchool = school
                                            schoolDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // District Filter Info Badge for Officers
                    if (targetDistrict.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Indigo600.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Active Officers in $targetDistrict: ${filteredEmployees.size} available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Indigo600
                                )
                            }
                        }
                    }

                    // Primary Officer Selection
                    ExposedDropdownMenuBox(
                        expanded = employee1DropdownExpanded,
                        onExpandedChange = { employee1DropdownExpanded = !employee1DropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedEmployee1?.let { "${it.name} (${it.district.ifBlank { "Unassigned" }})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Primary Field Officer (सक्रिय कर्मचारी 1) *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employee1DropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = employee1DropdownExpanded,
                            onDismissRequest = { employee1DropdownExpanded = false }
                        ) {
                            if (filteredEmployees.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No active officers available", color = Slate500, fontSize = 12.sp) },
                                    onClick = { employee1DropdownExpanded = false }
                                )
                            } else {
                                filteredEmployees.forEach { emp ->
                                    DropdownMenuItem(
                                        text = { Text("${emp.name} • ${emp.district} (${emp.email})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedEmployee1 = emp
                                            employee1DropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Optional Secondary Co-Officer Toggle & Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assign 2nd Co-Officer to Same School? (2 कर्मचारी जोड़ें)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                        Switch(
                            checked = enableCoOfficer,
                            onCheckedChange = {
                                enableCoOfficer = it
                                if (!it) selectedEmployee2 = null
                            }
                        )
                    }

                    if (enableCoOfficer) {
                        ExposedDropdownMenuBox(
                            expanded = employee2DropdownExpanded,
                            onExpandedChange = { employee2DropdownExpanded = !employee2DropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedEmployee2?.let { "${it.name} (${it.district.ifBlank { "Unassigned" }})" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Secondary Co-Officer (कर्मचारी 2)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employee2DropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = employee2DropdownExpanded,
                                onDismissRequest = { employee2DropdownExpanded = false }
                            ) {
                                filteredEmployees.filter { it.userId != selectedEmployee1?.userId }.forEach { emp ->
                                    DropdownMenuItem(
                                        text = { Text("${emp.name} • ${emp.district} (${emp.email})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedEmployee2 = emp
                                            employee2DropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                    ) {
                        OutlinedTextField(
                            value = visitDate,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Visit Scheduled Date (निरीक्षण तिथि) *") },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select Date from Calendar",
                                        tint = Indigo600
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Slate900,
                                disabledContainerColor = Color.White,
                                disabledBorderColor = Indigo600,
                                disabledLabelColor = Indigo600,
                                disabledTrailingIconColor = Indigo600
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Instructions / Guidelines") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (selectedSchool != null && selectedEmployee1 != null) {
                                if (completedSchoolIds.contains(selectedSchool!!.schoolId)) {
                                    message = "यह स्कूल पहले ही पूर्ण (Completed) हो चुका है। इसे दोबारा असाइन नहीं किया जा सकता।"
                                    return@Button
                                }
                                if (currentlyAssignedSchoolIds.contains(selectedSchool!!.schoolId)) {
                                    message = "इस स्कूल को पहले से कार्य असाइन है। नया असाइन करने के लिए पहले वाला टास्क डिलीट करें।"
                                    return@Button
                                }
                                isSubmitting = true
                                var assignedCount = 0
                                var hasError = false
                                var errorMsg = ""

                                fun checkDone() {
                                    isSubmitting = false
                                    if (!hasError) {
                                        val officersText = if (enableCoOfficer && selectedEmployee2 != null)
                                            "${selectedEmployee1?.name} & ${selectedEmployee2?.name}"
                                        else "${selectedEmployee1?.name}"
                                        message = "Task successfully assigned to $officersText!"
                                        selectedSchool = null
                                        selectedEmployee1 = null
                                        selectedEmployee2 = null
                                        notes = ""
                                    } else {
                                        message = "Error assigning task: $errorMsg"
                                    }
                                }

                                onAssignTask(selectedSchool!!, selectedEmployee1!!, visitDate, notes) { res1 ->
                                    if (res1.isFailure) {
                                        hasError = true
                                        errorMsg = res1.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                    }
                                    assignedCount++
                                    if (!enableCoOfficer || selectedEmployee2 == null) {
                                        checkDone()
                                    } else {
                                        onAssignTask(selectedSchool!!, selectedEmployee2!!, visitDate, notes) { res2 ->
                                            if (res2.isFailure) {
                                                hasError = true
                                                errorMsg = res2.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                            }
                                            assignedCount++
                                            checkDone()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting && selectedSchool != null && selectedEmployee1 != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (enableCoOfficer && selectedEmployee2 != null) "Assign Visit Task to Both Officers" else "Confirm & Assign Visit Task",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Assigned Tasks (${assignedTasks.take(6).size}/${assignedTasks.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                if (assignedTasks.isNotEmpty()) {
                    TextButton(
                        onClick = { showAllTasksDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "View All (${assignedTasks.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View All Assigned Tasks",
                            tint = Indigo600,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        if (assignedTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = Slate500, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No tasks currently assigned", fontSize = 14.sp, color = Slate500)
                    }
                }
            }
        } else {
            val recentTasks = assignedTasks.take(6)
            items(recentTasks) { task ->
                val matchedVisit = remember(task, visits) {
                    visits.find { it.schoolId == task.schoolId || it.schoolName == task.schoolName }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (matchedVisit != null) {
                                selectedVisitForDetails = matchedVisit
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(task.schoolName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(statusName = task.status.name)
                                IconButton(
                                    onClick = { taskToDelete = task },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Task",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Assigned To: ${task.employeeName} • Date: ${task.visitDate}", fontSize = 12.sp, color = Slate700)
                        if (matchedVisit != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("👉 Click to read submitted visit report details", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // All Assigned Tasks Full Dialog
    if (showAllTasksDialog) {
        AllAssignedTasksDialog(
            assignedTasks = assignedTasks,
            visits = visits,
            onDismiss = { showAllTasksDialog = false },
            onDeleteTask = onDeleteTask,
            onSelectVisit = { visit ->
                selectedVisitForDetails = visit
            }
        )
    }

    selectedVisitForDetails?.let { visit ->
        val matchedSchool = remember(visit.schoolId, schools) {
            schools.find { it.schoolId == visit.schoolId }
        }

        VisitDetailDialog(
            visit = visit,
            school = matchedSchool,
            isAdmin = true,
            onDismiss = { selectedVisitForDetails = null },
            onUpdateAnswers = { updatedAnswers ->
                onUpdateVisitAnswers?.invoke(visit.visitId, updatedAnswers)
            }
        )
    }

    if (taskToDelete != null) {
        val t = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Assigned Task", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Are you sure you want to delete the assigned task for ${t.schoolName} (${t.employeeName})? / क्या आप इस असाइन किए गए कार्य को हटाना चाहते हैं?", color = Slate700) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = t.taskId
                        taskToDelete = null
                        onDeleteTask(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { taskToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAssignedTasksDialog(
    assignedTasks: List<Task>,
    visits: List<Visit>,
    onDismiss: () -> Unit,
    onDeleteTask: (String) -> Unit = {},
    onSelectVisit: (Visit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("All") }
    var taskToDeleteInDialog by remember { mutableStateOf<Task?>(null) }

    val filteredTasks = remember(assignedTasks, searchQuery, selectedStatus) {
        assignedTasks.filter { task ->
            val matchesQuery = searchQuery.isBlank() ||
                    task.schoolName.contains(searchQuery, ignoreCase = true) ||
                    task.employeeName.contains(searchQuery, ignoreCase = true) ||
                    task.district.contains(searchQuery, ignoreCase = true) ||
                    task.block.contains(searchQuery, ignoreCase = true) ||
                    task.visitDate.contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatus == "All" || task.status.name.equals(selectedStatus, ignoreCase = true)

            matchesQuery && matchesStatus
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Slate100
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "All Assigned Tasks",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "Total ${assignedTasks.size} tasks assigned to officers",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Dialog", tint = Slate700)
                        }
                    }
                }

                // Search and Filter Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by school, officer, block, date...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Indigo600) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate500)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo600,
                            unfocusedBorderColor = Slate300
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Status Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "ASSIGNED", "SUBMITTED", "REVIEWED").forEach { status ->
                            val isSelected = selectedStatus == status
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Indigo600 else Slate200,
                                modifier = Modifier.clickable { selectedStatus = status }
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Task List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Showing ${filteredTasks.size} of ${assignedTasks.size} tasks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    }

                    if (filteredTasks.isEmpty()) {
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
                                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No matching tasks found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                    Text("Try adjusting your search query or filter", fontSize = 12.sp, color = Slate500)
                                }
                            }
                        }
                    } else {
                        items(filteredTasks) { task ->
                            val matchedVisit = remember(task, visits) {
                                visits.find { it.schoolId == task.schoolId || it.schoolName == task.schoolName }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (matchedVisit != null) {
                                            onSelectVisit(matchedVisit)
                                        }
                                    },
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
                                            text = task.schoolName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StatusChip(statusName = task.status.name)
                                            IconButton(
                                                onClick = { taskToDeleteInDialog = task },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Task",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${task.district} • ${task.block}",
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Assigned To: ${task.employeeName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate700
                                        )
                                        Text(
                                            text = "Date: ${task.visitDate}",
                                            fontSize = 12.sp,
                                            color = Indigo600,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (task.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Note: ${task.notes}",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }

                                    if (matchedVisit != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = "✓ Visit Report Available - Tap to view full report",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Indigo600,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

    if (taskToDeleteInDialog != null) {
        val t = taskToDeleteInDialog!!
        AlertDialog(
            onDismissRequest = { taskToDeleteInDialog = null },
            title = { Text("Delete Assigned Task", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Are you sure you want to delete the assigned task for ${t.schoolName} (${t.employeeName})? / क्या आप इस असाइन किए गए कार्य को हटाना चाहते हैं?", color = Slate700) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = t.taskId
                        taskToDeleteInDialog = null
                        onDeleteTask(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { taskToDeleteInDialog = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

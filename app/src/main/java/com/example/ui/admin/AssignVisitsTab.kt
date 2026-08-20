package com.example.ui.admin

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.data.model.VisitStatus
import com.example.ui.components.StatusChip
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandAccentDark
import com.example.ui.theme.BrandAccentLight
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.GoogleMapHelper
import com.example.util.IndiaLocationData

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
        mapLink: String,
        onComplete: (Result<Task>) -> Unit
    ) -> Unit
) {
    // 3-Step Wizard Current Step (1: Location, 2: School & Officer, 3: Schedule)
    var currentStep by remember { mutableIntStateOf(1) }

    // Persistent Form State
    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }

    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var schoolMapLink by remember { mutableStateOf("") }
    var selectedEmployee1 by remember { mutableStateOf<User?>(null) }
    var selectedEmployee2 by remember { mutableStateOf<User?>(null) }
    var enableCoOfficer by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val todayDateFormatted = remember {
        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
        sdf.format(Date())
    }
    var visitDate by remember { mutableStateOf(todayDateFormatted) }
    var notes by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember(context) {
        DatePickerDialog(
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

    // Dropdown Expansion States
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }
    var schoolDropdownExpanded by remember { mutableStateOf(false) }
    var employee1DropdownExpanded by remember { mutableStateOf(false) }
    var employee2DropdownExpanded by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }
    var showAllTasksDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    val deletedSchoolIds = remember(schools) {
        schools.filter { it.isDeleted }.map { it.schoolId }.toSet()
    }

    val activeAssignedTasks = remember(assignedTasks, deletedSchoolIds) {
        assignedTasks.filter { !deletedSchoolIds.contains(it.schoolId) }
    }

    val activeVisits = remember(visits, deletedSchoolIds) {
        visits.filter { !deletedSchoolIds.contains(it.schoolId) }
    }

    // Identify completed schools from school visitDate, visits reports, or completed tasks
    val completedSchoolIds = remember(schools, activeVisits, activeAssignedTasks) {
        val fromSchools = schools.filter { !it.isDeleted && it.visitDate.isNotBlank() }.map { it.schoolId }
        val fromVisits = activeVisits.filter { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }.map { it.schoolId }
        val fromTasks = activeAssignedTasks.filter { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }.map { it.schoolId }
        (fromSchools + fromVisits + fromTasks).toSet()
    }

    // Exclude schools that currently have an active (non-submitted) task in assignedTasks
    val currentlyAssignedSchoolIds = remember(activeAssignedTasks) {
        activeAssignedTasks.filter { it.status == VisitStatus.ASSIGNED || it.status == VisitStatus.STARTED || it.status == VisitStatus.IN_PROGRESS }.map { it.schoolId }.toSet()
    }

    val availableSchools = remember(schools, currentlyAssignedSchoolIds, completedSchoolIds) {
        schools.filter { school ->
            !school.isDeleted &&
            !completedSchoolIds.contains(school.schoolId) &&
            !currentlyAssignedSchoolIds.contains(school.schoolId)
        }
    }

    // Cascading state, district, and block lists
    val stateList = remember(availableSchools) {
        listOf("All States") + availableSchools.map { IndiaLocationData.normalizeState(it.state) }.distinct().sorted()
    }

    val districtList = remember(availableSchools, selectedState) {
        val base = if (selectedState == "All States") availableSchools else availableSchools.filter { IndiaLocationData.areEqual(it.state.ifBlank { "Rajasthan" }, selectedState) }
        listOf("All Districts") + base.map { IndiaLocationData.normalizeDistrict(it.state, it.district) }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val blockList = remember(availableSchools, selectedState, selectedDistrict) {
        val base = availableSchools.filter {
            (selectedState == "All States" || IndiaLocationData.areEqual(it.state.ifBlank { "Rajasthan" }, selectedState)) &&
            (selectedDistrict == "All Districts" || IndiaLocationData.areEqual(it.district, selectedDistrict))
        }
        listOf("All Blocks") + base.map { IndiaLocationData.normalizeBlock(it.block) }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredSchools = remember(availableSchools, selectedState, selectedDistrict, selectedBlock) {
        availableSchools.filter { school ->
            val sState = IndiaLocationData.normalizeState(school.state)
            val sDistrict = IndiaLocationData.normalizeDistrict(sState, school.district)
            val sBlock = IndiaLocationData.normalizeBlock(school.block)

            (selectedState == "All States" || IndiaLocationData.areEqual(sState, selectedState)) &&
            (selectedDistrict == "All Districts" || IndiaLocationData.areEqual(sDistrict, selectedDistrict)) &&
            (selectedBlock == "All Blocks" || IndiaLocationData.areEqual(sBlock, selectedBlock))
        }
    }

    // Only ACTIVE employees can be assigned tasks
    val activeEmployees = remember(employees) {
        employees.filter { it.status == UserStatus.ACTIVE }
    }

    val targetDistrict = remember(selectedSchool, selectedDistrict) {
        val d = selectedSchool?.district?.ifBlank { selectedDistrict } ?: selectedDistrict
        if (d == "All Districts") "" else IndiaLocationData.normalizeDistrict("", d)
    }

    val filteredEmployees = remember(activeEmployees, targetDistrict) {
        if (targetDistrict.isBlank()) {
            activeEmployees
        } else {
            val matched = activeEmployees.filter { IndiaLocationData.areEqual(it.district, targetDistrict) }
            if (matched.isNotEmpty()) matched else activeEmployees
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = BrandAccent,
        unfocusedBorderColor = Slate300
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Assign Visits",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = "Assign school field visits to SOE officers in 3 easy steps",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }

            // Progress Stepper Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    ProgressStepper(
                        currentStep = currentStep,
                        onStepClick = { step ->
                            // Allow clicking previous steps
                            if (step < currentStep) {
                                currentStep = step
                            } else if (step == 2 && currentStep == 1) {
                                currentStep = 2
                            } else if (step == 3 && currentStep == 2 && selectedSchool != null && selectedEmployee1 != null) {
                                currentStep = 3
                            }
                        }
                    )
                }
            }

            // Wizard Step Container Card
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
                        // Success / Error Feedback Banners
                        if (message != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message!!,
                                        color = Color(0xFF065F46),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { message = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFF065F46),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = Color(0xFF991B1B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { errorMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFF991B1B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Animated Step Content
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                } else {
                                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                }
                            },
                            label = "WizardStepTransition"
                        ) { step ->
                            when (step) {
                                1 -> Step1Location(
                                    selectedState = selectedState,
                                    selectedDistrict = selectedDistrict,
                                    selectedBlock = selectedBlock,
                                    stateList = stateList,
                                    districtList = districtList,
                                    blockList = blockList,
                                    stateExpanded = stateExpanded,
                                    districtExpanded = districtExpanded,
                                    blockExpanded = blockExpanded,
                                    availableSchoolsCount = filteredSchools.size,
                                    textFieldColors = textFieldColors,
                                    onStateExpandChange = { stateExpanded = it },
                                    onDistrictExpandChange = { districtExpanded = it },
                                    onBlockExpandChange = { blockExpanded = it },
                                    onSelectState = {
                                        selectedState = it
                                        selectedDistrict = "All Districts"
                                        selectedBlock = "All Blocks"
                                        selectedSchool = null
                                        stateExpanded = false
                                    },
                                    onSelectDistrict = {
                                        selectedDistrict = it
                                        selectedBlock = "All Blocks"
                                        selectedSchool = null
                                        districtExpanded = false
                                    },
                                    onSelectBlock = {
                                        selectedBlock = it
                                        selectedSchool = null
                                        blockExpanded = false
                                    },
                                    onContinue = {
                                        errorMessage = null
                                        currentStep = 2
                                    }
                                )

                                2 -> Step2SchoolAndOfficer(
                                    selectedSchool = selectedSchool,
                                    filteredSchools = filteredSchools,
                                    schoolDropdownExpanded = schoolDropdownExpanded,
                                    selectedEmployee1 = selectedEmployee1,
                                    selectedEmployee2 = selectedEmployee2,
                                    enableCoOfficer = enableCoOfficer,
                                    filteredEmployees = filteredEmployees,
                                    employee1DropdownExpanded = employee1DropdownExpanded,
                                    employee2DropdownExpanded = employee2DropdownExpanded,
                                    textFieldColors = textFieldColors,
                                    targetDistrict = targetDistrict,
                                    onSchoolExpandChange = { schoolDropdownExpanded = it },
                                    onSelectSchool = { school ->
                                        selectedSchool = school
                                        schoolMapLink = school.mapLink
                                        schoolDropdownExpanded = false
                                        errorMessage = null
                                    },
                                    onEmployee1ExpandChange = { employee1DropdownExpanded = it },
                                    onSelectEmployee1 = { emp ->
                                        selectedEmployee1 = emp
                                        employee1DropdownExpanded = false
                                        if (selectedEmployee2?.userId == emp.userId) {
                                            selectedEmployee2 = null
                                        }
                                        errorMessage = null
                                    },
                                    onCoOfficerToggle = { enabled ->
                                        enableCoOfficer = enabled
                                        if (!enabled) selectedEmployee2 = null
                                    },
                                    onEmployee2ExpandChange = { employee2DropdownExpanded = it },
                                    onSelectEmployee2 = { emp ->
                                        selectedEmployee2 = emp
                                        employee2DropdownExpanded = false
                                        errorMessage = null
                                    },
                                    onBack = { currentStep = 1 },
                                    onContinue = {
                                        if (selectedSchool == null) {
                                            errorMessage = "Please select a target school. (कृपया लक्षित विद्यालय चुनें)"
                                            return@Step2SchoolAndOfficer
                                        }
                                        if (selectedEmployee1 == null) {
                                            errorMessage = "Please select a primary field officer. (कृपया प्राथमिक फील्ड अधिकारी चुनें)"
                                            return@Step2SchoolAndOfficer
                                        }
                                        if (enableCoOfficer && selectedEmployee2 == null) {
                                            errorMessage = "Please select a 2nd co-officer or disable the toggle. (कृपया सह-अधिकारी चुनें या टॉगल बंद करें)"
                                            return@Step2SchoolAndOfficer
                                        }
                                        errorMessage = null
                                        currentStep = 3
                                    }
                                )

                                3 -> Step3Schedule(
                                    selectedSchool = selectedSchool,
                                    selectedEmployee1 = selectedEmployee1,
                                    selectedEmployee2 = selectedEmployee2,
                                    enableCoOfficer = enableCoOfficer,
                                    visitDate = visitDate,
                                    schoolMapLink = schoolMapLink,
                                    notes = notes,
                                    isSubmitting = isSubmitting,
                                    textFieldColors = textFieldColors,
                                    onOpenDatePicker = { datePickerDialog.show() },
                                    onMapLinkChange = { schoolMapLink = it },
                                    onNotesChange = { notes = it },
                                    onTestMapLink = {
                                        GoogleMapHelper.openLocationOnMap(
                                            context = context,
                                            mapLink = schoolMapLink,
                                            schoolName = selectedSchool?.schoolName ?: "School Location",
                                            address = selectedSchool?.districtName ?: ""
                                        )
                                    },
                                    onBack = { currentStep = 2 },
                                    onSubmit = {
                                        if (selectedSchool == null || selectedEmployee1 == null) {
                                            errorMessage = "Incomplete assignment details. Please check Step 2."
                                            return@Step3Schedule
                                        }
                                        if (visitDate.isBlank()) {
                                            errorMessage = "Please select a visit date. (कृपया विज़िट दिनांक चुनें)"
                                            return@Step3Schedule
                                        }
                                        if (completedSchoolIds.contains(selectedSchool!!.schoolId)) {
                                            errorMessage = "यह स्कूल पहले ही पूर्ण (Completed) हो चुका है। इसे दोबारा असाइन नहीं किया जा सकता।"
                                            return@Step3Schedule
                                        }
                                        if (currentlyAssignedSchoolIds.contains(selectedSchool!!.schoolId)) {
                                            errorMessage = "इस स्कूल को पहले से कार्य असाइन है। नया असाइन करने के लिए पहले वाला टास्क डिलीट करें।"
                                            return@Step3Schedule
                                        }

                                        isSubmitting = true
                                        var hasError = false
                                        var errorMsgText = ""

                                        fun checkDone() {
                                            isSubmitting = false
                                            if (!hasError) {
                                                val officersText = if (enableCoOfficer && selectedEmployee2 != null)
                                                    "${selectedEmployee1?.name} & ${selectedEmployee2?.name}"
                                                else "${selectedEmployee1?.name}"
                                                message = "Task successfully assigned to $officersText!"
                                                errorMessage = null
                                                // Reset form state for next task
                                                selectedSchool = null
                                                selectedEmployee1 = null
                                                selectedEmployee2 = null
                                                schoolMapLink = ""
                                                notes = ""
                                                currentStep = 1
                                            } else {
                                                errorMessage = "Error assigning task: $errorMsgText"
                                            }
                                        }

                                        onAssignTask(selectedSchool!!, selectedEmployee1!!, visitDate, notes, schoolMapLink) { res1 ->
                                            if (res1.isFailure) {
                                                hasError = true
                                                errorMsgText = res1.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                            }
                                            if (!enableCoOfficer || selectedEmployee2 == null) {
                                                checkDone()
                                            } else {
                                                onAssignTask(selectedSchool!!, selectedEmployee2!!, visitDate, notes, schoolMapLink) { res2 ->
                                                    if (res2.isFailure) {
                                                        hasError = true
                                                        errorMsgText = res2.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                                    }
                                                    checkDone()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Assigned Tasks Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Assigned Visits",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "${activeAssignedTasks.size} tasks in progress",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    if (activeAssignedTasks.isNotEmpty()) {
                        TextButton(
                            onClick = { showAllTasksDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "View All (${activeAssignedTasks.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = BrandAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Tasks List
            if (activeAssignedTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active tasks assigned yet",
                                fontSize = 13.sp,
                                color = Slate500
                            )
                        }
                    }
                }
            } else {
                val recentTasks = activeAssignedTasks.take(5)
                items(recentTasks, key = { it.taskId }) { task ->
                    val matchedVisit = remember(task, activeVisits) {
                        activeVisits.find { it.schoolId == task.schoolId || it.schoolName == task.schoolName }
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900,
                                    modifier = Modifier.weight(1f)
                                )
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
                            Text(
                                text = "Officer: ${task.employeeName} • Scheduled: ${task.visitDate}",
                                fontSize = 12.sp,
                                color = Slate700
                            )
                            if (matchedVisit != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "👉 Click to view submitted visit report",
                                    fontSize = 11.sp,
                                    color = BrandAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // All Assigned Tasks Full Dialog
    if (showAllTasksDialog) {
        AllAssignedTasksDialog(
            assignedTasks = activeAssignedTasks,
            visits = activeVisits,
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
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = { Text("Delete Assigned Task", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Are you sure you want to delete the assigned task for ${t.schoolName} (${t.employeeName})? / क्या आप इस असाइन किए गए कार्य को हटाना चाहते हैं?", color = Slate700, fontSize = 13.sp) },
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
                    Text("Cancel", fontWeight = FontWeight.Bold, color = Slate700)
                }
            }
        )
    }
}

// ----------------------------------------------------
// Top Progress Stepper Component
// ----------------------------------------------------
@Composable
private fun ProgressStepper(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf(
        Pair("Location", "स्थान"),
        Pair("School & Officer", "विद्यालय"),
        Pair("Schedule", "निर्धारण")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, (enLabel, hiLabel) ->
            val stepNumber = index + 1
            val isCompleted = currentStep > stepNumber
            val isActive = currentStep == stepNumber

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStepClick(stepNumber) }
            ) {
                // Circle with number or checkmark
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> BrandAccent
                                isCompleted -> BrandAccentDark
                                else -> Slate200
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            color = if (isActive) Color.White else Slate600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = enLabel,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) BrandAccent else Slate700
                )
                Text(
                    text = hiLabel,
                    fontSize = 9.sp,
                    color = if (isActive) BrandAccent else Slate400
                )
            }

            // Connecting line between steps
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.6f)
                        .background(if (currentStep > stepNumber) BrandAccent else Slate200)
                )
            }
        }
    }
}

// ----------------------------------------------------
// Step 1: Location Selection
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1Location(
    selectedState: String,
    selectedDistrict: String,
    selectedBlock: String,
    stateList: List<String>,
    districtList: List<String>,
    blockList: List<String>,
    stateExpanded: Boolean,
    districtExpanded: Boolean,
    blockExpanded: Boolean,
    availableSchoolsCount: Int,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onStateExpandChange: (Boolean) -> Unit,
    onDistrictExpandChange: (Boolean) -> Unit,
    onBlockExpandChange: (Boolean) -> Unit,
    onSelectState: (String) -> Unit,
    onSelectDistrict: (String) -> Unit,
    onSelectBlock: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Step Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandAccentLight),
                contentAlignment = Alignment.Center
            ) {
                Text("1", color = BrandAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Select Location", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text("लक्ष्य स्थान का चयन करें", fontSize = 11.sp, color = Slate500)
            }
        }

        HorizontalDivider(color = Slate200)

        // State Dropdown
        ExposedDropdownMenuBox(
            expanded = stateExpanded,
            onExpandedChange = onStateExpandChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedState,
                onValueChange = {},
                readOnly = true,
                label = { Text("State (राज्य)", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = stateExpanded,
                onDismissRequest = { onStateExpandChange(false) }
            ) {
                stateList.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s, fontSize = 13.sp, fontWeight = if (selectedState == s) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onSelectState(s) }
                    )
                }
            }
        }

        // District Dropdown
        ExposedDropdownMenuBox(
            expanded = districtExpanded,
            onExpandedChange = onDistrictExpandChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDistrict,
                onValueChange = {},
                readOnly = true,
                label = { Text("District (ज़िला)", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = districtExpanded,
                onDismissRequest = { onDistrictExpandChange(false) }
            ) {
                districtList.forEach { d ->
                    DropdownMenuItem(
                        text = { Text(d, fontSize = 13.sp, fontWeight = if (selectedDistrict == d) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onSelectDistrict(d) }
                    )
                }
            }
        }

        // Block Dropdown
        ExposedDropdownMenuBox(
            expanded = blockExpanded,
            onExpandedChange = onBlockExpandChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedBlock,
                onValueChange = {},
                readOnly = true,
                label = { Text("Block (ब्लॉक)", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = blockExpanded,
                onDismissRequest = { onBlockExpandChange(false) }
            ) {
                blockList.forEach { b ->
                    DropdownMenuItem(
                        text = { Text(b, fontSize = 13.sp, fontWeight = if (selectedBlock == b) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onSelectBlock(b) }
                    )
                }
            }
        }

        // Available Schools Count Indicator
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrandAccentLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$availableSchoolsCount unassigned schools found in this region",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Continue (आगे बढ़ें)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ----------------------------------------------------
// Step 2: School & Officer Selection
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2SchoolAndOfficer(
    selectedSchool: School?,
    filteredSchools: List<School>,
    schoolDropdownExpanded: Boolean,
    selectedEmployee1: User?,
    selectedEmployee2: User?,
    enableCoOfficer: Boolean,
    filteredEmployees: List<User>,
    employee1DropdownExpanded: Boolean,
    employee2DropdownExpanded: Boolean,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    targetDistrict: String,
    onSchoolExpandChange: (Boolean) -> Unit,
    onSelectSchool: (School) -> Unit,
    onEmployee1ExpandChange: (Boolean) -> Unit,
    onSelectEmployee1: (User) -> Unit,
    onCoOfficerToggle: (Boolean) -> Unit,
    onEmployee2ExpandChange: (Boolean) -> Unit,
    onSelectEmployee2: (User) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Step Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandAccentLight),
                contentAlignment = Alignment.Center
            ) {
                Text("2", color = BrandAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Select School & Field Officer", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text("विद्यालय एवं फील्ड अधिकारी का चयन करें", fontSize = 11.sp, color = Slate500)
            }
        }

        HorizontalDivider(color = Slate200)

        // Select School Dropdown
        ExposedDropdownMenuBox(
            expanded = schoolDropdownExpanded,
            onExpandedChange = onSchoolExpandChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedSchool?.schoolName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Target School (लक्षित विद्यालय) * (${filteredSchools.size} available)") },
                placeholder = { Text("Choose a school from list...") },
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
                onDismissRequest = { onSchoolExpandChange(false) }
            ) {
                if (filteredSchools.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No unassigned schools in this location", color = Slate500, fontSize = 12.sp) },
                        onClick = { onSchoolExpandChange(false) }
                    )
                } else {
                    filteredSchools.forEach { school ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(school.schoolName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                                    Text("${school.block}, ${school.districtName.ifBlank { school.district }} • UDISE: ${school.referenceCode.ifBlank { "N/A" }}", fontSize = 11.sp, color = Slate500)
                                }
                            },
                            onClick = { onSelectSchool(school) }
                        )
                    }
                }
            }
        }

        // Selected School Preview Info Box
        if (selectedSchool != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate100,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedSchool.schoolName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Navy900
                    )
                    Text(
                        text = "Location: ${selectedSchool.villageName.ifBlank { selectedSchool.block }}, ${selectedSchool.districtName.ifBlank { selectedSchool.district }}",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                    if (selectedSchool.principalName.isNotBlank() || selectedSchool.principalMobile.isNotBlank()) {
                        Text(
                            text = "Principal: ${selectedSchool.principalName} (${selectedSchool.principalMobile.ifBlank { selectedSchool.mobile }})",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }
            }
        }

        // Primary Field Officer Selection
        ExposedDropdownMenuBox(
            expanded = employee1DropdownExpanded,
            onExpandedChange = onEmployee1ExpandChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedEmployee1?.let { "${it.name} (${it.district.ifBlank { "Active" }})" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Primary Field Officer (प्राथमिक फील्ड अधिकारी) *") },
                placeholder = { Text("Select primary officer...") },
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
                onDismissRequest = { onEmployee1ExpandChange(false) }
            ) {
                if (filteredEmployees.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No active field officers found", color = Slate500, fontSize = 12.sp) },
                        onClick = { onEmployee1ExpandChange(false) }
                    )
                } else {
                    filteredEmployees.forEach { emp ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(emp.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                                        Text(emp.email, fontSize = 11.sp, color = Slate500)
                                    }
                                    if (emp.district.isNotBlank()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = BrandAccentLight
                                        ) {
                                            Text(
                                                text = emp.district,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandAccent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = { onSelectEmployee1(emp) }
                        )
                    }
                }
            }
        }

        // 2nd Co-Officer Toggle Switch Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate100,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assign 2nd Co-Officer (सह-अधिकारी जोड़ें)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Text(
                            text = "Assign a 2nd officer for joint inspection",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    Switch(
                        checked = enableCoOfficer,
                        onCheckedChange = onCoOfficerToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandAccent
                        )
                    )
                }

                if (enableCoOfficer) {
                    ExposedDropdownMenuBox(
                        expanded = employee2DropdownExpanded,
                        onExpandedChange = onEmployee2ExpandChange,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedEmployee2?.let { "${it.name} (${it.district.ifBlank { "Active" }})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Secondary Co-Officer (सह-अधिकारी 2) *") },
                            placeholder = { Text("Select 2nd officer...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employee2DropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = employee2DropdownExpanded,
                            onDismissRequest = { onEmployee2ExpandChange(false) }
                        ) {
                            val availableCoOfficers = filteredEmployees.filter { it.userId != selectedEmployee1?.userId }
                            if (availableCoOfficers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No other officers available", color = Slate500, fontSize = 12.sp) },
                                    onClick = { onEmployee2ExpandChange(false) }
                                )
                            } else {
                                availableCoOfficers.forEach { emp ->
                                    DropdownMenuItem(
                                        text = { Text("${emp.name} • ${emp.district} (${emp.email})", fontSize = 13.sp) },
                                        onClick = { onSelectEmployee2(emp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Back and Continue Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back (पीछे)", fontWeight = FontWeight.SemiBold, color = Slate700)
            }

            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Continue (आगे)", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ----------------------------------------------------
// Step 3: Schedule & Submission
// ----------------------------------------------------
@Composable
private fun Step3Schedule(
    selectedSchool: School?,
    selectedEmployee1: User?,
    selectedEmployee2: User?,
    enableCoOfficer: Boolean,
    visitDate: String,
    schoolMapLink: String,
    notes: String,
    isSubmitting: Boolean,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onOpenDatePicker: () -> Unit,
    onMapLinkChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTestMapLink: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Step Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandAccentLight),
                contentAlignment = Alignment.Center
            ) {
                Text("3", color = BrandAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Schedule & Confirm", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text("विज़िट दिनांक एवं पुष्टि करें", fontSize = 11.sp, color = Slate500)
            }
        }

        HorizontalDivider(color = Slate200)

        // Summary Review Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate100,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Assignment Overview (कार्य सारांश)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandAccent
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("School:", fontSize = 12.sp, color = Slate500)
                    Text(
                        text = selectedSchool?.schoolName ?: "-",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Primary Officer:", fontSize = 12.sp, color = Slate500)
                    Text(
                        text = selectedEmployee1?.name ?: "-",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }

                if (enableCoOfficer && selectedEmployee2 != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Co-Officer:", fontSize = 12.sp, color = Slate500)
                        Text(
                            text = selectedEmployee2.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    }
                }
            }
        }

        // Visit Date Selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDatePicker() }
        ) {
            OutlinedTextField(
                value = visitDate,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Visit Scheduled Date (निरीक्षण तिथि) *") },
                trailingIcon = {
                    IconButton(onClick = onOpenDatePicker) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date from Calendar",
                            tint = BrandAccent
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Slate900,
                    disabledContainerColor = Color.White,
                    disabledBorderColor = BrandAccent,
                    disabledLabelColor = BrandAccent,
                    disabledTrailingIconColor = BrandAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Google Maps Link Input
        OutlinedTextField(
            value = schoolMapLink,
            onValueChange = onMapLinkChange,
            label = { Text("Google Map Link (गूगल मैप लिंक / URL)") },
            placeholder = { Text("e.g. https://maps.app.goo.gl/... or 26.9124, 75.7873") },
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandAccent)
            },
            trailingIcon = {
                if (schoolMapLink.isNotBlank()) {
                    IconButton(onClick = onTestMapLink) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Test Map Link in Google Maps",
                            tint = BrandAccent
                        )
                    }
                }
            },
            supportingText = {
                Text("💡 Add Google Maps link for direct navigation for the field officer.", fontSize = 11.sp, color = Slate500)
            },
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        // Special Instructions / Notes
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Instructions / Remarks (विशेष निर्देश)") },
            placeholder = { Text("Optional notes or guidelines for the officer...") },
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back (पीछे)", fontWeight = FontWeight.SemiBold, color = Slate700)
            }

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    disabledContainerColor = Slate300
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Submit (असाइन करें)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// All Assigned Tasks Full Dialog
// ----------------------------------------------------
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
            color = BrandBackground
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
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandAccent) },
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
                            focusedBorderColor = BrandAccent,
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
                                color = if (isSelected) BrandAccent else Slate200,
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
                        items(filteredTasks, key = { it.taskId }) { task ->
                            val matchedVisit = remember(task, visits) {
                                visits.find {
                                    (task.visitId.isNotBlank() && it.visitId == task.visitId) ||
                                    (task.taskId.isNotBlank() && it.taskId == task.taskId) ||
                                    (it.schoolId == task.schoolId && it.employeeId == task.employeeId)
                                } ?: visits.find { it.schoolId == task.schoolId }
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
                                            color = BrandAccent,
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
                                            color = BrandAccentLight
                                        ) {
                                            Text(
                                                text = "✓ Visit Report Available - Tap to view full report",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandAccent,
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
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = { Text("Delete Assigned Task", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Are you sure you want to delete the assigned task for ${t.schoolName} (${t.employeeName})? / क्या आप इस असाइन किए गए कार्य को हटाना चाहते हैं?", color = Slate700, fontSize = 13.sp) },
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
                    Text("Cancel", fontWeight = FontWeight.Bold, color = Slate700)
                }
            }
        )
    }
}

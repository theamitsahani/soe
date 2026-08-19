package com.example.ui.employee

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.PhotoCategory
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.data.model.VisitStatus
import com.example.ui.components.SyncStatusBanner
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo700
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.MediaStorageHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.util.UUID

private val BrandPurple = Color(0xFF4F46E5)
private val BrandPurpleDark = Color(0xFF4338CA)
private val BrandPurpleLight = Color(0xFFEEF2FF)
private val BrandPurpleBg = Color(0xFFF0F4FF)
private val FormBackground = Color(0xFFF8FAFC)
private val CardBorderColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFormScreen(
    employeeUser: User,
    task: Task?,
    initialSchool: School?,
    existingVisit: Visit? = null,
    isOnline: Boolean,
    pendingSyncCount: Int,
    onBackClick: () -> Unit,
    onSubmitVisit: (Visit, (Result<Unit>) -> Unit) -> Unit,
    onUpdateSchoolInfo: (School) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5

    // Parse existing answers if editing
    val parsedExistingAnswers = remember(existingVisit) {
        if (existingVisit != null && existingVisit.answersJson.isNotBlank()) {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                moshi.adapter(VisitAnswers::class.java).fromJson(existingVisit.answersJson) ?: VisitAnswers()
            } catch (e: Exception) {
                VisitAnswers()
            }
        } else {
            VisitAnswers()
        }
    }

    // Parse existing photos if editing
    val parsedExistingPhotos = remember(existingVisit) {
        if (existingVisit != null && existingVisit.photosJson.isNotBlank()) {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
                val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
                adapter.fromJson(existingVisit.photosJson) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    // School Details State
    var schoolName by remember { mutableStateOf(existingVisit?.schoolName?.ifBlank { null } ?: task?.schoolName ?: initialSchool?.schoolName ?: "") }
    var udiseCode by remember { mutableStateOf(parsedExistingAnswers.q4_udiseCode.ifBlank { initialSchool?.referenceCode ?: "" }) }
    var stateName by remember { mutableStateOf(existingVisit?.state?.ifBlank { null } ?: initialSchool?.state ?: "Rajasthan") }
    var district by remember { mutableStateOf(existingVisit?.district?.ifBlank { null } ?: task?.district ?: initialSchool?.district ?: "") }
    var block by remember { mutableStateOf(existingVisit?.block?.ifBlank { null } ?: task?.block ?: initialSchool?.block ?: "") }
    // BUG FIX: principalName/principalMobile were reading ONLY from `initialSchool`, unlike the
    // other fields on this screen (schoolName, district, block, visitDate) which correctly fall
    // back to `task` first. The "View Details" dialog on the tasks list (EmployeeMainScreen)
    // prioritizes task.principalMobile over the school record, so whenever a task carried a
    // principal mobile that the school record didn't have (or had different), View Details
    // showed the number but the Start Visit form showed it blank. Now both screens use the same
    // priority order: task -> school.
    var principalName by remember {
        mutableStateOf(
            parsedExistingAnswers.q7_principalName.ifBlank {
                task?.principalName?.ifBlank { initialSchool?.principalName } ?: initialSchool?.principalName ?: ""
            }
        )
    }
    var principalMobile by remember {
        mutableStateOf(
            parsedExistingAnswers.q8_principalMobile.ifBlank {
                task?.principalMobile?.ifBlank { null }
                    ?: initialSchool?.principalMobile?.ifBlank { initialSchool.mobile }
                    ?: ""
            }
        )
    }
    val defaultTodayDate = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date()) }
    var visitDate by remember { mutableStateOf(existingVisit?.visitDate?.ifBlank { null } ?: task?.visitDate ?: defaultTodayDate) }

    // Participating Classes Checkboxes (Class 6th to 12th)
    val availableClasses = remember { listOf("Class 6th", "Class 7th", "Class 8th", "Class 9th", "Class 10th", "Class 11th", "Class 12th") }
    var selectedClasses by remember {
        val initialSelected = if (parsedExistingAnswers.q22_participatingClasses.isNotBlank()) {
            parsedExistingAnswers.q22_participatingClasses.split(",").map { it.trim() }.toSet()
        } else {
            emptySet()
        }
        mutableStateOf(initialSelected)
    }

    // Questionnaire Answers
    var metPrincipal by remember { mutableStateOf(parsedExistingAnswers.q9_metPrincipal) }
    var missionGyanAwareness by remember { mutableStateOf(parsedExistingAnswers.q10_missionGyanAwareness) }
    var studentCount by remember { mutableStateOf(parsedExistingAnswers.q11_studentCount) }
    var schoolResponse by remember { mutableStateOf(parsedExistingAnswers.q12_schoolResponse) }

    // Point 13: BCI Details
    var bciName by remember {
        val initialBciName = parsedExistingAnswers.q13_bciName.ifBlank {
            if (parsedExistingAnswers.q13_bciContactDetails.contains("-")) parsedExistingAnswers.q13_bciContactDetails.substringBefore("-").trim()
            else parsedExistingAnswers.q13_bciContactDetails
        }
        mutableStateOf(initialBciName)
    }
    var bciMobile by remember {
        val initialBciMobile = parsedExistingAnswers.q13_bciMobile.ifBlank {
            if (parsedExistingAnswers.q13_bciContactDetails.contains("-")) parsedExistingAnswers.q13_bciContactDetails.substringAfter("-").trim()
            else ""
        }
        mutableStateOf(initialBciMobile)
    }

    var whatsappGroupAdded by remember { mutableStateOf(parsedExistingAnswers.q14_whatsappGroupAdded) }
    var posterInstalled by remember { mutableStateOf(parsedExistingAnswers.q15_posterInstalled) }
    var keyObservations by remember { mutableStateOf(parsedExistingAnswers.q16_keyObservations) }
    var problemsOrAssistance by remember { mutableStateOf(parsedExistingAnswers.q17_problemsOrAssistance) }
    var followupRequired by remember { mutableStateOf(parsedExistingAnswers.q18_followupRequired) }
    var dataRequiredOnHardDisk by remember { mutableStateOf(parsedExistingAnswers.q19_dataRequiredOnHardDisk) }
    var finalRemarks by remember { mutableStateOf(parsedExistingAnswers.q20_finalRemarks) }
    var smartClassStatus by remember { mutableStateOf(parsedExistingAnswers.q21_smartClassStatus) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Photo Map Category ID -> List of Uri Strings
    val photoMap = remember {
        mutableStateMapOf<String, MutableList<String>>().apply {
            PhotoCategory.entries.forEach { cat ->
                val existingList = parsedExistingPhotos[cat.categoryId]?.toMutableList() ?: mutableListOf()
                put(cat.categoryId, existingList)
            }
        }
    }

    var showEditSchoolDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var showSubmissionSuccessDialog by remember { mutableStateOf(false) }
    var submissionSuccessMessage by remember { mutableStateOf("") }
    var activePhotoCategory by remember { mutableStateOf<PhotoCategory?>(null) }
    var previewMediaUrl by remember { mutableStateOf<String?>(null) }

    // Multi-Photo Picker (Unlimited)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        activePhotoCategory?.let { cat ->
            coroutineScope.launch {
                val list = (photoMap[cat.categoryId] ?: mutableListOf()).toMutableList()
                uris.forEach { uri ->
                    val savedLocalUri = MediaStorageHelper.saveMediaLocally(context, uri)
                    list.add(savedLocalUri)
                }
                photoMap[cat.categoryId] = list
            }
        }
    }

    // Multi-Video Picker (Unlimited)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        activePhotoCategory?.let { cat ->
            coroutineScope.launch {
                val list = (photoMap[cat.categoryId] ?: mutableListOf()).toMutableList()
                uris.forEach { uri ->
                    val savedLocalUri = MediaStorageHelper.saveMediaLocally(context, uri)
                    list.add(savedLocalUri)
                }
                photoMap[cat.categoryId] = list
            }
        }
    }

    val stepTitles = listOf(
        "School & Principal Details",
        "App Awareness & Attendance",
        "Operations & Smart Class",
        "Observations & Remarks",
        "Photos & Final Review"
    )

    val stepShortLabels = listOf(
        "Details",
        "Attendance",
        "Infra",
        "Teaching",
        "Review"
    )

    val isExistingVisitEditable = remember(existingVisit) {
        if (existingVisit == null) true
        else {
            val timeSinceSubmission = System.currentTimeMillis() - existingVisit.createdAt
            val twelveHoursMillis = 12 * 60 * 60 * 1000L
            (timeSinceSubmission in 0..twelveHoursMillis) && existingVisit.editCount < 1
        }
    }

    Scaffold(
        containerColor = FormBackground,
        topBar = {
            // Clean Top Header matching reference
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular back button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onBackClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Navy900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (existingVisit != null) "Edit Visit Report" else "SOE School Visit Form",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            if (schoolName.isNotBlank()) {
                                Text(
                                    text = schoolName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                }
            }
        },
        bottomBar = {
            // Large bottom action buttons with gradient
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color.White,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                submitError = null
                                currentStep--
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("Back", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(if (currentStep > 1) 2f else 1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSubmitting) {
                                submitError = null

                                // Validate Step 1: 11-digit UDISE & 10-digit Principal Mobile
                                if (currentStep == 1) {
                                    val cleanUdise = udiseCode.trim().filter { it.isDigit() }
                                    if (cleanUdise.isBlank()) {
                                        submitError = "कृपया 11 अंकों का UDISE कोड दर्ज करें (Please enter 11-digit UDISE code)"
                                        return@clickable
                                    }
                                    if (cleanUdise.length != 11) {
                                        submitError = "UDISE कोड अमान्य है! यह अनिवार्य रूप से ठीक 11 अंकों का होना चाहिए (UDISE Code must be exactly 11 digits)"
                                        return@clickable
                                    }
                                    val cleanPMobile = principalMobile.trim().filter { it.isDigit() }
                                    if (cleanPMobile.isNotBlank() && cleanPMobile.length != 10) {
                                        submitError = "प्रधानाचार्य का मोबाइल नंबर ठीक 10 अंकों का होना अनिवार्य है (Principal mobile must be 10 digits)"
                                        return@clickable
                                    }
                                }

                                // Validate Step 3: 10-digit BCI Contact
                                if (currentStep == 3) {
                                    val cleanBciMobile = bciMobile.trim().filter { it.isDigit() }
                                    if (cleanBciMobile.isNotBlank() && cleanBciMobile.length != 10) {
                                        submitError = "BCI मोबाइल नंबर ठीक 10 अंकों का होना अनिवार्य है (BCI mobile must be 10 digits)"
                                        return@clickable
                                    }
                                }

                                if (currentStep < totalSteps) {
                                    currentStep++
                                } else {
                                    // Validate 5 mandatory photo categories
                                    val missingPhotoCategories = PhotoCategory.entries
                                        .filter { it.minRequired > 0 }
                                        .filter { (photoMap[it.categoryId] ?: emptyList()).size < it.minRequired }

                                    if (missingPhotoCategories.isNotEmpty()) {
                                        val missingNames = missingPhotoCategories.joinToString("\n• ") { it.displayName }
                                        submitError = "कृपया निम्नलिखित अनिवार्य फोटो अपलोड करें:\n• $missingNames"
                                        return@clickable
                                    }

                                    if (existingVisit != null && !isExistingVisitEditable) {
                                        submitError = if (existingVisit.editCount >= 1) "यह रिपोर्ट 1 बार संशोधित हो चुकी है (1/1 Edit Limit Reached)। और बदलाव नहीं किए जा सकते।" else "12 घंटे की संशोधन अवधि समाप्त हो चुकी है।"
                                        return@clickable
                                    }

                                    // Final Submission logic
                                    isSubmitting = true
                                    submitError = null

                                    val combinedBciDetails = if (bciName.isNotBlank() || bciMobile.isNotBlank()) {
                                        listOf(bciName.trim(), bciMobile.trim()).filter { it.isNotBlank() }.joinToString(" - ")
                                    } else {
                                        ""
                                    }

                                    val answers = VisitAnswers(
                                        q1_soeName = employeeUser.name,
                                        q2_visitDate = visitDate,
                                        q3_schoolName = schoolName,
                                        q4_udiseCode = udiseCode,
                                        q5_district = district,
                                        q6_block = block,
                                        q7_principalName = principalName,
                                        q8_principalMobile = principalMobile,
                                        q9_metPrincipal = metPrincipal,
                                        q10_missionGyanAwareness = missionGyanAwareness,
                                        q11_studentCount = studentCount,
                                        q12_schoolResponse = schoolResponse,
                                        q13_bciName = bciName.trim(),
                                        q13_bciMobile = bciMobile.trim(),
                                        q13_bciContactDetails = combinedBciDetails,
                                        q14_whatsappGroupAdded = whatsappGroupAdded,
                                        q15_posterInstalled = posterInstalled,
                                        q16_keyObservations = keyObservations,
                                        q17_problemsOrAssistance = problemsOrAssistance,
                                        q18_followupRequired = followupRequired,
                                        q19_dataRequiredOnHardDisk = dataRequiredOnHardDisk,
                                        q20_finalRemarks = finalRemarks,
                                        q21_smartClassStatus = smartClassStatus,
                                        q22_participatingClasses = selectedClasses.sorted().joinToString(", "),
                                        q23_state = stateName
                                    )

                                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                                    val answersAdapter = moshi.adapter(VisitAnswers::class.java)
                                    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
                                    val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

                                    val schoolId = existingVisit?.schoolId ?: task?.schoolId ?: initialSchool?.schoolId ?: ("sch_" + UUID.randomUUID().toString().take(8))
                                    val now = System.currentTimeMillis()
                                    val visitId = existingVisit?.visitId ?: task?.visitId?.takeIf { it.isNotBlank() } ?: if (task != null) "vst_${task.taskId}_${employeeUser.userId}" else "vst_${schoolId}_$now"

                                    val finalVisit = Visit(
                                        visitId = visitId,
                                        taskId = existingVisit?.taskId ?: task?.taskId ?: "",
                                        schoolId = schoolId,
                                        employeeId = employeeUser.userId,
                                        employeeName = employeeUser.name,
                                        schoolName = schoolName,
                                        state = stateName,
                                        district = district,
                                        block = block,
                                        villageName = existingVisit?.villageName ?: task?.villageName ?: initialSchool?.villageName ?: "",
                                        schoolType = existingVisit?.schoolType ?: task?.schoolType ?: initialSchool?.schoolType ?: "Government School",
                                        udiseCode = udiseCode,
                                        principalName = principalName,
                                        principalMobile = principalMobile,
                                        visitDate = visitDate,
                                        status = VisitStatus.SUBMITTED,
                                        answersJson = answersAdapter.toJson(answers),
                                        photosJson = photosAdapter.toJson(photoMap.mapValues { it.value.filter { u -> u.isNotBlank() }.distinct() }),
                                        startedAt = existingVisit?.startedAt ?: (now - 15 * 60 * 1000L),
                                        completedAt = now,
                                        submittedAt = now,
                                        appVersion = "1.0.0",
                                        editCount = if (existingVisit != null) existingVisit.editCount + 1 else 0,
                                        createdAt = existingVisit?.createdAt ?: now,
                                        updatedAt = now
                                    )

                                    onSubmitVisit(finalVisit) { res ->
                                        isSubmitting = false
                                        if (res.isSuccess) {
                                            if (isOnline) {
                                                submissionSuccessMessage = "विज़िट रिपोर्ट सफलतापूर्वक सबमिट और सर्वर पर सिंक हो गई है।"
                                            } else {
                                                submissionSuccessMessage = "विज़िट रिपोर्ट स्थानीय रूप से सुरक्षित सहेज ली गई है (Saved Locally)!\n\nनेटवर्क उपलब्ध होते ही ऐप इसे अपने आप सर्वर पर अपलोड (Sync) कर देगा।"
                                            }
                                            showSubmissionSuccessDialog = true
                                        } else {
                                            submitError = res.exceptionOrNull()?.localizedMessage ?: "Failed to submit visit report"
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (currentStep == totalSteps && existingVisit != null && !isExistingVisitEditable) {
                                        Brush.horizontalGradient(colors = listOf(Slate500, Slate700))
                                    } else {
                                        Brush.horizontalGradient(colors = listOf(BrandPurple, BrandPurpleDark))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (currentStep == totalSteps) {
                                            if (existingVisit != null && !isExistingVisitEditable) "Report Locked (1/1 Limit)" else "Submit Report"
                                        } else "Next Step",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = if (currentStep == totalSteps) {
                                            if (existingVisit != null && !isExistingVisitEditable) Icons.Default.Lock else Icons.Default.Check
                                        } else Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FormBackground)
        ) {
            // Online/Offline Sync Status Banner
            SyncStatusBanner(
                isOnline = isOnline,
                pendingCount = pendingSyncCount,
                onSyncClick = {}
            )

            // Edit Limit / Lock Banner
            if (existingVisit != null && !isExistingVisitEditable) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (existingVisit.editCount >= 1) "यह रिपोर्ट 1 बार संशोधित हो चुकी है (1/1 Limit Reached)। और बदलाव मान्य नहीं हैं।" else "12 घंटे की संशोधन अवधि समाप्त हो चुकी है।",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // Step Progress Indicator Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color.Transparent
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stepTitles.getOrElse(currentStep - 1) { "School Visit Details" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurpleDark
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "Step $currentStep of $totalSteps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5-Segment Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 1..totalSteps) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (i <= currentStep) BrandPurple else Color(0xFFE2E8F0))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 5-Step Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        stepShortLabels.forEachIndexed { index, label ->
                            val stepNumber = index + 1
                            val isActive = stepNumber == currentStep
                            val isCompleted = stepNumber < currentStep
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) BrandPurpleDark else if (isCompleted) Slate700 else Slate500
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (submitError != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Red600, modifier = Modifier.size(20.dp))
                            Text(
                                text = submitError!!,
                                color = Red600,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                when (currentStep) {
                    1 -> {
                        // ==========================================
                        // STEP 1: School Information & UDISE Code
                        // ==========================================

                        // Auto-filled School Information Card (Matching Reference Design)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "School Information",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    // Pencil Edit button in rounded square
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = BrandPurpleLight,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showEditSchoolDialog = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit School Info",
                                                tint = BrandPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Verify the auto-filled details below. Tap the pencil icon only if a correction is needed.",
                                    fontSize = 12.sp,
                                    color = Slate500,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Inner Highlight Card for School Name & UDISE
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = BrandPurpleBg,
                                    border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        val displayUdise = if (udiseCode.isNotBlank()) udiseCode else "N/A"
                                        Text(
                                            text = "SCHOOL NAME · UDISE $displayUdise",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = schoolName.ifBlank { "GOVT. SENIOR SECONDARY SCHOOL" }.uppercase(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                DetailRowItem(label = "State", value = stateName)
                                DetailRowItem(label = "District", value = district)
                                DetailRowItem(label = "Block", value = block)
                                DetailRowItem(
                                    label = "Principal Name",
                                    value = principalName.ifBlank { "Not specified" },
                                    isUnspecified = principalName.isBlank()
                                )
                                DetailRowItem(
                                    label = "Principal Mobile",
                                    value = principalMobile.ifBlank { "Not specified" },
                                    isUnspecified = principalMobile.isBlank()
                                )
                                DetailRowItem(label = "Visit Date", value = visitDate)
                            }
                        }

                        // UDISE Code Input Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("UDISE Code", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("*", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Red600)
                                    }
                                    val digitsOnly = udiseCode.filter { it.isDigit() }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (digitsOnly.length == 11) Color(0xFFECFDF5) else Color(0xFFF1F5F9)
                                    ) {
                                        Text(
                                            text = "${digitsOnly.length}/11",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (digitsOnly.length == 11) Emerald600 else Slate500,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = udiseCode,
                                    onValueChange = { input ->
                                        udiseCode = input.filter { it.isDigit() }.take(11)
                                        submitError = null
                                    },
                                    placeholder = { Text("Enter 11-digit code", color = Slate500, fontSize = 14.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FormBackground,
                                        unfocusedContainerColor = FormBackground,
                                        focusedBorderColor = BrandPurple,
                                        unfocusedBorderColor = CardBorderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "e.g. 08010100101 — check the school signboard if unsure",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }
                        }

                        // Q9: Met Principal Sir?
                        ModernSingleChoiceCard(
                            title = "9. प्रधानाचार्य महोदय से मुलाकात हुई?",
                            subtitle = "Met Principal Sir?",
                            options = listOf("हाँ", "नहीं"),
                            selectedOption = metPrincipal,
                            onOptionSelected = { metPrincipal = it }
                        )
                    }

                    2 -> {
                        // ==========================================
                        // STEP 2: App Awareness & Attendance
                        // ==========================================
                        ModernSingleChoiceCard(
                            title = "10. क्या प्रधानाचार्य महोदय को Mission Gyan App के बारे में जानकारी थी?",
                            subtitle = "Was Principal Sir aware of Mission Gyan App?",
                            options = listOf("हाँ", "नहीं", "थोड़ी जानकारी थी"),
                            selectedOption = missionGyanAwareness,
                            onOptionSelected = { missionGyanAwareness = it }
                        )

                        // Participating Classes (Class 6th to 12th)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "11. भाग लेने वाली कक्षाएं (Participating Classes)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "जिन कक्षाओं ने कार्यक्रम/विज़िट में भाग लिया उन्हें चुनें:",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                availableClasses.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        pair.forEach { cls ->
                                            val isChecked = selectedClasses.contains(cls)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        selectedClasses = if (isChecked) selectedClasses - cls else selectedClasses + cls
                                                    },
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isChecked) BrandPurpleLight else FormBackground,
                                                border = BorderStroke(
                                                    width = if (isChecked) 1.5.dp else 1.dp,
                                                    color = if (isChecked) BrandPurple else CardBorderColor
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            selectedClasses = if (checked == true) selectedClasses + cls else selectedClasses - cls
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = BrandPurple,
                                                            uncheckedColor = Slate500
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = cls,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isChecked) BrandPurpleDark else Slate700
                                                    )
                                                }
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }

                        // Student Attendance Count
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "12. उपस्थित विद्यार्थियों की संख्या",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Total Student Attendance Count",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = studentCount,
                                    // BUG FIX: this field had a Number keyboard hint but no actual
                                    // input filtering, unlike principalMobile/udiseCode/bciMobile
                                    // elsewhere in this same form. A pasted value could contain
                                    // letters, symbols, a decimal point, or a negative sign and be
                                    // submitted as-is. Restrict to digits only (matches how the
                                    // other numeric fields in this form are handled) and cap the
                                    // length to a sane school-attendance size.
                                    onValueChange = { studentCount = it.filter { c -> c.isDigit() }.take(5) },
                                    placeholder = { Text("e.g. 120", color = Slate500) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FormBackground,
                                        unfocusedContainerColor = FormBackground,
                                        focusedBorderColor = BrandPurple,
                                        unfocusedBorderColor = CardBorderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // School Response
                        ModernSingleChoiceCard(
                            title = "13. विद्यालय की प्रतिक्रिया",
                            subtitle = "School Response Rating",
                            options = listOf("बहुत अच्छी", "अच्छी", "सामान्य", "कमजोर"),
                            selectedOption = schoolResponse,
                            onOptionSelected = { schoolResponse = it }
                        )
                    }

                    3 -> {
                        // ==========================================
                        // STEP 3: Operations & Smart Class
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "14. BCI संपर्क विवरण (BCI Details)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )

                                // Field 1: BCI Officer Name
                                Column {
                                    Text("BCI Officer Name (BCI का नाम)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = bciName,
                                        onValueChange = { bciName = it },
                                        placeholder = { Text("Enter BCI Officer Name", color = Slate500) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = FormBackground,
                                            unfocusedContainerColor = FormBackground,
                                            focusedBorderColor = BrandPurple,
                                            unfocusedBorderColor = CardBorderColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Field 2: BCI Contact Mobile Number
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("BCI Contact Number (BCI मोबाइल नंबर)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                        val digits = bciMobile.filter { it.isDigit() }
                                        Text(
                                            text = "${digits.length}/10",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (digits.length == 10) Emerald600 else Slate500
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = bciMobile,
                                        onValueChange = { input ->
                                            bciMobile = input.filter { it.isDigit() }.take(10)
                                            submitError = null
                                        },
                                        placeholder = { Text("10-digit mobile number", color = Slate500) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        isError = bciMobile.isNotBlank() && bciMobile.length != 10,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = FormBackground,
                                            unfocusedContainerColor = FormBackground,
                                            focusedBorderColor = BrandPurple,
                                            unfocusedBorderColor = CardBorderColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        ModernSingleChoiceCard(
                            title = "15. विद्यालय/SMC WhatsApp समूह में जोड़े गए?",
                            subtitle = "Added in WhatsApp Group?",
                            options = listOf("हाँ", "नहीं", "लंबित"),
                            selectedOption = whatsappGroupAdded,
                            onOptionSelected = { whatsappGroupAdded = it }
                        )

                        ModernSingleChoiceCard(
                            title = "16. पोस्टर लगाया गया?",
                            subtitle = "Poster Installed in School?",
                            options = listOf("हाँ", "नहीं"),
                            selectedOption = posterInstalled,
                            onOptionSelected = { posterInstalled = it }
                        )

                        ModernSingleChoiceCard(
                            title = "17. स्मार्ट क्लास की स्थिति",
                            subtitle = "Smart Class Status & Operations",
                            options = listOf("बहुत अच्छी", "अच्छी", "सामान्य", "खराब", "उपयोग में नहीं है", "स्मार्ट क्लास उपलब्ध नहीं है"),
                            selectedOption = smartClassStatus,
                            onOptionSelected = { smartClassStatus = it }
                        )
                    }

                    4 -> {
                        // ==========================================
                        // STEP 4: Observations & Follow-up
                        // ==========================================
                        ModernMultilineCard(
                            title = "18. मुख्य अवलोकन",
                            subtitle = "Key Observations during school visit",
                            value = keyObservations,
                            placeholder = "Write key observations during school visit...",
                            minLines = 3,
                            onValueChange = { keyObservations = it }
                        )

                        ModernMultilineCard(
                            title = "19. समस्याएं / सहायता आवश्यकता",
                            subtitle = "Problems Faced / Assistance Needed",
                            value = problemsOrAssistance,
                            placeholder = "Describe any problems faced or support needed...",
                            minLines = 2,
                            onValueChange = { problemsOrAssistance = it }
                        )

                        ModernSingleChoiceCard(
                            title = "20. फॉलो-अप आवश्यक है?",
                            subtitle = "Follow-up Required?",
                            options = listOf("हाँ", "नहीं"),
                            selectedOption = followupRequired,
                            onOptionSelected = { followupRequired = it }
                        )

                        ModernSingleChoiceCard(
                            title = "21. हार्ड डिस्क में डेटा आवश्यक है?",
                            subtitle = "Data Required on Hard Disk?",
                            options = listOf("हाँ", "नहीं"),
                            selectedOption = dataRequiredOnHardDisk,
                            onOptionSelected = { dataRequiredOnHardDisk = it }
                        )

                        ModernMultilineCard(
                            title = "22. अंतिम टिप्पणी",
                            subtitle = "Final Remarks / Overall Assessment",
                            value = finalRemarks,
                            placeholder = "Final remarks / overall assessment...",
                            minLines = 2,
                            onValueChange = { finalRemarks = it }
                        )
                    }

                    5 -> {
                        // ==========================================
                        // STEP 5: Photos Upload & Full Review
                        // ==========================================

                        // Photos Section Header
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BrandPurpleBg,
                            border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Photo & Media Uploads", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandPurpleDark)
                                    Text("Upload school photos and videos for the visit record", fontSize = 12.sp, color = Slate700)
                                }
                            }
                        }

                        PhotoCategory.entries.forEach { category ->
                            val currentList = photoMap[category.categoryId] ?: emptyList()
                            val isSatisfied = if (category.minRequired > 0) currentList.size >= category.minRequired else true

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, CardBorderColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(category.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (category.minRequired > 0) {
                                                    if (isSatisfied) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                                                } else {
                                                    if (currentList.isNotEmpty()) Color(0xFFECFDF5) else Color(0xFFF1F5F9)
                                                }
                                            ) {
                                                Text(
                                                    text = if (category.minRequired > 0) {
                                                        if (isSatisfied) "✓ Mandatory attached" else "• Mandatory (Min 1 required)"
                                                    } else {
                                                        if (currentList.isNotEmpty()) "✓ ${currentList.size} attached" else "Add photo"
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (category.minRequired > 0) {
                                                        if (isSatisfied) Emerald600 else Red600
                                                    } else {
                                                        if (currentList.isNotEmpty()) Emerald600 else Slate500
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (category.supportsVideo) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = BrandPurpleLight,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            activePhotoCategory = category
                                                            videoPickerLauncher.launch("video/*")
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Videocam, contentDescription = "Add Video", tint = BrandPurple, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = BrandPurpleLight,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        activePhotoCategory = category
                                                        photoPickerLauncher.launch("image/*")
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.CameraAlt, contentDescription = "Add Photo", tint = BrandPurple, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (currentList.isEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(84.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    activePhotoCategory = category
                                                    photoPickerLauncher.launch("image/*")
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            color = FormBackground,
                                            border = BorderStroke(1.dp, CardBorderColor)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (category.supportsVideo) "Add Photos / Videos (No limit)" else "+ Add Photo",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = BrandPurple
                                                )
                                            }
                                        }
                                    } else {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${currentList.size} item(s) attached",
                                                    fontSize = 12.sp,
                                                    color = Slate500,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "+ Add More",
                                                    fontSize = 12.sp,
                                                    color = BrandPurple,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        activePhotoCategory = category
                                                        photoPickerLauncher.launch("image/*")
                                                    }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                items(currentList) { uriStr ->
                                                    val isVideo = MediaStorageHelper.isMediaVideo(uriStr, context)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(88.dp)
                                                            .border(1.5.dp, if (isVideo) BrandPurple else Emerald600, RoundedCornerShape(14.dp))
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .clickable {
                                                                if (isVideo) {
                                                                    MediaStorageHelper.openMedia(context, uriStr)
                                                                } else {
                                                                    previewMediaUrl = uriStr
                                                                }
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = uriStr,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )

                                                        if (isVideo) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(Color.Black.copy(alpha = 0.4f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.PlayCircleFilled,
                                                                    contentDescription = "Play Video",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(32.dp)
                                                                )
                                                            }
                                                        }

                                                        // Delete Button Badge
                                                        IconButton(
                                                            onClick = {
                                                                val updatedList = (photoMap[category.categoryId] ?: mutableListOf()).toMutableList()
                                                                updatedList.remove(uriStr)
                                                                photoMap[category.categoryId] = updatedList
                                                            },
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .align(Alignment.TopEnd)
                                                                .padding(2.dp)
                                                                .clip(CircleShape)
                                                                .background(Red600)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Remove",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
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

                        // Complete Review Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Visit Summary Review", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(10.dp))

                                DetailRowItem("School Name", schoolName)
                                DetailRowItem("UDISE Code", udiseCode)
                                DetailRowItem("State / District", "$stateName / $district")
                                DetailRowItem("Principal Met", metPrincipal.ifBlank { "Not filled" })
                                DetailRowItem("App Knowledge", missionGyanAwareness.ifBlank { "Not filled" })
                                DetailRowItem("Participating Classes", if (selectedClasses.isNotEmpty()) selectedClasses.sorted().joinToString(", ") else "None")
                                DetailRowItem("Student Attendance", studentCount.ifBlank { "Not filled" })
                                DetailRowItem("School Response", schoolResponse.ifBlank { "Not filled" })
                                DetailRowItem("BCI Details", if (bciName.isNotBlank() || bciMobile.isNotBlank()) "$bciName ($bciMobile)" else "Not filled")
                                DetailRowItem("Smart Class Status", smartClassStatus.ifBlank { "Not filled" })
                                DetailRowItem("Key Observations", keyObservations.ifBlank { "None" })
                                DetailRowItem("Final Remarks", finalRemarks.ifBlank { "None" })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Submission Success Dialog
    if (showSubmissionSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubmissionSuccessDialog = false
                onBackClick()
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (isOnline) "Report Submitted & Synced" else "Report Saved Locally (ऑफलाइन सुरक्षित)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Navy900
                )
            },
            text = {
                Text(
                    text = submissionSuccessMessage,
                    fontSize = 14.sp,
                    color = Slate700,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmissionSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("OK / डैशबोर्ड पर जाएँ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }

    // Full Media Preview Dialog
    if (previewMediaUrl != null) {
        Dialog(onDismissRequest = { previewMediaUrl = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val isVid = MediaStorageHelper.isMediaVideo(previewMediaUrl ?: "", context)
                    if (isVid) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    MediaStorageHelper.openMedia(context, previewMediaUrl ?: "")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to Play Video", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        AsyncImage(
                            model = previewMediaUrl,
                            contentDescription = "Full Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(
                        onClick = { previewMediaUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }

    // Correct School Info Dialog
    if (showEditSchoolDialog) {
        AlertDialog(
            onDismissRequest = { showEditSchoolDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White,
            title = {
                Text("Correct School Details", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy900)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = udiseCode,
                        onValueChange = { input -> udiseCode = input.filter { it.isDigit() }.take(11) },
                        label = { Text("UDISE Code (11 Digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stateName,
                        onValueChange = { stateName = it },
                        label = { Text("State") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text("Block") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = principalName,
                        onValueChange = { principalName = it },
                        label = { Text("Principal Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = principalMobile,
                        onValueChange = { input -> principalMobile = input.filter { it.isDigit() }.take(10) },
                        label = { Text("Principal Mobile (10 Digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditSchoolDialog = false
                        initialSchool?.let {
                            onUpdateSchoolInfo(
                                it.copy(
                                    schoolName = schoolName,
                                    stateName = stateName,
                                    districtName = district,
                                    blockName = block,
                                    principalName = principalName,
                                    principalMobile = principalMobile
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Update Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSchoolDialog = false }) {
                    Text("Cancel", color = Slate500)
                }
            }
        )
    }
}

@Composable
private fun ModernSingleChoiceCard(
    title: String,
    subtitle: String? = null,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = Slate500)
            }
            Spacer(modifier = Modifier.height(14.dp))

            val chunkedOptions = options.chunked(2)
            chunkedOptions.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { option ->
                        val isSelected = selectedOption == option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOptionSelected(option) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) BrandPurpleLight else FormBackground,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) BrandPurple else CardBorderColor
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onOptionSelected(option) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = BrandPurple,
                                        unselectedColor = Slate500
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandPurpleDark else Slate700,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ModernMultilineCard(
    title: String,
    subtitle: String,
    value: String,
    placeholder: String,
    minLines: Int = 2,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = Slate500)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Slate500, fontSize = 13.sp) },
                minLines = minLines,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FormBackground,
                    unfocusedContainerColor = FormBackground,
                    focusedBorderColor = BrandPurple,
                    unfocusedBorderColor = CardBorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DetailRowItem(
    label: String,
    value: String,
    isUnspecified: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Slate500
        )

        if (isUnspecified) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF3C7)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Amber600,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Not specified",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber600
                    )
                }
            }
        } else {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
        }
    }
}

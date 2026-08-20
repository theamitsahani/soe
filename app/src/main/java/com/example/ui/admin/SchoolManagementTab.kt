package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.data.model.User
import com.example.ui.components.SearchTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileUpload
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper
import com.example.util.GoogleMapHelper
import com.example.util.ImportValidationResult
import com.example.util.IndiaLocationData
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SchoolViewFilter {
    ALL_ACTIVE,
    COMPLETED,
    PENDING,
    DELETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolManagementTab(
    schools: List<School>,
    employees: List<User> = emptyList(),
    onImportSchools: (List<School>, List<com.example.data.model.Visit>, (Result<Int>) -> Unit) -> Unit,
    onUpdateSchool: (School) -> Unit,
    onMarkSchoolCompleted: ((schoolId: String, visitDate: String, employeeId: String, employeeName: String, remarks: String) -> Unit)? = null,
    onMarkSchoolPending: ((schoolId: String) -> Unit)? = null,
    onDeleteSchool: ((String, ((Result<Unit>) -> Unit)?) -> Unit)? = null,
    onRestoreSchool: ((String, ((Result<Unit>) -> Unit)?) -> Unit)? = null,
    onPermanentDeleteSchool: ((String, ((Result<Unit>) -> Unit)?) -> Unit)? = null,
    onRefreshSchools: ((Result<Int>) -> Unit) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSchoolForEdit by remember { mutableStateOf<School?>(null) }
    var showAddSchoolDialog by remember { mutableStateOf(false) }
    var schoolToDelete by remember { mutableStateOf<School?>(null) }
    var schoolToPermanentDelete by remember { mutableStateOf<School?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isPerformingDeleteAction by remember { mutableStateOf(false) }
    var refreshErrorMessage by remember { mutableStateOf<String?>(null) }
    var successNotification by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(SchoolViewFilter.ALL_ACTIVE) }
    var isImporting by remember { mutableStateOf(false) }
    var isParsingExcel by remember { mutableStateOf(false) }
    var importValidationResult by remember { mutableStateOf<ImportValidationResult?>(null) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val activeSchools = remember(schools) { schools.filter { !it.isDeleted } }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isParsingExcel = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    ExcelHelper.parseSchoolCsv(context, uri, activeSchools)
                }
                isParsingExcel = false
                importValidationResult = result
            }
        }
    }
    val completedSchools = remember(activeSchools) { activeSchools.filter { it.visitDate.isNotBlank() } }
    val pendingSchools = remember(activeSchools) { activeSchools.filter { it.visitDate.isBlank() } }
    val deletedSchools = remember(schools) { schools.filter { it.isDeleted } }

    fun triggerRefresh() {
        isRefreshing = true
        refreshErrorMessage = null
        onRefreshSchools { res ->
            isRefreshing = false
            if (res.isFailure) {
                val err = res.exceptionOrNull()
                val errorMsg = err?.message ?: "Unknown error"
                refreshErrorMessage = "Sync notice: $errorMsg"
            }
        }
    }

    LaunchedEffect(Unit) {
        triggerRefresh()
    }

    val currentList = when (selectedFilter) {
        SchoolViewFilter.ALL_ACTIVE -> activeSchools
        SchoolViewFilter.COMPLETED -> completedSchools
        SchoolViewFilter.PENDING -> pendingSchools
        SchoolViewFilter.DELETED -> deletedSchools
    }

    val filteredSchools = remember(currentList, searchQuery) {
        if (searchQuery.isBlank()) currentList
        else currentList.filter {
            it.schoolName.contains(searchQuery, ignoreCase = true) ||
                    it.districtName.contains(searchQuery, ignoreCase = true) ||
                    it.blockName.contains(searchQuery, ignoreCase = true) ||
                    it.villageName.contains(searchQuery, ignoreCase = true) ||
                    it.principalName.contains(searchQuery, ignoreCase = true) ||
                    it.principalMobile.contains(searchQuery, ignoreCase = true) ||
                    it.visitDate.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (successNotification != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = successNotification!!,
                            color = Color(0xFF15803D),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { successNotification = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("OK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
            }
        }

        // Top Header with Actions (Manual Add + Refresh)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("School Directory (${activeSchools.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Text("स्कूल प्रबंधन एवं सूची", fontSize = 11.sp, color = Slate500)
                    }
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Indigo600)
                    } else {
                        IconButton(
                            onClick = { triggerRefresh() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Schools from Firestore",
                                tint = Indigo600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Equal-sized Action Buttons (Import Excel & Add School)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Import Excel Button
                    Button(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        enabled = !isParsingExcel && !isImporting
                    ) {
                        if (isParsingExcel) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reading...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Excel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Add School Button
                    Button(
                        onClick = { showAddSchoolDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add School", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Filter Chips: All Active vs Completed vs Pending vs Recently Deleted
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == SchoolViewFilter.ALL_ACTIVE,
                        onClick = { selectedFilter = SchoolViewFilter.ALL_ACTIVE },
                        label = { Text("All Active (${activeSchools.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Indigo600,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == SchoolViewFilter.COMPLETED,
                        onClick = { selectedFilter = SchoolViewFilter.COMPLETED },
                        label = { Text("✓ Completed (${completedSchools.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF059669),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == SchoolViewFilter.PENDING,
                        onClick = { selectedFilter = SchoolViewFilter.PENDING },
                        label = { Text("⏳ Pending (${pendingSchools.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.PendingActions, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == SchoolViewFilter.DELETED,
                        onClick = { selectedFilter = SchoolViewFilter.DELETED },
                        label = { Text("Deleted (${deletedSchools.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Red600,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }
        }

        // Search Bar
        item {
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = when (selectedFilter) {
                    SchoolViewFilter.ALL_ACTIVE -> "Search all active schools..."
                    SchoolViewFilter.COMPLETED -> "Search completed schools..."
                    SchoolViewFilter.PENDING -> "Search pending schools..."
                    SchoolViewFilter.DELETED -> "Search deleted schools in trash..."
                }
            )
        }

        if (filteredSchools.isEmpty()) {
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
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp, color = Indigo600)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Fetching schools from Firebase...", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Indigo600)
                        } else if (selectedFilter == SchoolViewFilter.DELETED) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Slate500, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No deleted schools in trash", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                            Text("Deleted schools can be restored here within 24 hours.", fontSize = 11.sp, color = Slate500)
                        } else if (selectedFilter == SchoolViewFilter.COMPLETED) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No completed schools yet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                            Text("Completed schools will show here.", fontSize = 11.sp, color = Slate500)
                        } else if (selectedFilter == SchoolViewFilter.PENDING) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No pending schools", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                        } else if (searchQuery.isNotBlank()) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No schools found matching search", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate500)
                        } else {
                            Icon(Icons.Default.School, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No schools available", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap '+ Add School' to enroll schools.", fontSize = 12.sp, color = Slate500)
                        }
                    }
                }
            }
        } else {
            items(filteredSchools, key = { it.schoolId }) { sch ->
                if (selectedFilter != SchoolViewFilter.DELETED) {
                    SchoolCardItem(
                        school = sch,
                        onEditClick = { selectedSchoolForEdit = sch },
                        onDeleteClick = { schoolToDelete = sch }
                    )
                } else {
                    DeletedSchoolCardItem(
                        school = sch,
                        onRestoreClick = {
                            if (onRestoreSchool != null) {
                                isPerformingDeleteAction = true
                                onRestoreSchool(sch.schoolId) { res ->
                                    isPerformingDeleteAction = false
                                    if (res.isSuccess) {
                                        successNotification = "${sch.schoolName} restored successfully!"
                                        triggerRefresh()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Failed to restore school", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onPermanentDeleteClick = { schoolToPermanentDelete = sch }
                    )
                }
            }
        }
    }

    // Soft Delete School Confirmation Dialog
    if (schoolToDelete != null) {
        val targetSchool = schoolToDelete!!
        AlertDialog(
            onDismissRequest = { if (!isPerformingDeleteAction) schoolToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Red600, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Delete School?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy900)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to delete '${targetSchool.schoolName}'?",
                        fontSize = 13.sp,
                        color = Navy900,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "This school will be moved to the Trash bin and can be restored within 24 hours.",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onDeleteSchool != null) {
                            isPerformingDeleteAction = true
                            onDeleteSchool(targetSchool.schoolId) { res ->
                                isPerformingDeleteAction = false
                                schoolToDelete = null
                                if (res.isSuccess) {
                                    successNotification = "${targetSchool.schoolName} moved to trash."
                                    triggerRefresh()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Failed to delete school", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val updated = targetSchool.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
                            onUpdateSchool(updated)
                            schoolToDelete = null
                            triggerRefresh()
                        }
                    },
                    enabled = !isPerformingDeleteAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Red600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isPerformingDeleteAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deleting...")
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolToDelete = null }, enabled = !isPerformingDeleteAction) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permanent Delete Confirmation Dialog
    if (schoolToPermanentDelete != null) {
        val targetSchool = schoolToPermanentDelete!!
        AlertDialog(
            onDismissRequest = { if (!isPerformingDeleteAction) schoolToPermanentDelete = null },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Red600, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Permanent Delete?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Red600)
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete '${targetSchool.schoolName}'? This action CANNOT be undone.",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onPermanentDeleteSchool != null) {
                            isPerformingDeleteAction = true
                            onPermanentDeleteSchool(targetSchool.schoolId) { res ->
                                isPerformingDeleteAction = false
                                schoolToPermanentDelete = null
                                if (res.isSuccess) {
                                    successNotification = "${targetSchool.schoolName} permanently deleted."
                                    triggerRefresh()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Failed to permanently delete school", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            schoolToPermanentDelete = null
                        }
                    },
                    enabled = !isPerformingDeleteAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Permanently Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolToPermanentDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add School Dialog
    if (showAddSchoolDialog) {
        var mStateName by remember { mutableStateOf("Rajasthan") }
        var mDistrictName by remember { mutableStateOf("") }
        var mSchoolName by remember { mutableStateOf("") }
        var mSchoolType by remember { mutableStateOf("") }
        var mVillageName by remember { mutableStateOf("") }
        var mPrincipalName by remember { mutableStateOf("") }
        var mBlockName by remember { mutableStateOf("") }
        var mPrincipalMobile by remember { mutableStateOf("") }
        var mMapLink by remember { mutableStateOf("") }
        var mError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        val isDuplicate = remember(mSchoolName, mDistrictName, activeSchools) {
            val cleanName = mSchoolName.trim().lowercase()
            val cleanDistrict = mDistrictName.trim().lowercase()
            cleanName.isNotBlank() && activeSchools.any {
                it.schoolName.trim().equals(cleanName, ignoreCase = true) &&
                (cleanDistrict.isBlank() || it.districtName.trim().equals(cleanDistrict, ignoreCase = true))
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddSchoolDialog = false },
            title = {
                Column {
                    Text("Add School Manually", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                    Text("नया स्कूल मैन्युअल रूप से जोड़ें", fontSize = 11.sp, color = Slate500)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mError != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = mError!!,
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = mSchoolName,
                        onValueChange = {
                            mSchoolName = it
                            mError = null
                        },
                        label = { Text("School Name (स्कूल का नाम) *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. GSSS Model School") },
                        singleLine = true,
                        isError = isDuplicate,
                        supportingText = {
                            if (isDuplicate) {
                                Text("⚠️ इस नाम का स्कूल पहले से मौजूद है", color = Red600, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mDistrictName,
                        onValueChange = {
                            mDistrictName = it
                            mError = null
                        },
                        label = { Text("District Name (जिला) *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Jaipur") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mBlockName,
                        onValueChange = { mBlockName = it },
                        label = { Text("Block Name (ब्लॉक का नाम)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Sanganer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mVillageName,
                        onValueChange = { mVillageName = it },
                        label = { Text("Village / City Name (गांव / शहर)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mSchoolType,
                        onValueChange = { mSchoolType = it },
                        label = { Text("School Type (प्रकार)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Sr. Sec., Sec., Primary") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mPrincipalName,
                        onValueChange = { mPrincipalName = it },
                        label = { Text("Principal Name (प्रधानाचार्य का नाम)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mPrincipalMobile,
                        onValueChange = { input ->
                            mPrincipalMobile = input.filter { it.isDigit() }.take(10)
                            mError = null
                        },
                        label = { Text("Principal Mobile (10 अंकों का मोबाइल)", fontSize = 12.sp) },
                        placeholder = { Text("10-digit mobile number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = mPrincipalMobile.isNotBlank() && mPrincipalMobile.length != 10,
                        supportingText = {
                            if (mPrincipalMobile.isNotBlank() && mPrincipalMobile.length != 10) {
                                Text("Mobile must be 10 digits (${mPrincipalMobile.length}/10)", color = Red600, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mStateName,
                        onValueChange = { mStateName = it },
                        label = { Text("State Name (राज्य)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mMapLink,
                        onValueChange = { mMapLink = it },
                        label = { Text("Google Map Link (मैप लिंक)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. https://maps.app.goo.gl/... or location URL") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Indigo600)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanState = IndiaLocationData.normalizeState(mStateName)
                        val cleanDistrict = IndiaLocationData.normalizeDistrict(cleanState, mDistrictName)
                        val cleanSchoolName = IndiaLocationData.normalizeSchoolName(mSchoolName)
                        val cleanMobile = mPrincipalMobile.trim().filter { it.isDigit() }

                        if (cleanSchoolName.isBlank()) {
                            mError = "School Name is required!"
                            return@Button
                        }
                        if (cleanDistrict.isBlank()) {
                            mError = "District Name is required!"
                            return@Button
                        }
                        if (isDuplicate) {
                            mError = "A school with this name and district already exists."
                            return@Button
                        }
                        if (cleanMobile.isNotBlank() && cleanMobile.length != 10) {
                            mError = "Principal mobile number must be exactly 10 digits."
                            return@Button
                        }

                        isSaving = true
                        val newSchool = School(
                            schoolId = "sch_" + java.util.UUID.randomUUID().toString().take(8),
                            stateName = cleanState,
                            districtName = cleanDistrict,
                            schoolName = cleanSchoolName,
                            schoolType = IndiaLocationData.normalizeSchoolType(mSchoolType),
                            villageName = IndiaLocationData.normalizeVillage(mVillageName),
                            principalName = IndiaLocationData.normalizeGenericName(mPrincipalName),
                            blockName = IndiaLocationData.normalizeBlock(mBlockName),
                            principalMobile = cleanMobile,
                            visitDate = "",
                            mapLink = mMapLink.trim(),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onImportSchools(listOf(newSchool), emptyList()) { res ->
                            isSaving = false
                            if (res.isSuccess) {
                                showAddSchoolDialog = false
                                successNotification = "$cleanSchoolName added successfully!"
                                triggerRefresh()
                            } else {
                                mError = res.exceptionOrNull()?.localizedMessage ?: "Failed to add school."
                            }
                        }
                    },
                    enabled = !isSaving && mSchoolName.isNotBlank() && mDistrictName.isNotBlank() && !isDuplicate,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saving...")
                    } else {
                        Text("Save School")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSchoolDialog = false }, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        )
    }

    // Excel Import Confirmation & Validation Dialog
    if (importValidationResult != null) {
        val result = importValidationResult!!
        var importError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isImporting) importValidationResult = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = Emerald600,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Excel Import Summary", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy900)
                    Text("फ़ाइल समीक्षा एवं पुष्टि", fontSize = 11.sp, color = Slate500)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (importError != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = importError!!,
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Stats Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Rows in File:", fontSize = 12.sp, color = Slate700)
                                Text("${result.totalRows}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Valid Schools to Import:", fontSize = 12.sp, color = Emerald600, fontWeight = FontWeight.SemiBold)
                                Text("${result.schoolsToImport.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                            }
                            if (result.duplicateRows > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Existing / Updated Schools:", fontSize = 12.sp, color = Color(0xFFD97706))
                                    Text("${result.duplicateRows}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                            }
                            if (result.completedVisitsToImport.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Completed Visits in File:", fontSize = 12.sp, color = Indigo600)
                                    Text("${result.completedVisitsToImport.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                                }
                            }
                            if (result.invalidRows > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Skipped / Invalid Rows:", fontSize = 12.sp, color = Red600)
                                    Text("${result.invalidRows}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red600)
                                }
                            }
                        }
                    }

                    // Preview of first few schools
                    if (result.schoolsToImport.isNotEmpty()) {
                        Text(
                            text = "Preview (First ${result.schoolsToImport.take(4).size} of ${result.schoolsToImport.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            result.schoolsToImport.take(4).forEach { sch ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(sch.schoolName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                        Text(
                                            "${sch.districtName} • Block: ${sch.blockName.ifBlank { "N/A" }} • Principal: ${sch.principalName.ifBlank { "N/A" }}",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Warning / Error list if any
                    if (result.errors.isNotEmpty()) {
                        Text("Errors / Warnings (${result.errors.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red600)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            result.errors.take(3).forEach { err ->
                                Text("• $err", fontSize = 11.sp, color = Red600)
                            }
                            if (result.errors.size > 3) {
                                Text("...and ${result.errors.size - 3} more rows skipped", fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImporting = true
                        importError = null
                        onImportSchools(result.schoolsToImport, result.completedVisitsToImport) { res ->
                            isImporting = false
                            if (res.isSuccess) {
                                val count = res.getOrNull() ?: result.schoolsToImport.size
                                importValidationResult = null
                                successNotification = "Successfully imported $count schools from Excel!"
                                triggerRefresh()
                            } else {
                                importError = res.exceptionOrNull()?.localizedMessage ?: "Failed to import schools to Firestore"
                            }
                        }
                    },
                    enabled = !isImporting && result.schoolsToImport.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importing (${result.schoolsToImport.size})...")
                    } else {
                        Text("Confirm & Import (${result.schoolsToImport.size})")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { importValidationResult = null }, enabled = !isImporting) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit School Dialog
    if (selectedSchoolForEdit != null) {
        val sch = selectedSchoolForEdit!!
        var eState by remember { mutableStateOf(sch.stateName) }
        var eDistrict by remember { mutableStateOf(sch.districtName) }
        var eSchoolName by remember { mutableStateOf(sch.schoolName) }
        var eSchoolType by remember { mutableStateOf(sch.schoolType) }
        var eVillage by remember { mutableStateOf(sch.villageName) }
        var ePrincipal by remember { mutableStateOf(sch.principalName) }
        var eBlock by remember { mutableStateOf(sch.blockName) }
        var eMobile by remember { mutableStateOf(sch.principalMobile) }
        var eMapLink by remember { mutableStateOf(sch.mapLink) }
        var eError by remember { mutableStateOf<String?>(null) }
        var isSavingEdit by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSavingEdit) selectedSchoolForEdit = null },
            title = {
                Column {
                    Text("Edit School Details (स्कूल संपादित करें)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                    Text("Update school information", fontSize = 11.sp, color = Slate500)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (eError != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = eError!!,
                                color = Red600,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = eSchoolName,
                        onValueChange = { eSchoolName = it },
                        label = { Text("School Name (स्कूल का नाम) *", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eDistrict,
                        onValueChange = { eDistrict = it },
                        label = { Text("District Name (जिला) *", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eBlock,
                        onValueChange = { eBlock = it },
                        label = { Text("Block Name (ब्लॉक)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eVillage,
                        onValueChange = { eVillage = it },
                        label = { Text("Village / City (गांव / शहर)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eSchoolType,
                        onValueChange = { eSchoolType = it },
                        label = { Text("School Type (प्रकार)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ePrincipal,
                        onValueChange = { ePrincipal = it },
                        label = { Text("Principal Name (प्रधानाचार्य का नाम)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eMobile,
                        onValueChange = { input ->
                            eMobile = input.filter { it.isDigit() }.take(10)
                        },
                        label = { Text("Principal Mobile (10-digit mobile)", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eState,
                        onValueChange = { eState = it },
                        label = { Text("State Name (राज्य)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eMapLink,
                        onValueChange = { eMapLink = it },
                        label = { Text("Google Map Link (मैप लिंक)", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Indigo600)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanState = IndiaLocationData.normalizeState(eState)
                        val cleanDist = IndiaLocationData.normalizeDistrict(cleanState, eDistrict)
                        val cleanName = IndiaLocationData.normalizeSchoolName(eSchoolName)
                        val cleanMob = eMobile.trim().filter { it.isDigit() }

                        if (cleanName.isBlank()) {
                            eError = "School Name is required"
                            return@Button
                        }
                        if (cleanDist.isBlank()) {
                            eError = "District Name is required"
                            return@Button
                        }
                        if (cleanMob.isNotBlank() && cleanMob.length != 10) {
                            eError = "Mobile number must be exactly 10 digits"
                            return@Button
                        }

                        isSavingEdit = true
                        val updated = sch.copy(
                            schoolName = cleanName,
                            districtName = cleanDist,
                            blockName = IndiaLocationData.normalizeBlock(eBlock),
                            villageName = IndiaLocationData.normalizeVillage(eVillage),
                            schoolType = IndiaLocationData.normalizeSchoolType(eSchoolType),
                            principalName = IndiaLocationData.normalizeGenericName(ePrincipal),
                            principalMobile = cleanMob,
                            stateName = cleanState,
                            mapLink = eMapLink.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onUpdateSchool(updated)
                        isSavingEdit = false
                        selectedSchoolForEdit = null
                        successNotification = "${updated.schoolName} updated successfully!"
                        triggerRefresh()
                    },
                    enabled = !isSavingEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSavingEdit) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saving...")
                    } else {
                        Text("Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSchoolForEdit = null }, enabled = !isSavingEdit) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SchoolCardItem(
    school: School,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = school.schoolName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    if (school.schoolType.isNotBlank()) {
                        Text(
                            text = school.schoolType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Indigo600
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit School", tint = Indigo600, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete School", tint = Red600, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val locationText = listOfNotNull(
                school.blockName.takeIf { it.isNotBlank() }?.let { "Block: $it" },
                school.villageName.takeIf { it.isNotBlank() }?.let { "Village: $it" },
                school.districtName.takeIf { it.isNotBlank() }?.let { "District: $it" },
                school.stateName.takeIf { it.isNotBlank() && it != "Rajasthan" }?.let { "State: $it" }
            ).joinToString(" • ")

            if (locationText.isNotBlank()) {
                Text(
                    text = locationText,
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Principal: ${school.principalName.ifBlank { "N/A" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (school.principalMobile.isNotBlank() && school.principalMobile != "N/A") {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${school.principalMobile}"))
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call Principal", tint = Indigo600, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = school.principalMobile.ifBlank { "N/A" },
                        fontSize = 12.sp,
                        color = if (school.principalMobile.isNotBlank() && school.principalMobile != "N/A") Indigo600 else Slate700,
                        fontWeight = if (school.principalMobile.isNotBlank() && school.principalMobile != "N/A") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            if (school.mapLink.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        GoogleMapHelper.startNavigation(
                            context = context,
                            mapLink = school.mapLink,
                            latitude = school.latitude,
                            longitude = school.longitude,
                            schoolName = school.schoolName,
                            address = school.districtName
                        )
                    }
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Map Location", tint = Indigo600, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Admin Google Maps Link", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
            Spacer(modifier = Modifier.height(8.dp))

            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (school.visitDate.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFFD1FAE5),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "✓ Completed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = school.visitDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⏳ Pending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (school.mapLink.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            GoogleMapHelper.startNavigation(
                                context = context,
                                mapLink = school.mapLink,
                                latitude = school.latitude,
                                longitude = school.longitude,
                                schoolName = school.schoolName,
                                address = school.districtName
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(12.dp), tint = Indigo600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Navigation", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeletedSchoolCardItem(
    school: School,
    onRestoreClick: () -> Unit,
    onPermanentDeleteClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val elapsedMillis = if (school.deletedAt > 0L) now - school.deletedAt else 0L
    val twentyFourHoursMillis = 24 * 60 * 60 * 1000L
    val isWithin24Hours = elapsedMillis <= twentyFourHoursMillis
    val remainingHours = if (isWithin24Hours) ((twentyFourHoursMillis - elapsedMillis) / (1000 * 60 * 60)).coerceAtLeast(0) else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = school.schoolName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = "${school.districtName} • ${school.blockName.ifBlank { "Block N/A" }}",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }

                Surface(
                    color = if (isWithin24Hours) Color(0xFFFEF3C7) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isWithin24Hours) "Restorable (${remainingHours}h left)" else "Expired (>24h)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWithin24Hours) Color(0xFFB45309) else Red600,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Principal: ${school.principalName.ifBlank { "N/A" }} (${school.principalMobile.ifBlank { "No mobile" }})",
                fontSize = 11.sp,
                color = Slate700
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPermanentDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Permanent Delete", tint = Red600, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onRestoreClick,
                    enabled = isWithin24Hours,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

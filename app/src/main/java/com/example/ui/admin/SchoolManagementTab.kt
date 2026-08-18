package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.ui.components.SearchTextField
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper
import com.example.util.ImportValidationResult

enum class SchoolViewFilter {
    ALL_ACTIVE,
    COMPLETED,
    PENDING,
    DELETED
}

@Composable
fun SchoolManagementTab(
    schools: List<School>,
    onImportSchools: (List<School>, List<com.example.data.model.Visit>, (Result<Int>) -> Unit) -> Unit,
    onUpdateSchool: (School) -> Unit,
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
    var importValidationResult by remember { mutableStateOf<ImportValidationResult?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isPerformingDeleteAction by remember { mutableStateOf(false) }
    var refreshErrorMessage by remember { mutableStateOf<String?>(null) }
    var successNotification by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(SchoolViewFilter.ALL_ACTIVE) }

    val context = LocalContext.current

    val activeSchools = remember(schools) { schools.filter { !it.isDeleted } }
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = ExcelHelper.parseSchoolCsv(context, uri, activeSchools)
            importValidationResult = result
        }
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

        // Top Header with Actions (Manual Add + Import Excel + Refresh)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("School Directory (${activeSchools.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            Text("स्कूल प्रबंधन एवं सूची", fontSize = 11.sp, color = Slate500)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Indigo600)
                        } else {
                            IconButton(
                                onClick = { triggerRefresh() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Schools from Firestore",
                                    tint = Indigo600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showAddSchoolDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Indigo600)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Import Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Filter Chips: All Active vs Completed vs Pending vs Recently Deleted
        item {
            androidx.compose.foundation.lazy.LazyRow(
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
                            Text("Schools completed via Excel import or submitted reports will show here.", fontSize = 11.sp, color = Slate500)
                        } else if (selectedFilter == SchoolViewFilter.PENDING) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No pending schools", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                            Text("All active schools are marked completed!", fontSize = 11.sp, color = Slate500)
                        } else if (searchQuery.isNotBlank()) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No schools found matching search", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate500)
                        } else {
                            Icon(Icons.Default.School, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No schools available", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap '+ Add' or 'Import Excel' to enroll schools.", fontSize = 12.sp, color = Slate500)
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("District: ${targetSchool.districtName.ifBlank { "N/A" }}", fontSize = 11.sp, color = Slate700)
                            Text("Principal: ${targetSchool.principalName.ifBlank { "N/A" }}", fontSize = 11.sp, color = Slate700)
                        }
                    }
                    Text(
                        text = "ℹ️ Note: This school will be moved to the Trash / Recently Deleted tab. You can restore it anytime within 24 hours.",
                        fontSize = 12.sp,
                        color = Indigo600,
                        fontWeight = FontWeight.Medium
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
                                    successNotification = "${targetSchool.schoolName} deleted (Restorable for 24h)"
                                    triggerRefresh()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Failed to delete school", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isPerformingDeleteAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deleting...")
                    } else {
                        Text("Delete School")
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
                    text = "This will permanently erase '${targetSchool.schoolName}' from Firestore and the local database. This action cannot be undone.",
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
                                    Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Failed to permanently delete", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolToPermanentDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add School Dialog (With Duplicate Prevention & 10-Digit Mobile Validation)
    if (showAddSchoolDialog) {
        var mStateName by remember { mutableStateOf("Rajasthan") }
        var mDistrictName by remember { mutableStateOf("") }
        var mSchoolName by remember { mutableStateOf("") }
        var mSchoolType by remember { mutableStateOf("") }
        var mVillageName by remember { mutableStateOf("") }
        var mPrincipalName by remember { mutableStateOf("") }
        var mBlockName by remember { mutableStateOf("") }
        var mPrincipalMobile by remember { mutableStateOf("") }
        var mVisitDate by remember { mutableStateOf("") }
        var mError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        // Duplicate Check: Same name & district in active schools
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

                    // 1. School Name
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
                                Text("⚠️ इस नाम का स्कूल इस जिले में पहले से मौजूद है (Duplicate school detected)", color = Red600, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. District Name
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

                    // 3. Block Name
                    OutlinedTextField(
                        value = mBlockName,
                        onValueChange = { mBlockName = it },
                        label = { Text("Block Name (ब्लॉक का नाम)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Sanganer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. Village Name
                    OutlinedTextField(
                        value = mVillageName,
                        onValueChange = { mVillageName = it },
                        label = { Text("Village / City Name (गांव / शहर)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 5. School Type
                    OutlinedTextField(
                        value = mSchoolType,
                        onValueChange = { mSchoolType = it },
                        label = { Text("School Type (प्रकार)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Sr. Sec., Sec., Primary") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 6. Principal Name
                    OutlinedTextField(
                        value = mPrincipalName,
                        onValueChange = { mPrincipalName = it },
                        label = { Text("Principal Name (प्रधानाचार्य का नाम)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 7. Principal Mobile Number (Strict 10 Digits)
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
                                Text("Mobile number must be exactly 10 digits (${mPrincipalMobile.length}/10)", color = Red600, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 8. State Name
                    OutlinedTextField(
                        value = mStateName,
                        onValueChange = { mStateName = it },
                        label = { Text("State Name (राज्य)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanSchoolName = mSchoolName.trim()
                        val cleanDistrict = mDistrictName.trim()
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
                            mError = "A school with this name and district already exists. Duplicate schools cannot be added."
                            return@Button
                        }
                        if (cleanMobile.isNotBlank() && cleanMobile.length != 10) {
                            mError = "Principal mobile number must be exactly 10 digits."
                            return@Button
                        }

                        isSaving = true
                        val newSchool = School(
                            schoolId = "sch_" + java.util.UUID.randomUUID().toString().take(8),
                            stateName = mStateName.trim().ifBlank { "Rajasthan" },
                            districtName = cleanDistrict,
                            schoolName = cleanSchoolName,
                            schoolType = mSchoolType.trim(),
                            villageName = mVillageName.trim(),
                            principalName = mPrincipalName.trim(),
                            blockName = mBlockName.trim(),
                            principalMobile = cleanMobile,
                            visitDate = mVisitDate.trim(),
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

    // Edit School Dialog (With 10-Digit Mobile Validation)
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
        var eVisitDate by remember { mutableStateOf(sch.visitDate) }
        var eError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { selectedSchoolForEdit = null },
            title = {
                Text("Edit School Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
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
                        onValueChange = {
                            eSchoolName = it
                            eError = null
                        },
                        label = { Text("School Name *", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eDistrict,
                        onValueChange = { eDistrict = it },
                        label = { Text("District", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eBlock,
                        onValueChange = { eBlock = it },
                        label = { Text("Block", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eVillage,
                        onValueChange = { eVillage = it },
                        label = { Text("Village", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eSchoolType,
                        onValueChange = { eSchoolType = it },
                        label = { Text("School Type", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ePrincipal,
                        onValueChange = { ePrincipal = it },
                        label = { Text("Principal Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eMobile,
                        onValueChange = { input ->
                            eMobile = input.filter { it.isDigit() }.take(10)
                            eError = null
                        },
                        label = { Text("Principal Mobile (10 Digits)", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = eMobile.isNotBlank() && eMobile.length != 10,
                        supportingText = {
                            if (eMobile.isNotBlank() && eMobile.length != 10) {
                                Text("Mobile must be 10 digits", color = Red600, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eVisitDate,
                        onValueChange = { eVisitDate = it },
                        label = { Text("Visit Date / Completion (विजिट की तारीख)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 15-Mar-2025 (Leave empty if pending)") },
                        singleLine = true,
                        supportingText = {
                            if (eVisitDate.isNotBlank()) {
                                Text("✓ This school is marked as COMPLETED (पूर्ण)", color = Color(0xFF059669), fontSize = 11.sp)
                            } else {
                                Text("⏳ This school is PENDING (विजिट बाकी)", color = Color(0xFFB45309), fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanMobile = eMobile.trim().filter { it.isDigit() }
                        if (eSchoolName.isBlank()) {
                            eError = "School Name cannot be empty"
                            return@Button
                        }
                        if (cleanMobile.isNotBlank() && cleanMobile.length != 10) {
                            eError = "Principal mobile number must be exactly 10 digits."
                            return@Button
                        }
                        val updated = sch.copy(
                            schoolName = eSchoolName.trim(),
                            districtName = eDistrict.trim(),
                            blockName = eBlock.trim(),
                            villageName = eVillage.trim(),
                            schoolType = eSchoolType.trim(),
                            principalName = ePrincipal.trim(),
                            principalMobile = cleanMobile,
                            visitDate = eVisitDate.trim(),
                            stateName = eState.trim().ifBlank { "Rajasthan" },
                            updatedAt = System.currentTimeMillis()
                        )
                        onUpdateSchool(updated)
                        selectedSchoolForEdit = null
                        successNotification = "${updated.schoolName} updated successfully!"
                        triggerRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSchoolForEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Excel Preview & Validation Dialog
    if (importValidationResult != null) {
        val res = importValidationResult!!
        var previewSearchQuery by remember { mutableStateOf("") }
        val previewFiltered = remember(res.schoolsToImport, previewSearchQuery) {
            if (previewSearchQuery.isBlank()) res.schoolsToImport
            else res.schoolsToImport.filter {
                it.schoolName.contains(previewSearchQuery, ignoreCase = true) ||
                        it.districtName.contains(previewSearchQuery, ignoreCase = true) ||
                        it.blockName.contains(previewSearchQuery, ignoreCase = true) ||
                        it.villageName.contains(previewSearchQuery, ignoreCase = true) ||
                        it.principalName.contains(previewSearchQuery, ignoreCase = true) ||
                        it.principalMobile.contains(previewSearchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isImporting) importValidationResult = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Excel Import Preview", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy900)
                        Text("वैध स्कूलों का पूर्वावलोकन", fontSize = 11.sp, color = Slate500)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald100
                    ) {
                        Text(
                            text = "${res.validRows} Valid",
                            color = Emerald600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total File Rows: ${res.totalRows}", fontSize = 12.sp, color = Slate700)
                                Text("Valid Schools: ${res.validRows}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                            }
                            if (res.duplicateRows > 0) {
                                Text("Duplicates Skipped: ${res.duplicateRows}", fontSize = 11.sp, color = Slate500)
                            }
                            if (res.invalidRows > 0) {
                                Text("Skipped (Empty Name): ${res.invalidRows}", fontSize = 11.sp, color = Red600, fontWeight = FontWeight.Medium)
                            }
                            if (res.completedVisitsToImport.isNotEmpty()) {
                                Text("Completed Visits Included: ${res.completedVisitsToImport.size}", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Parsed Valid Schools (${res.schoolsToImport.size}):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    if (res.schoolsToImport.size > 5) {
                        OutlinedTextField(
                            value = previewSearchQuery,
                            onValueChange = { previewSearchQuery = it },
                            placeholder = { Text("Filter preview list...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (previewFiltered.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "No valid schools found in preview matching filter.",
                                fontSize = 12.sp,
                                color = Slate500,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        previewFiltered.forEachIndexed { idx, school ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${school.schoolName}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (school.schoolType.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Slate100
                                            ) {
                                                Text(
                                                    text = school.schoolType,
                                                    fontSize = 10.sp,
                                                    color = Indigo600,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val loc = listOfNotNull(
                                        school.villageName.takeIf { it.isNotBlank() }?.let { "Village: $it" },
                                        school.blockName.takeIf { it.isNotBlank() }?.let { "Block: $it" },
                                        school.districtName.takeIf { it.isNotBlank() }?.let { "District: $it" }
                                    ).joinToString(" • ")

                                    if (loc.isNotBlank()) {
                                        Text(text = loc, fontSize = 11.sp, color = Slate500)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImporting = true
                        onImportSchools(res.schoolsToImport, res.completedVisitsToImport) { importRes ->
                            isImporting = false
                            if (importRes.isSuccess) {
                                Toast.makeText(context, "${res.schoolsToImport.size} schools imported successfully!", Toast.LENGTH_SHORT).show()
                                triggerRefresh()
                            } else {
                                Toast.makeText(context, "Import failed: ${importRes.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                            importValidationResult = null
                        }
                    },
                    enabled = !isImporting && res.schoolsToImport.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importing...")
                    } else {
                        Text("Confirm Import (${res.schoolsToImport.size} Schools)")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { importValidationResult = null },
                    enabled = !isImporting
                ) {
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

                val context = LocalContext.current
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

            Spacer(modifier = Modifier.height(6.dp))
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
                            text = "✓ Completed (विजिट पूर्ण)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Date: ${school.visitDate}",
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
                        text = "⏳ Pending (विजिट बाकी)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
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
                    Text("Restore (पुनर्स्थापित करें)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

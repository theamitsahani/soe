package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.data.model.VisitStatus
import com.example.ui.components.StatusChip
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandAccentDark
import com.example.ui.theme.BrandAccentLight
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.ExcelHelper
import com.example.util.IndiaLocationData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTab(
    visits: List<Visit>,
    schools: List<School> = emptyList(),
    initialStatusFilter: String = "All",
    onUpdateVisitAnswers: ((String, VisitAnswers) -> Unit)? = null,
    onDeletePhoto: ((visitId: String, categoryId: String, photoUrl: String) -> Unit)? = null,
    onAddPhoto: ((visitId: String, categoryId: String, photoUrl: String) -> Unit)? = null,
    onReviewVisit: ((visitId: String, isApproved: Boolean, notes: String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusChip by remember { mutableStateOf(initialStatusFilter) }
    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }

    var isFiltersExpanded by remember { mutableStateOf(false) }

    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }

    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val statusChips = listOf("All", "Data Required", "Pending Review", "Completed", "Rejected")

    // Combine explicit visits with synthesized records for completed schools, excluding deleted schools
    val uniqueVisits = remember(visits, schools) {
        val deletedSchoolIds = schools.filter { it.isDeleted }.map { it.schoolId }.toSet()
        val nonDeletedVisits = visits.filter { !deletedSchoolIds.contains(it.schoolId) }
        val existingSchoolIds = nonDeletedVisits.map { it.schoolId }.toSet() +
                nonDeletedVisits.map { it.schoolId.removePrefix("sch_") }.toSet() +
                nonDeletedVisits.map { "sch_" + it.schoolId.removePrefix("sch_") }.toSet()
        val missingVisits = schools.filter {
            it.visitDate.isNotBlank() && !it.isDeleted &&
            !existingSchoolIds.contains(it.schoolId) &&
            !existingSchoolIds.contains(it.schoolId.removePrefix("sch_"))
        }.map { sch ->
            Visit(
                visitId = "vst_" + sch.schoolId.removePrefix("sch_") + "_legacy",
                schoolId = sch.schoolId,
                employeeId = "emp_admin",
                employeeName = "Admin (Prior Completion)",
                schoolName = sch.schoolName,
                state = sch.stateName,
                district = sch.districtName,
                block = sch.blockName,
                villageName = sch.villageName,
                schoolType = sch.schoolType,
                principalName = sch.principalName,
                principalMobile = sch.principalMobile,
                visitDate = sch.visitDate,
                status = VisitStatus.SUBMITTED,
                answersJson = "{\"q1_soeName\":\"Admin (Prior Completion)\",\"q2_visitDate\":\"${sch.visitDate}\",\"q3_schoolName\":\"${sch.schoolName}\",\"q5_district\":\"${sch.districtName}\",\"q6_block\":\"${sch.blockName}\",\"q7_principalName\":\"${sch.principalName}\",\"q8_principalMobile\":\"${sch.principalMobile}\",\"q9_metPrincipal\":\"हाँ\",\"q10_missionGyanAwareness\":\"हाँ\",\"q11_studentCount\":\"Verified\",\"q12_schoolResponse\":\"Completed (Previous Visit)\",\"q20_finalRemarks\":\"Completed prior to app launch / Verified by Admin (Date: ${sch.visitDate})\"}",
                photosJson = "{}",
                syncStatus = com.example.data.model.SyncStatus.SYNCED,
                createdAt = sch.createdAt,
                updatedAt = sch.updatedAt
            )
        }
        (nonDeletedVisits + missingVisits).distinctBy { it.visitId }
    }

    val stateList = remember(uniqueVisits) {
        listOf("All States") + uniqueVisits.map { IndiaLocationData.normalizeState(it.state) }.distinct().sorted()
    }

    val districtList = remember(uniqueVisits, selectedState) {
        val base = if (selectedState == "All States") uniqueVisits else uniqueVisits.filter { IndiaLocationData.areEqual(it.state.ifBlank { "Rajasthan" }, selectedState) }
        listOf("All Districts") + base.map { IndiaLocationData.normalizeDistrict(it.state, it.district) }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val blockList = remember(uniqueVisits, selectedState, selectedDistrict) {
        val base = uniqueVisits.filter {
            (selectedState == "All States" || IndiaLocationData.areEqual(it.state.ifBlank { "Rajasthan" }, selectedState)) &&
            (selectedDistrict == "All Districts" || IndiaLocationData.areEqual(it.district, selectedDistrict))
        }
        listOf("All Blocks") + base.map { IndiaLocationData.normalizeBlock(it.block) }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val activeFiltersCount = remember(selectedState, selectedDistrict, selectedBlock, selectedStatusChip, searchQuery) {
        var count = 0
        if (selectedState != "All States") count++
        if (selectedDistrict != "All Districts") count++
        if (selectedBlock != "All Blocks") count++
        if (selectedStatusChip != "All") count++
        count
    }

    val filteredVisits = remember(uniqueVisits, searchQuery, selectedStatusChip, selectedState, selectedDistrict, selectedBlock) {
        uniqueVisits.filter { v ->
            val vState = IndiaLocationData.normalizeState(v.state)
            val vDistrict = IndiaLocationData.normalizeDistrict(vState, v.district)
            val vBlock = IndiaLocationData.normalizeBlock(v.block)

            val matchState = selectedState == "All States" || IndiaLocationData.areEqual(vState, selectedState)
            val matchDistrict = selectedDistrict == "All Districts" || IndiaLocationData.areEqual(vDistrict, selectedDistrict)
            val matchBlock = selectedBlock == "All Blocks" || IndiaLocationData.areEqual(vBlock, selectedBlock)

            val matchStatus = when (selectedStatusChip) {
                "Completed" -> v.status == VisitStatus.SUBMITTED || v.status == VisitStatus.REVIEWED
                "Pending Review" -> v.status == VisitStatus.ASSIGNED || v.status == VisitStatus.STARTED || (v.status == VisitStatus.SUBMITTED && v.reviewedBy.isNullOrBlank())
                "Data Required" -> v.answersJson.contains("\"q19_dataRequiredOnHardDisk\":\"हाँ\"")
                "Rejected" -> v.status == VisitStatus.REJECTED || v.rejectionReason.isNotBlank() || v.answersJson.contains("\"q18_followupRequired\":\"हाँ\"")
                else -> true
            }

            val matchQuery = searchQuery.isBlank() || (
                v.schoolName.contains(searchQuery, ignoreCase = true) ||
                v.district.contains(searchQuery, ignoreCase = true) ||
                v.block.contains(searchQuery, ignoreCase = true) ||
                v.employeeName.contains(searchQuery, ignoreCase = true)
            )
            matchState && matchDistrict && matchBlock && matchStatus && matchQuery
        }
    }

    val dropdownFieldColors = OutlinedTextFieldDefaults.colors(
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Title and Export Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Visit Reports",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "${uniqueVisits.size} field reports in total",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }

                    // Primary Export Button
                    Button(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandAccent,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export Reports",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top Search Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        placeholder = {
                            Text(
                                "Search by school, district, block, officer...",
                                fontSize = 13.sp,
                                color = Slate500
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = BrandAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = Slate500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            }

            // Collapsible Filters Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Toggle Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isFiltersExpanded = !isFiltersExpanded }
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = BrandAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Filters",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )

                                // Active Count Badge
                                if (activeFiltersCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandAccentLight,
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = "$activeFiltersCount active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (activeFiltersCount > 0) {
                                    TextButton(
                                        onClick = {
                                            selectedState = "All States"
                                            selectedDistrict = "All Districts"
                                            selectedBlock = "All Blocks"
                                            selectedStatusChip = "All"
                                            searchQuery = ""
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Clear all",
                                            fontSize = 11.sp,
                                            color = Red600,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isFiltersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isFiltersExpanded) "Collapse" else "Expand",
                                    tint = Slate500,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Expandable Content
                        AnimatedVisibility(
                            visible = isFiltersExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Status Filter Chips (Horizontal Scrollable)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Status Filter",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate500
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        statusChips.forEach { chipName ->
                                            val isSelected = selectedStatusChip == chipName
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) BrandAccent else Slate100,
                                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                                modifier = Modifier
                                                    .clickable { selectedStatusChip = chipName }
                                            ) {
                                                Text(
                                                    text = chipName,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else Slate700,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Cascading Location Dropdowns (State, District, Block)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Location Category",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate500
                                    )

                                    // State Filter
                                    ExposedDropdownMenuBox(
                                        expanded = stateExpanded,
                                        onExpandedChange = { stateExpanded = !stateExpanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = selectedState,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("State (राज्य)", fontSize = 11.sp) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = dropdownFieldColors,
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = stateExpanded,
                                            onDismissRequest = { stateExpanded = false }
                                        ) {
                                            stateList.forEach { s ->
                                                DropdownMenuItem(
                                                    text = { Text(s, fontSize = 13.sp, fontWeight = if (selectedState == s) FontWeight.Bold else FontWeight.Normal) },
                                                    onClick = {
                                                        selectedState = s
                                                        selectedDistrict = "All Districts"
                                                        selectedBlock = "All Blocks"
                                                        stateExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // District Filter
                                        ExposedDropdownMenuBox(
                                            expanded = districtExpanded,
                                            onExpandedChange = { districtExpanded = !districtExpanded },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                value = selectedDistrict,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("District (ज़िला)", fontSize = 11.sp) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = dropdownFieldColors,
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = districtExpanded,
                                                onDismissRequest = { districtExpanded = false }
                                            ) {
                                                districtList.forEach { d ->
                                                    DropdownMenuItem(
                                                        text = { Text(d, fontSize = 13.sp, fontWeight = if (selectedDistrict == d) FontWeight.Bold else FontWeight.Normal) },
                                                        onClick = {
                                                            selectedDistrict = d
                                                            selectedBlock = "All Blocks"
                                                            districtExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Block Filter
                                        ExposedDropdownMenuBox(
                                            expanded = blockExpanded,
                                            onExpandedChange = { blockExpanded = !blockExpanded },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                value = selectedBlock,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Block (ब्लॉक)", fontSize = 11.sp) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = dropdownFieldColors,
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = blockExpanded,
                                                onDismissRequest = { blockExpanded = false }
                                            ) {
                                                blockList.forEach { b ->
                                                    DropdownMenuItem(
                                                        text = { Text(b, fontSize = 13.sp, fontWeight = if (selectedBlock == b) FontWeight.Bold else FontWeight.Normal) },
                                                        onClick = {
                                                            selectedBlock = b
                                                            blockExpanded = false
                                                        }
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

            // Results count indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredVisits.size} reports",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }

            // Empty State
            if (filteredVisits.isEmpty()) {
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
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(BrandAccentLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = BrandAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "No reports found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )

                            Text(
                                text = "No visit reports match your active search and filter criteria. Try adjusting the keywords or clearing the filters.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            if (activeFiltersCount > 0 || searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedStatusChip = "All"
                                        selectedState = "All States"
                                        selectedDistrict = "All Districts"
                                        selectedBlock = "All Blocks"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
                                ) {
                                    Text("Clear All Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Reports List
                items(filteredVisits, key = { it.visitId }) { visit ->
                    val answers = remember(visit.answersJson) {
                        try {
                            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                            moshi.adapter(VisitAnswers::class.java).fromJson(visit.answersJson) ?: VisitAnswers()
                        } catch (e: Exception) {
                            VisitAnswers()
                        }
                    }
                    val hasFollowup = answers.q18_followupRequired.trim().equals("हाँ", ignoreCase = true)
                    val hasHardDisk = answers.q19_dataRequiredOnHardDisk.trim().equals("हाँ", ignoreCase = true)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVisitForDetails = visit },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "District: ${visit.district} • Block: ${visit.block}",
                                fontSize = 12.sp,
                                color = Slate500
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Officer: ${visit.employeeName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate700
                                )
                                Text(
                                    text = "Date: ${visit.visitDate}",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }

                            // Follow-up & Hard Disk Status Badges / Action Row
                            if (hasFollowup || hasHardDisk) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (hasFollowup) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Red100)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Red600,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Follow-up Required (फॉलो-अप)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Red600
                                                )
                                            }
                                            if (onUpdateVisitAnswers != null) {
                                                Button(
                                                    onClick = {
                                                        onUpdateVisitAnswers(visit.visitId, answers.copy(q18_followupRequired = "नहीं"))
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Uncheck", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    if (hasHardDisk) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Amber100)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Download,
                                                    contentDescription = null,
                                                    tint = Amber600,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Hard Disk Data Needed (डेटा आवश्यक)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Amber600
                                                )
                                            }
                                            if (onUpdateVisitAnswers != null) {
                                                Button(
                                                    onClick = {
                                                        onUpdateVisitAnswers(visit.visitId, answers.copy(q19_dataRequiredOnHardDisk = "नहीं"))
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Mark Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        }
    }

    // Comprehensive Single-Page Aligned Visit Detail Dialog
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
                selectedVisitForDetails = null
            },
            onDeletePhoto = { catId, url ->
                onDeletePhoto?.invoke(visit.visitId, catId, url)
                selectedVisitForDetails = null
            },
            onAddPhoto = { catId, url ->
                onAddPhoto?.invoke(visit.visitId, catId, url)
            },
            onReviewVisit = if (onReviewVisit != null) { isApproved, notes ->
                onReviewVisit(visit.visitId, isApproved, notes)
                selectedVisitForDetails = null
            } else null
        )
    }

    // Format Selection Dialog (PDF, Excel, CSV)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandAccentLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Export Reports",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Select format for ${filteredVisits.size} visit records",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // PDF Option
                    ExportOptionCard(
                        title = "PDF Document (.pdf)",
                        subtitle = "Printable summary report with headers and table pages",
                        badgeColor = Color(0xFFDC2626),
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            showExportDialog = false
                            ExcelHelper.exportVisitsToPdf(context, filteredVisits)
                        }
                    )

                    // Excel Option
                    ExportOptionCard(
                        title = "Excel Spreadsheet (.xlsx)",
                        subtitle = "Complete dataset for Microsoft Excel & Google Sheets",
                        badgeColor = Color(0xFF16A34A),
                        icon = Icons.Default.TableChart,
                        onClick = {
                            showExportDialog = false
                            ExcelHelper.exportVisitsToExcel(context, filteredVisits)
                        }
                    )

                    // CSV Option
                    ExportOptionCard(
                        title = "CSV File (.csv)",
                        subtitle = "Comma-separated text file with UTF-8 BOM encoding",
                        badgeColor = Color(0xFF2563EB),
                        icon = Icons.Default.Description,
                        onClick = {
                            showExportDialog = false
                            ExcelHelper.exportVisitsToCsv(context, filteredVisits)
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Medium, color = Slate500)
                }
            }
        )
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    badgeColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text(subtitle, fontSize = 11.sp, color = Slate500)
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

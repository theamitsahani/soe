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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.components.SearchTextField
import com.example.ui.components.StatusChip
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper
import com.example.util.IndiaLocationData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTab(
    visits: List<Visit>,
    schools: List<School> = emptyList(),
    initialStatusFilter: String = "All Statuses",
    onUpdateVisitAnswers: ((String, VisitAnswers) -> Unit)? = null,
    onDeletePhoto: ((visitId: String, categoryId: String, photoUrl: String) -> Unit)? = null,
    onAddPhoto: ((visitId: String, categoryId: String, photoUrl: String) -> Unit)? = null,
    onReviewVisit: ((visitId: String, isApproved: Boolean, notes: String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(initialStatusFilter) }
    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }

    var statusExpanded by remember { mutableStateOf(false) }
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }

    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val statusList = listOf("All Statuses", "Completed", "Data Required on Hard Disk", "Follow-up Required", "Pending")

    // Combine explicit visits with synthesized records for any completed schools that don't have an explicit visit object yet,
    // and strictly exclude any visits that belong to deleted schools
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

    val filteredVisits = remember(uniqueVisits, searchQuery, selectedStatus, selectedState, selectedDistrict, selectedBlock) {
        uniqueVisits.filter { v ->
            val vState = IndiaLocationData.normalizeState(v.state)
            val vDistrict = IndiaLocationData.normalizeDistrict(vState, v.district)
            val vBlock = IndiaLocationData.normalizeBlock(v.block)

            val matchState = selectedState == "All States" || IndiaLocationData.areEqual(vState, selectedState)
            val matchDistrict = selectedDistrict == "All Districts" || IndiaLocationData.areEqual(vDistrict, selectedDistrict)
            val matchBlock = selectedBlock == "All Blocks" || IndiaLocationData.areEqual(vBlock, selectedBlock)
            
            val matchStatus = when (selectedStatus) {
                "Completed" -> v.status == VisitStatus.SUBMITTED || v.status == VisitStatus.REVIEWED
                "Pending" -> v.status == VisitStatus.ASSIGNED || v.status == VisitStatus.STARTED
                "Follow-up Required" -> v.answersJson.contains("\"q18_followupRequired\":\"हाँ\"")
                "Data Required on Hard Disk" -> v.answersJson.contains("\"q19_dataRequiredOnHardDisk\":\"हाँ\"")
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
                    Text("Visit Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("${visits.size} submitted field reports", fontSize = 12.sp, color = Slate500)
                }

                Button(
                    onClick = {
                        showExportDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Status Filter Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "Filter Status: $selectedStatus",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Visit Status Filter", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusList.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st, fontSize = 13.sp, fontWeight = if (selectedStatus == st) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedStatus = st
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // State Filter
                    ExposedDropdownMenuBox(
                        expanded = stateExpanded,
                        onExpandedChange = { stateExpanded = !stateExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedState,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("State", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = stateExpanded,
                            onDismissRequest = { stateExpanded = false }
                        ) {
                            stateList.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, fontSize = 12.sp) },
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
                        label = { Text("District", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        districtList.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d, fontSize = 12.sp) },
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
                        label = { Text("Block", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = blockExpanded,
                        onDismissRequest = { blockExpanded = false }
                    ) {
                        blockList.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b, fontSize = 12.sp) },
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

        item {
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search reports by school, district, block, or officer..."
            )
        }

        if (filteredVisits.isEmpty()) {
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
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No reports found matching criteria", fontSize = 14.sp, color = Slate500)
                    }
                }
            }
        } else {
            items(filteredVisits) { visit ->
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
                            Text(visit.schoolName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900, modifier = Modifier.weight(1f))
                            StatusChip(statusName = visit.status.name)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("District: ${visit.district} • Block: ${visit.block}", fontSize = 12.sp, color = Slate500)

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Officer: ${visit.employeeName}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate700)
                            Text("Date: ${visit.visitDate}", fontSize = 12.sp, color = Slate500)
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
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Red600, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Follow-up Required (फॉलो-अप)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red600)
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
                                            Icon(Icons.Default.Download, contentDescription = null, tint = Amber600, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Hard Disk Data Needed (डेटा आवश्यक)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber600)
                                        }
                                        if (onUpdateVisitAnswers != null) {
                                            Button(
                                                onClick = {
                                                    onUpdateVisitAnswers(visit.visitId, answers.copy(q19_dataRequiredOnHardDisk = "नहीं"))
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
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
                            .background(Indigo600.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Indigo600,
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

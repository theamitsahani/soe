package com.example.ui.employee

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.ui.components.AppHeader
import com.example.ui.components.EmptyState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectionScreen(
    schools: List<School>,
    districts: List<String>,
    onSelectSchool: (School) -> Unit,
    onAddNewSchool: (School) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var showAddSchoolDialog by remember { mutableStateOf(false) }

    val filteredSchools = remember(schools, searchQuery, selectedDistrict) {
        schools.filter { school ->
            val matchesSearch = searchQuery.isBlank() ||
                    school.schoolName.contains(searchQuery, ignoreCase = true) ||
                    school.udiseCode.contains(searchQuery, ignoreCase = true) ||
                    school.blockName.contains(searchQuery, ignoreCase = true)

            val matchesDistrict = selectedDistrict == null || school.districtName.equals(selectedDistrict, ignoreCase = true)

            matchesSearch && matchesDistrict
        }
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppHeader(
                title = "Select School for Visit",
                subtitle = "Choose target school to begin questionnaire",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = { showAddSchoolDialog = true },
                        modifier = Modifier.testTag("btn_add_new_school_top")
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Add School", tint = Indigo600)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, UDISE or block...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Slate400)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("input_search_school"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Indigo600,
                    unfocusedBorderColor = Slate300
                ),
                singleLine = true
            )

            // District Filter Chips
            if (districts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDistrict == null,
                            onClick = { selectedDistrict = null },
                            label = { Text("All Districts (${schools.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(districts) { district ->
                        val count = schools.count { it.districtName.equals(district, ignoreCase = true) }
                        FilterChip(
                            selected = selectedDistrict == district,
                            onClick = { selectedDistrict = if (selectedDistrict == district) null else district },
                            label = { Text("$district ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredSchools.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Schools Found",
                    description = "No schools matched your search criteria. You can add a new school below.",
                    modifier = Modifier.weight(1f),
                    actionButton = {
                        Button(
                            onClick = { showAddSchoolDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add New School")
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                ) {
                    items(filteredSchools, key = { it.schoolId }) { school ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSchool(school) }
                                .testTag("school_item_${school.schoolId}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = Indigo600,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = school.schoolName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${school.blockName}, ${school.districtName}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate600
                                        )
                                    )
                                    if (school.udiseCode.isNotBlank()) {
                                        Text(
                                            text = "UDISE: ${school.udiseCode} • ${school.schoolType}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate400
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSchoolDialog) {
        var name by remember { mutableStateOf("") }
        var district by remember { mutableStateOf(selectedDistrict ?: "Jaipur") }
        var block by remember { mutableStateOf("") }
        var udise by remember { mutableStateOf("") }
        var principal by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSchoolDialog = false },
            title = { Text("Add New School", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
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
                        label = { Text("Principal / In-Charge Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Contact Mobile") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && district.isNotBlank()) {
                            val newSchool = School(
                                schoolId = "SCH_${System.currentTimeMillis()}",
                                schoolName = name,
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

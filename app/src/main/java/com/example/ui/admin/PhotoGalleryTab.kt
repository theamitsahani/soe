package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.PhotoCategory
import com.example.data.model.Visit
import com.example.ui.components.SearchTextField
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandAccentLight
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.ExcelHelper
import com.example.util.IndiaLocationData
import com.example.util.MediaStorageHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch

data class PhotoGridItem(
    val url: String,
    val categoryId: String,
    val categoryName: String,
    val visitId: String,
    val schoolName: String,
    val state: String,
    val district: String,
    val block: String,
    val date: String
)

data class CategoryFolderData(
    val categoryName: String,
    val categoryId: String,
    val photos: List<PhotoGridItem>,
    val schoolCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryTab(
    visits: List<Visit>,
    onDeletePhoto: ((visitId: String, categoryId: String, photoUrl: String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var isFiltersExpanded by remember { mutableStateOf(false) }

    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }
    var selectedSchoolName by remember { mutableStateOf("All Schools") }
    var selectedCategory by remember { mutableStateOf("All") }

    var openedCategoryName by remember { mutableStateOf<String?>(null) }

    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }
    var schoolExpanded by remember { mutableStateOf(false) }

    var selectedPhotoItem by remember { mutableStateOf<PhotoGridItem?>(null) }
    var photoToDelete by remember { mutableStateOf<PhotoGridItem?>(null) }

    val context = LocalContext.current
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
    val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

    val allPhotos = remember(visits) {
        val list = mutableListOf<PhotoGridItem>()
        val cleanVisits = visits.distinctBy { it.visitId }
        for (v in cleanVisits) {
            try {
                val photoMap = photosAdapter.fromJson(v.photosJson) ?: emptyMap()
                for ((catId, urls) in photoMap) {
                    val catObj = PhotoCategory.fromId(catId)
                    val cleanUrls = urls.distinct()
                    for (u in cleanUrls) {
                        list.add(
                            PhotoGridItem(
                                url = u,
                                categoryId = catId,
                                categoryName = catObj.displayName,
                                visitId = v.visitId,
                                schoolName = v.schoolName,
                                state = if (v.state.isNotBlank()) v.state else "Rajasthan",
                                district = v.district,
                                block = v.block,
                                date = v.visitDate
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        list.distinctBy { "${it.visitId}_${it.categoryId}_${it.url}" }
    }

    // Standardized States
    val stateList = remember(allPhotos) {
        listOf("All States") + allPhotos.map { IndiaLocationData.normalizeState(it.state) }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Standardized Districts
    val districtList = remember(allPhotos, selectedState) {
        val base = if (selectedState == "All States") allPhotos else allPhotos.filter { IndiaLocationData.areEqual(it.state.ifBlank { "Rajasthan" }, selectedState) }
        listOf("All Districts") + base.map { IndiaLocationData.normalizeDistrict(it.state, it.district) }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Standardized Blocks
    val blockList = remember(allPhotos, selectedState, selectedDistrict) {
        val base = allPhotos.filter {
            (selectedState == "All States" || IndiaLocationData.areEqual(it.state.ifBlank { "Rajasthan" }, selectedState)) &&
            (selectedDistrict == "All Districts" || IndiaLocationData.areEqual(it.district, selectedDistrict))
        }
        listOf("All Blocks") + base.map { IndiaLocationData.normalizeBlock(it.block) }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // School Names
    val schoolNamesList = remember(allPhotos, selectedState, selectedDistrict, selectedBlock) {
        val base = allPhotos.filter {
            (selectedState == "All States" || IndiaLocationData.areEqual(it.state.ifBlank { "Rajasthan" }, selectedState)) &&
            (selectedDistrict == "All Districts" || IndiaLocationData.areEqual(it.district, selectedDistrict)) &&
            (selectedBlock == "All Blocks" || IndiaLocationData.areEqual(it.block, selectedBlock))
        }
        listOf("All Schools") + base.map { it.schoolName }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val categoryChips = remember {
        listOf("All") + PhotoCategory.entries.map { it.displayName }
    }

    val activeFiltersCount = remember(selectedState, selectedDistrict, selectedBlock, selectedSchoolName, selectedCategory, searchQuery) {
        var count = 0
        if (selectedState != "All States") count++
        if (selectedDistrict != "All Districts") count++
        if (selectedBlock != "All Blocks") count++
        if (selectedSchoolName != "All Schools") count++
        if (selectedCategory != "All" && selectedCategory != "All Categories") count++
        if (searchQuery.isNotBlank()) count++
        count
    }

    val filteredPhotos = remember(
        allPhotos,
        selectedState,
        selectedDistrict,
        selectedBlock,
        selectedSchoolName,
        selectedCategory,
        searchQuery
    ) {
        allPhotos.filter { item ->
            val itemState = IndiaLocationData.normalizeState(item.state)
            val itemDistrict = IndiaLocationData.normalizeDistrict(itemState, item.district)
            val itemBlock = IndiaLocationData.normalizeBlock(item.block)

            val matchState = selectedState == "All States" || IndiaLocationData.areEqual(itemState, selectedState)
            val matchDistrict = selectedDistrict == "All Districts" || IndiaLocationData.areEqual(itemDistrict, selectedDistrict)
            val matchBlock = selectedBlock == "All Blocks" || IndiaLocationData.areEqual(itemBlock, selectedBlock)
            val matchSchool = selectedSchoolName == "All Schools" || item.schoolName == selectedSchoolName
            val matchCategory = selectedCategory == "All" || selectedCategory == "All Categories" || item.categoryName == selectedCategory

            val matchQuery = searchQuery.isBlank() || (
                item.schoolName.contains(searchQuery, ignoreCase = true) ||
                item.district.contains(searchQuery, ignoreCase = true) ||
                item.block.contains(searchQuery, ignoreCase = true) ||
                item.categoryName.contains(searchQuery, ignoreCase = true) ||
                item.date.contains(searchQuery, ignoreCase = true)
            )

            matchState && matchDistrict && matchBlock && matchSchool && matchCategory && matchQuery
        }
    }

    // Grouping by Category Folders based on current filter state
    val categoryFolders = remember(filteredPhotos, selectedCategory) {
        val groups = filteredPhotos.groupBy { it.categoryName }
        if (selectedCategory != "All" && selectedCategory != "All Categories") {
            val photosForCat = groups[selectedCategory] ?: emptyList()
            if (photosForCat.isNotEmpty()) {
                listOf(
                    CategoryFolderData(
                        categoryName = selectedCategory,
                        categoryId = photosForCat.first().categoryId,
                        photos = photosForCat,
                        schoolCount = photosForCat.map { it.schoolName }.distinct().size
                    )
                )
            } else {
                emptyList()
            }
        } else {
            groups.map { (catName, pList) ->
                CategoryFolderData(
                    categoryName = catName,
                    categoryId = pList.firstOrNull()?.categoryId ?: "",
                    photos = pList,
                    schoolCount = pList.map { it.schoolName }.distinct().size
                )
            }.sortedByDescending { it.photos.size }
        }
    }

    val filterDescription = remember(selectedState, selectedDistrict, selectedBlock, selectedSchoolName, selectedCategory, openedCategoryName) {
        val parts = mutableListOf<String>()
        if (selectedState != "All States") parts.add(selectedState)
        if (selectedDistrict != "All Districts") parts.add(selectedDistrict)
        if (selectedBlock != "All Blocks") parts.add(selectedBlock)
        if (selectedSchoolName != "All Schools") parts.add(selectedSchoolName)
        if (openedCategoryName != null) parts.add(openedCategoryName!!)
        else if (selectedCategory != "All" && selectedCategory != "All Categories") parts.add(selectedCategory)
        if (parts.isEmpty()) "All_Schools_Photos" else parts.joinToString("_")
    }

    val scope = rememberCoroutineScope()
    var isExportingZip by remember { mutableStateOf(false) }
    var exportProgressText by remember { mutableStateOf("") }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    fun downloadZip(photosToExport: List<PhotoGridItem>, desc: String) {
        if (photosToExport.isEmpty() || isExportingZip) return
        isExportingZip = true
        exportProgressText = "Preparing ${photosToExport.size} photos..."
        exportErrorMessage = null
        scope.launch {
            try {
                ExcelHelper.exportPhotosAsZip(
                    context = context,
                    photos = photosToExport,
                    filterDescription = desc
                ) { current, total ->
                    exportProgressText = "Archiving photo $current of $total..."
                }
            } catch (e: Exception) {
                exportErrorMessage = e.localizedMessage ?: "Failed to export ZIP file."
            } finally {
                isExportingZip = false
            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Title and Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "School Photo Gallery",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = if (openedCategoryName != null) {
                            "Folder: $openedCategoryName (${filteredPhotos.count { it.categoryName == openedCategoryName }} photos)"
                        } else {
                            "${categoryFolders.size} Folders • ${filteredPhotos.size} Total Photos"
                        },
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }

                Button(
                    onClick = {
                        val targetPhotos = if (openedCategoryName != null) {
                            filteredPhotos.filter { it.categoryName == openedCategoryName }
                        } else {
                            filteredPhotos
                        }
                        downloadZip(targetPhotos, filterDescription)
                    },
                    enabled = filteredPhotos.isNotEmpty() && !isExportingZip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAccent,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isExportingZip) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporting...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val count = if (openedCategoryName != null) filteredPhotos.count { it.categoryName == openedCategoryName } else filteredPhotos.size
                        Text("Export ZIP ($count)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (exportErrorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exportErrorMessage!!,
                            color = Red600,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { exportErrorMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Red600, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Top Search Bar (Compact)
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search photos by school, district, block, category..."
            )

            // Collapsible Filters Bar
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
                                        selectedSchoolName = "All Schools"
                                        selectedCategory = "All"
                                        searchQuery = ""
                                        openedCategoryName = null
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
                            // Category Filter Chips (Horizontal Scrollable)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Photo Category Filter",
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
                                    categoryChips.forEach { chipName ->
                                        val isSelected = (selectedCategory == chipName) || (selectedCategory == "All Categories" && chipName == "All")
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) BrandAccent else Slate100,
                                            border = if (isSelected) null else BorderStroke(1.dp, Slate200),
                                            modifier = Modifier
                                                .clickable {
                                                    selectedCategory = chipName
                                                    openedCategoryName = null
                                                }
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

                            // Cascading Location Dropdowns (State, District, Block, School)
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
                                                    selectedSchoolName = "All Schools"
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
                                                        selectedSchoolName = "All Schools"
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
                                                        selectedSchoolName = "All Schools"
                                                        blockExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // School Filter Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = schoolExpanded,
                                    onExpandedChange = { schoolExpanded = !schoolExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedSchoolName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Target School (स्कूल फ़िल्टर)", fontSize = 11.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolExpanded) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = dropdownFieldColors,
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = schoolExpanded,
                                        onDismissRequest = { schoolExpanded = false }
                                    ) {
                                        schoolNamesList.forEach { sName ->
                                            DropdownMenuItem(
                                                text = { Text(sName, fontSize = 13.sp, fontWeight = if (selectedSchoolName == sName) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    selectedSchoolName = sName
                                                    schoolExpanded = false
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

            // VIEW RENDERING: Opened Folder vs Folders Overview
            if (openedCategoryName != null) {
                // INSIDE AN OPENED CATEGORY FOLDER
                val currentCatName = openedCategoryName!!
                val currentPhotos = filteredPhotos.filter { it.categoryName == currentCatName }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Amber100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { openedCategoryName = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Folders",
                                    tint = Amber600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Amber600, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentCatName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                }
                                Text(
                                    text = "${currentPhotos.size} Photos from ${currentPhotos.map { it.schoolName }.distinct().size} Schools" +
                                            if (selectedBlock != "All Blocks") " • $selectedBlock" else if (selectedDistrict != "All Districts") " • $selectedDistrict" else "",
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { downloadZip(currentPhotos, "${filterDescription}_${currentCatName}") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Folder ZIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (currentPhotos.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Photos Found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No photos match the selected filters or search query in this category.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(currentPhotos) { photo ->
                            val isVideo = MediaStorageHelper.isMediaVideo(photo.url, context)
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.clickable {
                                    if (isVideo) {
                                        MediaStorageHelper.openMedia(context, photo.url)
                                    } else {
                                        selectedPhotoItem = photo
                                    }
                                }
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                    ) {
                                        AsyncImage(
                                            model = photo.url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (isVideo) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.35f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.Default.PlayCircleFilled,
                                                        contentDescription = "Play Video",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Text("VIDEO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(photo.schoolName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900, maxLines = 1)
                                        Text(
                                            text = "${photo.block} • ${photo.date}",
                                            fontSize = 10.sp,
                                            color = Slate500,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CATEGORY FOLDERS LIST / OVERVIEW
                if (categoryFolders.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Photo Folders Match",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try clearing or adjusting your search query and location filters.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoryFolders) { folder ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        openedCategoryName = folder.categoryName
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Amber100,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = Amber600,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = folder.categoryName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BrandAccentLight,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "${folder.photos.size} Photos",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "From ${folder.schoolCount} Schools" +
                                                            if (selectedBlock != "All Blocks") " • $selectedBlock" else if (selectedDistrict != "All Districts") " • $selectedDistrict" else "",
                                                    fontSize = 11.sp,
                                                    color = Slate500
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                downloadZip(folder.photos, "${filterDescription}_${folder.categoryName}")
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Slate100)
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = "Download Folder ZIP",
                                                tint = Slate700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Open Folder",
                                            tint = Slate400,
                                            modifier = Modifier.size(22.dp)
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

    // Full Media Preview Dialog
    selectedPhotoItem?.let { photo ->
        Dialog(onDismissRequest = { selectedPhotoItem = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = photo.url,
                        contentDescription = "Full Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top control bar (Close & Delete for Admin)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDeletePhoto != null) {
                            IconButton(
                                onClick = { photoToDelete = photo },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Photo",
                                    tint = Red600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }

                        IconButton(
                            onClick = { selectedPhotoItem = null },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Preview",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bottom info label
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(12.dp)
                    ) {
                        Text(photo.schoolName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${photo.categoryName} • ${photo.district} (${photo.block}) • ${photo.date}",
                            color = Color(0xFFD1D5DB),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting
    photoToDelete?.let { targetPhoto ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Photo? (फोटो हटाएं?)", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Text(
                    "Are you sure you want to permanently delete this photo from ${targetPhoto.schoolName} (${targetPhoto.categoryName})?",
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePhoto?.invoke(targetPhoto.visitId, targetPhoto.categoryId, targetPhoto.url)
                        photoToDelete = null
                        if (selectedPhotoItem?.url == targetPhoto.url) {
                            selectedPhotoItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { photoToDelete = null }) {
                    Text("Cancel", color = Slate700)
                }
            }
        )
    }
}

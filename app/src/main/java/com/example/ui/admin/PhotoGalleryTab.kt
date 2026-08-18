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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.PhotoCategory
import com.example.data.model.Visit
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper
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
    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }
    var selectedSchoolName by remember { mutableStateOf("All Schools") }
    var selectedCategory by remember { mutableStateOf("All Categories") }

    var openedCategoryName by remember { mutableStateOf<String?>(null) }

    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }
    var schoolExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var selectedPhotoItem by remember { mutableStateOf<PhotoGridItem?>(null) }
    var photoToDelete by remember { mutableStateOf<PhotoGridItem?>(null) }

    val context = LocalContext.current
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
    val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

    val allPhotos = remember(visits) {
        val list = mutableListOf<PhotoGridItem>()
        // BUG FIX: was distinctBy { "${it.schoolId}_${it.employeeId}" }, which threw away every
        // photo belonging to a legitimate second visit (re-visit) by the same employee to the
        // same school — those photos simply never appeared in the gallery or any ZIP/CSV export
        // built from it. Dedup of true duplicate documents already happens upstream at sync
        // time; visitId is the real key, and the line below already dedupes individual photo
        // URLs within/across those visits.
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

    val stateList = remember(allPhotos) {
        listOf("All States") + allPhotos.map { it.state }.filter { it.isNotBlank() }.distinct()
    }

    val districtList = remember(allPhotos, selectedState) {
        val base = if (selectedState == "All States") allPhotos else allPhotos.filter { it.state == selectedState }
        listOf("All Districts") + base.map { it.district }.filter { it.isNotBlank() }.distinct()
    }

    val blockList = remember(allPhotos, selectedState, selectedDistrict) {
        val base = allPhotos.filter {
            (selectedState == "All States" || it.state == selectedState) &&
            (selectedDistrict == "All Districts" || it.district == selectedDistrict)
        }
        listOf("All Blocks") + base.map { it.block }.filter { it.isNotBlank() }.distinct()
    }

    val schoolNamesList = remember(allPhotos, selectedState, selectedDistrict, selectedBlock) {
        val base = allPhotos.filter {
            (selectedState == "All States" || it.state == selectedState) &&
            (selectedDistrict == "All Districts" || it.district == selectedDistrict) &&
            (selectedBlock == "All Blocks" || it.block == selectedBlock)
        }
        listOf("All Schools") + base.map { it.schoolName }.distinct()
    }

    val categoryList = remember {
        listOf("All Categories") + PhotoCategory.entries.map { it.displayName }
    }

    val filteredPhotos = remember(allPhotos, selectedState, selectedDistrict, selectedBlock, selectedSchoolName, selectedCategory) {
        allPhotos.filter {
            (selectedState == "All States" || it.state == selectedState) &&
            (selectedDistrict == "All Districts" || it.district == selectedDistrict) &&
            (selectedBlock == "All Blocks" || it.block == selectedBlock) &&
            (selectedSchoolName == "All Schools" || it.schoolName == selectedSchoolName) &&
            (selectedCategory == "All Categories" || it.categoryName == selectedCategory)
        }
    }

    // Grouping by Category Folders based on current filter state
    val categoryFolders = remember(filteredPhotos, selectedCategory) {
        val groups = filteredPhotos.groupBy { it.categoryName }
        if (selectedCategory != "All Categories") {
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
        else if (selectedCategory != "All Categories") parts.add(selectedCategory)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("School Photo Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text(
                    text = if (openedCategoryName != null) {
                        "Folder: $openedCategoryName (${filteredPhotos.count { it.categoryName == openedCategoryName }} photos)"
                    } else {
                        "${categoryFolders.size} Categories • ${filteredPhotos.size} Total Photos"
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
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
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

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Location Filters: State, District, Block
        Text("1. Location Filters (स्थान फ़िल्टर)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    label = { Text("State", fontSize = 11.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = stateExpanded,
                    onDismissRequest = { stateExpanded = false }
                ) {
                    stateList.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item, fontSize = 12.sp) },
                            onClick = {
                                selectedState = item
                                selectedDistrict = "All Districts"
                                selectedBlock = "All Blocks"
                                selectedSchoolName = "All Schools"
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
                    label = { Text("District", fontSize = 11.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = districtExpanded,
                    onDismissRequest = { districtExpanded = false }
                ) {
                    districtList.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item, fontSize = 12.sp) },
                            onClick = {
                                selectedDistrict = item
                                selectedBlock = "All Blocks"
                                selectedSchoolName = "All Schools"
                                districtExpanded = false
                            }
                        )
                    }
                }
            }

            // Block Dropdown
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
                    blockList.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item, fontSize = 12.sp) },
                            onClick = {
                                selectedBlock = item
                                selectedSchoolName = "All Schools"
                                blockExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Photo Category & School Filters
        Text("2. Category & School Filters (श्रेणी एवं स्कूल फ़िल्टर)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Photo Style / Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.weight(1.1f)
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category Filter", fontSize = 11.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categoryList.forEach { cName ->
                        DropdownMenuItem(
                            text = { Text(cName, fontSize = 12.sp) },
                            onClick = {
                                selectedCategory = cName
                                openedCategoryName = null // Reset opened view to show the folder
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // School Filter Dropdown
            ExposedDropdownMenuBox(
                expanded = schoolExpanded,
                onExpandedChange = { schoolExpanded = !schoolExpanded },
                modifier = Modifier.weight(0.9f)
            ) {
                OutlinedTextField(
                    value = selectedSchoolName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter School", fontSize = 11.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = schoolExpanded,
                    onDismissRequest = { schoolExpanded = false }
                ) {
                    schoolNamesList.forEach { sName ->
                        DropdownMenuItem(
                            text = { Text(sName, fontSize = 12.sp) },
                            onClick = {
                                selectedSchoolName = sName
                                schoolExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            if (currentPhotos.isEmpty()) {
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
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No photos in this folder for current filters", fontSize = 14.sp, color = Slate500)
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
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No photo folders found for selected filters", fontSize = 14.sp, color = Slate500)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categoryFolders) { folder ->
                        com.example.ui.components.LiquidGlassCard(
                            cornerRadius = 16.dp,
                            elevation = 2.dp,
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
                                                color = Slate100,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = "${folder.photos.size} Photos",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Indigo600,
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
                                    .clip(CircleShape)
                                    .background(Red600.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Photo",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }

                        IconButton(
                            onClick = { selectedPhotoItem = null },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Bottom info caption
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(photo.categoryName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${photo.schoolName} • ${photo.district} • ${photo.date}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete Photo Confirmation Dialog for Admin
    photoToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Photo?", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Are you sure you want to permanently delete this photo from ${item.schoolName}?", color = Slate700) },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = item
                        photoToDelete = null
                        selectedPhotoItem = null
                        onDeletePhoto?.invoke(toDel.visitId, toDel.categoryId, toDel.url)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Cancel", color = Slate700)
                }
            }
        )
    }

    // Export Progress Dialog
    if (isExportingZip) {
        Dialog(onDismissRequest = { /* prevent dismiss while creating zip */ }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Indigo600,
                        strokeWidth = 4.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Exporting Photos ZIP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exportProgressText.ifBlank { "Packaging filtered photos into ZIP..." },
                            fontSize = 13.sp,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}

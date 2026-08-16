package com.example.ui.employee

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.JsonHelper
import com.example.data.model.PhotoCategories
import com.example.data.model.Visit
import com.example.data.model.VisitPhoto
import com.example.ui.components.AppHeader
import com.example.ui.components.SyncBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    visit: Visit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val answers = remember(visit) { JsonHelper.fromJson(visit.answersJson) }
    val photos = remember(visit) { JsonHelper.photosFromJson(visit.photosJson) }
    var selectedPhotoForPreview by remember { mutableStateOf<VisitPhoto?>(null) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppHeader(
                title = "Visit Report Details",
                subtitle = "${visit.schoolName} (${visit.visitDate})",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // Overview Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            SyncBadge(isSynced = visit.isSynced)
                            Text(
                                text = "${visit.visitDate} • ${visit.visitTime}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = visit.schoolName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Location: ${visit.block}, ${visit.district}, ${visit.state}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Visited by: ${visit.employeeName}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Indigo600,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Section 1: Verification
            item {
                DetailSection(title = "1. School Verification & Authority") {
                    DetailRow(label = "UDISE Code", value = answers.udiseCode.ifBlank { "Not provided" })
                    DetailRow(label = "Met Principal in Person", value = answers.metPrincipal)
                    if (answers.bciTeacherName.isNotBlank()) {
                        DetailRow(label = "BCI / In-Charge Teacher", value = answers.bciTeacherName)
                    }
                    if (answers.bciMobile.isNotBlank()) {
                        DetailRow(
                            label = "Teacher Mobile",
                            value = answers.bciMobile,
                            action = {
                                TextButton(onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${answers.bciMobile}"))
                                    context.startActivity(intent)
                                }) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Emerald600)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", color = Emerald600)
                                }
                            }
                        )
                    }
                }
            }

            // Section 2: Demo & Student Awareness
            item {
                DetailSection(title = "2. Student Demo & Awareness") {
                    DetailRow(label = "Mission Gyan Awareness", value = answers.missionGyanAwareness)
                    DetailRow(label = "Total Students Attended Demo", value = "${answers.totalStudentsAttended} students")
                    DetailRow(
                        label = "Participating Classes",
                        value = if (answers.participatingClasses.isNotEmpty()) answers.participatingClasses.joinToString(", ") else "None specified"
                    )
                    DetailRow(label = "Added to WhatsApp Group", value = answers.whatsappGroupAdded)
                }
            }

            // Section 3: Infrastructure & Assessment
            item {
                DetailSection(title = "3. Infrastructure & Remarks") {
                    DetailRow(label = "Promotional Poster Installed", value = answers.posterInstalled)
                    DetailRow(label = "Smart Class / Lab Status", value = answers.smartClassStatus)
                    DetailRow(label = "Key Observations", value = answers.keyObservations.ifBlank { "None" })
                    if (answers.problemsOrAssistance.isNotBlank()) {
                        DetailRow(label = "Problems / Assistance Needed", value = answers.problemsOrAssistance)
                    }
                    DetailRow(label = "Follow-up Required", value = answers.followupRequired)
                    if (answers.additionalNotes.isNotBlank()) {
                        DetailRow(label = "Additional Notes", value = answers.additionalNotes)
                    }
                }
            }

            // Section 4: Photo Gallery
            item {
                DetailSection(title = "4. Evidence Photos (${photos.size})") {
                    if (photos.isEmpty()) {
                        Text(
                            text = "No photos were attached with this visit report.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            PhotoCategories.ALL.forEach { cat ->
                                val catPhotos = photos.filter { it.categoryId == cat.id }
                                if (catPhotos.isNotEmpty()) {
                                    Text(
                                        text = "${cat.title} (${cat.titleHindi})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Indigo700
                                        )
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        catPhotos.forEach { photo ->
                                            Box(
                                                modifier = Modifier
                                                    .size(90.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(1.dp, Slate300, RoundedCornerShape(10.dp))
                                                    .clickable { selectedPhotoForPreview = photo }
                                            ) {
                                                AsyncImage(
                                                    model = photo.remoteUrl.ifBlank { photo.localUri },
                                                    contentDescription = photo.caption,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Photo Preview Modal
    selectedPhotoForPreview?.let { photo ->
        Dialog(onDismissRequest = { selectedPhotoForPreview = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = photo.caption,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { selectedPhotoForPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = photo.remoteUrl.ifBlank { photo.localUri },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Slate100
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
            )
        }
        if (action != null) {
            action()
        }
    }
}

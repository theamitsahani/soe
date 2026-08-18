package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.PhotoCategory
import com.example.data.model.School
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.data.model.VisitEvent
import com.example.data.model.VisitStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.MediaStorageHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Composable
fun VisitDetailDialog(
    visit: Visit,
    school: School? = null,
    events: List<VisitEvent> = emptyList(),
    onDismiss: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onUpdateAnswers: ((VisitAnswers) -> Unit)? = null,
    onDeletePhoto: ((categoryId: String, photoUrl: String) -> Unit)? = null,
    onReviewVisit: ((isApproved: Boolean, notes: String) -> Unit)? = null,
    isAdmin: Boolean = false,
    isEditable: Boolean = false,
    editTimeRemainingText: String = ""
) {
    val context = LocalContext.current
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }
    var photoToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewApprovalMode by remember { mutableStateOf(true) }
    var reviewNotesText by remember { mutableStateOf("") }

    val initialAnswers = remember(visit.answersJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            moshi.adapter(VisitAnswers::class.java).fromJson(visit.answersJson) ?: VisitAnswers()
        } catch (e: Exception) {
            VisitAnswers()
        }
    }
    var currentAnswers by remember(visit.answersJson) { mutableStateOf(initialAnswers) }
    val answers = currentAnswers

    val photoMap = remember(visit.photosJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
            val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
            val rawMap = adapter.fromJson(visit.photosJson) ?: emptyMap()
            rawMap.mapValues { entry -> entry.value.distinct() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Consolidated Principal details (prefer visit snapshot if available)
    val principalName = visit.principalName.ifBlank { answers.q7_principalName.ifBlank { school?.principalName?.ifBlank { "Not Specified" } ?: "Not Specified" } }
    val principalMobile = visit.principalMobile.ifBlank { answers.q8_principalMobile.ifBlank { school?.principalMobile?.ifBlank { school?.mobile ?: "" } ?: "" } }

    // Consolidated BCI details
    val bciName = answers.q13_bciName.ifBlank {
        if (answers.q13_bciContactDetails.contains("-")) answers.q13_bciContactDetails.substringBefore("-").trim()
        else answers.q13_bciContactDetails
    }
    val bciMobile = answers.q13_bciMobile.ifBlank {
        if (answers.q13_bciContactDetails.contains("-")) answers.q13_bciContactDetails.substringAfter("-").trim()
        else ""
    }

    // Consolidated Village, UDISE & School Type (prefer visit snapshot)
    val villageName = visit.villageName.ifBlank { school?.villageName?.ifBlank { "Not Specified" } ?: "Not Specified" }
    val udiseCode = visit.udiseCode.ifBlank { answers.q4_udiseCode.ifBlank { school?.referenceCode ?: "Not Available" } }
    val schoolType = visit.schoolType.ifBlank { school?.schoolType?.ifBlank { "Government School" } ?: "Government School" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.94f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = Navy900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Indigo600),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = visit.schoolName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Full Visit Report (सम्पूर्ण विज़िट विवरण)",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Scrollable Content Body - All Details Aligned on a Single Page
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF8FAFC))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Compact Status & Officer Info Bar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(statusName = visit.status.name)
                                Text(
                                    text = "Visit Date: ${visit.visitDate}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Indigo600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Field Officer: ${visit.employeeName.ifBlank { "Field Officer" }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Navy900
                                    )
                                }

                                if (isEditable && editTimeRemainingText.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = editTimeRemainingText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Indigo600,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. School Master Details (स्कूल की मास्टर जानकारी)
                    DetailSectionCard(
                        title = "1. School & Master Information (स्कूल मास्टर विवरण)",
                        icon = Icons.Default.School
                    ) {
                        DetailGridRow("School Name (विद्यालय का नाम)", visit.schoolName)
                        DetailGridRow("School Type / Category", schoolType)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("District (जिला)", visit.district)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("Block (ब्लॉक)", visit.block)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("Village (गांव का नाम)", villageName)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("State (राज्य)", visit.state.ifBlank { "Rajasthan" })
                            }
                        }
                        DetailGridRow("UDISE / Ref Code (यू-डाइस कोड)", udiseCode)
                    }

                    // 2. Principal Contact Details (प्रधानाचार्य संपर्क विवरण) with Direct Call
                    DetailSectionCard(
                        title = "2. Principal Information (प्रधानाचार्य विवरण)",
                        icon = Icons.Default.Person
                    ) {
                        DetailGridRow("Principal Name (प्रधानाचार्य का नाम)", principalName)

                        // Call Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Principal Mobile (मोबाइल नंबर)", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (principalMobile.isNotBlank()) principalMobile else "Not Provided",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                }

                                if (principalMobile.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$principalMobile"))
                                            context.startActivity(dialIntent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. BCI Contact Details (BCI अधिकारी विवरण - Point 13) with Direct Call
                    DetailSectionCard(
                        title = "3. BCI Officer Details (BCI अधिकारी विवरण)",
                        icon = Icons.Default.Phone
                    ) {
                        DetailGridRow("BCI Officer Name (BCI का नाम)", bciName.ifBlank { "Not Specified" })

                        // BCI Call Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("BCI Contact Number (BCI मोबाइल नंबर)", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (bciMobile.isNotBlank()) bciMobile else "Not Provided",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                }

                                if (bciMobile.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$bciMobile"))
                                            context.startActivity(dialIntent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call BCI", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call BCI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Form Questionnaire Responses (विज़िट फॉर्म के सभी बिंदु)
                    DetailSectionCard(
                        title = "4. Field Survey Responses (विज़िट फॉर्म प्रश्नोत्तरी)",
                        icon = Icons.Default.Assignment
                    ) {
                        DetailGridRow("9. Met Principal? (प्रधानाचार्य से मुलाकात)", answers.q9_metPrincipal.ifBlank { "Not Recorded" })
                        DetailGridRow("10. Mission Gyan App Awareness (ऐप की जानकारी)", answers.q10_missionGyanAwareness.ifBlank { "Not Recorded" })
                        DetailGridRow("11. Participating Classes (शामिल कक्षाएं)", answers.q22_participatingClasses.ifBlank { "None Selected" })
                        DetailGridRow("12. Student Attendance Count (उपस्थित विद्यार्थी संख्या)", answers.q11_studentCount.ifBlank { "Not Recorded" })
                        DetailGridRow("13. School Response / Reception (विद्यालय प्रतिक्रिया)", answers.q12_schoolResponse.ifBlank { "Not Recorded" })
                        DetailGridRow("14. BCI Officer Details (BCI विवरण)", if (bciName.isNotBlank() || bciMobile.isNotBlank()) "$bciName ($bciMobile)" else "Not Recorded")
                        DetailGridRow("15. Added to WhatsApp Group (WhatsApp ग्रुप में जोड़े गए)", answers.q14_whatsappGroupAdded.ifBlank { "Not Recorded" })
                        DetailGridRow("16. Poster Installed Status (पोस्टर लगाया गया)", answers.q15_posterInstalled.ifBlank { "Not Recorded" })
                        DetailGridRow("17. Smart Class Status (स्मार्ट क्लास की स्थिति)", answers.q21_smartClassStatus.ifBlank { "Not Recorded" })
                        DetailGridRow("18. Key Observations (मुख्य अवलोकन)", answers.q16_keyObservations.ifBlank { "None" })
                        DetailGridRow("19. Problems / Help Required (समस्याएं / आवश्यकता)", answers.q17_problemsOrAssistance.ifBlank { "None" })
                        
                        DetailGridRow("20. Follow-up Required (फॉलो-अप आवश्यकता)", answers.q18_followupRequired.ifBlank { "नहीं" })
                        if (answers.q18_followupRequired.trim().equals("हाँ", ignoreCase = true) && onUpdateAnswers != null) {
                            Button(
                                onClick = {
                                    onUpdateAnswers(answers.copy(q18_followupRequired = "नहीं"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("फॉलो-अप पूर्ण / हटाएं (Uncheck Follow-up)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        DetailGridRow("21. Data Required on Hard Disk (हार्ड डिस्क डेटा आवश्यक)", answers.q19_dataRequiredOnHardDisk.ifBlank { "नहीं" })
                        if (answers.q19_dataRequiredOnHardDisk.trim().equals("हाँ", ignoreCase = true) && onUpdateAnswers != null) {
                            Button(
                                onClick = {
                                    onUpdateAnswers(answers.copy(q19_dataRequiredOnHardDisk = "नहीं"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("हार्ड डिस्क डेटा दिया गया - पूर्ण मार्क करें (Mark Data Delivered)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        DetailGridRow("22. Final Remarks (अंतिम टिप्पणी)", answers.q20_finalRemarks.ifBlank { "None" })
                    }

                    // 5. Uploaded Photos Section
                    val totalPhotos = photoMap.values.sumOf { it.size }
                    DetailSectionCard(
                        title = "5. Attached Photos ($totalPhotos Photographs)",
                        icon = Icons.Default.Image
                    ) {
                        if (totalPhotos == 0) {
                            Text(
                                text = "No photos uploaded for this visit.",
                                fontSize = 12.sp,
                                color = Slate500,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PhotoCategory.entries.forEach { category ->
                                    val urls = photoMap[category.categoryId] ?: emptyList()
                                    if (urls.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "${category.displayName} (${urls.size})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(urls) { url ->
                                                    val isVideo = MediaStorageHelper.isMediaVideo(url, context)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(80.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .border(1.dp, if (isVideo) Indigo600 else Slate200, RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                if (isVideo) {
                                                                    MediaStorageHelper.openMedia(context, url)
                                                                } else {
                                                                    previewPhotoUrl = url
                                                                }
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = category.displayName,
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
                                                                Icon(
                                                                    Icons.Default.PlayCircleFilled,
                                                                    contentDescription = "Play Video",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(28.dp)
                                                                )
                                                            }
                                                        }

                                                        if (isAdmin && onDeletePhoto != null) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(3.dp)
                                                                    .size(22.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFFEF4444))
                                                                    .clickable {
                                                                        photoToDelete = category.categoryId to url
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    contentDescription = "Delete Photo",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(14.dp)
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
                    }

                    // 6. Review & Verification Information (समीक्षा एवं सत्यापन)
                    if (visit.status == VisitStatus.REVIEWED || visit.status == VisitStatus.REJECTED || visit.reviewNotes.isNotBlank() || visit.rejectionReason.isNotBlank()) {
                        DetailSectionCard(
                            title = "6. Admin Review & Verification (एडमिन समीक्षा स्थिति)",
                            icon = if (visit.status == VisitStatus.REVIEWED) Icons.Default.CheckCircle else Icons.Default.Warning
                        ) {
                            DetailGridRow("Review Status (समीक्षा स्थिति)", visit.status.name)
                            if (visit.reviewedBy.isNotBlank()) {
                                DetailGridRow("Reviewed By (समीक्षक)", visit.reviewedBy)
                            }
                            if (visit.reviewedAt != null && visit.reviewedAt > 0) {
                                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                DetailGridRow("Reviewed On (समीक्षा समय)", sdf.format(Date(visit.reviewedAt)))
                            }
                            if (visit.reviewNotes.isNotBlank()) {
                                DetailGridRow("Review Notes / Instructions", visit.reviewNotes)
                            }
                            if (visit.rejectionReason.isNotBlank()) {
                                DetailGridRow("Correction Required / Remarks", visit.rejectionReason)
                            }
                        }
                    }

                    // 7. Lifecycle Timestamps & Metadata (टाइमस्टैम्प एवं मेटाडेटा)
                    DetailSectionCard(
                        title = "7. Lifecycle & Metadata (विज़िट टाइमस्टैम्प एवं ट्रैकिंग)",
                        icon = Icons.Default.Info
                    ) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        DetailGridRow("Visit Session ID", visit.visitId)
                        if (visit.taskId.isNotBlank()) {
                            DetailGridRow("Assigned Task ID", visit.taskId)
                        }
                        if (visit.startedAt != null && visit.startedAt > 0) {
                            DetailGridRow("Visit Started At (शुरू समय)", sdf.format(Date(visit.startedAt)))
                        }
                        if (visit.completedAt != null && visit.completedAt > 0) {
                            DetailGridRow("Form Completed At (समापन समय)", sdf.format(Date(visit.completedAt)))
                        }
                        if (visit.submittedAt != null && visit.submittedAt > 0) {
                            DetailGridRow("Submitted At (सबमिट समय)", sdf.format(Date(visit.submittedAt)))
                        }
                        DetailGridRow("Sync Status", visit.syncStatus.name)
                        DetailGridRow("Edit Count (संशोधन संख्या)", visit.editCount.toString())
                        DetailGridRow("App Version", visit.appVersion)
                    }

                    // 8. Audit Trail / Events (ऑडिट ट्रेल - घटनाक्रम)
                    if (events.isNotEmpty()) {
                        DetailSectionCard(
                            title = "8. Audit Trail (${events.size} Events Recorded)",
                            icon = Icons.Default.Assignment
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                events.sortedByDescending { it.timestamp }.forEach { ev ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${ev.eventType}: ${ev.actorRole.ifBlank { "System" }} (${ev.actorId.take(8)})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Navy900
                                                )
                                                if (ev.details.isNotBlank()) {
                                                    Text(
                                                        text = ev.details,
                                                        fontSize = 11.sp,
                                                        color = Slate700
                                                    )
                                                }
                                            }
                                            Text(
                                                text = sdf.format(Date(ev.timestamp)),
                                                fontSize = 10.sp,
                                                color = Slate500,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Admin Review Action Buttons
                        if (isAdmin && onReviewVisit != null && visit.status != VisitStatus.REVIEWED) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        reviewApprovalMode = false
                                        reviewNotesText = ""
                                        showReviewDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject / Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        reviewApprovalMode = true
                                        reviewNotesText = "Approved after review."
                                        showReviewDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve (स्वीकृत)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close (बंद करें)", fontWeight = FontWeight.SemiBold)
                            }

                            if (isAdmin || isEditable || onEditClick != null || onUpdateAnswers != null) {
                                Button(
                                    onClick = {
                                        if (onEditClick != null) {
                                            onDismiss()
                                            onEditClick()
                                        } else if (onUpdateAnswers != null) {
                                            showEditDialog = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Visit (संशोधन करें)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Visit Report Modal Dialog for Admin / Editable Mode
    if (showEditDialog && onUpdateAnswers != null) {
        EditVisitAnswersDialog(
            initialAnswers = currentAnswers,
            schoolName = visit.schoolName,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                currentAnswers = updated
                onUpdateAnswers(updated)
                showEditDialog = false
            }
        )
    }

    // Admin Review / Approval Confirmation Dialog
    if (showReviewDialog && onReviewVisit != null) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (reviewApprovalMode) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (reviewApprovalMode) Emerald600 else Red600
                    )
                    Text(
                        text = if (reviewApprovalMode) "Approve Visit Report" else "Request Changes / Reject",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (reviewApprovalMode)
                            "Are you sure you want to approve this visit report? It will be marked as REVIEWED in the audit trail."
                        else
                            "Please specify what changes or corrections are needed by the field officer:",
                        fontSize = 13.sp,
                        color = Slate700
                    )
                    OutlinedTextField(
                        value = reviewNotesText,
                        onValueChange = { reviewNotesText = it },
                        label = { Text(if (reviewApprovalMode) "Review Notes (Optional)" else "Correction Instructions / Reason (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val notes = reviewNotesText.trim()
                        if (!reviewApprovalMode && notes.isBlank()) {
                            return@Button
                        }
                        showReviewDialog = false
                        onReviewVisit(reviewApprovalMode, notes)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reviewApprovalMode) Emerald600 else Red600
                    )
                ) {
                    Text(if (reviewApprovalMode) "Confirm Approval" else "Submit Rejection", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReviewDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Photo Confirmation Dialog for Admin
    if (photoToDelete != null) {
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Photo?", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Are you sure you want to permanently remove this media from this visit report?", color = Slate700) },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = photoToDelete
                        photoToDelete = null
                        if (toDelete != null) {
                            onDeletePhoto?.invoke(toDelete.first, toDelete.second)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Cancel", color = Slate500)
                }
            }
        )
    }

    // Full Photo Preview Dialog
    if (previewPhotoUrl != null) {
        Dialog(onDismissRequest = { previewPhotoUrl = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = previewPhotoUrl,
                        contentDescription = "Full Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { previewPhotoUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
            }
            Divider(color = Slate100, thickness = 1.dp)
            content()
        }
    }
}

@Composable
private fun DetailGridRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value.ifBlank { "Not Specified" },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVisitAnswersDialog(
    initialAnswers: VisitAnswers,
    schoolName: String,
    onDismiss: () -> Unit,
    onSave: (VisitAnswers) -> Unit
) {
    var q7_principalName by remember { mutableStateOf(initialAnswers.q7_principalName) }
    var q8_principalMobile by remember { mutableStateOf(initialAnswers.q8_principalMobile) }
    var q9_metPrincipal by remember { mutableStateOf(initialAnswers.q9_metPrincipal) }
    var q10_missionGyanAwareness by remember { mutableStateOf(initialAnswers.q10_missionGyanAwareness) }
    var q22_participatingClasses by remember { mutableStateOf(initialAnswers.q22_participatingClasses) }
    var q11_studentCount by remember { mutableStateOf(initialAnswers.q11_studentCount) }
    var q12_schoolResponse by remember { mutableStateOf(initialAnswers.q12_schoolResponse) }
    var q13_bciName by remember { mutableStateOf(initialAnswers.q13_bciName) }
    var q13_bciMobile by remember { mutableStateOf(initialAnswers.q13_bciMobile) }
    var q14_whatsappGroupAdded by remember { mutableStateOf(initialAnswers.q14_whatsappGroupAdded) }
    var q15_posterInstalled by remember { mutableStateOf(initialAnswers.q15_posterInstalled) }
    var q21_smartClassStatus by remember { mutableStateOf(initialAnswers.q21_smartClassStatus) }
    var q16_keyObservations by remember { mutableStateOf(initialAnswers.q16_keyObservations) }
    var q17_problemsOrAssistance by remember { mutableStateOf(initialAnswers.q17_problemsOrAssistance) }
    var q18_followupRequired by remember { mutableStateOf(initialAnswers.q18_followupRequired) }
    var q19_dataRequiredOnHardDisk by remember { mutableStateOf(initialAnswers.q19_dataRequiredOnHardDisk) }
    var q20_finalRemarks by remember { mutableStateOf(initialAnswers.q20_finalRemarks) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = Indigo600,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Edit Submitted Report (रिपोर्ट संशोधित करें)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = schoolName,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF8FAFC))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Principal Section
                    EditFormSectionCard(title = "1. Principal Information (प्रधानाचार्य विवरण)") {
                        OutlinedTextField(
                            value = q7_principalName,
                            onValueChange = { q7_principalName = it },
                            label = { Text("Principal Name (प्रधानाचार्य नाम)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = q8_principalMobile,
                            onValueChange = { q8_principalMobile = it },
                            label = { Text("Principal Mobile (प्रधानाचार्य मोबाइल)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ChoiceSelectorRow(
                            label = "Met Principal? (प्रधानाचार्य से मुलाकात)",
                            options = listOf("हाँ", "नहीं"),
                            selected = q9_metPrincipal,
                            onSelect = { q9_metPrincipal = it }
                        )
                    }

                    // 2. School Survey Response Section
                    EditFormSectionCard(title = "2. Survey & Student Response (सर्वे विवरण)") {
                        ChoiceSelectorRow(
                            label = "Mission Gyan App Awareness (ऐप की जानकारी)",
                            options = listOf("हाँ", "नहीं", "थोड़ी जानकारी थी"),
                            selected = q10_missionGyanAwareness,
                            onSelect = { q10_missionGyanAwareness = it }
                        )
                        OutlinedTextField(
                            value = q22_participatingClasses,
                            onValueChange = { q22_participatingClasses = it },
                            label = { Text("Participating Classes (कक्षाएं e.g. Class 6th, 7th)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = q11_studentCount,
                            onValueChange = { q11_studentCount = it },
                            label = { Text("Student Count (उपस्थित विद्यार्थी संख्या)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ChoiceSelectorRow(
                            label = "School Reception (विद्यालय प्रतिक्रिया)",
                            options = listOf("बहुत अच्छी", "अच्छी", "सामान्य", "कमजोर"),
                            selected = q12_schoolResponse,
                            onSelect = { q12_schoolResponse = it }
                        )
                    }

                    // 3. BCI Officer Section
                    EditFormSectionCard(title = "3. BCI Officer Details (BCI अधिकारी विवरण)") {
                        OutlinedTextField(
                            value = q13_bciName,
                            onValueChange = { q13_bciName = it },
                            label = { Text("BCI Officer Name (BCI का नाम)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = q13_bciMobile,
                            onValueChange = { q13_bciMobile = it },
                            label = { Text("BCI Contact Number (BCI मोबाइल)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 4. Setup & Status Section
                    EditFormSectionCard(title = "4. Setup & Installations (स्थापना स्थिति)") {
                        ChoiceSelectorRow(
                            label = "WhatsApp Group Added (WhatsApp ग्रुप जोड़ा)",
                            options = listOf("हाँ", "नहीं", "लंबित"),
                            selected = q14_whatsappGroupAdded,
                            onSelect = { q14_whatsappGroupAdded = it }
                        )
                        ChoiceSelectorRow(
                            label = "Poster Installed (पोस्टर लगाया गया)",
                            options = listOf("हाँ", "नहीं"),
                            selected = q15_posterInstalled,
                            onSelect = { q15_posterInstalled = it }
                        )
                        ChoiceSelectorRow(
                            label = "Smart Class Status (स्मार्ट क्लास स्थिति)",
                            options = listOf("बहुत अच्छी", "अच्छी", "सामान्य", "खराब", "उपयोग में नहीं है", "स्मार्ट क्लास उपलब्ध नहीं है"),
                            selected = q21_smartClassStatus,
                            onSelect = { q21_smartClassStatus = it }
                        )
                    }

                    // 5. Observations, Followup & Hard Disk
                    EditFormSectionCard(title = "5. Observations & Remarks (अवलोकन एवं टिप्पणी)") {
                        OutlinedTextField(
                            value = q16_keyObservations,
                            onValueChange = { q16_keyObservations = it },
                            label = { Text("Key Observations (मुख्य अवलोकन)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        OutlinedTextField(
                            value = q17_problemsOrAssistance,
                            onValueChange = { q17_problemsOrAssistance = it },
                            label = { Text("Problems / Assistance Needed (समस्याएं/आवश्यकता)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        ChoiceSelectorRow(
                            label = "Follow-up Required (फॉलो-अप आवश्यकता)",
                            options = listOf("हाँ", "नहीं"),
                            selected = q18_followupRequired,
                            onSelect = { q18_followupRequired = it }
                        )
                        ChoiceSelectorRow(
                            label = "Data Required on Hard Disk (हार्ड डिस्क डेटा आवश्यक)",
                            options = listOf("हाँ", "नहीं"),
                            selected = q19_dataRequiredOnHardDisk,
                            onSelect = { q19_dataRequiredOnHardDisk = it }
                        )
                        OutlinedTextField(
                            value = q20_finalRemarks,
                            onValueChange = { q20_finalRemarks = it },
                            label = { Text("Final Remarks (अंतिम टिप्पणी)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }

                // Footer Actions
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel (रद्द करें)", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val updated = initialAnswers.copy(
                                    q7_principalName = q7_principalName,
                                    q8_principalMobile = q8_principalMobile,
                                    q9_metPrincipal = q9_metPrincipal,
                                    q10_missionGyanAwareness = q10_missionGyanAwareness,
                                    q22_participatingClasses = q22_participatingClasses,
                                    q11_studentCount = q11_studentCount,
                                    q12_schoolResponse = q12_schoolResponse,
                                    q13_bciName = q13_bciName,
                                    q13_bciMobile = q13_bciMobile,
                                    q14_whatsappGroupAdded = q14_whatsappGroupAdded,
                                    q15_posterInstalled = q15_posterInstalled,
                                    q21_smartClassStatus = q21_smartClassStatus,
                                    q16_keyObservations = q16_keyObservations,
                                    q17_problemsOrAssistance = q17_problemsOrAssistance,
                                    q18_followupRequired = q18_followupRequired,
                                    q19_dataRequiredOnHardDisk = q19_dataRequiredOnHardDisk,
                                    q20_finalRemarks = q20_finalRemarks
                                )
                                onSave(updated)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Changes (सहेजें)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
            Divider(color = Slate100, thickness = 1.dp)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSelectorRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Slate700
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(options) { opt ->
                val isSel = selected.trim().equals(opt.trim(), ignoreCase = true)
                FilterChip(
                    selected = isSel,
                    onClick = { onSelect(opt) },
                    label = { Text(opt, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Indigo600,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF1F5F9),
                        labelColor = Slate700
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

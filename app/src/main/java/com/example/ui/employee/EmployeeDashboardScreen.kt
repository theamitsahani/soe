package com.example.ui.employee

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JsonHelper
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun EmployeeDashboardScreen(
    currentUser: User,
    tasks: List<Task>,
    visits: List<Visit>,
    unsyncedCount: Int,
    isSyncing: Boolean,
    onStartTaskVisit: (Task) -> Unit,
    onStartNewVisit: () -> Unit,
    onViewVisitDetail: (Visit) -> Unit,
    onSyncClick: () -> Unit,
    onSwitchUser: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Assigned Tasks, 1 = Completed Visits

    val myAssignedTasks = tasks.filter { it.status == VisitStatus.ASSIGNED || it.status == VisitStatus.IN_PROGRESS }
    val myVisits = visits

    Scaffold(
        containerColor = Slate50,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartNewVisit,
                modifier = Modifier.testTag("fab_start_visit"),
                containerColor = Indigo600,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Start Visit", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile & App Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Indigo700, Indigo600)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = currentUser.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "${currentUser.designation} • SOE Field Portal",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Indigo100
                                    )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onSyncClick,
                                modifier = Modifier.testTag("btn_sync")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    BadgedBox(
                                        badge = {
                                            if (unsyncedCount > 0) {
                                                Badge(
                                                    containerColor = Amber600,
                                                    contentColor = Color.White
                                                ) {
                                                    Text("$unsyncedCount")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Sync",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = onSwitchUser,
                                modifier = Modifier.testTag("btn_switch_user")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Switch Profile",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${myAssignedTasks.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Assigned",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Indigo100
                                    )
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${myVisits.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Indigo100
                                    )
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (unsyncedCount > 0) Amber600.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$unsyncedCount",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Unsynced",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Indigo100
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Sync Banner if unsynced visits exist
            AnimatedVisibility(visible = unsyncedCount > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onSyncClick() },
                    shape = RoundedCornerShape(12.dp),
                    color = Amber50,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Amber700)
                            Text(
                                text = "$unsyncedCount offline visit(s) ready to sync",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Amber700
                                )
                            )
                        }
                        Text(
                            text = if (isSyncing) "Syncing..." else "Sync Now →",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Amber700
                            )
                        )
                    }
                }
            }

            // Section Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_assigned_tasks"),
                    text = {
                        Text(
                            "Assigned Tasks (${myAssignedTasks.size})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_my_visits"),
                    text = {
                        Text(
                            "Visit History (${myVisits.size})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            // Content List
            if (selectedTab == 0) {
                if (myAssignedTasks.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.AssignmentTurnedIn,
                        title = "No Pending Tasks",
                        description = "You don't have any assigned school visits. Tap 'Start Visit' to record an unscheduled visit.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        items(myAssignedTasks, key = { it.taskId }) { task ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_card_${task.taskId}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusBadge(status = task.status)
                                        if (task.visitDate.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Event,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Slate500
                                                )
                                                Text(
                                                    text = task.visitDate,
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = task.schoolName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Slate400
                                        )
                                        Text(
                                            text = "${task.block}, ${task.district}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Slate600
                                            )
                                        )
                                    }

                                    if (task.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Slate50,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Instructions: ${task.notes}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Slate600
                                                ),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { onStartTaskVisit(task) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_conduct_visit_${task.taskId}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Indigo600
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Start Visit Questionnaire", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (myVisits.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.HistoryEdu,
                        title = "No Completed Visits Yet",
                        description = "You have not submitted any school visits yet. Record your first visit using the 'Start Visit' button.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        items(myVisits, key = { it.visitId }) { visit ->
                            val photos = JsonHelper.photosFromJson(visit.photosJson)
                            val answers = JsonHelper.fromJson(visit.answersJson)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onViewVisitDetail(visit) }
                                    .testTag("visit_card_${visit.visitId}"),
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
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate500
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = visit.schoolName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${visit.block}, ${visit.district} • ${photos.size} photo(s)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate600
                                        )
                                    )

                                    if (answers.totalStudentsAttended > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "👥 ${answers.totalStudentsAttended} students attended demo session",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = Indigo600,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedButton(
                                        onClick = { onViewVisitDetail(visit) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("View Visit Details & Report")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
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

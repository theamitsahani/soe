package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassEmptyState
import com.example.ui.components.GlassKpiCard
import com.example.ui.components.GlassSectionHeader
import com.example.ui.components.StatusChip
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun AdminDashboardTab(
    visits: List<Visit>,
    totalSchoolsCount: Int,
    totalEmployeesCount: Int,
    onNavigateTab: (AdminTab) -> Unit,
    onNavigateTabWithFilter: (AdminTab, String) -> Unit = { tab, _ -> onNavigateTab(tab) },
    onVisitClick: (Visit) -> Unit
) {
    val uniqueVisits = remember(visits) {
        visits.distinctBy { it.visitId }
    }
    val completedCount = uniqueVisits.count { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }
    val hardDiskDataCount = uniqueVisits.count { it.answersJson.contains("\"q19_dataRequiredOnHardDisk\":\"हाँ\"") }
    val followUpCount = uniqueVisits.count { it.answersJson.contains("\"q18_followupRequired\":\"हाँ\"") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary KPI Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassSectionHeader(
                    title = "Visit Summary & Metrics",
                    subtitle = "Real-time verification telemetry (टैप करके फ़िल्टर देखें)"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassKpiCard(
                        title = "Completed Visits",
                        value = completedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        accentColor = Emerald600,
                        bgGlowColor = Emerald100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Completed") },
                        modifier = Modifier.weight(1f)
                    )
                    GlassKpiCard(
                        title = "Data on Hard Disk",
                        value = hardDiskDataCount.toString(),
                        icon = Icons.Default.Download,
                        accentColor = Amber600,
                        bgGlowColor = Amber100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Data Required on Hard Disk") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassKpiCard(
                        title = "Follow-up Required",
                        value = followUpCount.toString(),
                        icon = Icons.Default.Warning,
                        accentColor = Red600,
                        bgGlowColor = Red100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Follow-up Required") },
                        modifier = Modifier.weight(1f)
                    )
                    GlassKpiCard(
                        title = "Schools Enrolled",
                        value = totalSchoolsCount.toString(),
                        icon = Icons.Default.School,
                        accentColor = Indigo600,
                        bgGlowColor = Color(0xFFE0E7FF),
                        onClick = { onNavigateTabWithFilter(AdminTab.SCHOOLS, "") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Glass Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = GlassSurfaceElevated,
                contentPadding = PaddingValues(16.dp),
                elevation = 2.dp
            ) {
                Text(
                    text = "Quick Operations",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    QuickActionButton(
                        title = "Assign Visit",
                        icon = Icons.Default.AssignmentTurnedIn,
                        gradient = Brush.linearGradient(listOf(Indigo600, Color(0xFF7C3AED))),
                        onClick = { onNavigateTab(AdminTab.ASSIGN_VISITS) }
                    )
                    QuickActionButton(
                        title = "Officers",
                        icon = Icons.Default.Group,
                        gradient = Brush.linearGradient(listOf(Color(0xFF0891B2), Color(0xFF06B6D4))),
                        onClick = { onNavigateTab(AdminTab.EMPLOYEES) }
                    )
                    QuickActionButton(
                        title = "Schools",
                        icon = Icons.Default.School,
                        gradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1))),
                        onClick = { onNavigateTab(AdminTab.SCHOOLS) }
                    )
                    QuickActionButton(
                        title = "Reports",
                        icon = Icons.Default.Download,
                        gradient = Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981))),
                        onClick = { onNavigateTab(AdminTab.VISIT_REPORTS) }
                    )
                }
            }
        }

        // Recent Visits Section Header
        item {
            GlassSectionHeader(
                title = "Recent Visit Reports",
                badgeText = "${uniqueVisits.size} Total",
                action = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier.clickable { onNavigateTab(AdminTab.VISIT_REPORTS) }
                    ) {
                        Text(
                            text = "View All →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            )
        }

        if (visits.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.AssignmentTurnedIn,
                    title = "No visit reports submitted yet",
                    subtitle = "Submitted school verification reports will appear here in real-time."
                )
            }
        } else {
            items(visits.take(6), key = { it.visitId }) { visit ->
                VisitCardItem(visit = visit, onClick = { onVisitClick(visit) })
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    gradient: Brush = Brush.linearGradient(listOf(Indigo600, Color(0xFF7C3AED))),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(gradient)
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
        )
    }
}

@Composable
fun VisitCardItem(
    visit: Visit,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = GlassSurfaceElevated,
        contentPadding = PaddingValues(14.dp),
        elevation = 1.5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = visit.schoolName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1
                    )
                    Text(
                        text = "${visit.district} • ${visit.block}",
                        fontSize = 11.5.sp,
                        color = Slate500
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(statusName = visit.status.name)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Officer: ${visit.employeeName}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700
                )
            }
            Text(
                text = visit.visitDate,
                fontSize = 11.sp,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

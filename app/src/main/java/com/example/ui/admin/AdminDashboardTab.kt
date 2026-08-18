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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassIndigoGradient
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Teal600

@Composable
fun AdminDashboardTab(
    visits: List<Visit>,
    totalSchoolsCount: Int,
    totalEmployeesCount: Int,
    onNavigateTab: (AdminTab) -> Unit,
    onNavigateTabWithFilter: (AdminTab, String) -> Unit = { tab, _ -> onNavigateTab(tab) },
    onVisitClick: (Visit) -> Unit
) {
    val uniqueVisits = androidx.compose.runtime.remember(visits) {
        visits.distinctBy { it.visitId }
    }
    val completedCount = uniqueVisits.count { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }
    val hardDiskDataCount = uniqueVisits.count { it.answersJson.contains("\"q19_dataRequiredOnHardDisk\":\"हाँ\"") }
    val followUpCount = uniqueVisits.count { it.answersJson.contains("\"q18_followupRequired\":\"हाँ\"") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Visit Summary & Metrics (टैप करके फ़िल्टर देखें)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy900
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Completed",
                        value = completedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Emerald600,
                        bgColor = Emerald100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Completed") },
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Data on Hard Disk",
                        value = hardDiskDataCount.toString(),
                        icon = Icons.Default.Download,
                        color = Amber600,
                        bgColor = Amber100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Data Required on Hard Disk") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Follow-up Required",
                        value = followUpCount.toString(),
                        icon = Icons.Default.Warning,
                        color = Red600,
                        bgColor = Red100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Follow-up Required") },
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Schools Enrolled",
                        value = totalSchoolsCount.toString(),
                        icon = Icons.Default.School,
                        color = Indigo600,
                        bgColor = Color(0xFFE0E7FF),
                        onClick = { onNavigateTabWithFilter(AdminTab.SCHOOLS, "") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionButton(
                            title = "Assign Visit",
                            icon = Icons.Default.AssignmentTurnedIn,
                            onClick = { onNavigateTab(AdminTab.ASSIGN_VISITS) }
                        )
                        QuickActionButton(
                            title = "Officers",
                            icon = Icons.Default.Group,
                            onClick = { onNavigateTab(AdminTab.EMPLOYEES) }
                        )
                        QuickActionButton(
                            title = "Schools",
                            icon = Icons.Default.School,
                            onClick = { onNavigateTab(AdminTab.SCHOOLS) }
                        )
                        QuickActionButton(
                            title = "Reports",
                            icon = Icons.Default.Download,
                            onClick = { onNavigateTab(AdminTab.VISIT_REPORTS) }
                        )
                    }
                }
            }
        }

        // Recent Visits Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Visit Reports",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy900
                )
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo600,
                    modifier = Modifier.clickable { onNavigateTab(AdminTab.VISIT_REPORTS) }
                )
            }
        }

        if (visits.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Indigo50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = Indigo600.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No visit reports submitted yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }
                }
            }
        } else {
            items(visits.take(5)) { visit ->
                VisitCardItem(visit = visit, onClick = { onVisitClick(visit) })
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        cornerRadius = 18.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(BorderStroke(1.dp, color.copy(alpha = 0.25f)), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy900,
                    lineHeight = 24.sp
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Slate500,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(elevation = 4.dp, shape = CircleShape, ambientColor = Indigo600.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .border(BorderStroke(1.dp, GlassBorderLight), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Indigo600, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
    }
}

@Composable
fun VisitCardItem(
    visit: Visit,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visit.schoolName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(statusName = visit.status.name)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${visit.district} • ${visit.block}",
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
                    text = visit.visitDate,
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


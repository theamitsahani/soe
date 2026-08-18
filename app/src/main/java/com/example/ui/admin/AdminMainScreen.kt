package com.example.ui.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.model.AppNotification
import com.example.data.model.User
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassOutlinedButton
import com.example.ui.components.NotificationBellIcon
import com.example.ui.components.NotificationDialog
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassIndigoGradient
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red500
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch

enum class AdminTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    EMPLOYEES("Employees", Icons.Default.Group),
    SCHOOLS("Schools", Icons.Default.School),
    ASSIGN_VISITS("Assign Visits", Icons.Default.AssignmentTurnedIn),
    VISIT_REPORTS("Visit Reports", Icons.Default.Assessment),
    PHOTO_GALLERY("Photo Gallery", Icons.Default.PhotoLibrary),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    adminUser: User,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    notifications: List<AppNotification> = emptyList(),
    onMarkAllNotificationsRead: () -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onLogoutClick: () -> Unit,
    content: @Composable (AdminTab) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val tabs = AdminTab.entries
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A).copy(alpha = 0.96f),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(290.dp)
                    .border(
                        BorderStroke(1.dp, GlassBorderDark),
                        RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Indigo600.copy(alpha = 0.4f),
                                    spotColor = Indigo600.copy(alpha = 0.5f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassIndigoGradient)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(adminUser.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Administrator Portal", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onTabSelected(index)
                                    scope.launch { drawerState.close() }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Indigo600.copy(alpha = 0.85f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) Color.White else Slate400,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showLogoutDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Logout",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) {
        LiquidGlassBackground(
            modifier = Modifier.fillMaxSize(),
            enableOrbs = true
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Surface(
                        color = Color.White.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, GlassBorderLight),
                        shadowElevation = 4.dp
                    ) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("SOE Admin", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Navy900)
                                    Text(tabs[selectedTab].title, fontSize = 12.sp, color = Indigo600, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Navy900)
                                }
                            },
                            actions = {
                                NotificationBellIcon(
                                    unreadCount = notifications.count { !it.isRead },
                                    onClick = { showNotificationDialog = true }
                                )
                                IconButton(onClick = { showLogoutDialog = true }) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Slate700)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Top Scrollable Tab Selector Bar for fast touch navigation
                    Surface(
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, GlassBorderLight)
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Indigo600,
                            edgePadding = 12.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    height = 3.dp,
                                    color = Indigo600
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { onTabSelected(index) },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (selectedTab == index) Indigo600 else Slate400
                                            )
                                            Text(
                                                text = tab.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selectedTab == index) Indigo600 else Slate600
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        content(tabs[selectedTab])
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        LiquidGlassDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = "Confirm Logout / लॉगआउट पुष्टि",
            confirmButton = {
                LiquidGlassButton(
                    text = "Yes / हाँ",
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    gradient = Brush.horizontalGradient(listOf(Red500, Red600)),
                    height = 42.dp
                )
            },
            dismissButton = {
                LiquidGlassOutlinedButton(
                    text = "No / नहीं",
                    onClick = { showLogoutDialog = false },
                    height = 42.dp
                )
            }
        ) {
            Text("Do you want to logout? / क्या आप लॉगआउट करना चाहते हैं?", color = Slate700, fontSize = 14.sp)
        }
    }

    if (showNotificationDialog) {
        NotificationDialog(
            notifications = notifications,
            onDismiss = { showNotificationDialog = false },
            onMarkAllRead = onMarkAllNotificationsRead,
            onClearAll = onClearAllNotifications
        )
    }
}


package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun SyncStatusBanner(
    isOnline: Boolean,
    pendingCount: Int,
    isSyncing: Boolean = false,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor, borderColor, glowColor) = if (!isOnline) {
        listOf(Color(0xFFFEF2F2).copy(alpha = 0.90f), Color(0xFFDC2626), Color(0xFFFECACA), Color(0xFFEF4444))
    } else if (isSyncing || pendingCount > 0) {
        listOf(Color(0xFFFFFBEB).copy(alpha = 0.90f), Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFFF59E0B))
    } else {
        listOf(Color(0xFFF0FDF4).copy(alpha = 0.90f), Color(0xFF059669), Color(0xFFBBF7D0), Color(0xFF10B981))
    }

    val icon = if (!isOnline) Icons.Default.CloudOff else if (pendingCount > 0) Icons.Default.CloudQueue else Icons.Default.CheckCircle
    val text = if (!isOnline) "Offline · Data saved locally"
    else if (isSyncing) "Syncing data to cloud..."
    else if (pendingCount > 0) "$pendingCount Visit(s) Pending Sync"
    else "Online · All data synced"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = text,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }

            if (pendingCount > 0 && isOnline) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = contentColor,
                    modifier = Modifier.clickable { if (!isSyncing) onSyncClick() }
                ) {
                    Text(
                        text = if (isSyncing) "Syncing..." else "Sync Now",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(statusName: String) {
    GlassStatusBadge(statusName = statusName)
}

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.5.sp, color = Slate400) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Slate500, modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900,
            focusedContainerColor = GlassSurfaceElevated,
            unfocusedContainerColor = GlassSurfaceLight,
            focusedBorderColor = Indigo600,
            unfocusedBorderColor = GlassBorderSubtle
        ),
        modifier = modifier.fillMaxWidth()
    )
}

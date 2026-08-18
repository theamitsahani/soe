package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceLightElevated
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red500
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal500

@Composable
fun SyncStatusBanner(
    isOnline: Boolean,
    pendingCount: Int,
    isSyncing: Boolean = false,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgGradient, contentColor, borderColor, icon, statusText) = when {
        !isOnline -> Quintuple(
            Brush.horizontalGradient(listOf(Color(0xFFFFF1F2).copy(alpha = 0.92f), Color(0xFFFFE4E6).copy(alpha = 0.85f))),
            Red600,
            Red500.copy(alpha = 0.35f),
            Icons.Default.CloudOff,
            "Offline · Data saved locally"
        )
        isSyncing -> Quintuple(
            Brush.horizontalGradient(listOf(Color(0xFFFFFBEB).copy(alpha = 0.95f), Color(0xFFFEF3C7).copy(alpha = 0.88f))),
            Amber600,
            Amber500.copy(alpha = 0.40f),
            Icons.Default.Refresh,
            "Syncing data to cloud..."
        )
        pendingCount > 0 -> Quintuple(
            Brush.horizontalGradient(listOf(Color(0xFFFFFBEB).copy(alpha = 0.92f), Color(0xFFFEF3C7).copy(alpha = 0.85f))),
            Amber600,
            Amber500.copy(alpha = 0.40f),
            Icons.Default.CloudQueue,
            "$pendingCount Visit(s) Pending Sync"
        )
        else -> Quintuple(
            Brush.horizontalGradient(listOf(Color(0xFFF0FDF4).copy(alpha = 0.92f), Color(0xFFDCFCE7).copy(alpha = 0.85f))),
            Emerald600,
            Emerald500.copy(alpha = 0.35f),
            Icons.Default.CheckCircle,
            "Online · All data synced"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = contentColor.copy(alpha = 0.1f),
                spotColor = contentColor.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncing) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(spinAngle)
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }

                if (pendingCount > 0 && isOnline) {
                    LiquidGlassButton(
                        text = if (isSyncing) "Syncing..." else "Sync Now",
                        onClick = { if (!isSyncing) onSyncClick() },
                        enabled = !isSyncing,
                        isLoading = isSyncing,
                        gradient = Brush.linearGradient(listOf(Amber500, Color(0xFFEA580C))),
                        shape = RoundedCornerShape(10.dp),
                        height = 32.dp,
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@Composable
fun StatusChip(statusName: String) {
    LiquidGlassStatusBadge(status = statusName)
}

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    LiquidGlassTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    )
}


package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue600
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassBorderLightSubtle
import com.example.ui.theme.GlassCardBorderGradient
import com.example.ui.theme.GlassIndigoGradient
import com.example.ui.theme.GlassSurfaceGradient
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceLightElevated
import com.example.ui.theme.GlassSurfaceLightSubtle
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Purple500
import com.example.ui.theme.Red500
import com.example.ui.theme.Red600
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal500
import com.example.ui.theme.Teal600

/**
 * Liquid Glass Elevation & Hierarchy Levels
 */
enum class GlassLevel {
    LEVEL_1_BACKGROUND, // Very subtle background surface
    LEVEL_1_CARD,       // Subtle card surface
    LEVEL_2_CARD,       // Standard card glass for items/content
    LEVEL_2_SURFACE,    // Standard glass surface
    LEVEL_3_FLOATING,   // Elevated floating glass for modals/floating bars
    LEVEL_4_HIGHLIGHT   // Active/selected state with glowing border
}

/**
 * Animated Ambient Mesh / Orb Background for Liquid Glass scenes
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    enableOrbs: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glass_transition")
    
    val orb1Offset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1_anim"
    )
    
    val orb2Offset by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFEEF2F6),
                        Color(0xFFE2E8F0)
                    )
                )
            )
    ) {
        if (enableOrbs) {
            // Subtle ambient light orbs behind the glass content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.55f }
            ) {
                // Top-right soft indigo orb
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .offset(x = 180.dp + orb1Offset.dp, y = (-40).dp + orb2Offset.dp)
                        .blur(80.dp)
                        .background(Indigo400.copy(alpha = 0.35f), CircleShape)
                )

                // Top-left cyan orb
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .offset(x = (-80).dp - orb2Offset.dp, y = 140.dp + orb1Offset.dp)
                        .blur(80.dp)
                        .background(Cyan500.copy(alpha = 0.25f), CircleShape)
                )

                // Bottom-right emerald/teal orb
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 60.dp + orb2Offset.dp, y = 80.dp - orb1Offset.dp)
                        .blur(90.dp)
                        .background(Teal500.copy(alpha = 0.20f), CircleShape)
                )

                // Mid-left purple/rose subtle accent
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-60).dp + orb1Offset.dp, y = 180.dp)
                        .blur(75.dp)
                        .background(Purple500.copy(alpha = 0.18f), CircleShape)
                )
            }
        }

        content()
    }
}

/**
 * Liquid Glass Card - The core building block of the Glassmorphic UI
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.LEVEL_2_CARD,
    shape: Shape? = null,
    cornerRadius: Dp? = null,
    elevation: Dp? = null,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "glass_card_press_scale"
    )

    val finalShape = shape ?: RoundedCornerShape(cornerRadius ?: 20.dp)

    val surfaceColor = containerColor ?: backgroundColor ?: when (level) {
        GlassLevel.LEVEL_1_BACKGROUND, GlassLevel.LEVEL_1_CARD -> GlassSurfaceLightSubtle
        GlassLevel.LEVEL_2_CARD, GlassLevel.LEVEL_2_SURFACE -> GlassSurfaceLight
        GlassLevel.LEVEL_3_FLOATING -> GlassSurfaceLightElevated
        GlassLevel.LEVEL_4_HIGHLIGHT -> Color.White.copy(alpha = 0.92f)
    }

    val defaultBorder = border ?: if (borderColor != null) {
        BorderStroke(1.2.dp, borderColor)
    } else {
        when (level) {
            GlassLevel.LEVEL_1_BACKGROUND, GlassLevel.LEVEL_1_CARD -> BorderStroke(1.dp, GlassBorderLightSubtle)
            GlassLevel.LEVEL_2_CARD, GlassLevel.LEVEL_2_SURFACE -> BorderStroke(1.2.dp, Color.White.copy(alpha = 0.85f))
            GlassLevel.LEVEL_3_FLOATING -> BorderStroke(1.5.dp, Color.White)
            GlassLevel.LEVEL_4_HIGHLIGHT -> BorderStroke(1.5.dp, Indigo500.copy(alpha = 0.7f))
        }
    }

    val shadowElevation = elevation ?: when (level) {
        GlassLevel.LEVEL_1_BACKGROUND, GlassLevel.LEVEL_1_CARD -> 0.dp
        GlassLevel.LEVEL_2_CARD, GlassLevel.LEVEL_2_SURFACE -> 4.dp
        GlassLevel.LEVEL_3_FLOATING -> 12.dp
        GlassLevel.LEVEL_4_HIGHLIGHT -> 8.dp
    }

    Surface(
        modifier = modifier
            .scale(scaleAnim)
            .shadow(
                elevation = shadowElevation,
                shape = finalShape,
                ambientColor = Indigo600.copy(alpha = 0.08f),
                spotColor = Navy900.copy(alpha = 0.12f)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = finalShape,
        color = surfaceColor,
        border = defaultBorder
    ) {
        // Specular highlight gradient on the glass card
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 120f
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Liquid Glass Button with smooth spring scale on touch, gradient surface & specular gloss
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    gradient: Brush = GlassIndigoGradient,
    textColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(14.dp),
    height: Dp = 48.dp,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "btn_scale"
    )

    Surface(
        modifier = modifier
            .height(height)
            .scale(scaleAnim)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = shape,
                ambientColor = Indigo600.copy(alpha = 0.25f),
                spotColor = Indigo600.copy(alpha = 0.35f)
            )
            .clip(shape)
            .then(
                if (enabled && !isLoading) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = Color.Transparent,
        border = if (enabled) BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (enabled) gradient else Brush.linearGradient(listOf(Slate300, Slate400)))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 40f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = textColor,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp
                )
            } else if (content != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = content
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass Button - overload accepting text as first parameter
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    gradient: Brush = GlassIndigoGradient,
    textColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(14.dp),
    height: Dp = 48.dp
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        text = text,
        enabled = enabled,
        isLoading = isLoading,
        icon = icon,
        gradient = gradient,
        textColor = textColor,
        shape = shape,
        height = height
    )
}

/**
 * Liquid Glass Outlined / Secondary Button
 */
@Composable
fun LiquidGlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = Indigo600,
    shape: Shape = RoundedCornerShape(14.dp),
    height: Dp = 44.dp,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "outlined_btn_scale"
    )

    Surface(
        modifier = modifier
            .height(height)
            .scale(scaleAnim)
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = GlassSurfaceLight,
        border = BorderStroke(1.2.dp, if (enabled) contentColor.copy(alpha = 0.45f) else Slate300)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 14.dp),
                    content = content
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) contentColor else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            color = if (enabled) contentColor else Slate400,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass Outlined / Secondary Button - overload accepting text as first parameter
 */
@Composable
fun LiquidGlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = Indigo600,
    shape: Shape = RoundedCornerShape(14.dp),
    height: Dp = 44.dp
) {
    LiquidGlassOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        text = text,
        enabled = enabled,
        icon = icon,
        contentColor = contentColor,
        shape = shape,
        height = height
    )
}

/**
 * Liquid Glass Text Input with glowing focused border and translucent backdrop
 */
@Composable
fun LiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Red500
            isFocused -> Indigo600
            else -> Slate300.copy(alpha = 0.8f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "tf_border_color"
    )

    val containerColor = if (isFocused) {
        Color.White.copy(alpha = 0.95f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) Indigo600 else Slate700,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }

        Surface(
            shape = shape,
            color = containerColor,
            border = BorderStroke(if (isFocused || isError) 1.5.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isFocused) 4.dp else 1.dp,
                    shape = shape,
                    ambientColor = if (isFocused) Indigo600.copy(alpha = 0.15f) else Color.Transparent,
                    spotColor = if (isFocused) Indigo600.copy(alpha = 0.20f) else Color.Transparent
                )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder?.let { { Text(it, color = Slate400, fontSize = 14.sp) } },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                readOnly = readOnly,
                enabled = enabled,
                shape = shape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    cursorColor = Indigo600
                ),
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = Red600,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Liquid Glass Status Badge
 */
@Composable
fun LiquidGlassStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (status.uppercase().replace(" ", "_")) {
        "SUBMITTED", "COMPLETED", "ACTIVE", "SYNCED" -> Triple(
            Emerald500.copy(alpha = 0.12f),
            Emerald600,
            Emerald500.copy(alpha = 0.35f)
        )
        "ASSIGNED", "STARTED", "IN_PROGRESS", "PENDING" -> Triple(
            Amber500.copy(alpha = 0.14f),
            Amber600,
            Amber500.copy(alpha = 0.40f)
        )
        "REVIEWED" -> Triple(
            Indigo500.copy(alpha = 0.12f),
            Indigo600,
            Indigo500.copy(alpha = 0.35f)
        )
        "FAILED", "INACTIVE", "REJECTED" -> Triple(
            Red500.copy(alpha = 0.12f),
            Red600,
            Red500.copy(alpha = 0.35f)
        )
        else -> Triple(
            Slate200.copy(alpha = 0.7f),
            Slate700,
            Slate300
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = status.replace("_", " "),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * Liquid Glass Section Header & Container
 */
@Composable
fun LiquidGlassSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = Indigo600,
    badgeText: String? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.LEVEL_2_CARD,
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconTint.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, iconTint.copy(alpha = 0.25f)), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Indigo50)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600
                        )
                    }
                }
            }

            if (action != null) {
                action()
            }
        }

        content()
    }
}

/**
 * Liquid Glass Stat / Metric Counter Card
 */
@Composable
fun LiquidGlassStatCard(
    count: String,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    LiquidGlassCard(
        modifier = modifier,
        level = GlassLevel.LEVEL_2_CARD,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy900
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate500
            )
        }
    }
}

/**
 * Liquid Glass Modal Dialog with Frosted Glass Card
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleComposable: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    iconTint: Color = Indigo600,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // block click from dismissing
                    ),
                level = GlassLevel.LEVEL_3_FLOATING,
                shape = RoundedCornerShape(26.dp),
                backgroundColor = Color.White.copy(alpha = 0.94f),
                border = BorderStroke(1.5.dp, Color.White),
                contentPadding = PaddingValues(22.dp)
            ) {
                if (title != null || titleComposable != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(iconTint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (titleComposable != null) {
                            titleComposable()
                        } else if (title != null) {
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }
                    }
                }

                if (text != null) {
                    text()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (content != null) {
                    content()
                }

                if (confirmButton != null || dismissButton != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (confirmButton != null) {
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

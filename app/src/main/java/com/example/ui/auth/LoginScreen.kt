package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassLevel
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassTextField
import com.example.ui.theme.Cyan500
import com.example.ui.theme.GlassIndigoGradient
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Purple500
import com.example.ui.theme.Red500
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun LoginScreen(
    onLoginClick: (String, String, (Result<Unit>) -> Unit) -> Unit
) {
    var emailOrUserId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LiquidGlassBackground(
        modifier = Modifier.fillMaxSize(),
        enableOrbs = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                level = GlassLevel.LEVEL_3_FLOATING,
                shape = RoundedCornerShape(28.dp),
                backgroundColor = Color.White.copy(alpha = 0.88f),
                border = BorderStroke(1.5.dp, Color.White),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(26.dp)
            ) {
                // Top Brand Logo with Glowing Liquid Glass Effect
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.CenterHorizontally)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(22.dp),
                            ambientColor = Indigo600.copy(alpha = 0.35f),
                            spotColor = Indigo600.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(GlassIndigoGradient)
                        .border(
                            BorderStroke(1.2.dp, Color.White.copy(alpha = 0.6f)),
                            RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SOE / Mission Gyan",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy900,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    letterSpacing = 0.3.sp
                )

                Text(
                    text = "School Visit Management Portal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error message banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFEF2F2).copy(alpha = 0.95f))
                                .border(BorderStroke(1.dp, Red500.copy(alpha = 0.4f)), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Red500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Red600,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage!!,
                                color = Red600,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Email / User ID Input
                LiquidGlassTextField(
                    value = emailOrUserId,
                    onValueChange = { emailOrUserId = it },
                    label = "Email Address / User ID",
                    placeholder = "e.g. employee@missiongyan.org",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Input
                LiquidGlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Enter your secure password",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Slate500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Submit Button
                LiquidGlassButton(
                    text = "Sign In to Portal",
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onLoginClick(emailOrUserId, password) { result ->
                            isLoading = false
                            if (result.isFailure) {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Login failed"
                            }
                        }
                    },
                    enabled = !isLoading && emailOrUserId.isNotBlank() && password.isNotBlank(),
                    isLoading = isLoading,
                    gradient = GlassIndigoGradient,
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


package com.example.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate500

@Composable
fun LoginScreen(
    onLoginClick: (String, String, (Result<Unit>) -> Unit) -> Unit
) {
    var emailOrUserId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Navy900, Navy800)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Indigo600, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SOE / Mission Gyan",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )

                Text(
                    text = "School Visit Management Portal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error message banner
                if (errorMessage != null) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .background(
                                color = Red600.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = null,
                            tint = Red600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Red600,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Email / User ID Input (Max 15 characters)
                OutlinedTextField(
                    value = emailOrUserId,
                    onValueChange = {
                        if (it.length <= 15) {
                            emailOrUserId = it.trim()
                        }
                    },
                    label = { Text("Email Address (Max 15 chars)") },
                    supportingText = {
                        Text(
                            text = "${emailOrUserId.length}/15 chars (अधिकतम 15 अक्षर)",
                            fontSize = 11.sp,
                            color = if (emailOrUserId.length > 15) Red600 else Slate500
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate500) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password Input (Strictly 8 characters/digits)
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 8) {
                            password = it
                        }
                    },
                    label = { Text("Password (8 digits/chars)") },
                    supportingText = {
                        Text(
                            text = "${password.length}/8 digits/chars (ठीक 8 अक्षर)",
                            fontSize = 11.sp,
                            color = if (password.length == 8) Indigo600 else Slate500
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (emailOrUserId.length > 15) {
                            errorMessage = "ईमेल 15 अक्षरों से अधिक नहीं हो सकता (Email must be at most 15 characters)"
                            return@Button
                        }
                        if (password.length != 8) {
                            errorMessage = "पासवर्ड ठीक 8 अंकों/अक्षरों का होना अनिवार्य है (Password must be exactly 8 digits/characters)"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        onLoginClick(emailOrUserId, password) { result ->
                            isLoading = false
                            if (result.isFailure) {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Login failed"
                            }
                        }
                    },
                    enabled = !isLoading && emailOrUserId.isNotBlank() && password.length == 8,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Login to Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

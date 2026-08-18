package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassSectionHeader
import com.example.ui.components.StatusChip
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryGradient
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun SettingsTab(
    adminUser: User,
    onChangePassword: (String, (Result<Unit>) -> Unit) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassSectionHeader(
            title = "Account & System Settings",
            subtitle = "Admin credentials, security & sync status"
        )

        // Admin Account Profile Glass Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = GlassSurfaceElevated,
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(PrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = adminUser.name.take(1).uppercase().ifBlank { "A" },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = adminUser.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = adminUser.email,
                        fontSize = 12.5.sp,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(statusName = "ADMINISTRATOR")
                }
            }
        }

        // Change Password Glass Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = GlassSurfaceElevated,
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Change Admin Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Set a secure new password for this portal",
                        fontSize = 11.5.sp,
                        color = Slate500
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (statusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isError) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, if (isError) Color(0xFFFECACA) else Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.Info else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) Red600 else Emerald600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage!!,
                            color = if (isError) Red600 else Emerald600,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    statusMessage = null
                },
                label = { Text("New Password (नया पासवर्ड)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Indigo600) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Slate400
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Indigo600,
                    unfocusedBorderColor = GlassBorderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    statusMessage = null
                },
                label = { Text("Confirm New Password (पासवर्ड दोहराएं)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Indigo600) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Indigo600,
                    unfocusedBorderColor = GlassBorderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            GlassButton(
                onClick = {
                    if (newPassword.length < 6) {
                        statusMessage = "Password must be at least 6 characters"
                        isError = true
                        return@GlassButton
                    }
                    if (newPassword != confirmPassword) {
                        statusMessage = "Passwords do not match"
                        isError = true
                        return@GlassButton
                    }
                    isUpdating = true
                    statusMessage = null
                    onChangePassword(newPassword) { result ->
                        isUpdating = false
                        if (result.isSuccess) {
                            statusMessage = "Password updated successfully!"
                            isError = false
                            newPassword = ""
                            confirmPassword = ""
                        } else {
                            statusMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update password"
                            isError = true
                        }
                    }
                },
                enabled = !isUpdating && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                text = "Update Password",
                isLoading = isUpdating,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cloud & System Information Glass Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = GlassSurfaceElevated,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "System & Cloud Connectivity",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Firebase Firestore & Cloudinary Connected",
                        fontSize = 11.5.sp,
                        color = Emerald600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "SOE Mission Gyan School Verification Portal v2.0 · Liquid Glass Architecture",
                fontSize = 11.sp,
                color = Slate500
            )
        }
    }
}

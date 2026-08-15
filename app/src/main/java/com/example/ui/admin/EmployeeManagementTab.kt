package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.ui.components.SearchTextField
import com.example.ui.components.StatusChip
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import android.widget.Toast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun EmployeeManagementTab(
    employees: List<User>,
    onSaveEmployee: (User, String?, (Result<Unit>) -> Unit) -> Unit,
    onResetPassword: ((email: String, onComplete: (Result<Unit>) -> Unit) -> Unit)? = null,
    onRefreshEmployees: ((onComplete: (Result<Int>) -> Unit) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshErrorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerRefresh() {
        if (onRefreshEmployees != null) {
            isRefreshing = true
            refreshErrorMessage = null
            onRefreshEmployees.invoke { result ->
                isRefreshing = false
                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    val rawMsg = ex?.message ?: ""
                    refreshErrorMessage = if (rawMsg.contains("network", ignoreCase = true) ||
                        rawMsg.contains("unavailable", ignoreCase = true) ||
                        rawMsg.contains("connection", ignoreCase = true) ||
                        rawMsg.contains("timeout", ignoreCase = true)) {
                        "Unable to load employees. Please check your internet connection."
                    } else if (rawMsg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                        "Permission denied while fetching employees from Firestore."
                    } else {
                        ex?.localizedMessage ?: "Unable to load employees. Please check your internet connection."
                    }
                }
            }
        } else {
            onRefresh?.invoke()
        }
    }

    LaunchedEffect(Unit) {
        triggerRefresh()
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<User?>(null) }
    var selectedEmployeeForDetails by remember { mutableStateOf<User?>(null) }
    var employeeToResetPassword by remember { mutableStateOf<User?>(null) }
    var isSendingResetPassword by remember { mutableStateOf(false) }
    var resetPasswordErrorMessage by remember { mutableStateOf<String?>(null) }
    var successNotification by remember { mutableStateOf<String?>(null) }
    var newlyCreatedCredentials by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true) ||
            it.mobile.contains(searchQuery, ignoreCase = true) ||
            it.district.contains(searchQuery, ignoreCase = true) ||
            it.state.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (successNotification != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = successNotification!!,
                            color = Color(0xFF15803D),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { successNotification = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("OK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
            }
        }

        if (refreshErrorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Firestore Sync Notice",
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = refreshErrorMessage!!,
                                color = Color(0xFF991B1B),
                                fontSize = 11.sp
                            )
                        }
                        TextButton(
                            onClick = { triggerRefresh() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red600)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SOE Field Officers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("${employees.count { it.status == UserStatus.ACTIVE }} Active • ${employees.size} Total Officers", fontSize = 11.sp, color = Slate500)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { triggerRefresh() },
                        enabled = !isRefreshing,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Indigo600
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh from Firestore",
                                tint = Indigo600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { showAddEmployeeDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Officer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search officer by name, district, state..."
            )
        }

        if (filteredEmployees.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = Indigo600
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Syncing field officers from Firestore...", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Indigo600)
                        } else if (refreshErrorMessage != null && employees.isEmpty()) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Red600, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Unable to load employees.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Red600)
                            Text("Please check your internet connection and try again.", fontSize = 12.sp, color = Slate500)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { triggerRefresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (searchQuery.isNotBlank()) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Slate500, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No officers found matching search", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate500)
                        } else {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Slate500, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No officers found in Firestore", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap '+ Add Officer' to view steps for adding officers via Firebase Console.", fontSize = 12.sp, color = Slate500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        } else {
            items(filteredEmployees) { emp ->
                CompactEmployeeCardItem(
                    employee = emp,
                    onClick = { selectedEmployeeForDetails = emp },
                    onEditClick = { employeeToEdit = emp },
                    onToggleStatus = { newStatus ->
                        onSaveEmployee(emp.copy(status = newStatus), null) {}
                    }
                )
            }
        }
    }

    // View Details Dialog on Tap
    if (selectedEmployeeForDetails != null) {
        val emp = selectedEmployeeForDetails!!
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { selectedEmployeeForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy900)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (emp.status == UserStatus.ACTIVE) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (emp.status == UserStatus.ACTIVE) "ACTIVE (सक्रिय)" else "INACTIVE (निष्क्रिय)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (emp.status == UserStatus.ACTIVE) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Location
                    DetailItem(label = "State & District (राज्य व जिला)", value = "${emp.state.ifBlank { "Rajasthan" }} • ${emp.district.ifBlank { "All Districts" }}")

                    // Email
                    DetailItem(label = "Email Address (ईमेल)", value = emp.email)

                    // Mobile Number with Direct Call Action
                    Column {
                        Text("Mobile Number (मोबाइल नंबर)", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = emp.mobile.ifBlank { "Not provided" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            if (emp.mobile.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emp.mobile}"))
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = Indigo600)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 11.sp, color = Indigo600)
                                }
                            }
                        }
                    }

                    // Reset Password Button
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Password Reset", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                                Text("Firebase Auth Email Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                            }
                            OutlinedButton(
                                onClick = {
                                    val targetEmp = emp
                                    selectedEmployeeForDetails = null
                                    resetPasswordErrorMessage = null
                                    employeeToResetPassword = targetEmp
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(13.dp), tint = Indigo600)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Password", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Toggle Status Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status (सक्रिय/निष्क्रिय)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate700)
                        Switch(
                            checked = emp.status == UserStatus.ACTIVE,
                            onCheckedChange = { checked ->
                                val newStatus = if (checked) UserStatus.ACTIVE else UserStatus.INACTIVE
                                val updated = emp.copy(status = newStatus)
                                selectedEmployeeForDetails = updated
                                onSaveEmployee(updated, null) {}
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo600
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toEdit = emp
                        selectedEmployeeForDetails = null
                        employeeToEdit = toEdit
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Details")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEmployeeForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Add New Employee Dialog
    if (showAddEmployeeDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("Officer@123") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var mobile by remember { mutableStateOf("") }
        var state by remember { mutableStateOf("Rajasthan") }
        var district by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        var addErrorMessage by remember { mutableStateOf<String?>(null) }

        val commonDistricts = remember {
            listOf(
                "Jaipur", "Jodhpur", "Kota", "Bikaner", "Ajmer", "Udaipur",
                "Alwar", "Bhilwara", "Sikar", "Bharatpur", "Pali", "Sri Ganganagar",
                "Churu", "Nagaur", "Jhunjhunu", "Barmer", "Tonk", "Chittorgarh"
            )
        }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddEmployeeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Indigo600.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Add New Field Officer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                        Text("नया फील्ड ऑफिसर जोड़ें", fontSize = 11.sp, color = Slate500)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (addErrorMessage != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = addErrorMessage!!,
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // 1. Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            addErrorMessage = null
                        },
                        label = { Text("Name (नाम) *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Rahul Sharma", fontSize = 12.sp, color = Slate500) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            addErrorMessage = null
                        },
                        label = { Text("Email (ईमेल / Login ID) *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. rahul.sharma@soe.com", fontSize = 12.sp, color = Slate500) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Initial Password
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                addErrorMessage = null
                            },
                            label = { Text("Initial Password (पासवर्ड) *", fontSize = 12.sp) },
                            placeholder = { Text("Officer@123", fontSize = 12.sp, color = Slate500) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                        tint = Slate500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "💡 Default password is set to Officer@123. The officer can login with this and reset it anytime.",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }

                    // 4. Mobile
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = {
                            mobile = it
                            addErrorMessage = null
                        },
                        label = { Text("Mobile (मोबाइल नंबर)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 9876543210", fontSize = 12.sp, color = Slate500) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 5. State & District
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State (राज्य)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = district,
                            onValueChange = {
                                district = it
                                addErrorMessage = null
                            },
                            label = { Text("District (जिला)", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Jaipur", fontSize = 12.sp, color = Slate500) },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // District quick selector chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Quick Select District (जिला चुनें):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonDistricts.forEach { dist ->
                                val isSelected = district.equals(dist, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Indigo600 else Slate100)
                                        .clickable { district = dist }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = dist,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Slate700
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val cleanEmail = email.trim()
                        val cleanPassword = password.trim().ifBlank { "Officer@123" }
                        val cleanMobile = mobile.trim()
                        val cleanState = state.trim().ifBlank { "Rajasthan" }
                        val cleanDistrict = district.trim()

                        if (cleanName.isBlank()) {
                            addErrorMessage = "Please enter the officer's full name."
                            return@Button
                        }
                        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                            addErrorMessage = "Please enter a valid email address."
                            return@Button
                        }
                        if (cleanPassword.length < 6) {
                            addErrorMessage = "Password must be at least 6 characters."
                            return@Button
                        }

                        isSaving = true
                        addErrorMessage = null

                        val newEmployee = User(
                            userId = "",
                            name = cleanName,
                            email = cleanEmail,
                            mobile = cleanMobile,
                            state = cleanState,
                            district = cleanDistrict,
                            role = UserRole.EMPLOYEE,
                            status = UserStatus.ACTIVE
                        )

                        onSaveEmployee(newEmployee, cleanPassword) { result ->
                            isSaving = false
                            if (result.isSuccess) {
                                showAddEmployeeDialog = false
                                successNotification = "Officer $cleanName added successfully!"
                                newlyCreatedCredentials = Triple(cleanName, cleanEmail, cleanPassword)
                                triggerRefresh()
                            } else {
                                addErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to add officer. Please try again."
                            }
                        }
                    },
                    enabled = !isSaving && name.isNotBlank() && email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adding...")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Officer")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddEmployeeDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Newly Created Credentials Share Dialog
    if (newlyCreatedCredentials != null) {
        val (empName, empEmail, empPassword) = newlyCreatedCredentials!!
        AlertDialog(
            onDismissRequest = { newlyCreatedCredentials = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Officer Account Ready!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Navy900
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "The officer account has been created in Firebase. Share these login credentials with the officer:",
                        fontSize = 12.sp,
                        color = Slate700
                    )

                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text("Officer Name:", fontSize = 11.sp, color = Slate500)
                                Text(empName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            }
                            Column {
                                Text("Email / Login ID:", fontSize = 11.sp, color = Slate500)
                                Text(empEmail, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            }
                            Column {
                                Text("Default Password:", fontSize = 11.sp, color = Slate500)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(empPassword, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(empPassword))
                                            Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = Indigo600, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val fullText = "SOE Portal Login Details\nName: $empName\nEmail: $empEmail\nPassword: $empPassword"
                                clipboardManager.setText(AnnotatedString(fullText))
                                Toast.makeText(context, "All credentials copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy All", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareText = "SOE Portal Login Details\nName: $empName\nEmail: $empEmail\nPassword: $empPassword\n\nPlease log in and change your password."
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "SOE Portal Login Credentials")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Login Credentials"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { newlyCreatedCredentials = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Edit Employee Dialog
    if (employeeToEdit != null) {
        val emp = employeeToEdit!!
        var name by remember { mutableStateOf(emp.name) }
        var email by remember { mutableStateOf(emp.email) }
        var mobile by remember { mutableStateOf(emp.mobile) }
        var state by remember { mutableStateOf(emp.state.ifBlank { "Rajasthan" }) }
        var district by remember { mutableStateOf(emp.district) }
        var status by remember { mutableStateOf(emp.status) }
        var isSaving by remember { mutableStateOf(false) }
        var editErrorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) employeeToEdit = null },
            title = { Text("Edit Officer Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editErrorMessage != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = editErrorMessage!!,
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            editErrorMessage = null
                        },
                        label = { Text("Full Name", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            editErrorMessage = null
                        },
                        label = { Text("Email Address", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Account Status (सक्रिय/निष्क्रिय)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = status == UserStatus.ACTIVE,
                            onCheckedChange = { checked -> status = if (checked) UserStatus.ACTIVE else UserStatus.INACTIVE },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo600
                            )
                        )
                    }

                    // Reset Password Button in Edit Dialog
                    OutlinedButton(
                        onClick = {
                            val targetEmp = emp.copy(email = email.trim())
                            employeeToEdit = null
                            resetPasswordErrorMessage = null
                            employeeToResetPassword = targetEmp
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Indigo600)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Password Reset Email", fontSize = 12.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        editErrorMessage = null
                        val updated = emp.copy(
                            name = name.trim(),
                            email = email.trim(),
                            mobile = mobile.trim(),
                            state = state.trim(),
                            district = district.trim(),
                            status = status
                        )
                        onSaveEmployee(updated, null) { result ->
                            isSaving = false
                            if (result.isSuccess) {
                                employeeToEdit = null
                            } else {
                                editErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update officer."
                            }
                        }
                    },
                    enabled = !isSaving && name.isNotBlank() && email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { employeeToEdit = null },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Password Reset Confirmation Dialog
    if (employeeToResetPassword != null) {
        val emp = employeeToResetPassword!!
        AlertDialog(
            onDismissRequest = {
                if (!isSendingResetPassword) {
                    employeeToResetPassword = null
                    resetPasswordErrorMessage = null
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Indigo600,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Officer Password",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Navy900
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Send password reset instructions to officer's registered email:",
                        fontSize = 13.sp,
                        color = Slate700
                    )
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = emp.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500
                            )
                            Text(
                                text = emp.email,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ℹ️ Default Password info:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Indigo600
                            )
                            Text(
                                text = "Default initial password for officers is Officer@123. If email reset is delayed, the officer can log in with Officer@123 and change their password.",
                                fontSize = 11.sp,
                                color = Slate700
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("Officer@123"))
                                        Toast.makeText(context, "Default password (Officer@123) copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Indigo600)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Officer@123", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "Firebase Authentication will deliver an email containing a secure password reset link to this address. Check Spam/Junk folder if not in Inbox.",
                        fontSize = 11.sp,
                        color = Slate500
                    )

                    if (resetPasswordErrorMessage != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = resetPasswordErrorMessage!!,
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onResetPassword != null && emp.email.isNotBlank()) {
                            isSendingResetPassword = true
                            resetPasswordErrorMessage = null
                            onResetPassword(emp.email) { result ->
                                isSendingResetPassword = false
                                if (result.isSuccess) {
                                    successNotification = "Password reset email sent to ${emp.email}."
                                    employeeToResetPassword = null
                                } else {
                                    resetPasswordErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to send password reset email. Please try again."
                                }
                            }
                        } else {
                            employeeToResetPassword = null
                        }
                    },
                    enabled = !isSendingResetPassword && emp.email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSendingResetPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sending...", fontSize = 13.sp)
                    } else {
                        Text("Send Reset Email", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        employeeToResetPassword = null
                        resetPasswordErrorMessage = null
                    },
                    enabled = !isSendingResetPassword
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
        Text(value.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy900)
    }
}

@Composable
fun CompactEmployeeCardItem(
    employee: User,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleStatus: (UserStatus) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Name & State/District
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${employee.state.ifBlank { "Rajasthan" }} • ${employee.district.ifBlank { "All Districts" }}",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            // Right Row: Status Dot (Green for Active, Red for Inactive) + Edit Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Indicator Dot: Green for Active, Red for Inactive
                val isActive = employee.status == UserStatus.ACTIVE
                val dotColor = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444)
                val dotBg = if (isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(dotBg)
                        .clickable {
                            onToggleStatus(if (isActive) UserStatus.INACTIVE else UserStatus.ACTIVE)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Officer",
                        tint = Indigo600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

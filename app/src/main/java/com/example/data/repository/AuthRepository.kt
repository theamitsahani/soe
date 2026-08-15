package com.example.data.repository

import android.content.Context
import android.util.Log
import android.util.Patterns
import com.example.data.local.AppDatabase
import com.example.data.local.UserEntity
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.util.FirebaseUtils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firebaseAuth: FirebaseAuth? get() = FirebaseUtils.auth
    private val firestore: FirebaseFirestore? get() = FirebaseUtils.firestore

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var usersListenerRegistration: ListenerRegistration? = null

    init {
        // Production clean initialization - No demo users seeded
    }

    fun startListeningToFirestoreUsers() {
        if (usersListenerRegistration != null) return
        val fAuth = firebaseAuth ?: return
        if (fAuth.currentUser == null) return
        val fStore = firestore ?: return

        try {
            usersListenerRegistration = fStore.collection("users").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AuthRepository", "Users collection snapshot listener notice: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val userEntities = snapshot.documents.mapNotNull { doc ->
                        parseDocToUserEntity(doc)
                    }
                    if (userEntities.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                db.userDao().insertUsers(userEntities)
                            } catch (e: Exception) {
                                Log.e("AuthRepository", "Failed to cache users in Room DB", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to attach snapshot listener", e)
        }
    }

    fun stopListeningToFirestoreUsers() {
        try {
            usersListenerRegistration?.remove()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Error removing snapshot listener", e)
        }
        usersListenerRegistration = null
    }

    private fun parseDocToUserEntity(doc: DocumentSnapshot): UserEntity? {
        val docId = doc.id.trim()
        val explicitUid = doc.getString("userId")?.trim()
        val userId = docId.ifBlank { explicitUid ?: "" }
        if (userId.isBlank()) return null

        val email = (doc.getString("email") ?: "").trim()
        val name = (doc.getString("name")
            ?: doc.getString("displayName")
            ?: doc.getString("fullName")
            ?: (if (email.isNotBlank()) email.substringBefore("@") else "Officer")).trim()

        val mobile = (doc.getString("mobile") ?: doc.getString("phone") ?: doc.getString("phoneNumber") ?: "").trim()
        val state = (doc.getString("state") ?: "Rajasthan").trim().ifBlank { "Rajasthan" }
        val district = (doc.getString("district") ?: "").trim()

        val rawRole = (doc.getString("role") ?: "EMPLOYEE").trim()
        val normalizedRole = when {
            rawRole.equals("ADMIN", ignoreCase = true) -> UserRole.ADMIN.name
            rawRole.equals("EMPLOYEE", ignoreCase = true) -> UserRole.EMPLOYEE.name
            else -> UserRole.EMPLOYEE.name
        }

        val rawStatus = (doc.getString("status") ?: "ACTIVE").trim()
        val normalizedStatus = when {
            rawStatus.equals("INACTIVE", ignoreCase = true) -> UserStatus.INACTIVE.name
            else -> UserStatus.ACTIVE.name
        }

        val isDeleted = doc.getBoolean("isDeleted") ?: false
        val deletedAt = doc.getLong("deletedAt") ?: 0L

        return UserEntity(
            userId = userId,
            name = name,
            email = email,
            mobile = mobile,
            state = state,
            district = district,
            role = normalizedRole,
            status = normalizedStatus,
            isDeleted = isDeleted,
            deletedAt = deletedAt
        )
    }

    suspend fun checkCurrentSession(): User? = withContext(Dispatchers.IO) {
        try {
            val fAuth = firebaseAuth ?: return@withContext null
            val currentFbUser = fAuth.currentUser ?: return@withContext null
            val uid = currentFbUser.uid
            val userEmail = currentFbUser.email ?: ""

            val fStore = firestore
            if (fStore != null) {
                try {
                    var userDoc: DocumentSnapshot? = null
                    val docTask = fStore.collection("users").document(uid).get()
                    val doc = Tasks.await(docTask)
                    if (doc.exists()) {
                        userDoc = doc
                    } else if (userEmail.isNotBlank()) {
                        val queryTask = fStore.collection("users").whereEqualTo("email", userEmail).limit(1).get()
                        val querySnap = Tasks.await(queryTask)
                        if (!querySnap.isEmpty) {
                            userDoc = querySnap.documents.firstOrNull()
                        }
                    }

                    if (userDoc != null && userDoc.exists()) {
                        val statusStr = userDoc.getString("status")?.trim()?.uppercase() ?: UserStatus.ACTIVE.name
                        if (statusStr == "INACTIVE") {
                            fAuth.signOut()
                            _currentUser.value = null
                            return@withContext null
                        }

                        val rawRole = userDoc.getString("role")?.trim()?.uppercase()
                        val role = when (rawRole) {
                            "ADMIN" -> UserRole.ADMIN
                            else -> UserRole.EMPLOYEE
                        }

                        val name = userDoc.getString("name")?.takeIf { it.isNotBlank() }
                            ?: currentFbUser.displayName
                            ?: userEmail.substringBefore("@")
                        val email = userDoc.getString("email") ?: userEmail
                        val mobile = userDoc.getString("mobile") ?: ""
                        val state = userDoc.getString("state") ?: "Rajasthan"
                        val district = userDoc.getString("district") ?: ""

                        val user = User(
                            userId = uid,
                            name = name,
                            email = email,
                            mobile = mobile,
                            state = state,
                            district = district,
                            role = role,
                            status = UserStatus.ACTIVE
                        )

                        db.userDao().insertUser(
                            UserEntity(
                                userId = user.userId,
                                name = user.name,
                                email = user.email,
                                mobile = user.mobile,
                                state = user.state,
                                district = user.district,
                                role = user.role.name,
                                status = user.status.name
                            )
                        )
                        _currentUser.value = user
                        startListeningToFirestoreUsers()
                        return@withContext user
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Failed to fetch session user profile from Firestore: ${e.message}")
                }
            }

            // Check cached session on this device
            val localUser = db.userDao().getUserById(uid)
            if (localUser != null) {
                if (localUser.status.uppercase() == "INACTIVE") {
                    fAuth.signOut()
                    _currentUser.value = null
                    return@withContext null
                }
                val role = when (localUser.role.uppercase()) {
                    "ADMIN" -> UserRole.ADMIN
                    else -> UserRole.EMPLOYEE
                }
                val user = User(
                    userId = localUser.userId,
                    name = localUser.name,
                    email = localUser.email,
                    mobile = localUser.mobile,
                    state = localUser.state,
                    district = localUser.district,
                    role = role,
                    status = UserStatus.ACTIVE
                )
                _currentUser.value = user
                startListeningToFirestoreUsers()
                return@withContext user
            }

            fAuth.signOut()
            _currentUser.value = null
            null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking current session", e)
            firebaseAuth?.signOut()
            _currentUser.value = null
            null
        }
    }

    suspend fun login(emailOrUserId: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val input = emailOrUserId.trim()
        if (input.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your email address."))
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your password."))
        }

        val fAuth = firebaseAuth
        if (fAuth == null) {
            return@withContext Result.failure(Exception("Firebase Authentication is not configured."))
        }
        val fStore = firestore

        try {
            val authTask = fAuth.signInWithEmailAndPassword(input, password)
            val authResult = Tasks.await(authTask)
            val fbUser = authResult.user
                ?: return@withContext Result.failure(Exception("Authentication failed. User record not found."))

            val uid = fbUser.uid
            val userEmail = fbUser.email ?: input

            var role = UserRole.EMPLOYEE
            var name = fbUser.displayName ?: userEmail.substringBefore("@")
            var mobile = ""
            var state = "Rajasthan"
            var district = ""
            var status = UserStatus.ACTIVE

            if (fStore != null) {
                try {
                    var userDoc: DocumentSnapshot? = null
                    val docTask = fStore.collection("users").document(uid).get()
                    val doc = Tasks.await(docTask)
                    if (doc.exists()) {
                        userDoc = doc
                    } else {
                        val queryTask = fStore.collection("users").whereEqualTo("email", userEmail).limit(1).get()
                        val querySnap = Tasks.await(queryTask)
                        if (!querySnap.isEmpty) {
                            userDoc = querySnap.documents.firstOrNull()
                        }
                    }

                    if (userDoc != null && userDoc.exists()) {
                        val statusStr = userDoc.getString("status")?.trim()?.uppercase() ?: UserStatus.ACTIVE.name
                        if (statusStr == "INACTIVE") {
                            fAuth.signOut()
                            return@withContext Result.failure(Exception("Your account has been deactivated. Please contact administrator."))
                        }

                        val rawRole = userDoc.getString("role")?.trim()?.uppercase()
                        role = when (rawRole) {
                            "ADMIN" -> UserRole.ADMIN
                            else -> UserRole.EMPLOYEE
                        }

                        name = userDoc.getString("name")?.takeIf { it.isNotBlank() } ?: name
                        mobile = userDoc.getString("mobile") ?: ""
                        state = userDoc.getString("state") ?: "Rajasthan"
                        district = userDoc.getString("district") ?: ""
                    } else {
                        // User exists in Firebase Auth but no Firestore record yet; initialize standard Employee record
                        val newUserData = mapOf(
                            "userId" to uid,
                            "name" to name,
                            "email" to userEmail,
                            "mobile" to mobile,
                            "state" to state,
                            "district" to district,
                            "role" to UserRole.EMPLOYEE.name,
                            "status" to UserStatus.ACTIVE.name,
                            "createdAt" to System.currentTimeMillis(),
                            "updatedAt" to System.currentTimeMillis()
                        )
                        fStore.collection("users").document(uid).set(newUserData, SetOptions.merge())
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Failed to fetch or update Firestore user doc: ${e.message}")
                }
            }

            val authenticatedUser = User(
                userId = uid,
                name = name,
                email = userEmail,
                mobile = mobile,
                state = state,
                district = district,
                role = role,
                status = status
            )

            db.userDao().insertUser(
                UserEntity(
                    userId = authenticatedUser.userId,
                    name = authenticatedUser.name,
                    email = authenticatedUser.email,
                    mobile = authenticatedUser.mobile,
                    state = authenticatedUser.state,
                    district = authenticatedUser.district,
                    role = authenticatedUser.role.name,
                    status = authenticatedUser.status.name
                )
            )

            _currentUser.value = authenticatedUser
            startListeningToFirestoreUsers()
            return@withContext Result.success(authenticatedUser)
        } catch (e: Throwable) {
            val rootCause = e.cause ?: e
            val rawMsg = (rootCause.message ?: "").lowercase()
            val className = rootCause.javaClass.simpleName
            val isNetworkIssue = rawMsg.contains("network") ||
                    rawMsg.contains("connection") ||
                    rawMsg.contains("unable to resolve host") ||
                    rawMsg.contains("timeout") ||
                    className.contains("FirebaseNetworkException") ||
                    rootCause is java.io.IOException

            if (isNetworkIssue) {
                // Check if user previously logged in on this device
                val localUser = db.userDao().getUserByEmail(input)
                if (localUser != null) {
                    if (localUser.status.uppercase() == "INACTIVE") {
                        return@withContext Result.failure(Exception("Your account has been deactivated. Please contact administrator."))
                    }
                    val role = when (localUser.role.uppercase()) {
                        "ADMIN" -> UserRole.ADMIN
                        else -> UserRole.EMPLOYEE
                    }
                    val authenticatedUser = User(
                        userId = localUser.userId,
                        name = localUser.name,
                        email = localUser.email,
                        mobile = localUser.mobile,
                        state = localUser.state,
                        district = localUser.district,
                        role = role,
                        status = UserStatus.ACTIVE
                    )
                    _currentUser.value = authenticatedUser
                    return@withContext Result.success(authenticatedUser)
                } else {
                    return@withContext Result.failure(Exception("Internet connection required to sign in for the first time."))
                }
            }

            // Real authentication failure (invalid password, user not found, account disabled, etc.)
            val friendlyMsg = mapAuthErrorToUserMessage(e)
            return@withContext Result.failure(Exception(friendlyMsg))
        }
    }

    private fun mapAuthErrorToUserMessage(e: Throwable): String {
        Log.e("AuthRepository", "Authentication failure", e)
        val rootCause = e.cause ?: e
        val rawMessage = (rootCause.message ?: "").lowercase()
        val className = rootCause::class.java.simpleName

        return when {
            rawMessage.contains("your account has been deactivated") || rawMessage.contains("account is inactive") -> {
                "Your account has been deactivated. Please contact administrator."
            }
            rawMessage.contains("please enter a valid email") -> {
                "Please enter a valid email address."
            }
            rawMessage.contains("please enter your email") -> {
                "Please enter your email address."
            }
            rawMessage.contains("please enter your password") -> {
                "Please enter your password."
            }
            rawMessage.contains("badly formatted") ||
            rawMessage.contains("invalid email") ||
            rawMessage.contains("invalid_email") ||
            className.contains("FirebaseAuthInvalidCredentialsException") && rawMessage.contains("email") -> {
                "Please enter a valid email address."
            }
            rawMessage.contains("invalid_credential") ||
            rawMessage.contains("invalid-credential") ||
            rawMessage.contains("wrong_password") ||
            rawMessage.contains("wrong password") ||
            rawMessage.contains("user_not_found") ||
            rawMessage.contains("user not found") ||
            rawMessage.contains("no user record") ||
            className.contains("FirebaseAuthInvalidCredentialsException") ||
            className.contains("FirebaseAuthInvalidUserException") -> {
                "Incorrect email address or password. Please verify and try again."
            }
            rawMessage.contains("network") ||
            rawMessage.contains("connection") ||
            rawMessage.contains("unable to resolve host") ||
            rawMessage.contains("timeout") ||
            rawMessage.contains("unreachable") ||
            className.contains("FirebaseNetworkException") ||
            rootCause is java.io.IOException -> {
                "Internet connection unavailable. Please check your network and try again."
            }
            rawMessage.contains("user_disabled") || rawMessage.contains("user disabled") -> {
                "Your account has been disabled. Please contact administrator."
            }
            else -> {
                "Authentication failed: ${rootCause.localizedMessage ?: "Please try again."}"
            }
        }
    }

    fun logout() {
        stopListeningToFirestoreUsers()
        firebaseAuth?.signOut()
        _currentUser.value = null
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = firebaseAuth?.currentUser ?: throw Exception("Not authenticated")
            val task = currentUser.updatePassword(newPassword)
            Tasks.await(task)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllEmployees(): Flow<List<User>> {
        return db.userDao().getAllUsers().map { entities ->
            entities.mapNotNull { e ->
                val isEmployee = e.role.equals(UserRole.EMPLOYEE.name, ignoreCase = true)
                if (!isEmployee) null
                else User(
                    userId = e.userId,
                    name = e.name,
                    email = e.email,
                    mobile = e.mobile,
                    state = e.state,
                    district = e.district,
                    role = UserRole.EMPLOYEE,
                    status = if (e.status.equals("INACTIVE", ignoreCase = true)) UserStatus.INACTIVE else UserStatus.ACTIVE,
                    isDeleted = e.isDeleted,
                    deletedAt = e.deletedAt,
                    createdAt = e.createdAt
                )
            }
        }
    }

    suspend fun refreshEmployeesFromFirestore(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val fStore = firestore
            val fAuth = firebaseAuth

            if (fStore != null && fAuth?.currentUser != null) {
                try {
                    val snapshotTask = fStore.collection("users").get()
                    val snapshot = Tasks.await(snapshotTask)

                    val employeeEntities = snapshot.documents.mapNotNull { doc ->
                        val entity = parseDocToUserEntity(doc)
                        if (entity != null && entity.role.equals(UserRole.EMPLOYEE.name, ignoreCase = true)) {
                            entity
                        } else {
                            null
                        }
                    }

                    if (employeeEntities.isNotEmpty()) {
                        db.userDao().insertUsers(employeeEntities)
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Remote employee fetch notice: ${e.message}")
                }
            }

            val localEntities = db.userDao().getAllUsersList().filter {
                it.role.equals(UserRole.EMPLOYEE.name, ignoreCase = true)
            }
            val usersList = localEntities.map { e ->
                User(
                    userId = e.userId,
                    name = e.name,
                    email = e.email,
                    mobile = e.mobile,
                    state = e.state,
                    district = e.district,
                    role = UserRole.EMPLOYEE,
                    status = if (e.status.equals("INACTIVE", ignoreCase = true)) UserStatus.INACTIVE else UserStatus.ACTIVE,
                    isDeleted = e.isDeleted,
                    deletedAt = e.deletedAt,
                    createdAt = e.createdAt
                )
            }

            Result.success(usersList)
        } catch (e: Exception) {
            Log.w("AuthRepository", "Employee sync fallback: ${e.message}")
            val localEntities = db.userDao().getAllUsersList().filter {
                it.role.equals(UserRole.EMPLOYEE.name, ignoreCase = true)
            }
            val usersList = localEntities.map { e ->
                User(
                    userId = e.userId,
                    name = e.name,
                    email = e.email,
                    mobile = e.mobile,
                    state = e.state,
                    district = e.district,
                    role = UserRole.EMPLOYEE,
                    status = if (e.status.equals("INACTIVE", ignoreCase = true)) UserStatus.INACTIVE else UserStatus.ACTIVE,
                    isDeleted = e.isDeleted,
                    deletedAt = e.deletedAt,
                    createdAt = e.createdAt
                )
            }
            Result.success(usersList)
        }
    }

    suspend fun syncEmployeesFromFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        val result = refreshEmployeesFromFirestore()
        if (result.isSuccess) {
            Result.success(result.getOrNull()?.size ?: 0)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to sync employees"))
        }
    }

    private fun getSecondaryAuth(): FirebaseAuth? {
        return try {
            val defaultApp = FirebaseApp.getInstance()
            val secondaryApp = try {
                FirebaseApp.getInstance("SecondaryAuthApp")
            } catch (e: Exception) {
                FirebaseApp.initializeApp(context, defaultApp.options, "SecondaryAuthApp")
            }
            FirebaseAuth.getInstance(secondaryApp)
        } catch (e: Exception) {
            Log.w("AuthRepository", "Secondary auth initialization fallback: ${e.message}")
            null
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim().lowercase()
            if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                return@withContext Result.failure(Exception("Please enter a valid email address."))
            }

            // Ensure account exists in Firebase Auth before sending reset email
            val secondaryAuth = getSecondaryAuth()
            if (secondaryAuth != null) {
                try {
                    // Pre-create user in Auth if it was only present in Firestore/Room
                    val createResult = Tasks.await(secondaryAuth.createUserWithEmailAndPassword(trimmedEmail, "Officer@123"))
                    secondaryAuth.signOut()
                } catch (e: Exception) {
                    // If user already exists in Auth, proceed smoothly
                }
            }

            val fAuth = firebaseAuth ?: return@withContext Result.failure(Exception("Internet connection unavailable. Please try again."))
            val task = fAuth.sendPasswordResetEmail(trimmedEmail)
            Tasks.await(task)
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e("AuthRepository", "Failed to send password reset email to $email", e)
            val friendlyMsg = mapPasswordResetErrorToUserMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    private fun mapPasswordResetErrorToUserMessage(throwable: Throwable): String {
        val rootCause: Throwable = throwable.cause ?: throwable
        val rawMessage = (rootCause.message ?: throwable.message ?: "").lowercase()
        val className = rootCause.javaClass.simpleName

        return when {
            rawMessage.contains("badly formatted") ||
            rawMessage.contains("invalid email") ||
            className.contains("FirebaseAuthInvalidCredentialsException") && rawMessage.contains("email") -> {
                "Please enter a valid email address."
            }
            rawMessage.contains("user_not_found") ||
            rawMessage.contains("user not found") ||
            rawMessage.contains("no user record") ||
            className.contains("FirebaseAuthInvalidUserException") -> {
                "No user account found with this email address."
            }
            rawMessage.contains("network") ||
            rawMessage.contains("connection") ||
            className.contains("FirebaseNetworkException") ||
            rootCause is java.io.IOException -> {
                "Internet connection unavailable. Please try again."
            }
            else -> {
                "Unable to send password reset email. Please try again."
            }
        }
    }

    suspend fun saveEmployee(user: User, initialPassword: String = "Officer@123"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = user.email.trim().lowercase()
            val cleanName = user.name.trim()
            val cleanMobile = user.mobile.trim().filter { it.isDigit() }
            val cleanState = user.state.trim().ifBlank { "Rajasthan" }
            val cleanDistrict = user.district.trim()
            val isNewUser = user.userId.isBlank()
            var userId = if (isNewUser) "" else user.userId

            if (cleanName.isBlank()) {
                return@withContext Result.failure(Exception("Please enter the officer's full name."))
            }
            if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                return@withContext Result.failure(Exception("Please enter a valid email address."))
            }
            if (cleanMobile.isNotEmpty() && (cleanMobile.length != 10 || !cleanMobile.all { it.isDigit() })) {
                return@withContext Result.failure(Exception("Mobile number must be exactly 10 digits."))
            }

            // Check local duplicate email for new user or email change
            val existingLocal = db.userDao().getUserByEmail(cleanEmail)
            if (existingLocal != null && existingLocal.userId != userId && !existingLocal.isDeleted) {
                return@withContext Result.failure(Exception("An officer with email $cleanEmail already exists. Please use a different email address."))
            }

            // If new user, create Firebase Auth account using secondary auth so Admin's active session is never logged out
            if (isNewUser) {
                val secAuth = getSecondaryAuth()
                if (secAuth != null) {
                    try {
                        val pass = initialPassword.ifBlank { "Officer@123" }
                        val createResult = Tasks.await(secAuth.createUserWithEmailAndPassword(cleanEmail, pass))
                        val authUid = createResult.user?.uid
                        if (!authUid.isNullOrBlank()) {
                            userId = authUid
                        }
                        secAuth.signOut()
                    } catch (e: Exception) {
                        Log.w("AuthRepository", "Secondary auth registration notice: ${e.message}")
                    }
                }
            }

            if (userId.isBlank()) {
                userId = "emp_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            }

            val fStore = firestore

            // Check Firestore for duplicate email if new user or changed
            if (fStore != null) {
                try {
                    val queryTask = fStore.collection("users").whereEqualTo("email", cleanEmail).limit(2).get()
                    val snap = Tasks.await(queryTask)
                    if (!snap.isEmpty && snap.documents.any { it.id != userId && !(it.getBoolean("isDeleted") ?: false) }) {
                        return@withContext Result.failure(Exception("An account with email $cleanEmail already exists. Please use a different email."))
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Duplicate check notice: ${e.message}")
                }
            }

            // Update in local Room database cache
            val entity = UserEntity(
                userId = userId,
                name = cleanName,
                email = cleanEmail,
                mobile = cleanMobile,
                state = cleanState,
                district = cleanDistrict,
                role = UserRole.EMPLOYEE.name,
                status = user.status.name,
                isDeleted = user.isDeleted,
                deletedAt = user.deletedAt,
                createdAt = if (user.createdAt > 0L) user.createdAt else System.currentTimeMillis()
            )
            db.userDao().insertUser(entity)

            // Update directly in Firestore users collection
            if (fStore != null) {
                val userMap = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "name" to cleanName,
                    "email" to cleanEmail,
                    "mobile" to cleanMobile,
                    "state" to cleanState,
                    "district" to cleanDistrict,
                    "role" to UserRole.EMPLOYEE.name,
                    "status" to user.status.name,
                    "isDeleted" to user.isDeleted,
                    "deletedAt" to user.deletedAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                if (isNewUser) {
                    userMap["createdAt"] = System.currentTimeMillis()
                }

                val setTask = fStore.collection("users").document(userId).set(userMap, SetOptions.merge())
                Tasks.await(setTask)
            }

            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving employee", e)
            Result.failure(e)
        }
    }

    suspend fun softDeleteEmployee(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            db.userDao().softDeleteUser(userId, now)
            val fStore = firestore
            if (fStore != null) {
                val map = mapOf<String, Any>(
                    "isDeleted" to true,
                    "deletedAt" to now,
                    "updatedAt" to now
                )
                val updateTask = fStore.collection("users").document(userId).set(map, SetOptions.merge())
                Tasks.await(updateTask)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error soft deleting employee", e)
            Result.failure(e)
        }
    }

    suspend fun restoreEmployee(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.userDao().restoreUser(userId)
            val fStore = firestore
            if (fStore != null) {
                val map = mapOf<String, Any>(
                    "isDeleted" to false,
                    "deletedAt" to 0L,
                    "updatedAt" to System.currentTimeMillis()
                )
                val updateTask = fStore.collection("users").document(userId).set(map, SetOptions.merge())
                Tasks.await(updateTask)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error restoring employee", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        softDeleteEmployee(userId)
    }

    suspend fun permanentDeleteEmployee(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.userDao().deleteUserById(userId)
            val fStore = firestore
            if (fStore != null) {
                val deleteTask = fStore.collection("users").document(userId).delete()
                Tasks.await(deleteTask)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error permanently deleting employee", e)
            Result.failure(e)
        }
    }
}

package com.example.data.model

enum class UserRole {
    ADMIN,
    EMPLOYEE
}

enum class UserStatus {
    ACTIVE,
    INACTIVE
}

data class User(
    val userId: String,
    val name: String,
    val email: String,
    val mobile: String = "",
    val state: String = "Rajasthan",
    val district: String = "",
    val role: UserRole = UserRole.EMPLOYEE,
    val status: UserStatus = UserStatus.ACTIVE,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

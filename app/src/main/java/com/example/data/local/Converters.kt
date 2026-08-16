package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.UserRole
import com.example.data.model.VisitStatus

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.EMPLOYEE
    }

    @TypeConverter
    fun fromVisitStatus(value: VisitStatus): String = value.name

    @TypeConverter
    fun toVisitStatus(value: String): VisitStatus = try {
        VisitStatus.valueOf(value)
    } catch (e: Exception) {
        VisitStatus.ASSIGNED
    }
}

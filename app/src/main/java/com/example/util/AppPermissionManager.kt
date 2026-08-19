package com.example.util

import com.example.data.model.UserRole
import com.example.data.model.VisitStatus

object AppPermissionManager {

    fun isAdmin(role: UserRole): Boolean {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
    }

    fun isReviewer(role: UserRole): Boolean {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.REVIEWER || role == UserRole.SUPERVISOR
    }

    fun canCreateTask(role: UserRole): Boolean {
        return isAdmin(role) || role == UserRole.SUPERVISOR
    }

    fun canAssignTask(role: UserRole): Boolean {
        return isAdmin(role) || role == UserRole.SUPERVISOR
    }

    fun canManageSchools(role: UserRole): Boolean {
        return isAdmin(role) || role == UserRole.SUPERVISOR
    }

    fun canManageEmployees(role: UserRole): Boolean {
        return isAdmin(role)
    }

    fun canReviewVisit(role: UserRole): Boolean {
        return isReviewer(role)
    }

    fun canExportReport(role: UserRole): Boolean {
        return isAdmin(role) || role == UserRole.SUPERVISOR || role == UserRole.REVIEWER
    }

    fun canStartVisit(role: UserRole, taskEmployeeId: String, currentUid: String): Boolean {
        if (isAdmin(role)) return true
        return taskEmployeeId.isNotBlank() && taskEmployeeId == currentUid
    }

    fun canEditVisit(
        role: UserRole,
        visitEmployeeId: String,
        currentUid: String,
        status: VisitStatus,
        submittedAt: Long? = null,
        editCount: Int = 0
    ): Boolean {
        if (isAdmin(role)) return true
        if (visitEmployeeId != currentUid) return false
        if (status == VisitStatus.REVIEWED) return false

        // Resubmission rule: If already submitted, employee can only edit if within 12 hours and editCount < 1
        if (status == VisitStatus.SUBMITTED) {
            if (editCount >= 1) return false
            if (submittedAt != null) {
                val twelveHoursMs = 12 * 60 * 60 * 1000L
                val timePassed = System.currentTimeMillis() - submittedAt
                if (timePassed > twelveHoursMs) return false
            }
        }
        return true
    }

    fun canSubmitVisit(
        role: UserRole,
        visitEmployeeId: String,
        currentUid: String,
        status: VisitStatus
    ): Boolean {
        if (status == VisitStatus.REVIEWED) return false
        if (isAdmin(role)) return true
        return visitEmployeeId == currentUid
    }
}

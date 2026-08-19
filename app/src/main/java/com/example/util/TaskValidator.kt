package com.example.util

import com.example.data.model.Task
import com.example.data.model.VisitStatus

object TaskValidator {

    fun validateTaskCreation(
        schoolId: String,
        employeeId: String,
        visitDate: String
    ): ValidationResult {
        if (schoolId.isBlank()) {
            return ValidationResult.Error("School ID is required for task assignment")
        }
        if (employeeId.isBlank()) {
            return ValidationResult.Error("Employee ID is required for task assignment")
        }
        if (visitDate.isBlank()) {
            return ValidationResult.Error("Visit date is required for task assignment")
        }
        return ValidationResult.Success
    }

    fun validateTaskStart(task: Task, currentUid: String, isAdmin: Boolean = false): ValidationResult {
        if (task.status == VisitStatus.CANCELLED) {
            return ValidationResult.Error("Cannot start a cancelled task")
        }
        if (task.status == VisitStatus.REVIEWED) {
            return ValidationResult.Error("This visit report has already been reviewed and finalized")
        }
        if (!isAdmin && task.employeeId.isNotBlank() && task.employeeId != currentUid) {
            return ValidationResult.Error("You are not authorized to start a task assigned to another employee")
        }
        return ValidationResult.Success
    }
}

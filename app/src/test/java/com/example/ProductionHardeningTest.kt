package com.example

import com.example.data.model.Task
import com.example.data.model.UserRole
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.util.AppPermissionManager
import com.example.util.ImportValidator
import com.example.util.PhotoValidator
import com.example.util.TaskValidator
import com.example.util.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionHardeningTest {

    @Test
    fun testRBACPermissions() {
        // Admin permissions
        assertTrue(AppPermissionManager.isAdmin(UserRole.ADMIN))
        assertTrue(AppPermissionManager.isAdmin(UserRole.SUPER_ADMIN))
        assertFalse(AppPermissionManager.isAdmin(UserRole.EMPLOYEE))

        // Reviewer permissions
        assertTrue(AppPermissionManager.canReviewVisit(UserRole.ADMIN))
        assertTrue(AppPermissionManager.canReviewVisit(UserRole.REVIEWER))
        assertTrue(AppPermissionManager.canReviewVisit(UserRole.SUPERVISOR))
        assertFalse(AppPermissionManager.canReviewVisit(UserRole.EMPLOYEE))

        // Employee task start permission
        assertTrue(AppPermissionManager.canStartVisit(UserRole.EMPLOYEE, "emp_123", "emp_123"))
        assertFalse(AppPermissionManager.canStartVisit(UserRole.EMPLOYEE, "emp_123", "emp_456"))

        // Reviewed visit edit prohibition
        assertFalse(
            AppPermissionManager.canEditVisit(
                role = UserRole.EMPLOYEE,
                visitEmployeeId = "emp_123",
                currentUid = "emp_123",
                status = VisitStatus.REVIEWED
            )
        )
    }

    @Test
    fun testTaskValidator() {
        // Invalid task creation with missing parameters
        val emptySchool = TaskValidator.validateTaskCreation("", "emp_1", "2026-08-18")
        assertTrue(emptySchool is ValidationResult.Error)

        val validCreation = TaskValidator.validateTaskCreation("sch_1", "emp_1", "2026-08-18")
        assertTrue(validCreation is ValidationResult.Success)

        // Authorized vs Unauthorized start
        val task = Task(
            taskId = "task_001",
            visitId = "",
            schoolId = "sch_101",
            employeeId = "emp_user_1",
            status = VisitStatus.ASSIGNED
        )
        val validStart = TaskValidator.validateTaskStart(task, "emp_user_1")
        assertTrue(validStart is ValidationResult.Success)

        val invalidStart = TaskValidator.validateTaskStart(task, "emp_user_2")
        assertTrue(invalidStart is ValidationResult.Error)
    }

    @Test
    fun testPhotoValidator() {
        // Empty photo json validation
        val emptyValidation = PhotoValidator.validatePhotosForSubmission("{}")
        assertTrue(emptyValidation is ValidationResult.Error)

        // Valid payload containing all required categories
        val validJson = """
            {
                "school_photo": ["http://res.cloudinary.com/demo/image1.jpg"],
                "explaining_app": ["http://res.cloudinary.com/demo/image2.jpg"],
                "students_smart_board": ["http://res.cloudinary.com/demo/image3.jpg"],
                "principal_photo": ["http://res.cloudinary.com/demo/image4.jpg"],
                "letter_photo": ["http://res.cloudinary.com/demo/image5.jpg"]
            }
        """.trimIndent()
        val validResult = PhotoValidator.validatePhotosForSubmission(validJson)
        assertTrue(validResult is ValidationResult.Success)
    }

    @Test
    fun testImportValidator() {
        // Empty row
        val emptyRowResult = ImportValidator.validateSchoolRow(emptyList(), 1)
        assertTrue(emptyRowResult is ValidationResult.Error)

        // Missing school name (Column C index 2)
        val missingSchoolName = ImportValidator.validateSchoolRow(listOf("1", "Jaipur", "", "Govt", "Village"), 2)
        assertTrue(missingSchoolName is ValidationResult.Error)

        // Valid row
        val validRow = ImportValidator.validateSchoolRow(listOf("1", "Jaipur", "Govt Sr Sec School", "Govt", "Village", "Principal", "9876543210"), 3)
        assertTrue(validRow is ValidationResult.Success)

        // Mobile number cleaning
        val cleaned = ImportValidator.cleanMobileNumber("9876543210.0")
        assertEquals("9876543210", cleaned)
    }

    @Test
    fun testVisitEntityDefaults() {
        val visit = Visit(
            visitId = "vst_test",
            schoolId = "sch_test",
            employeeId = "emp_test"
        )
        assertEquals("V1", visit.formVersion)
        assertEquals(1, visit.revisionNumber)
        assertEquals(VisitStatus.ASSIGNED, visit.status)
        assertEquals(0, visit.editCount)
    }
}

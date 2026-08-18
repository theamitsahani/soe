package com.example.util

import com.example.data.model.PhotoCategory
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

object VisitValidator {

    fun validateStartVisit(schoolId: String, employeeId: String): ValidationResult {
        if (schoolId.isBlank()) return ValidationResult.Error("विद्यालय पहचान (School ID) अनुपलब्ध है।")
        if (employeeId.isBlank()) return ValidationResult.Error("कर्मचारी पहचान (Employee ID) अनुपलब्ध है।")
        return ValidationResult.Success
    }

    fun validateSubmission(
        visit: Visit,
        answers: VisitAnswers,
        photosMap: Map<String, List<String>>
    ): ValidationResult {
        if (visit.schoolId.isBlank()) {
            return ValidationResult.Error("विद्यालय का चयन आवश्यक है (School is required).")
        }
        if (visit.employeeId.isBlank()) {
            return ValidationResult.Error("फील्ड ऑफिसर विवरण आवश्यक है (Employee ID is required).")
        }

        // 1. Validate UDISE Code (11 digits mandatory)
        val cleanUdise = answers.q4_udiseCode.trim().filter { it.isDigit() }
        if (cleanUdise.isBlank()) {
            return ValidationResult.Error("कृपया 11 अंकों का UDISE कोड दर्ज करें (Please enter 11-digit UDISE code).")
        }
        if (cleanUdise.length != 11) {
            return ValidationResult.Error("UDISE कोड अमान्य है! यह अनिवार्य रूप से ठीक 11 अंकों का होना चाहिए (UDISE Code must be exactly 11 digits).")
        }

        // 2. Validate Principal Mobile (10 digits if provided)
        val cleanPMobile = answers.q8_principalMobile.trim().filter { it.isDigit() }
        if (cleanPMobile.isNotBlank() && cleanPMobile.length != 10) {
            return ValidationResult.Error("प्रधानाचार्य का मोबाइल नंबर ठीक 10 अंकों का होना अनिवार्य है (Principal mobile must be 10 digits).")
        }

        // 3. Validate BCI Mobile (10 digits if provided)
        val cleanBciMobile = answers.q13_bciMobile.trim().filter { it.isDigit() }
        if (cleanBciMobile.isNotBlank() && cleanBciMobile.length != 10) {
            return ValidationResult.Error("BCI मोबाइल नंबर ठीक 10 अंकों का होना अनिवार्य है (BCI mobile must be 10 digits).")
        }

        // 4. Validate Mandatory Photos
        val missingCategories = PhotoCategory.entries
            .filter { it.minRequired > 0 }
            .filter { (photosMap[it.categoryId] ?: emptyList()).size < it.minRequired }

        if (missingCategories.isNotEmpty()) {
            val names = missingCategories.joinToString("\n• ") { it.displayName }
            return ValidationResult.Error("कृपया निम्नलिखित अनिवार्य फोटो अपलोड करें:\n• $names")
        }

        return ValidationResult.Success
    }

    fun validateAdminReview(
        visitId: String,
        adminId: String
    ): ValidationResult {
        if (visitId.isBlank()) return ValidationResult.Error("विज़िट पहचान (Visit ID) अनुपलब्ध है।")
        if (adminId.isBlank()) return ValidationResult.Error("प्रशासक पहचान (Admin ID) अनुपलब्ध है।")
        return ValidationResult.Success
    }
}

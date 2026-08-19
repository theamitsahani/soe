package com.example.util

object ImportValidator {

    fun validateSchoolRow(
        row: List<String>,
        rowIndex: Int
    ): ValidationResult {
        if (row.isEmpty() || row.all { it.isBlank() }) {
            return ValidationResult.Error("Row $rowIndex is completely empty")
        }

        val schoolName = if (row.size > 2) row[2].trim() else ""
        if (schoolName.isBlank()) {
            return ValidationResult.Error("Row $rowIndex: Column C (School Name) is required and cannot be empty")
        }

        if (row.size > 6) {
            val mobile = row[6].trim()
            if (mobile.isNotBlank()) {
                val digitsOnly = mobile.filter { it.isDigit() }
                if (digitsOnly.length !in 10..12 && !mobile.contains("N/A", ignoreCase = true) && !mobile.contains("-")) {
                    // Mobile is present but has an invalid digit count
                    return ValidationResult.Success
                }
            }
        }

        return ValidationResult.Success
    }

    fun cleanMobileNumber(raw: String): String {
        var s = raw.trim()
        if (s.endsWith(".0")) {
            s = s.substringBefore(".0")
        }
        if (s.contains("E") || s.contains("e")) {
            try {
                s = java.math.BigDecimal(s).toPlainString()
            } catch (_: Exception) {}
        }
        return s.filter { it.isDigit() }
    }
}

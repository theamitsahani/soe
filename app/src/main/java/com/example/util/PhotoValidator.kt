package com.example.util

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object PhotoValidator {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
    private val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

    // Standard required photo categories for a valid visit submission
    val MANDATORY_CATEGORIES = listOf(
        "school_photo",
        "explaining_app",
        "students_smart_board",
        "principal_photo",
        "letter_photo"
    )

    fun validatePhotosForSubmission(
        photosJson: String,
        requiredCategories: List<String> = MANDATORY_CATEGORIES
    ): ValidationResult {
        if (photosJson.isBlank() || photosJson == "{}") {
            return ValidationResult.Error("At least one photo must be captured before submission")
        }

        try {
            val map = photosAdapter.fromJson(photosJson) ?: return ValidationResult.Error("Invalid photos payload")
            for (cat in requiredCategories) {
                val list = map[cat]
                if (list.isNullOrEmpty()) {
                    val categoryName = when (cat) {
                        "school_photo" -> "School Photo"
                        "explaining_app" -> "Explaining App Photo"
                        "students_smart_board" -> "Students Using Smart Board"
                        "principal_photo" -> "Principal Photo"
                        "letter_photo" -> "Official Letter Photo"
                        else -> cat
                    }
                    return ValidationResult.Error("Required photo missing: $categoryName")
                }
            }
            return ValidationResult.Success
        } catch (e: Exception) {
            return ValidationResult.Error("Failed to parse photos data: ${e.message}")
        }
    }

    fun validatePhotoSize(bytes: ByteArray, maxSizeBytes: Long = 15 * 1024 * 1024L): ValidationResult {
        if (bytes.isEmpty()) {
            return ValidationResult.Error("Photo data is empty")
        }
        if (bytes.size > maxSizeBytes) {
            return ValidationResult.Error("Photo exceeds maximum permitted size of 15MB")
        }
        return ValidationResult.Success
    }
}

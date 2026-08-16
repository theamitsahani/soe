package com.example.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class VisitPhoto(
    val categoryId: String,
    val localUri: String = "",
    val remoteUrl: String = "",
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object JsonHelper {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val visitAnswersAdapter = moshi.adapter(VisitAnswers::class.java)
    private val photoListType = Types.newParameterizedType(List::class.java, VisitPhoto::class.java)
    private val photoListAdapter = moshi.adapter<List<VisitPhoto>>(photoListType)

    fun toJson(answers: VisitAnswers): String {
        return try {
            visitAnswersAdapter.toJson(answers)
        } catch (_: Exception) {
            ""
        }
    }

    fun fromJson(json: String): VisitAnswers {
        if (json.isBlank()) return VisitAnswers()
        return try {
            visitAnswersAdapter.fromJson(json) ?: VisitAnswers()
        } catch (_: Exception) {
            VisitAnswers()
        }
    }

    fun photosToJson(photos: List<VisitPhoto>): String {
        return try {
            photoListAdapter.toJson(photos)
        } catch (_: Exception) {
            "[]"
        }
    }

    fun photosFromJson(json: String): List<VisitPhoto> {
        if (json.isBlank()) return emptyList()
        return try {
            photoListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

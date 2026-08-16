package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class UserRole {
    ADMIN,
    EMPLOYEE
}

enum class VisitStatus {
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val mobile: String = "",
    val designation: String = "Field Officer",
    val role: UserRole = UserRole.EMPLOYEE,
    val assignedDistrict: String = "All"
)

@Entity(tableName = "schools")
data class School(
    @PrimaryKey val schoolId: String,
    val schoolName: String,
    val districtName: String,
    val blockName: String,
    val stateName: String = "Rajasthan",
    val udiseCode: String = "",
    val principalName: String = "",
    val principalMobile: String = "",
    val totalStudents: Int = 0,
    val smartClassroomsCount: Int = 1,
    val address: String = ""
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val taskId: String,
    val schoolId: String,
    val employeeId: String,
    val employeeName: String,
    val schoolName: String,
    val state: String = "Rajasthan",
    val district: String,
    val block: String,
    val assignedBy: String = "Admin",
    val visitDate: String,
    val status: VisitStatus = VisitStatus.ASSIGNED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey val visitId: String,
    val taskId: String? = null,
    val schoolId: String,
    val schoolName: String,
    val employeeId: String,
    val employeeName: String,
    val district: String,
    val block: String,
    val visitDate: String,
    val visitTime: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val answersJson: String = "{}",
    val photosJson: String = "[]"
)

data class VisitAnswers(
    val udiseCode: String = "",
    val metPrincipal: String = "Yes",
    val principalFeedback: String = "",
    val missionGyanAwareness: String = "Yes",
    val participatingClasses: List<String> = emptyList(),
    val totalStudentsAttended: Int = 0,
    val bciTeacherName: String = "",
    val bciMobile: String = "",
    val whatsappGroupAdded: String = "Yes",
    val posterInstalled: String = "Yes",
    val smartClassStatus: String = "Working Fine",
    val keyObservations: String = "",
    val problemsOrAssistance: String = "",
    val followupRequired: String = "No"
)

data class VisitPhoto(
    val id: String = System.currentTimeMillis().toString(),
    val label: String,
    val localUri: String,
    val timestamp: Long = System.currentTimeMillis()
)

object JsonHelper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val answersAdapter = moshi.adapter(VisitAnswers::class.java)
    private val photosListType = Types.newParameterizedType(List::class.java, VisitPhoto::class.java)
    private val photosAdapter = moshi.adapter<List<VisitPhoto>>(photosListType)

    fun toJson(answers: VisitAnswers): String {
        return try {
            answersAdapter.toJson(answers)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun fromJson(json: String): VisitAnswers {
        return try {
            answersAdapter.fromJson(json) ?: VisitAnswers()
        } catch (e: Exception) {
            VisitAnswers()
        }
    }

    fun photosToJson(photos: List<VisitPhoto>): String {
        return try {
            photosAdapter.toJson(photos)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun photosFromJson(json: String): List<VisitPhoto> {
        return try {
            photosAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

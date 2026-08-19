package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PhotoUploadStatus
import com.example.data.model.VisitPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitPhotoDao {
    @Query("SELECT * FROM visit_photos WHERE visitId = :visitId ORDER BY capturedAt ASC")
    fun getPhotosByVisitId(visitId: String): Flow<List<VisitPhoto>>

    @Query("SELECT * FROM visit_photos WHERE visitId = :visitId ORDER BY capturedAt ASC")
    suspend fun getPhotosListByVisitId(visitId: String): List<VisitPhoto>

    @Query("SELECT * FROM visit_photos WHERE photoId = :photoId LIMIT 1")
    suspend fun getPhotoById(photoId: String): VisitPhoto?

    @Query("SELECT * FROM visit_photos WHERE uploadStatus != 'UPLOADED' ORDER BY capturedAt ASC")
    suspend fun getPendingPhotos(): List<VisitPhoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: VisitPhoto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<VisitPhoto>)

    @Update
    suspend fun updatePhoto(photo: VisitPhoto)

    @Query("DELETE FROM visit_photos WHERE photoId = :photoId")
    suspend fun deletePhotoById(photoId: String)

    @Query("DELETE FROM visit_photos WHERE visitId = :visitId")
    suspend fun deletePhotosByVisitId(visitId: String)
}

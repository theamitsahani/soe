package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Tasks
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object MediaStorageHelper {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
    private val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

    /**
     * Copies a picked Uri (content://) to the app's persistent internal storage
     * so that media remains available indefinitely, even offline, across app restarts.
     * Automatically downsamples large images to reduce memory pressure.
     */
    suspend fun saveMediaLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(sourceUri)
            val isVideo = isMimeOrUriVideo(mimeType, sourceUri.toString())
            val extension = if (mimeType != null) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: if (isVideo) "mp4" else "jpg"
            } else {
                val uriStr = sourceUri.toString().lowercase()
                when {
                    uriStr.contains(".mp4") -> "mp4"
                    uriStr.contains(".mov") -> "mov"
                    uriStr.contains(".3gp") -> "3gp"
                    uriStr.contains(".mkv") -> "mkv"
                    uriStr.contains(".png") -> "png"
                    uriStr.contains(".webp") -> "webp"
                    else -> if (isVideo) "mp4" else "jpg"
                }
            }

            val mediaDir = File(context.filesDir, "visit_media").apply {
                if (!exists()) mkdirs()
            }

            val prefix = if (isVideo) "vid_" else "img_"
            val destFile = File(mediaDir, "${prefix}${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension")

            if (!isVideo) {
                try {
                    var inSampleSize = 1
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(input, null, options)
                        val maxDim = 1920
                        val rawWidth = options.outWidth
                        val rawHeight = options.outHeight
                        if (rawWidth > maxDim || rawHeight > maxDim) {
                            val halfWidth = rawWidth / 2
                            val halfHeight = rawHeight / 2
                            while ((halfWidth / inSampleSize) >= maxDim || (halfHeight / inSampleSize) >= maxDim) {
                                inSampleSize *= 2
                            }
                        }
                    }

                    var bitmap: Bitmap? = null
                    contentResolver.openInputStream(sourceUri)?.use { secondInput ->
                        val decodeOptions = BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        bitmap = BitmapFactory.decodeStream(secondInput, null, decodeOptions)
                    }

                    if (bitmap != null) {
                        FileOutputStream(destFile).use { output ->
                            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, output)
                        }
                        bitmap?.recycle()
                        return@withContext Uri.fromFile(destFile).toString()
                    }
                } catch (e: Exception) {
                    Log.w("MediaStorageHelper", "Image compression fallback to raw copy", e)
                }
            }

            // Fallback for video or if bitmap compression fails
            contentResolver.openInputStream(sourceUri)?.use { input: InputStream ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }

    /**
     * Uploads all photos/videos in photosJson directly to Firebase Storage
     * and saves photo metadata to Firestore ("visits/{visitId}/photos/{photoId}").
     * Returns updated photosJson containing permanent Firebase Storage download URLs.
     */
    suspend fun uploadPhotosJsonToFirebaseStorage(
        context: Context,
        visitId: String,
        schoolId: String,
        employeeId: String,
        photosJson: String,
        schoolName: String = "",
        visitDate: String = "",
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        if (photosJson.isBlank() || photosJson == "{}") return@withContext photosJson

        try {
            val originalMap = photosAdapter.fromJson(photosJson) ?: return@withContext photosJson
            val updatedMap = mutableMapOf<String, List<String>>()
            val firestore = FirebaseUtils.firestore
            val db = com.example.data.local.AppDatabase.getDatabase(context)

            val safeVisitId = visitId.ifBlank { "unknown_visit" }
            val safeSchoolId = schoolId.ifBlank { "unknown_school" }

            val dbVisit = try { db.visitDao().getVisitById(safeVisitId) } catch (_: Exception) { null }
            val dbPhotoMap = try {
                if (dbVisit != null && dbVisit.photosJson.isNotBlank()) photosAdapter.fromJson(dbVisit.photosJson) else null
            } catch (_: Exception) { null }

            var totalItems = 0
            originalMap.values.forEach { totalItems += it.distinct().size }
            var processedItems = 0

            for ((categoryId, uriList) in originalMap) {
                val updatedUris = mutableListOf<String>()
                val cleanUriList = uriList.distinct()

                val dbCategoryUrls = dbPhotoMap?.get(categoryId) ?: emptyList()

                for ((index, uriStr) in cleanUriList.withIndex()) {
                    if (uriStr.startsWith("http://") || uriStr.startsWith("https://") || uriStr.startsWith("gs://")) {
                        // Already uploaded to Cloudinary or remote URL
                        if (!updatedUris.contains(uriStr)) {
                            updatedUris.add(uriStr)
                        }
                        processedItems++
                        onProgress?.invoke(processedItems, totalItems)
                    } else if (index < dbCategoryUrls.size && (dbCategoryUrls[index].startsWith("http://") || dbCategoryUrls[index].startsWith("https://"))) {
                        // Found existing Cloudinary URL in local database for this photo index!
                        val existingRemoteUrl = dbCategoryUrls[index]
                        if (!updatedUris.contains(existingRemoteUrl)) {
                            updatedUris.add(existingRemoteUrl)
                        }
                        processedItems++
                        onProgress?.invoke(processedItems, totalItems)
                    } else {
                        val bytes = readMediaBytes(context, uriStr)
                        if (bytes != null && bytes.isNotEmpty()) {
                            try {
                                val isVideo = isMediaVideo(uriStr, context)
                                val mime = if (isVideo) "video/mp4" else "image/jpeg"
                                val ext = if (isVideo) "mp4" else "jpg"
                                val fileName = getStandardizedFileName(categoryId, index, ext)
                                val photoId = UUID.randomUUID().toString()

                                val uploadResult = CloudinaryUploader.uploadBytes(
                                    bytes = bytes,
                                    visitId = safeVisitId,
                                    schoolId = safeSchoolId,
                                    categoryId = categoryId,
                                    isVideo = isVideo
                                )

                                if (uploadResult != null) {
                                    val downloadUrl = uploadResult.downloadUrl
                                    val publicId = uploadResult.publicId
                                    val resourceType = uploadResult.resourceType
                                    val storagePath = "${uploadResult.folder}/$publicId"

                                    // Save photo metadata to Firestore: visits/{visitId}/photos/{photoId}
                                    if (firestore != null && safeVisitId.isNotBlank()) {
                                        try {
                                            val now = System.currentTimeMillis()
                                            val photoMeta = hashMapOf(
                                                "photoId" to photoId,
                                                "visitId" to safeVisitId,
                                                "schoolId" to safeSchoolId,
                                                "employeeId" to employeeId,
                                                "category" to categoryId,
                                                "publicId" to publicId,
                                                "resourceType" to resourceType,
                                                "storagePath" to storagePath,
                                                "downloadUrl" to downloadUrl,
                                                "createdAt" to now,
                                                "uploadedAt" to now,
                                                "status" to "UPLOADED",
                                                "fileName" to fileName,
                                                "contentType" to mime,
                                                "fileSize" to bytes.size.toLong()
                                            )

                                            val metaTask = firestore.collection("visits")
                                                .document(safeVisitId)
                                                .collection("photos")
                                                .document(photoId)
                                                .set(photoMeta)
                                            Tasks.await(metaTask)
                                        } catch (metaErr: Exception) {
                                            Log.w("MediaStorageHelper", "Error saving photo metadata in Firestore: ${metaErr.message}")
                                        }
                                    }

                                    Log.d("MediaStorageHelper", "Successfully uploaded $fileName to Cloudinary: $downloadUrl")
                                    if (!updatedUris.contains(downloadUrl)) {
                                        updatedUris.add(downloadUrl)
                                    }

                                    // Immediately update Room DB to store the new remote URL so any retry or subsequent read uses it
                                    try {
                                        if (dbVisit != null) {
                                            val tempMap = updatedMap.toMutableMap()
                                            tempMap[categoryId] = updatedUris
                                            val tempJson = photosAdapter.toJson(tempMap)
                                            db.visitDao().updateVisit(dbVisit.copy(photosJson = tempJson))
                                        }
                                    } catch (_: Exception) {}
                                } else {
                                    Log.e("MediaStorageHelper", "Cloudinary upload failed for $uriStr, keeping local reference")
                                    if (!updatedUris.contains(uriStr)) {
                                        updatedUris.add(uriStr)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MediaStorageHelper", "Cloudinary upload failed for $uriStr, keeping local reference: ${e.message}", e)
                                if (!updatedUris.contains(uriStr)) {
                                    updatedUris.add(uriStr)
                                }
                            }
                        } else {
                            Log.e("MediaStorageHelper", "Failed to read bytes for media URI: $uriStr")
                            if (!updatedUris.contains(uriStr)) {
                                updatedUris.add(uriStr)
                            }
                        }
                        processedItems++
                        onProgress?.invoke(processedItems, totalItems)
                    }
                }
                updatedMap[categoryId] = updatedUris
            }

            photosAdapter.toJson(updatedMap)
        } catch (e: Exception) {
            Log.e("MediaStorageHelper", "Error processing photosJson for Cloudinary upload", e)
            photosJson
        }
    }

    /**
     * Backward-compatible overload.
     */
    suspend fun uploadPhotosJsonToFirebaseStorage(
        context: Context,
        schoolId: String,
        visitId: String,
        photosJson: String
    ): String {
        return uploadPhotosJsonToFirebaseStorage(
            context = context,
            visitId = visitId,
            schoolId = schoolId,
            employeeId = "",
            photosJson = photosJson
        )
    }

    /**
     * Deletes photo metadata from Firestore and permanently destroys the asset in Cloudinary via Vercel backend.
     */
    suspend fun deletePhotoFromFirebaseStorage(
        visitId: String,
        photoUrlOrPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseUtils.firestore ?: return@withContext false

            if (photoUrlOrPath.startsWith("http://") || photoUrlOrPath.startsWith("https://") || photoUrlOrPath.startsWith("gs://")) {
                // Find photo metadata doc in Firestore, call deleteAsset on Vercel backend, and delete metadata doc
                if (visitId.isNotBlank()) {
                    try {
                        val photosCol = firestore.collection("visits").document(visitId).collection("photos")
                        val queryTask = photosCol.whereEqualTo("downloadUrl", photoUrlOrPath).get()
                        val snap = Tasks.await(queryTask)
                        for (doc in snap.documents) {
                            val publicId = doc.getString("publicId")
                            val resourceType = doc.getString("resourceType") ?: if (isMediaVideo(photoUrlOrPath)) "video" else "image"
                            
                            if (!publicId.isNullOrBlank()) {
                                try {
                                    val isVideo = resourceType.equals("video", ignoreCase = true)
                                    val deleted = CloudinaryUploader.deleteAsset(visitId, publicId, isVideo)
                                    if (!deleted) {
                                        Log.w("MediaStorageHelper", "Cloudinary asset delete notice for publicId: $publicId")
                                    }
                                } catch (delErr: Exception) {
                                    Log.w("MediaStorageHelper", "Cloudinary deleteAsset warning: ${delErr.message}")
                                }
                            }
                            Tasks.await(doc.reference.delete())
                        }
                    } catch (e: Exception) {
                        Log.w("MediaStorageHelper", "Failed to delete photo metadata document: ${e.message}")
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MediaStorageHelper", "Error deleting photo metadata: ${e.message}", e)
            false
        }
    }

    /**
     * Checks if 100% of media entries in photosJson are uploaded remote Cloudinary/HTTP URLs.
     * Returns false if any item remains a local URI (file://, content://, or private path).
     */
    fun isAllMediaUploaded(photosJson: String): Boolean {
        if (photosJson.isBlank() || photosJson == "{}") return true
        return try {
            val map = photosAdapter.fromJson(photosJson) ?: return true
            for ((_, uriList) in map) {
                for (uriStr in uriList) {
                    if (uriStr.isBlank()) continue
                    val isRemote = uriStr.startsWith("http://") || 
                                   uriStr.startsWith("https://") || 
                                   uriStr.startsWith("gs://")
                    if (!isRemote) return false
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readMediaBytes(context: Context, uriStr: String): ByteArray? {
        return try {
            val uri = Uri.parse(uriStr)
            // 1. Try opening via contentResolver (works for content:// and file:// URIs)
            val bytesFromResolver = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytesFromResolver != null && bytesFromResolver.isNotEmpty()) {
                return bytesFromResolver
            }

            // 2. Direct File path reading
            val path = uri.path ?: uriStr.removePrefix("file://").removePrefix("file:")
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                file.readBytes()
            } else {
                val cleanPath = uriStr.removePrefix("file://").removePrefix("file:")
                val directFile = File(cleanPath)
                if (directFile.exists() && directFile.length() > 0) directFile.readBytes() else null
            }
        } catch (e: Exception) {
            Log.e("MediaStorageHelper", "Error reading media bytes for $uriStr", e)
            try {
                val cleanPath = uriStr.removePrefix("file://").removePrefix("file:")
                val fallbackFile = File(cleanPath)
                if (fallbackFile.exists() && fallbackFile.length() > 0) fallbackFile.readBytes() else null
            } catch (e2: Exception) {
                Log.e("MediaStorageHelper", "Fallback readMediaBytes failed for $uriStr", e2)
                null
            }
        }
    }

    /**
     * Checks if a given media path or URL represents a video.
     */
    fun isMediaVideo(pathOrUrl: String, context: Context? = null): Boolean {
        val lower = pathOrUrl.lowercase()
        if (lower.contains("vid_") ||
            lower.contains(".mp4") ||
            lower.contains(".mov") ||
            lower.contains(".3gp") ||
            lower.contains(".mkv") ||
            lower.contains(".webm") ||
            lower.contains(".avi")
        ) {
            return true
        }

        if (context != null && (pathOrUrl.startsWith("content://") || pathOrUrl.startsWith("file://"))) {
            try {
                val mime = context.contentResolver.getType(Uri.parse(pathOrUrl))
                if (mime?.startsWith("video/") == true) {
                    return true
                }
            } catch (_: Exception) {}
        }

        return false
    }

    private fun isMimeOrUriVideo(mimeType: String?, uriString: String): Boolean {
        if (mimeType?.startsWith("video/") == true) return true
        val lower = uriString.lowercase()
        return lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".3gp") || lower.contains(".mkv") || lower.contains(".webm")
    }

    /**
     * Generates structured Storage folder path:
     * visits / visitId / schoolId / category
     */
    fun getStorageFolderPath(
        visitId: String,
        schoolId: String,
        category: String
    ): String {
        return "visits/$visitId/$schoolId/$category"
    }

    /**
     * Generates standardized filename for each photo category:
     * 1. School Photo -> School_Photo.jpg
     * 2. Explaining Our App -> Explaining_App.jpg
     * 3. Students Using Smart Board -> Smart_Board.jpg
     * 4. Photo With Principal Sir -> Principal.jpg
     * 5. Letter Photo -> Letter.jpg
     * 6. Other Photos -> Other_01.jpg, Other_02.jpg...
     */
    fun getStandardizedFileName(categoryId: String, index: Int, extension: String = "jpg"): String {
        val ext = extension.trimStart('.')
        return when (categoryId.lowercase().trim()) {
            "school_photo" -> "01_School_Photo.$ext"
            "explaining_app" -> "02_Explaining_App.$ext"
            "students_smart_board", "smart_board" -> "03_Smart_Board.$ext"
            "principal_photo", "principal" -> "04_Principal.$ext"
            "letter_photo", "letter" -> "05_Letter.$ext"
            else -> "06_Other_Media_${index + 1}.$ext"
        }
    }

    /**
     * Launches a viewer or system player for photo/video.
     */
    fun openMedia(context: Context, pathOrUrl: String) {
        try {
            val isVideo = isMediaVideo(pathOrUrl, context)
            val uri = when {
                pathOrUrl.startsWith("file://") -> {
                    val file = File(Uri.parse(pathOrUrl).path ?: "")
                    if (file.exists()) {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    } else {
                        Uri.parse(pathOrUrl)
                    }
                }
                pathOrUrl.startsWith("/") -> {
                    val file = File(pathOrUrl)
                    if (file.exists()) {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    } else {
                        Uri.parse(pathOrUrl)
                    }
                }
                else -> Uri.parse(pathOrUrl)
            }

            val mime = if (isVideo) "video/*" else "image/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

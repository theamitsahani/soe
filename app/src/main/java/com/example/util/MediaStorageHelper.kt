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
import com.google.firebase.storage.StorageMetadata
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
     * Uploads all photos/videos in photosJson to Google Drive via Firebase Cloud Function
     * and returns updated photosJson containing the permanent Google Drive direct URLs.
     */
    suspend fun uploadPhotosJsonToGoogleDrive(
        context: Context,
        schoolName: String,
        udiseCode: String = "",
        state: String = "Rajasthan",
        district: String = "",
        block: String = "",
        visitDate: String = "",
        photosJson: String
    ): String = withContext(Dispatchers.IO) {
        if (photosJson.isBlank() || photosJson == "{}") return@withContext photosJson

        try {
            val originalMap = photosAdapter.fromJson(photosJson) ?: return@withContext photosJson
            val updatedMap = mutableMapOf<String, List<String>>()
            val functions = FirebaseUtils.functions

            for ((categoryId, uriList) in originalMap) {
                val updatedUris = mutableListOf<String>()
                for ((index, uriStr) in uriList.withIndex()) {
                    if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                        // Already uploaded to Google Drive or remote URL
                        updatedUris.add(uriStr)
                    } else {
                        val bytes = readMediaBytes(context, uriStr)
                        if (bytes != null && bytes.isNotEmpty() && functions != null) {
                            try {
                                val isVideo = isMediaVideo(uriStr, context)
                                val mime = if (isVideo) "video/mp4" else "image/jpeg"
                                val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                val ext = if (isVideo) "mp4" else "jpg"
                                val fileName = getStandardizedFileName(categoryId, index, ext)

                                val year = if (visitDate.isNotBlank()) {
                                    Regex("""\b(20\d\d)\b""").find(visitDate)?.value ?: ""
                                } else ""

                                val payload = hashMapOf(
                                    "base64Data" to base64Data,
                                    "mimeType" to mime,
                                    "fileName" to fileName,
                                    "categoryId" to categoryId,
                                    "schoolName" to schoolName,
                                    "udiseCode" to udiseCode,
                                    "state" to state.ifBlank { "Rajasthan" },
                                    "district" to district,
                                    "block" to block,
                                    "visitDate" to visitDate,
                                    "year" to year,
                                    "index" to (index + 1)
                                )

                                val callable = functions.getHttpsCallable("uploadPhotoToDrive").apply {
                                    setTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                }
                                val task = callable.call(payload)
                                val result = Tasks.await(task)
                                val dataMap = result.data as? Map<*, *>
                                val driveUrl = (dataMap?.get("url") as? String)
                                    ?: (dataMap?.get("directUrl") as? String)
                                    ?: (dataMap?.get("webViewLink") as? String)

                                if (!driveUrl.isNullOrBlank()) {
                                    Log.d("MediaStorageHelper", "Successfully uploaded $fileName to Drive: $driveUrl")
                                    updatedUris.add(driveUrl)
                                } else {
                                    Log.w("MediaStorageHelper", "Drive returned blank URL for $fileName, response: $dataMap")
                                    updatedUris.add(uriStr)
                                }
                            } catch (e: Exception) {
                                Log.e("MediaStorageHelper", "Drive upload failed for $uriStr, keeping local reference: ${e.message}", e)
                                updatedUris.add(uriStr)
                            }
                        } else {
                            if (functions == null) Log.e("MediaStorageHelper", "FirebaseFunctions instance is null")
                            if (bytes == null || bytes.isEmpty()) Log.e("MediaStorageHelper", "Failed to read bytes for media URI: $uriStr")
                            updatedUris.add(uriStr)
                        }
                    }
                }
                updatedMap[categoryId] = updatedUris
            }

            photosAdapter.toJson(updatedMap)
        } catch (e: Exception) {
            Log.e("MediaStorageHelper", "Error processing photosJson for Google Drive upload", e)
            photosJson
        }
    }

    /**
     * Backward-compatible delegation to Google Drive upload.
     */
    suspend fun uploadPhotosJsonToFirebaseStorage(
        context: Context,
        schoolId: String,
        visitId: String,
        photosJson: String
    ): String {
        return uploadPhotosJsonToGoogleDrive(
            context = context,
            schoolName = schoolId,
            udiseCode = "",
            state = "Rajasthan",
            district = "",
            block = "",
            visitDate = "",
            photosJson = photosJson
        )
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
            val path = uri.path ?: uriStr.removePrefix("file://")
            val file = File(path)
            if (file.exists()) {
                file.readBytes()
            } else {
                val directFile = File(uriStr)
                if (directFile.exists()) directFile.readBytes() else null
            }
        } catch (e: Exception) {
            Log.e("MediaStorageHelper", "Error reading media bytes for $uriStr", e)
            try {
                val cleanPath = uriStr.removePrefix("file://")
                val fallbackFile = File(cleanPath)
                if (fallbackFile.exists()) fallbackFile.readBytes() else null
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
     * Generates structured Google Drive folder path:
     * SOE VISIT / State / District / Block / School Name / Visit Date /
     */
    fun getDriveFolderPath(
        state: String,
        district: String,
        block: String,
        schoolName: String,
        visitDate: String
    ): String {
        fun sanitize(s: String) = s.replace("/", "-").replace("\\", "-").trim().ifBlank { "Unknown" }
        val sState = sanitize(state.ifBlank { "Rajasthan" })
        val sDistrict = sanitize(district)
        val sBlock = sanitize(block)
        val sSchool = sanitize(schoolName)
        val sDate = sanitize(visitDate)

        return "SOE VISIT/$sState/$sDistrict/$sBlock/$sSchool/$sDate"
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

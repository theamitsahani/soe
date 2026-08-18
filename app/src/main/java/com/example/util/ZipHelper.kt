package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.PhotoCategory
import com.example.data.model.Visit
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipHelper {

    /**
     * Packages selected visits/schools into a single ZIP file organized school-wise with real image downloads.
     */
    suspend fun createSchoolWisePhotosZip(
        context: Context,
        visits: List<Visit>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "School_Photos_${System.currentTimeMillis()}.zip")
        val zos = ZipOutputStream(FileOutputStream(zipFile))

        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
        val adapter = moshi.adapter<Map<String, List<String>>>(type)

        var totalPhotos = 0
        var currentPhoto = 0

        // Count total photos
        for (v in visits) {
            val photoMap: Map<String, List<String>> = try {
                if (v.photosJson.isNotBlank()) adapter.fromJson(v.photosJson) ?: emptyMap() else emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
            totalPhotos += photoMap.values.sumOf { it.size }
        }

        try {
            // BUG FIX: entry paths were built purely from schoolName + category + index. Two
            // visits to the same school (a re-visit) — or two different schools that happen to
            // share a name — produced IDENTICAL zip entry paths. The second visit's photo would
            // then fail ZipOutputStream.putNextEntry() with a duplicate-entry exception, which
            // was silently caught and logged, so that visit's photos just vanished from the
            // export with no indication to the admin. Disambiguate the school folder with the
            // visit date (falling back to visitId) whenever more than one visit maps to the
            // same sanitized school name.
            val schoolNameCounts = visits.groupingBy {
                it.schoolName.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "School_${it.schoolId}" }
            }.eachCount()

            for (v in visits) {
                val baseSchoolName = v.schoolName.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "School_${v.schoolId}" }
                val sanitizeSchoolName = if ((schoolNameCounts[baseSchoolName] ?: 0) > 1) {
                    val disambiguator = v.visitDate.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { v.visitId }
                    "${baseSchoolName}_$disambiguator"
                } else {
                    baseSchoolName
                }
                val photoMap: Map<String, List<String>> = try {
                    if (v.photosJson.isNotBlank()) adapter.fromJson(v.photosJson) ?: emptyMap() else emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }

                PhotoCategory.entries.forEach { category ->
                    val categoryDirName = category.displayName.replace(Regex("""[\\/:*?"<>|]"""), "_")
                    val urls = photoMap[category.categoryId] ?: emptyList()

                    urls.forEachIndexed { index, url ->
                        currentPhoto++
                        withContext(Dispatchers.Main) {
                            onProgress(currentPhoto, totalPhotos)
                        }

                        val ext = getExtensionFromUrl(url)
                        val fileName = "${category.categoryId}_${index + 1}.$ext"
                        val entryPath = "$sanitizeSchoolName/$categoryDirName/$fileName"

                        try {
                            val bytes = readMediaBytes(context, url)
                            if (bytes != null && bytes.isNotEmpty()) {
                                zos.putNextEntry(ZipEntry(entryPath))
                                zos.write(bytes)
                                zos.closeEntry()
                            }
                        } catch (e: Exception) {
                            Log.e("ZipHelper", "Failed to package photo: $url", e)
                        }
                    }
                }
            }
        } finally {
            zos.flush()
            zos.close()
        }

        zipFile
    }

    private fun readMediaBytes(context: Context, urlOrPath: String): ByteArray? {
        return try {
            when {
                urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://") -> {
                    val conn = URL(urlOrPath).openConnection() as HttpURLConnection
                    conn.connectTimeout = 12000
                    conn.readTimeout = 15000
                    conn.instanceFollowRedirects = true
                    conn.inputStream.use { it.readBytes() }
                }
                urlOrPath.startsWith("content://") -> {
                    val uri = Uri.parse(urlOrPath)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                urlOrPath.startsWith("file://") -> {
                    val file = File(Uri.parse(urlOrPath).path ?: "")
                    if (file.exists()) file.readBytes() else null
                }
                else -> {
                    val file = File(urlOrPath)
                    if (file.exists()) file.readBytes() else null
                }
            }
        } catch (e: Exception) {
            Log.e("ZipHelper", "Error reading media bytes for $urlOrPath", e)
            null
        }
    }

    private fun getExtensionFromUrl(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".png") -> "png"
            lower.contains(".webp") -> "webp"
            lower.contains(".mp4") -> "mp4"
            lower.contains(".mov") -> "mov"
            lower.contains(".3gp") -> "3gp"
            else -> "jpg"
        }
    }
}

package com.example.util

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Simple unsigned upload to Cloudinary using OkHttp (already a project dependency).
 * No Firebase Blaze billing needed — Cloudinary free tier works with just an account.
 *
 * SETUP (do this once):
 * 1. Sign up free at https://cloudinary.com (no card needed)
 * 2. Dashboard home shows your "Cloud name" at the top -> paste it in CLOUD_NAME below
 * 3. Go to Settings (gear icon) -> Upload -> Scroll to "Upload presets" -> Add upload preset
 *    -> Set "Signing Mode" to "Unsigned" -> Save -> copy the preset name into UPLOAD_PRESET below
 */
object CloudinaryUploader {

    // TODO: replace these two with your own Cloudinary values
    private const val CLOUD_NAME = "your_cloud_name"
    private const val UPLOAD_PRESET = "your_unsigned_preset"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Uploads raw bytes to Cloudinary and returns the permanent secure_url, or null on failure.
     * folder + publicId together control where it lands, mirroring the old Storage path structure.
     */
    fun uploadBytes(bytes: ByteArray, folder: String, publicId: String, isVideo: Boolean): String? {
        return try {
            val resourceType = if (isVideo) "video" else "image"
            val url = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$resourceType/upload"

            val mediaType = (if (isVideo) "video/mp4" else "image/jpeg").toMediaTypeOrNull()
            val fileBody = bytes.toRequestBody(mediaType)
            val fileName = "$publicId.${if (isVideo) "mp4" else "jpg"}"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("folder", folder)
                .addFormDataPart("public_id", publicId)
                .build()

            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                JSONObject(body).optString("secure_url").ifBlank { null }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

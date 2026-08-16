package com.example.util

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CloudinaryUploadResult(
    val downloadUrl: String,
    val publicId: String,
    val folder: String,
    val resourceType: String
)

object CloudinaryUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun uploadBytes(
        bytes: ByteArray,
        visitId: String,
        schoolId: String,
        categoryId: String,
        isVideo: Boolean
    ): CloudinaryUploadResult? {
        return try {
            val resourceType = if (isVideo) "video" else "image"
            val functions = FirebaseFunctions.getInstance()
            val sigTask = functions.getHttpsCallable("getCloudinarySignature")
                .call(
                    mapOf(
                        "visitId" to visitId,
                        "schoolId" to schoolId,
                        "category" to categoryId
                    )
                )
            val sigResult = Tasks.await(sigTask)
            val data = sigResult.data as? Map<*, *> ?: return null

            val cloudName = data["cloudName"]?.toString() ?: return null
            val apiKey = data["apiKey"]?.toString() ?: return null
            val timestamp = data["timestamp"]?.toString() ?: return null
            val folder = data["folder"]?.toString() ?: return null
            val publicId = data["publicId"]?.toString() ?: return null
            val signature = data["signature"]?.toString() ?: return null

            val url = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"
            val mediaType = (if (isVideo) "video/mp4" else "image/jpeg").toMediaTypeOrNull()
            val fileBody = bytes.toRequestBody(mediaType)
            val fileName = "$publicId.${if (isVideo) "mp4" else "jpg"}"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .addFormDataPart("folder", folder)
                .addFormDataPart("public_id", publicId)
                .build()

            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("CloudinaryUploader", "Upload failed with HTTP ${response.code}: $errBody")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val secureUrl = json.optString("secure_url").ifBlank { null } ?: return null
                val retPublicId = json.optString("public_id").ifBlank { publicId }
                CloudinaryUploadResult(
                    downloadUrl = secureUrl,
                    publicId = retPublicId,
                    folder = folder,
                    resourceType = resourceType
                )
            }
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Error uploading media with signed Cloudinary signature", e)
            null
        }
    }
}


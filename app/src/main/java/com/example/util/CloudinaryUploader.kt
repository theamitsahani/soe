package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
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

data class CloudinarySignatureResponse(
    val cloudName: String,
    val apiKey: String,
    val timestamp: String,
    val folder: String,
    val publicId: String,
    val signature: String
)

object CloudinaryUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS) // 5 minutes write timeout for videos
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    private fun getFirebaseIdToken(): String? {
        return try {
            val user = FirebaseUtils.auth?.currentUser ?: FirebaseAuth.getInstance().currentUser ?: return null
            val tokenTask = user.getIdToken(false)
            val tokenResult = Tasks.await(tokenTask)
            tokenResult.token
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Failed to retrieve Firebase ID token: ${e.message}")
            null
        }
    }

    /**
     * Obtains signature from Vercel backend and uploads media directly to Cloudinary using signed multipart POST.
     * Retries up to 3 times on transient network failures.
     */
    fun uploadBytes(
        bytes: ByteArray,
        visitId: String,
        schoolId: String,
        categoryId: String,
        isVideo: Boolean
    ): CloudinaryUploadResult? {
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts) {
            attempts++
            val result = tryUploadBytes(bytes, visitId, schoolId, categoryId, isVideo)
            if (result != null) {
                return result
            }
            if (attempts < maxAttempts) {
                Log.w("CloudinaryUploader", "Upload attempt $attempts failed, retrying in ${attempts * 1500}ms...")
                try { Thread.sleep(attempts * 1500L) } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun tryUploadBytes(
        bytes: ByteArray,
        visitId: String,
        schoolId: String,
        categoryId: String,
        isVideo: Boolean
    ): CloudinaryUploadResult? {
        return try {
            val idToken = getFirebaseIdToken()
            if (idToken.isNullOrBlank()) {
                Log.e("CloudinaryUploader", "Cannot request upload signature: User is not authenticated with Firebase")
                return null
            }

            val resourceType = if (isVideo) "video" else "image"
            val baseUrl = BuildConfig.VERCEL_API_BASE_URL.trimEnd('/')

            // Step 1: Request upload signature from Vercel backend
            val sigRequestBody = JSONObject().apply {
                put("visitId", visitId)
                put("schoolId", schoolId)
                put("category", categoryId)
            }.toString().toRequestBody(jsonMediaType)

            val sigRequest = Request.Builder()
                .url("$baseUrl/api/get-signature")
                .addHeader("Authorization", "Bearer $idToken")
                .post(sigRequestBody)
                .build()

            val sigData = client.newCall(sigRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("CloudinaryUploader", "get-signature failed with HTTP ${response.code}: $errBody")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val cName = json.optString("cloudName").ifBlank { return null }
                val aKey = json.optString("apiKey").ifBlank { return null }
                val tStamp = json.optString("timestamp").ifBlank { return null }
                val fld = json.optString("folder").ifBlank { return null }
                val pId = json.optString("publicId").ifBlank { return null }
                val sig = json.optString("signature").ifBlank { return null }
                CloudinarySignatureResponse(
                    cloudName = cName,
                    apiKey = aKey,
                    timestamp = tStamp,
                    folder = fld,
                    publicId = pId,
                    signature = sig
                )
            } ?: return null

            // Step 2: Upload file bytes directly to Cloudinary signed endpoint
            val uploadUrl = "https://api.cloudinary.com/v1_1/${sigData.cloudName}/$resourceType/upload"
            val mediaType = (if (isVideo) "video/mp4" else "image/jpeg").toMediaTypeOrNull()
            val fileBody = bytes.toRequestBody(mediaType)
            val fileName = "${sigData.publicId}.${if (isVideo) "mp4" else "jpg"}"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("api_key", sigData.apiKey)
                .addFormDataPart("timestamp", sigData.timestamp)
                .addFormDataPart("signature", sigData.signature)
                .addFormDataPart("folder", sigData.folder)
                .addFormDataPart("public_id", sigData.publicId)
                .build()

            val uploadRequest = Request.Builder().url(uploadUrl).post(requestBody).build()

            client.newCall(uploadRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("CloudinaryUploader", "Cloudinary upload failed with HTTP ${response.code}: $errBody")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val secureUrl = json.optString("secure_url").ifBlank { null } ?: return null
                val retPublicId = json.optString("public_id").ifBlank { sigData.publicId }
                CloudinaryUploadResult(
                    downloadUrl = secureUrl,
                    publicId = retPublicId,
                    folder = sigData.folder,
                    resourceType = resourceType
                )
            }
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Error uploading media via Vercel-signed Cloudinary flow", e)
            null
        }
    }

    /**
     * Deletes an asset permanently in Cloudinary via the Vercel backend endpoint.
     */
    fun deleteAsset(
        visitId: String,
        publicId: String,
        isVideo: Boolean
    ): Boolean {
        return try {
            val idToken = getFirebaseIdToken()
            if (idToken.isNullOrBlank()) {
                Log.e("CloudinaryUploader", "Cannot request asset deletion: User is not authenticated with Firebase")
                return false
            }

            val resourceType = if (isVideo) "video" else "image"
            val baseUrl = BuildConfig.VERCEL_API_BASE_URL.trimEnd('/')

            val deleteRequestBody = JSONObject().apply {
                put("visitId", visitId)
                put("publicId", publicId)
                put("resourceType", resourceType)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$baseUrl/api/delete-asset")
                .addHeader("Authorization", "Bearer $idToken")
                .post(deleteRequestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.w("CloudinaryUploader", "delete-asset failed with HTTP ${response.code}: $errBody")
                    false
                } else {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    json.optBoolean("success", true)
                }
            }
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Error deleting asset via Vercel backend", e)
            false
        }
    }
}


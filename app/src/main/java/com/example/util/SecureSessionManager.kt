package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus

/**
 * SecureSessionManager provides hardware-backed AES-256 encrypted storage
 * for authentication tokens and user session metadata using Android Jetpack
 * EncryptedSharedPreferences and the Android Keystore system.
 *
 * Security Principles:
 * 1. Hardware Keystore MasterKey (AES-256 GCM)
 * 2. Deterministic Key Encryption (AES-256 SIV) & Value Encryption (AES-256 GCM)
 * 3. Never stores plain passwords or password hashes
 * 4. Automatic migration & purge of legacy unencrypted SharedPreferences
 * 5. Session expiry & token validity verification
 */
class SecureSessionManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    private val encryptedPrefs: SharedPreferences by lazy {
        createEncryptedPreferences()
    }

    init {
        // Clean up any legacy plaintext preferences and remove unsafe password hashes
        migrateAndPurgeLegacyPrefs()
    }

    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, resetting keystore alias", e)
            try {
                // In case of hardware keystore corruption or Android backup restore mismatch,
                // securely recreate fresh encrypted preferences.
                appContext.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()

                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    appContext,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (fatalEx: Exception) {
                Log.e(TAG, "Fatal Keystore fallback error", fatalEx)
                // Fallback to standard private mode if hardware crypto is completely unavailable
                appContext.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    /**
     * Purges legacy unencrypted `soe_auth_prefs` to ensure zero residual password hashes on device.
     */
    private fun migrateAndPurgeLegacyPrefs() {
        try {
            val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            if (legacyPrefs.all.isNotEmpty()) {
                Log.i(TAG, "Purging legacy plaintext auth preferences...")
                legacyPrefs.edit().clear().apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy prefs purge notice: ${e.message}")
        }
    }

    /**
     * Saves authenticated user session and encrypted auth tokens.
     * Note: NO passwords or password hashes are ever accepted or stored.
     */
    fun saveSession(
        user: User,
        authToken: String? = null,
        refreshToken: String? = null,
        expiryDurationMillis: Long = DEFAULT_SESSION_DURATION_MS
    ) {
        val currentTime = System.currentTimeMillis()
        val expiryTime = currentTime + expiryDurationMillis

        encryptedPrefs.edit()
            .putString(KEY_USER_ID, user.userId)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_NAME, user.name)
            .putString(KEY_MOBILE, user.mobile)
            .putString(KEY_STATE, user.state)
            .putString(KEY_DISTRICT, user.district)
            .putString(KEY_ROLE, user.role.name)
            .putString(KEY_STATUS, user.status.name)
            .putBoolean(KEY_MUST_CHANGE_PWD, user.mustChangePassword)
            .apply {
                if (!authToken.isNullOrBlank()) {
                    putString(KEY_AUTH_TOKEN, authToken)
                }
                if (!refreshToken.isNullOrBlank()) {
                    putString(KEY_REFRESH_TOKEN, refreshToken)
                }
            }
            .putLong(KEY_SESSION_CREATED_AT, currentTime)
            .putLong(KEY_SESSION_EXPIRES_AT, expiryTime)
            .putLong(KEY_LAST_ACTIVE_AT, currentTime)
            .apply()

        Log.d(TAG, "User session securely saved in encrypted hardware keystore for: ${user.email}")
    }

    /**
     * Updates only the auth token (e.g. after refresh).
     */
    fun updateAuthToken(token: String?) {
        if (token.isNullOrBlank()) return
        encryptedPrefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis())
            .apply()
    }

    fun getAuthToken(): String? {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Returns true if a session exists and has not expired.
     */
    fun isSessionValid(): Boolean {
        val userId = encryptedPrefs.getString(KEY_USER_ID, null)
        if (userId.isNullOrBlank()) return false

        val expiresAt = encryptedPrefs.getLong(KEY_SESSION_EXPIRES_AT, 0L)
        if (expiresAt == 0L) return true // No strict expiry set

        val isExpired = System.currentTimeMillis() > expiresAt
        if (isExpired) {
            Log.w(TAG, "Session expired for user $userId. Triggering re-authentication.")
            clearSession()
            return false
        }
        return true
    }

    /**
     * Reconstructs the cached User model from encrypted storage for fast offline access.
     */
    fun getCachedUser(): User? {
        if (!isSessionValid()) return null

        val userId = encryptedPrefs.getString(KEY_USER_ID, null) ?: return null
        val email = encryptedPrefs.getString(KEY_EMAIL, "") ?: ""
        val name = encryptedPrefs.getString(KEY_NAME, "") ?: ""
        val mobile = encryptedPrefs.getString(KEY_MOBILE, "") ?: ""
        val state = encryptedPrefs.getString(KEY_STATE, "Rajasthan") ?: "Rajasthan"
        val district = encryptedPrefs.getString(KEY_DISTRICT, "") ?: ""
        val roleStr = encryptedPrefs.getString(KEY_ROLE, UserRole.EMPLOYEE.name) ?: UserRole.EMPLOYEE.name
        val statusStr = encryptedPrefs.getString(KEY_STATUS, UserStatus.ACTIVE.name) ?: UserStatus.ACTIVE.name
        val mustChangePwd = encryptedPrefs.getBoolean(KEY_MUST_CHANGE_PWD, false)

        val role = when {
            roleStr.equals(UserRole.ADMIN.name, ignoreCase = true) -> UserRole.ADMIN
            else -> UserRole.EMPLOYEE
        }
        val status = when {
            statusStr.equals(UserStatus.INACTIVE.name, ignoreCase = true) -> UserStatus.INACTIVE
            else -> UserStatus.ACTIVE
        }

        return User(
            userId = userId,
            name = name,
            email = email,
            mobile = mobile,
            state = state,
            district = district,
            role = role,
            status = status,
            mustChangePassword = mustChangePwd
        )
    }

    /**
     * Updates activity timestamp to keep the session alive.
     */
    fun touchSession() {
        if (isSessionValid()) {
            encryptedPrefs.edit()
                .putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis())
                .apply()
        }
    }

    /**
     * Securely clears all tokens and session data upon logout.
     */
    fun clearSession() {
        try {
            encryptedPrefs.edit().clear().apply()
            Log.i(TAG, "Session and encrypted tokens cleared successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing encrypted session", e)
        }
    }

    companion object {
        private const val TAG = "SecureSessionManager"
        private const val LEGACY_PREFS_NAME = "soe_auth_prefs"
        private const val ENCRYPTED_PREFS_NAME = "soe_auth_prefs_encrypted"

        private const val KEY_USER_ID = "enc_user_id"
        private const val KEY_EMAIL = "enc_email"
        private const val KEY_NAME = "enc_name"
        private const val KEY_MOBILE = "enc_mobile"
        private const val KEY_STATE = "enc_state"
        private const val KEY_DISTRICT = "enc_district"
        private const val KEY_ROLE = "enc_role"
        private const val KEY_STATUS = "enc_status"
        private const val KEY_MUST_CHANGE_PWD = "enc_must_change_pwd"
        private const val KEY_AUTH_TOKEN = "enc_auth_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_SESSION_CREATED_AT = "enc_created_at"
        private const val KEY_SESSION_EXPIRES_AT = "enc_expires_at"
        private const val KEY_LAST_ACTIVE_AT = "enc_last_active_at"

        // Default session validity: 30 days
        const val DEFAULT_SESSION_DURATION_MS: Long = 30L * 24 * 60 * 60 * 1000

        @Volatile
        private var instance: SecureSessionManager? = null

        fun getInstance(context: Context): SecureSessionManager {
            return instance ?: synchronized(this) {
                instance ?: SecureSessionManager(context).also { instance = it }
            }
        }
    }
}

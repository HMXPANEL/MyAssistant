package com.gitsync.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var githubUsername: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var githubToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var defaultBranch: String
        get() = prefs.getString(KEY_BRANCH, "main") ?: "main"
        set(value) = prefs.edit().putString(KEY_BRANCH, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isAuthenticated(): Boolean {
        return githubUsername.isNotBlank() && githubToken.isNotBlank()
    }

    companion object {
        private const val PREFS_NAME = "gitsync_secure_prefs"
        private const val KEY_USERNAME = "github_username"
        private const val KEY_TOKEN = "github_token"
        private const val KEY_BRANCH = "default_branch"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
    }
}

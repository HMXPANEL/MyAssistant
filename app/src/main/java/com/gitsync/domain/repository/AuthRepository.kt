package com.gitsync.domain.repository

interface AuthRepository {
    suspend fun validateCredentials(username: String, token: String): Result<Boolean>
    fun isAuthenticated(): Boolean
    fun isSetupComplete(): Boolean
    fun getUsername(): String
    fun getToken(): String
    fun getDefaultBranch(): String
    suspend fun saveCredentials(username: String, token: String, branch: String)
    suspend fun updateBranch(branch: String)
    fun clearCredentials()
}

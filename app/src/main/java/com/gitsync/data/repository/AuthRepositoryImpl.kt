package com.gitsync.data.repository

import com.gitsync.core.network.GitHubApi
import com.gitsync.core.security.SecureStorage
import com.gitsync.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    private val gitHubApi: GitHubApi
) : AuthRepository {

    override suspend fun validateCredentials(username: String, token: String): Result<Boolean> {
        return try {
            // Temporarily write the token so AuthInterceptor can attach it
            // to the validation request. Roll it back on failure.
            val previousToken = secureStorage.githubToken
            secureStorage.githubToken = token
            val result = try {
                val user = gitHubApi.getUser()
                if (user.login.equals(username, ignoreCase = true)) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Username does not match token owner. Expected: $username, got: ${user.login}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            // If validation failed, restore the old token (or clear it)
            if (result.isFailure) {
                secureStorage.githubToken = previousToken
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isAuthenticated(): Boolean = secureStorage.isAuthenticated()

    override fun isSetupComplete(): Boolean = secureStorage.isSetupComplete

    override fun getUsername(): String = secureStorage.githubUsername

    override fun getToken(): String = secureStorage.githubToken

    override fun getDefaultBranch(): String = secureStorage.defaultBranch

    override suspend fun saveCredentials(username: String, token: String, branch: String) {
        secureStorage.githubUsername = username
        secureStorage.githubToken = token
        secureStorage.defaultBranch = branch
        secureStorage.isSetupComplete = true
    }

    override suspend fun updateBranch(branch: String) {
        secureStorage.defaultBranch = branch
    }

    override fun clearCredentials() {
        secureStorage.clear()
    }
}

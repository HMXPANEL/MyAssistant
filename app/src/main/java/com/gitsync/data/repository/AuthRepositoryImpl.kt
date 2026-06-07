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
            val user = gitHubApi.getUser()
            if (user.login.equals(username, ignoreCase = true)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Username does not match token owner"))
            }
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

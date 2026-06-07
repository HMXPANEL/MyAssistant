package com.gitsync

import com.gitsync.core.network.GitHubApi
import com.gitsync.core.security.SecureStorage
import com.gitsync.data.remote.dto.GitHubUserDto
import com.gitsync.data.repository.AuthRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var secureStorage: SecureStorage
    private lateinit var gitHubApi: GitHubApi
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        secureStorage = mockk(relaxed = true)
        gitHubApi = mockk()
        repository = AuthRepositoryImpl(secureStorage, gitHubApi)
    }

    @Test
    fun `validate credentials returns success when username matches`() = runTest {
        val username = "testuser"
        val token = "test_token_12345"

        coEvery { gitHubApi.getUser() } returns GitHubUserDto(
            login = "testuser",
            id = 12345L,
            avatarUrl = "https://avatars.githubusercontent.com/u/12345",
            name = "Test User",
            email = "test@example.com",
            publicRepos = 10
        )

        val result = repository.validateCredentials(username, token)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `validate credentials returns failure when username does not match`() = runTest {
        val username = "otheruser"
        val token = "test_token_12345"

        coEvery { gitHubApi.getUser() } returns GitHubUserDto(
            login = "testuser",
            id = 12345L,
            avatarUrl = "",
            name = "Test User",
            email = "test@example.com",
            publicRepos = 10
        )

        val result = repository.validateCredentials(username, token)

        assertTrue(result.isFailure)
    }

    @Test
    fun `isAuthenticated returns true when credentials are stored`() {
        every { secureStorage.isAuthenticated() } returns true

        assertTrue(repository.isAuthenticated())
    }

    @Test
    fun `isAuthenticated returns false when credentials are not stored`() {
        every { secureStorage.isAuthenticated() } returns false

        assertFalse(repository.isAuthenticated())
    }

    @Test
    fun `getUsername returns stored username`() {
        every { secureStorage.githubUsername } returns "testuser"

        assertEquals("testuser", repository.getUsername())
    }

    @Test
    fun `getToken returns stored token`() {
        every { secureStorage.githubToken } returns "ghp_test123"

        assertEquals("ghp_test123", repository.getToken())
    }

    @Test
    fun `saveCredentials stores credentials correctly`() = runTest {
        repository.saveCredentials("user", "token", "main")

        verify { secureStorage.githubUsername = "user" }
        verify { secureStorage.githubToken = "token" }
        verify { secureStorage.defaultBranch = "main" }
        verify { secureStorage.isSetupComplete = true }
    }

    @Test
    fun `clearCredentials clears storage`() {
        repository.clearCredentials()

        verify { secureStorage.clear() }
    }
}

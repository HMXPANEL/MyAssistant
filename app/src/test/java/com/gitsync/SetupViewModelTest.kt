package com.gitsync

import com.gitsync.data.repository.AuthRepositoryImpl
import com.gitsync.presentation.setup.SetupViewModel
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var viewModel: SetupViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        viewModel = SetupViewModel(authRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.state.value

        assertEquals("", state.username)
        assertEquals("", state.token)
        assertEquals("main", state.branch)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
        assertEquals(false, state.isSuccess)
    }

    @Test
    fun `updating username changes state`() {
        viewModel.onUsernameChanged("testuser")

        assertEquals("testuser", viewModel.state.value.username)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `updating token changes state`() {
        viewModel.onTokenChanged("ghp_test123")

        assertEquals("ghp_test123", viewModel.state.value.token)
    }

    @Test
    fun `updating branch changes state`() {
        viewModel.onBranchChanged("develop")

        assertEquals("develop", viewModel.state.value.branch)
    }

    @Test
    fun `validation fails with empty username`() {
        viewModel.onUsernameChanged("")
        viewModel.onTokenChanged("valid_token")
        viewModel.onBranchChanged("main")

        viewModel.validateAndSave()

        assertNotNull(viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.isSuccess)
    }

    @Test
    fun `validation fails with empty token`() {
        viewModel.onUsernameChanged("user")
        viewModel.onTokenChanged("")
        viewModel.onBranchChanged("main")

        viewModel.validateAndSave()

        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `validation fails with empty branch`() {
        viewModel.onUsernameChanged("user")
        viewModel.onTokenChanged("valid_token")
        viewModel.onBranchChanged("")

        viewModel.validateAndSave()

        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `validation passes with valid inputs`() = runTest {
        viewModel.onUsernameChanged("testuser")
        viewModel.onTokenChanged("ghp_valid_token_123")
        viewModel.onBranchChanged("main")

        viewModel.validateAndSave()

        coVerify { authRepository.saveCredentials("testuser", "ghp_valid_token_123", "main") }
    }
}

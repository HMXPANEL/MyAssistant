package com.gitsync

import com.gitsync.domain.repository.AuthRepository
import com.gitsync.presentation.splash.SplashViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SplashViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        every { authRepository.isSetupComplete() } returns false
        every { authRepository.isAuthenticated() } returns false

        viewModel = SplashViewModel(authRepository)

        assertEquals(true, viewModel.state.value.isLoading)
        assertEquals(false, viewModel.state.value.isSetupComplete)
    }

    @Test
    fun `setup complete after delay when authenticated`() = runTest {
        every { authRepository.isSetupComplete() } returns true
        every { authRepository.isAuthenticated() } returns true

        viewModel = SplashViewModel(authRepository)

        advanceTimeBy(1600)

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(true, viewModel.state.value.isSetupComplete)
    }

    @Test
    fun `setup not complete when not authenticated`() = runTest {
        every { authRepository.isSetupComplete() } returns false
        every { authRepository.isAuthenticated() } returns false

        viewModel = SplashViewModel(authRepository)

        advanceTimeBy(1600)

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(false, viewModel.state.value.isSetupComplete)
    }
}

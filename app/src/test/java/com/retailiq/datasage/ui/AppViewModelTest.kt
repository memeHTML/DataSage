package com.retailiq.datasage.ui

import com.retailiq.datasage.core.AuthEvent
import com.retailiq.datasage.core.AuthEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginSuccess_setsLoggedInTrue() = runTest {
        val viewModel = AppViewModel(AuthEventBus())
        viewModel.onLoginSuccess()
        assertTrue(viewModel.isLoggedIn.value)
    }

    @Test
    fun sessionExpired_setsLoggedInFalse() = runTest {
        val bus = AuthEventBus()
        val viewModel = AppViewModel(bus)
        viewModel.onLoginSuccess()
        bus.emit(AuthEvent.SessionExpired)
        assertFalse(viewModel.isLoggedIn.value)
    }
}

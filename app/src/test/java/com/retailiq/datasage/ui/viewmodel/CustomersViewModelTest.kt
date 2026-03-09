package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.api.Customer
import com.retailiq.datasage.data.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CustomersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CustomerRepository
    private lateinit var viewModel: CustomersViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(CustomerRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadCustomers success emits Loaded state`() = runTest {
        val dummyCustomers = listOf(
            Customer(customerId = 1, name = "John Doe", totalSpend = 100.0)
        )
        whenever(repository.listCustomers(search = null)).thenReturn(Result.success(dummyCustomers))

        viewModel = CustomersViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is CustomersListUiState.Loaded)
        assertEquals(dummyCustomers, (state as CustomersListUiState.Loaded).customers)
    }

    @Test
    fun `loadCustomers error emits Error state`() = runTest {
        whenever(repository.listCustomers(search = null)).thenReturn(Result.failure(Exception("API Error")))

        viewModel = CustomersViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is CustomersListUiState.Error)
        assertEquals("API Error", (state as CustomersListUiState.Error).message)
    }
}

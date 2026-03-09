package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.Customer
import com.retailiq.datasage.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CustomersListUiState {
    data object Loading : CustomersListUiState()
    data class Loaded(val customers: List<Customer>) : CustomersListUiState()
    data class Error(val message: String) : CustomersListUiState()
}

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomersListUiState>(CustomersListUiState.Loading)
    val uiState: StateFlow<CustomersListUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    fun loadCustomers(search: String? = null) {
        viewModelScope.launch {
            _uiState.value = CustomersListUiState.Loading
            val result = repository.listCustomers(search = search)
            result.onSuccess { customers ->
                _uiState.value = CustomersListUiState.Loaded(customers)
            }.onFailure { exception ->
                _uiState.value = CustomersListUiState.Error(exception.message ?: "Failed to load customers")
            }
        }
    }
}

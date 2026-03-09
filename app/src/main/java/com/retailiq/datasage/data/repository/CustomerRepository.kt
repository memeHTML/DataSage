package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.Customer
import com.retailiq.datasage.data.api.CustomerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val api: CustomerApiService
) {
    suspend fun listCustomers(page: Int = 1, search: String? = null): Result<List<Customer>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.listCustomers(page = page, search = search)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.error?.message ?: "Unknown error fetching customers"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

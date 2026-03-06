package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.StaffApiService
import com.retailiq.datasage.data.model.DailyTargetRequest
import com.retailiq.datasage.data.model.StaffPerformanceSummaryDto
import com.retailiq.datasage.data.model.StaffSessionDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import retrofit2.Response

class StaffRepositoryTest {

    private lateinit var api: StaffApiService
    private lateinit var repository: StaffRepository

    @Before
    fun setup() {
        api = mock(StaffApiService::class.java)
        repository = StaffRepository(api)
    }

    @Test
    fun `startSession returns Success when API succeeds`() = runTest {
        val mockData = StaffSessionDto("s1", "ACTIVE", "2023-10-10T10:00:00Z", null, true, 0.0)
        `when`(api.startSession()).thenReturn(Response.success(mockData))

        val result = repository.startSession()

        assertTrue(result.isSuccess)
        assertEquals(mockData, result.getOrNull())
    }

    @Test
    fun `endSession returns error when API fails`() = runTest {
        val errorBody = "{}".toResponseBody("application/json".toMediaType())
        `when`(api.endSession()).thenReturn(Response.error(400, errorBody))

        val result = repository.endSession()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("400") == true)
    }

    @Test
    fun `getDailyPerformance returns list on Success`() = runTest {
        val mockData = listOf(
            StaffPerformanceSummaryDto("u1", "John", 100.0, 5, 0.0, 0.0, 500.0, 0.2)
        )
        val apiResponse = com.retailiq.datasage.data.api.ApiResponse(
            success = true, data = mockData, error = null, meta = null
        )
        `when`(api.getDailyPerformance("2023-10-10")).thenReturn(apiResponse)

        val result = repository.getDailyPerformance("2023-10-10")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("John", result.getOrNull()?.get(0)?.name)
    }

    @Test
    fun `setDailyTarget returns Success on API success`() = runTest {
        val req = DailyTargetRequest("2023-10-10", 500.0, 10, "u1")
        `when`(api.setDailyTarget(req)).thenReturn(Response.success(Unit))

        val result = repository.setDailyTarget(req)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `startSession returns Error on exception`() = runTest {
        `when`(api.startSession()).thenThrow(RuntimeException("Network failure"))

        val result = repository.startSession()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Network failure") == true)
    }
}

package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.ReceiptsApiService
import com.retailiq.datasage.data.model.BarcodeDto
import com.retailiq.datasage.data.model.BarcodeProductDto
import com.retailiq.datasage.data.model.PrintJobRequest
import com.retailiq.datasage.data.model.PrintJobResponse
import com.retailiq.datasage.data.model.PrintJobStatusDto
import com.retailiq.datasage.data.model.ReceiptTemplateDto
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.data.model.RegisterBarcodeRequest
import com.retailiq.datasage.data.repository.ReceiptsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun barcodeLookup_idle_initially() = runTest {
        val repo = ReceiptsRepository(FakeReceiptsApi(false))
        val viewModel = ReceiptsViewModel(repo)

        assertEquals(BarcodeLookupUiState.Idle, viewModel.barcodeLookupState.value)
    }

    @Test
    fun barcodeLookup_loading_thenSuccess() = runTest {
        val repo = ReceiptsRepository(FakeReceiptsApi(false))
        val viewModel = ReceiptsViewModel(repo)

        viewModel.lookupBarcode("123456")

        val lastState = viewModel.barcodeLookupState.value
        assertTrue(lastState is BarcodeLookupUiState.Success)
        assertEquals("Test Product", (lastState as BarcodeLookupUiState.Success).product.productName)
    }

    @Test
    fun barcodeLookup_loading_thenError() = runTest {
        val repo = ReceiptsRepository(FakeReceiptsApi(true))
        val viewModel = ReceiptsViewModel(repo)

        viewModel.lookupBarcode("999999")

        val lastState = viewModel.barcodeLookupState.value
        assertTrue(lastState is BarcodeLookupUiState.Error)
    }

    private class FakeReceiptsApi(val notFound: Boolean) : ReceiptsApiService {
        override suspend fun getTemplate(): Response<ApiResponse<ReceiptTemplateDto>> = TODO()
        override suspend fun updateTemplate(body: ReceiptTemplateRequest): Response<ApiResponse<ReceiptTemplateDto>> = TODO()
        override suspend fun createPrintJob(body: PrintJobRequest): Response<ApiResponse<PrintJobResponse>> = TODO()
        override suspend fun pollPrintJob(jobId: String): Response<ApiResponse<PrintJobStatusDto>> = TODO()

        override suspend fun lookupBarcode(value: String): Response<ApiResponse<BarcodeProductDto>> {
            if (notFound) return Response.error(404, "".toResponseBody(null))
            val dto = BarcodeProductDto(1, "Test Product", 10.0, 100.0)
            return Response.success(ApiResponse(success = true, data = dto, error = null, meta = null))
        }

        override suspend fun registerBarcode(body: RegisterBarcodeRequest): Response<ApiResponse<BarcodeDto>> = TODO()
    }
}

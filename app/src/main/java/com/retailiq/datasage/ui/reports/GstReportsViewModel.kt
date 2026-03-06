package com.retailiq.datasage.ui.reports

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.GstSlabDto
import com.retailiq.datasage.data.model.GstSummaryDto
import com.retailiq.datasage.data.repository.GstRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class GstReportUiState {
    data object Loading : GstReportUiState()
    data class Success(val summary: GstSummaryDto, val slabs: List<GstSlabDto>) : GstReportUiState()
    data class Error(val message: String) : GstReportUiState()
}

@HiltViewModel
class GstReportsViewModel @Inject constructor(
    private val repository: GstRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GstReportUiState>(GstReportUiState.Loading)
    val uiState: StateFlow<GstReportUiState> = _uiState.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    // Default to current month, format YYYY-MM
    private val _currentPeriod = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
    val currentPeriod: StateFlow<String> = _currentPeriod.asStateFlow()

    init {
        loadData(_currentPeriod.value)
    }

    fun setPeriod(period: String) {
        _currentPeriod.value = period
        loadData(period)
    }

    private fun loadData(period: String) = viewModelScope.launch {
        _uiState.value = GstReportUiState.Loading
        
        val summaryResult = repository.getSummary(period)
        val slabsResult = repository.getLiabilitySlabs(period)

        if (summaryResult is NetworkResult.Success && slabsResult is NetworkResult.Success) {
            _uiState.value = GstReportUiState.Success(
                summary = summaryResult.data,
                slabs = slabsResult.data
            )
        } else if (summaryResult is NetworkResult.Error) {
            _uiState.value = GstReportUiState.Error("Summary Error: ${summaryResult.message}")
        } else if (slabsResult is NetworkResult.Error) {
            _uiState.value = GstReportUiState.Error("Slabs Error: ${slabsResult.message}")
        }
    }

    fun exportGstr1(context: Context) = viewModelScope.launch {
        _exportMessage.value = "Starting export..."
        when (val result = repository.getGstr1(_currentPeriod.value)) {
            is NetworkResult.Success<*> -> {
                val jsonContent = Gson().toJson(result.data)
                saveJsonToDownloads(context, jsonContent, _currentPeriod.value)
            }
            is NetworkResult.Error<*> -> {
                _exportMessage.value = "Export failed: ${result.message}"
            }
            is NetworkResult.Loading<*> -> Unit
        }
    }

    private fun saveJsonToDownloads(context: Context, jsonContent: String, period: String) {
        try {
            val filename = "GSTR1_$period.json"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // API 29+ — use MediaStore
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RetailIQ")
                }
                val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                        outputStream.write(jsonContent.toByteArray())
                    }
                    _exportMessage.value = "Exported to Downloads/RetailIQ/$filename"
                } else {
                    _exportMessage.value = "Failed to create file in Downloads"
                }
            } else {
                // Legacy fallback for API < 29
                @Suppress("DEPRECATION")
                val downloadsDir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "RetailIQ"
                )
                downloadsDir.mkdirs()
                val file = java.io.File(downloadsDir, filename)
                file.writeText(jsonContent)
                _exportMessage.value = "Exported to Downloads/RetailIQ/$filename"
            }
        } catch (e: Exception) {
            _exportMessage.value = "Export error: ${e.message}"
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }
}

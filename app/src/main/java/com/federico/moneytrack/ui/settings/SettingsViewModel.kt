package com.federico.moneytrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.data.local.CsvBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val csvBackupManager: CsvBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun exportData(outputStream: OutputStream) {
        _uiState.value = SettingsUiState.Exporting
        viewModelScope.launch {
            csvBackupManager.exportToFile(outputStream)
                .onSuccess { _uiState.value = SettingsUiState.Success(SuccessType.EXPORT) }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Error desconocido", ErrorType.EXPORT) }
        }
    }

    fun importData(inputStream: InputStream) {
        _uiState.value = SettingsUiState.Importing
        viewModelScope.launch {
            csvBackupManager.importFromFile(inputStream)
                .onSuccess { count -> _uiState.value = SettingsUiState.Success(SuccessType.IMPORT, count) }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Error desconocido", ErrorType.IMPORT) }
        }
    }

    fun clearState() {
        _uiState.value = SettingsUiState.Idle
    }
}

sealed class SettingsUiState {
    data object Idle : SettingsUiState()
    data object Exporting : SettingsUiState()
    data object Importing : SettingsUiState()
    data class Success(val type: SuccessType, val recordCount: Int = 0) : SettingsUiState()
    data class Error(val message: String, val type: ErrorType) : SettingsUiState()
}

enum class SuccessType { EXPORT, IMPORT }
enum class ErrorType { EXPORT, IMPORT }

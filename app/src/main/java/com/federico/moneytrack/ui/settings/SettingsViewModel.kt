package com.federico.moneytrack.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.data.local.CsvBackupManager
import com.federico.moneytrack.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val csvBackupManager: CsvBackupManager,
    private val prefs: SharedPreferences,
    private val reminderScheduler: ReminderScheduler
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

    fun getReminderTime(): Pair<Int, Int> {
        val hour = prefs.getInt(KEY_REMINDER_HOUR, 21)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun saveReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        reminderScheduler.schedule(hour, minute)
    }

    companion object {
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
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

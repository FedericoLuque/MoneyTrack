package com.federico.moneytrack.ui.bitcoin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.BitcoinHolding
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.usecase.bitcoin.DeleteBitcoinTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BitcoinHoldingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bitcoinRepository: BitcoinRepository,
    private val deleteBitcoinTransactionUseCase: DeleteBitcoinTransactionUseCase
) : ViewModel() {

    private val holdingId: Long = savedStateHandle["holdingId"]!!

    private val _state = MutableStateFlow<BitcoinHoldingDetailState>(BitcoinHoldingDetailState.Loading)
    val state: StateFlow<BitcoinHoldingDetailState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    init {
        loadHolding()
    }

    private fun loadHolding() {
        viewModelScope.launch {
            try {
                val holding = bitcoinRepository.getHoldingById(holdingId)
                    ?: run {
                        _eventFlow.emit(UiEvent.Error("Operación no encontrada"))
                        return@launch
                    }
                _state.value = BitcoinHoldingDetailState.Loaded(holding)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error(e.message ?: "Error al cargar"))
            }
        }
    }

    fun deleteHolding() {
        val currentState = _state.value as? BitcoinHoldingDetailState.Loaded ?: return
        viewModelScope.launch {
            try {
                deleteBitcoinTransactionUseCase(currentState.holding)
                _eventFlow.emit(UiEvent.Deleted)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error(e.message ?: "Error al eliminar"))
            }
        }
    }

    sealed class BitcoinHoldingDetailState {
        object Loading : BitcoinHoldingDetailState()
        data class Loaded(val holding: BitcoinHolding) : BitcoinHoldingDetailState()
    }

    sealed class UiEvent {
        object Deleted : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}

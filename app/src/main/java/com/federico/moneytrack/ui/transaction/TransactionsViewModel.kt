package com.federico.moneytrack.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionWithCategory>> = transactionRepository.getAllTransactionsWithCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                // TODO: Aquí deberíamos actualizar el saldo de la cuenta implicada (sumar lo gastado o restar lo ingresado).
                // Por ahora solo borramos el registro para no complicar en exceso esta iteración.
                // Idealmente usaríamos un DeleteTransactionUseCase que revierta el saldo.
                
                transactionRepository.deleteTransaction(transaction)
                _eventFlow.emit(UiEvent.DeleteSuccess)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error(e.message ?: "Error al eliminar"))
            }
        }
    }

    sealed class UiEvent {
        object DeleteSuccess : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}

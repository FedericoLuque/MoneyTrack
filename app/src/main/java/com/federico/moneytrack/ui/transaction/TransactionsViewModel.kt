package com.federico.moneytrack.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.domain.usecase.DeleteTransactionUseCase
import com.federico.moneytrack.domain.usecase.bitcoin.DeleteBitcoinTransactionUseCase
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
    private val transactionRepository: TransactionRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val deleteBitcoinTransactionUseCase: DeleteBitcoinTransactionUseCase,
    private val bitcoinRepository: BitcoinRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionWithCategory>> = transactionRepository.getAllTransactionsWithCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    fun deleteTransaction(transaction: Transaction, categoryType: String?) {
        viewModelScope.launch {
            try {
                if (categoryType == "BITCOIN") {
                    val holding = bitcoinRepository.getHoldingByTransactionId(transaction.id)
                    if (holding != null) {
                        deleteBitcoinTransactionUseCase(holding)
                    } else {
                        // Holding no encontrado, borrar solo la transacción revirtiendo saldo
                        deleteTransactionUseCase(transaction, transaction.amount < 0)
                    }
                } else {
                    val isExpense = categoryType == "EXPENSE" || transaction.amount < 0
                    deleteTransactionUseCase(transaction, isExpense)
                }
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

package com.federico.moneytrack.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.CategoryRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.domain.usecase.DeleteTransactionUseCase
import com.federico.moneytrack.domain.usecase.bitcoin.DeleteBitcoinTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val deleteBitcoinTransactionUseCase: DeleteBitcoinTransactionUseCase,
    private val bitcoinRepository: BitcoinRepository
) : ViewModel() {

    private val transactionId: Long = savedStateHandle["transactionId"]!!

    private val _state = MutableStateFlow<TransactionDetailState>(TransactionDetailState.Loading)
    val state: StateFlow<TransactionDetailState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            try {
                val transaction = transactionRepository.getTransactionById(transactionId)
                    ?: run {
                        _eventFlow.emit(UiEvent.Error("Transacción no encontrada"))
                        return@launch
                    }
                val category = transaction.categoryId?.let { categoryRepository.getCategoryById(it) }
                val account = accountRepository.getAccountById(transaction.accountId)

                _state.value = TransactionDetailState.Loaded(
                    transaction = transaction,
                    category = category,
                    account = account
                )
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error(e.message ?: "Error al cargar"))
            }
        }
    }

    fun deleteTransaction() {
        val currentState = _state.value as? TransactionDetailState.Loaded ?: return
        viewModelScope.launch {
            try {
                val transaction = currentState.transaction
                val categoryType = currentState.category?.transactionType

                if (categoryType == "BITCOIN") {
                    val holding = bitcoinRepository.getHoldingByTransactionId(transaction.id)
                    if (holding != null) {
                        deleteBitcoinTransactionUseCase(holding)
                    } else {
                        deleteTransactionUseCase(transaction, transaction.amount < 0)
                    }
                } else {
                    val isExpense = categoryType == "EXPENSE" || transaction.amount < 0
                    deleteTransactionUseCase(transaction, isExpense)
                }
                _eventFlow.emit(UiEvent.Deleted)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error(e.message ?: "Error al eliminar"))
            }
        }
    }

    sealed class TransactionDetailState {
        object Loading : TransactionDetailState()
        data class Loaded(
            val transaction: Transaction,
            val category: Category?,
            val account: Account?
        ) : TransactionDetailState()
    }

    sealed class UiEvent {
        object Deleted : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}

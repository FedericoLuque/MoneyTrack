package com.federico.moneytrack.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.BitcoinRepository
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
    transactionRepository: TransactionRepository,
    private val bitcoinRepository: BitcoinRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionWithCategory>> = transactionRepository.getAllTransactionsWithCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    fun onTransactionClicked(item: TransactionWithCategory) {
        val categoryType = item.category?.transactionType
        if (categoryType == "BITCOIN") {
            viewModelScope.launch {
                val holding = bitcoinRepository.getHoldingByTransactionId(item.transaction.id)
                if (holding != null) {
                    _eventFlow.emit(UiEvent.NavigateToBitcoinDetail(holding.id))
                } else {
                    _eventFlow.emit(UiEvent.NavigateToTransactionDetail(item.transaction.id))
                }
            }
        } else {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.NavigateToTransactionDetail(item.transaction.id))
            }
        }
    }

    sealed class UiEvent {
        data class NavigateToTransactionDetail(val transactionId: Long) : UiEvent()
        data class NavigateToBitcoinDetail(val holdingId: Long) : UiEvent()
    }
}

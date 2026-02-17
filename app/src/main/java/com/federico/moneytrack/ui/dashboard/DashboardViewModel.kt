package com.federico.moneytrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.domain.usecase.GetBitcoinValueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    getBitcoinValueUseCase: GetBitcoinValueUseCase,
    private val bitcoinRepository: BitcoinRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAllAccounts(),
        transactionRepository.getRecentTransactionsWithCategory(5),
        getBitcoinValueUseCase("eur")
    ) { accounts, recentTransactions, btcValue ->
        val totalBalance = accounts.sumOf { it.currentBalance }
        DashboardUiState.Success(
            fiatBalance = totalBalance,
            bitcoinValue = btcValue,
            recentTransactions = recentTransactions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

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

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val fiatBalance: Double,
        val bitcoinValue: Double,
        val recentTransactions: List<TransactionWithCategory>
    ) : DashboardUiState()
}

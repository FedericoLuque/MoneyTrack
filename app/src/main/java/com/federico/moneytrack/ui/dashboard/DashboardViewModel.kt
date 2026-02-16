package com.federico.moneytrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.domain.usecase.GetBitcoinValueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    getBitcoinValueUseCase: GetBitcoinValueUseCase
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
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val fiatBalance: Double,
        val bitcoinValue: Double,
        val recentTransactions: List<TransactionWithCategory>
    ) : DashboardUiState()
}
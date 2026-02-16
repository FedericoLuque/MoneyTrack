package com.federico.moneytrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.domain.usecase.GetTotalBalanceUseCase
import com.federico.moneytrack.domain.usecase.GetBitcoinValueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getTotalBalanceUseCase: GetTotalBalanceUseCase,
    transactionRepository: TransactionRepository,
    getBitcoinValueUseCase: GetBitcoinValueUseCase
) : ViewModel() {

    // Estado combinado para la UI
    val uiState: StateFlow<DashboardUiState> = combine(
        getTotalBalanceUseCase(),
        transactionRepository.getAllTransactions(),
        getBitcoinValueUseCase("usd") // Usamos USD por defecto
    ) { totalBalance, transactions, btcValue ->
        DashboardUiState.Success(
            fiatBalance = totalBalance,
            recentTransactions = transactions.take(5), // Solo las 5 últimas
            bitcoinValue = btcValue
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val fiatBalance: Double,
        val recentTransactions: List<Transaction>,
        val bitcoinValue: Double
    ) : DashboardUiState()
}
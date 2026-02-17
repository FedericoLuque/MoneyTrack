package com.federico.moneytrack.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.DailyCashFlow
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.usecase.GetBitcoinValueUseCase
import com.federico.moneytrack.domain.usecase.GetMonthlyCashFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor(
    getMonthlyCashFlowUseCase: GetMonthlyCashFlowUseCase,
    accountRepository: AccountRepository,
    getBitcoinValueUseCase: GetBitcoinValueUseCase
) : ViewModel() {

    val uiState: StateFlow<ChartsUiState> = combine(
        getMonthlyCashFlowUseCase(),
        accountRepository.getAllAccounts(),
        getBitcoinValueUseCase("eur")
    ) { monthlyCashFlow, accounts, btcValue ->
        val fiatBalance = accounts.sumOf { it.currentBalance }
        ChartsUiState.Success(
            monthlyCashFlow = monthlyCashFlow,
            fiatBalance = fiatBalance,
            bitcoinValue = btcValue
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartsUiState.Loading)
}

sealed class ChartsUiState {
    object Loading : ChartsUiState()
    data class Success(
        val monthlyCashFlow: List<DailyCashFlow>,
        val fiatBalance: Double,
        val bitcoinValue: Double
    ) : ChartsUiState()
}

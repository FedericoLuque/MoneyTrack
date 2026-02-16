package com.federico.moneytrack.ui.bitcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.usecase.GetBitcoinValueUseCase
import com.federico.moneytrack.domain.usecase.bitcoin.AddBitcoinTransactionUseCase
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
class BitcoinViewModel @Inject constructor(
    private val addBitcoinTransactionUseCase: AddBitcoinTransactionUseCase,
    private val getBitcoinValueUseCase: GetBitcoinValueUseCase,
    accountRepository: AccountRepository,
    bitcoinRepository: BitcoinRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<BitcoinUiState> = combine(
        bitcoinRepository.getBitcoinHoldings(),
        getBitcoinValueUseCase("eur")
    ) { holdings, fiatValue ->
        val totalSats = holdings.sumOf { it.satsAmount }
        BitcoinUiState(
            totalSats = totalSats,
            fiatValue = fiatValue
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BitcoinUiState())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    fun executeTransaction(
        sats: Long,
        fiatAmount: Double,
        accountId: Long,
        isBuy: Boolean,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                val btcAmount = sats / 100_000_000.0
                val impliedPrice = if (btcAmount > 0) fiatAmount / btcAmount else 0.0

                addBitcoinTransactionUseCase(
                    satsAmount = sats,
                    fiatAmount = fiatAmount,
                    accountId = accountId,
                    isBuy = isBuy,
                    price = impliedPrice,
                    note = note
                )
                _eventFlow.emit(UiEvent.TransactionSuccess)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error(e.message ?: "Error en la operación"))
            }
        }
    }

    data class BitcoinUiState(
        val totalSats: Long = 0,
        val fiatValue: Double = 0.0
    )

    sealed class UiEvent {
        object TransactionSuccess : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}

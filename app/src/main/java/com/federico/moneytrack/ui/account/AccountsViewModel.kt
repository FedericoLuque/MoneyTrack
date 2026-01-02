package com.federico.moneytrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    fun addAccount(name: String, initialBalance: Double, type: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _eventFlow.emit(UiEvent.Error("El nombre no puede estar vacío"))
                return@launch
            }

            val account = Account(
                name = name,
                currentBalance = initialBalance,
                type = type
            )
            accountRepository.insertAccount(account)
            _eventFlow.emit(UiEvent.SaveSuccess)
        }
    }

    sealed class UiEvent {
        object SaveSuccess : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}

package com.federico.moneytrack.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.CategoryRepository
import com.federico.moneytrack.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    fun saveTransaction(
        amount: Double,
        note: String?,
        accountId: Long,
        categoryId: Long?,
        isExpense: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (amount <= 0) {
                    _eventFlow.emit(UiEvent.Error("La cantidad debe ser mayor a 0"))
                    return@launch
                }

                val transaction = Transaction(
                    amount = amount,
                    note = note,
                    accountId = accountId,
                    categoryId = categoryId,
                    date = System.currentTimeMillis()
                )

                addTransactionUseCase(transaction, isExpense)
                _eventFlow.emit(UiEvent.SaveSuccess)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error(e.message ?: "Error desconocido"))
            }
        }
    }

    sealed class UiEvent {
        object SaveSuccess : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}

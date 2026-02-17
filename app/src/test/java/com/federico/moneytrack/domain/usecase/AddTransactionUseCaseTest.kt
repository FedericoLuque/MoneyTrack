package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.exception.InsufficientBalanceException
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AddTransactionUseCaseTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var useCase: AddTransactionUseCase

    @Before
    fun setUp() {
        transactionRepository = mock()
        accountRepository = mock()
        useCase = AddTransactionUseCase(transactionRepository, accountRepository)
    }

    @Test
    fun `gasto con saldo insuficiente lanza InsufficientBalanceException`() = runTest {
        val account = Account(id = 1, name = "Cuenta", currentBalance = 50.0, type = "EFECTIVO")
        whenever(accountRepository.getAccountById(1)).thenReturn(account)

        val transaction = Transaction(accountId = 1, categoryId = null, amount = 100.0, date = 0L, note = null)

        try {
            useCase(transaction, isExpense = true)
            fail("Debería lanzar InsufficientBalanceException")
        } catch (e: InsufficientBalanceException) {
            assertEquals("Saldo insuficiente en la cuenta", e.message)
        }

        verify(transactionRepository, never()).insertTransaction(any())
        verify(accountRepository, never()).updateAccount(any())
    }

    @Test
    fun `gasto exacto al saldo funciona correctamente`() = runTest {
        val account = Account(id = 1, name = "Cuenta", currentBalance = 100.0, type = "EFECTIVO")
        whenever(accountRepository.getAccountById(1)).thenReturn(account)

        val transaction = Transaction(accountId = 1, categoryId = null, amount = 100.0, date = 0L, note = null)

        useCase(transaction, isExpense = true)

        verify(transactionRepository).insertTransaction(transaction)
        verify(accountRepository).updateAccount(account.copy(currentBalance = 0.0))
    }

    @Test
    fun `ingreso no valida saldo`() = runTest {
        val account = Account(id = 1, name = "Cuenta", currentBalance = 0.0, type = "EFECTIVO")
        whenever(accountRepository.getAccountById(1)).thenReturn(account)

        val transaction = Transaction(accountId = 1, categoryId = null, amount = 500.0, date = 0L, note = null)

        useCase(transaction, isExpense = false)

        verify(transactionRepository).insertTransaction(transaction)
        verify(accountRepository).updateAccount(account.copy(currentBalance = 500.0))
    }
}

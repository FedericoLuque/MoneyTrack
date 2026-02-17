package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddTransactionUseCaseTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var useCase: AddTransactionUseCase

    @Before
    fun setUp() {
        transactionRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        useCase = AddTransactionUseCase(transactionRepository, accountRepository)
    }

    @Test
    fun `gasto resta amount del saldo de la cuenta`() = runTest {
        val account = Account(id = 1, name = "Efectivo", currentBalance = 1000.0, type = "cash")
        val transaction = Transaction(accountId = 1, categoryId = 1, amount = 200.0, date = 0L, note = null)

        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(transaction, isExpense = true)

        coVerify {
            accountRepository.updateAccount(match { it.currentBalance == 800.0 })
        }
    }

    @Test
    fun `ingreso suma amount al saldo de la cuenta`() = runTest {
        val account = Account(id = 1, name = "Efectivo", currentBalance = 1000.0, type = "cash")
        val transaction = Transaction(accountId = 1, categoryId = 1, amount = 500.0, date = 0L, note = null)

        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(transaction, isExpense = false)

        coVerify {
            accountRepository.updateAccount(match { it.currentBalance == 1500.0 })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cuenta no encontrada lanza IllegalArgumentException`() = runTest {
        val transaction = Transaction(accountId = 99, categoryId = 1, amount = 100.0, date = 0L, note = null)

        coEvery { accountRepository.getAccountById(99) } returns null

        useCase(transaction, isExpense = true)
    }

    @Test
    fun `verifica que se llama a insertTransaction y updateAccount`() = runTest {
        val account = Account(id = 1, name = "Efectivo", currentBalance = 500.0, type = "cash")
        val transaction = Transaction(accountId = 1, categoryId = 1, amount = 100.0, date = 0L, note = null)

        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(transaction, isExpense = true)

        coVerify(exactly = 1) { transactionRepository.insertTransaction(transaction) }
        coVerify(exactly = 1) { accountRepository.updateAccount(any()) }
    }
}

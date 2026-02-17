package com.federico.moneytrack.domain.usecase.bitcoin

import com.federico.moneytrack.domain.exception.InsufficientBalanceException
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
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

class AddBitcoinTransactionUseCaseTest {

    private lateinit var bitcoinRepository: BitcoinRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var useCase: AddBitcoinTransactionUseCase

    @Before
    fun setUp() {
        bitcoinRepository = mock()
        accountRepository = mock()
        transactionRepository = mock()
        useCase = AddBitcoinTransactionUseCase(bitcoinRepository, accountRepository, transactionRepository)
    }

    @Test
    fun `compra BTC con saldo fiat insuficiente lanza InsufficientBalanceException`() = runTest {
        val account = Account(id = 1, name = "Cuenta", currentBalance = 50.0, type = "EFECTIVO")
        whenever(accountRepository.getAccountById(1)).thenReturn(account)

        try {
            useCase(
                satsAmount = 100_000L,
                fiatAmount = 100.0,
                accountId = 1,
                isBuy = true,
                price = 50000.0,
                note = null
            )
            fail("Debería lanzar InsufficientBalanceException")
        } catch (e: InsufficientBalanceException) {
            assertEquals("Saldo insuficiente en la cuenta", e.message)
        }

        verify(bitcoinRepository, never()).insertBitcoinHolding(any())
        verify(accountRepository, never()).updateAccount(any())
        verify(transactionRepository, never()).insertTransaction(any())
    }

    @Test
    fun `venta BTC con sats insuficientes lanza InsufficientBalanceException`() = runTest {
        val account = Account(id = 1, name = "Cuenta", currentBalance = 1000.0, type = "EFECTIVO")
        whenever(accountRepository.getAccountById(1)).thenReturn(account)
        whenever(bitcoinRepository.getTotalSats()).thenReturn(50_000L)

        try {
            useCase(
                satsAmount = 100_000L,
                fiatAmount = 100.0,
                accountId = 1,
                isBuy = false,
                price = 50000.0,
                note = null
            )
            fail("Debería lanzar InsufficientBalanceException")
        } catch (e: InsufficientBalanceException) {
            assertEquals("Saldo insuficiente de Bitcoin", e.message)
        }

        verify(bitcoinRepository, never()).insertBitcoinHolding(any())
        verify(accountRepository, never()).updateAccount(any())
        verify(transactionRepository, never()).insertTransaction(any())
    }

    @Test
    fun `compra BTC con saldo exacto funciona correctamente`() = runTest {
        val account = Account(id = 1, name = "Cuenta", currentBalance = 100.0, type = "EFECTIVO")
        whenever(accountRepository.getAccountById(1)).thenReturn(account)

        useCase(
            satsAmount = 100_000L,
            fiatAmount = 100.0,
            accountId = 1,
            isBuy = true,
            price = 50000.0,
            note = null
        )

        verify(bitcoinRepository).insertBitcoinHolding(any())
        verify(accountRepository).updateAccount(account.copy(currentBalance = 0.0))
        verify(transactionRepository).insertTransaction(any())
    }

    @Test
    fun `venta BTC con sats exactos funciona correctamente`() = runTest {
        val account = Account(id = 1, name = "Cuenta", currentBalance = 1000.0, type = "EFECTIVO")
        whenever(accountRepository.getAccountById(1)).thenReturn(account)
        whenever(bitcoinRepository.getTotalSats()).thenReturn(100_000L)

        useCase(
            satsAmount = 100_000L,
            fiatAmount = 100.0,
            accountId = 1,
            isBuy = false,
            price = 50000.0,
            note = null
        )

        verify(bitcoinRepository).insertBitcoinHolding(any())
        verify(accountRepository).updateAccount(account.copy(currentBalance = 1100.0))
        verify(transactionRepository).insertTransaction(any())
    }
}

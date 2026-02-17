package com.federico.moneytrack.domain.usecase.bitcoin

import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddBitcoinTransactionUseCaseTest {

    private lateinit var bitcoinRepository: BitcoinRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var useCase: AddBitcoinTransactionUseCase

    @Before
    fun setUp() {
        bitcoinRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        useCase = AddBitcoinTransactionUseCase(bitcoinRepository, accountRepository, transactionRepository)
    }

    @Test
    fun `compra BTC registra sats positivos y saldo fiat decrece`() = runTest {
        val account = Account(id = 1, name = "Banco", currentBalance = 5000.0, type = "bank")
        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(satsAmount = 100_000, fiatAmount = 50.0, accountId = 1, isBuy = true, price = 50000.0, note = null)

        coVerify {
            bitcoinRepository.insertBitcoinHolding(match { it.satsAmount == 100_000L })
        }
        coVerify {
            accountRepository.updateAccount(match { it.currentBalance == 4950.0 })
        }
    }

    @Test
    fun `venta BTC registra sats negativos y saldo fiat aumenta`() = runTest {
        val account = Account(id = 1, name = "Banco", currentBalance = 5000.0, type = "bank")
        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(satsAmount = 100_000, fiatAmount = 50.0, accountId = 1, isBuy = false, price = 50000.0, note = null)

        coVerify {
            bitcoinRepository.insertBitcoinHolding(match { it.satsAmount == -100_000L })
        }
        coVerify {
            accountRepository.updateAccount(match { it.currentBalance == 5050.0 })
        }
    }

    @Test
    fun `compra BTC registra amount negativo en transaccion`() = runTest {
        val account = Account(id = 1, name = "Banco", currentBalance = 5000.0, type = "bank")
        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(satsAmount = 100_000, fiatAmount = 50.0, accountId = 1, isBuy = true, price = 50000.0, note = null)

        coVerify {
            transactionRepository.insertTransaction(match { it.amount == -50.0 })
        }
    }

    @Test
    fun `venta BTC registra amount positivo en transaccion`() = runTest {
        val account = Account(id = 1, name = "Banco", currentBalance = 5000.0, type = "bank")
        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(satsAmount = 100_000, fiatAmount = 50.0, accountId = 1, isBuy = false, price = 50000.0, note = null)

        coVerify {
            transactionRepository.insertTransaction(match { it.amount == 50.0 })
        }
    }

    @Test
    fun `nota personalizada se concatena con la nota por defecto`() = runTest {
        val account = Account(id = 1, name = "Banco", currentBalance = 5000.0, type = "bank")
        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(satsAmount = 50_000, fiatAmount = 25.0, accountId = 1, isBuy = true, price = 50000.0, note = "DCA semanal")

        coVerify {
            transactionRepository.insertTransaction(match {
                it.note == "Compra Bitcoin (50000 sats) - DCA semanal"
            })
        }
    }

    @Test
    fun `nota nula usa solo la nota por defecto`() = runTest {
        val account = Account(id = 1, name = "Banco", currentBalance = 5000.0, type = "bank")
        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(satsAmount = 50_000, fiatAmount = 25.0, accountId = 1, isBuy = true, price = 50000.0, note = null)

        coVerify {
            transactionRepository.insertTransaction(match {
                it.note == "Compra Bitcoin (50000 sats)"
            })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cuenta no encontrada lanza IllegalArgumentException`() = runTest {
        coEvery { accountRepository.getAccountById(99) } returns null

        useCase(satsAmount = 100_000, fiatAmount = 50.0, accountId = 99, isBuy = true, price = 50000.0, note = null)
    }

    @Test
    fun `verifica las 3 operaciones atomicas insertHolding updateAccount insertTransaction`() = runTest {
        val account = Account(id = 1, name = "Banco", currentBalance = 5000.0, type = "bank")
        coEvery { accountRepository.getAccountById(1) } returns account

        useCase(satsAmount = 100_000, fiatAmount = 50.0, accountId = 1, isBuy = true, price = 50000.0, note = null)

        coVerify(exactly = 1) { bitcoinRepository.insertBitcoinHolding(any()) }
        coVerify(exactly = 1) { accountRepository.updateAccount(any()) }
        coVerify(exactly = 1) { transactionRepository.insertTransaction(any()) }
    }
}

package com.federico.moneytrack.domain.usecase.bitcoin

import com.federico.moneytrack.domain.exception.InsufficientBalanceException
import com.federico.moneytrack.domain.model.BitcoinHolding
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.CategoryRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import javax.inject.Inject

class AddBitcoinTransactionUseCase @Inject constructor(
    private val bitcoinRepository: BitcoinRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    /**
     * @param satsAmount Cantidad de Satoshis (siempre positivo).
     * @param fiatAmount Cantidad Fiat equivalente (siempre positivo).
     * @param accountId ID de la cuenta fiat involucrada.
     * @param isBuy True si es Compra (Swap In), False si es Venta (Swap Out).
     * @param price Precio actual del BTC en el momento de la operación.
     */
    suspend operator fun invoke(
        satsAmount: Long,
        fiatAmount: Double,
        accountId: Long,
        isBuy: Boolean,
        price: Double,
        note: String?
    ) {
        // 1. Obtener la cuenta fiat y validar saldo
        val account = accountRepository.getAccountById(accountId)
            ?: throw IllegalArgumentException("Account not found")

        if (isBuy) {
            // Compra BTC: verificar saldo fiat suficiente
            if (account.currentBalance < fiatAmount) {
                throw InsufficientBalanceException("Saldo insuficiente en la cuenta")
            }
        } else {
            // Venta BTC: verificar saldo de sats suficiente
            val totalSats = bitcoinRepository.getTotalSats()
            if (totalSats < satsAmount) {
                throw InsufficientBalanceException("Saldo insuficiente de Bitcoin")
            }
        }

        // 2. Afectar la cuenta Fiat (Account)
        val newBalance = if (isBuy) {
            account.currentBalance - fiatAmount
        } else {
            account.currentBalance + fiatAmount
        }
        accountRepository.updateAccount(account.copy(currentBalance = newBalance))

        // 3. Registrar la transacción Fiat para historial
        val btcCategory = categoryRepository.getCategoryByTransactionType("BITCOIN")
        val defaultNote = if (isBuy) "Compra Bitcoin ($satsAmount sats)" else "Venta Bitcoin ($satsAmount sats)"
        val finalNote = if (!note.isNullOrBlank()) "$defaultNote - $note" else defaultNote

        val transaction = Transaction(
            accountId = accountId,
            categoryId = btcCategory?.id,
            amount = if (isBuy) -fiatAmount else fiatAmount,
            date = System.currentTimeMillis(),
            note = finalNote
        )
        val transactionId = transactionRepository.insertTransaction(transaction)

        // 4. Registrar el movimiento de Bitcoin (Holding) vinculado a la transacción
        val finalSats = if (isBuy) satsAmount else -satsAmount

        val holding = BitcoinHolding(
            satsAmount = finalSats,
            lastFiatPrice = price,
            lastUpdate = System.currentTimeMillis(),
            transactionId = transactionId
        )
        bitcoinRepository.insertBitcoinHolding(holding)
    }
}

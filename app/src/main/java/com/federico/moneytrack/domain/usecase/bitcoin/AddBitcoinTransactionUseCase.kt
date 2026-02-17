package com.federico.moneytrack.domain.usecase.bitcoin

import com.federico.moneytrack.domain.model.BitcoinHolding
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import javax.inject.Inject

class AddBitcoinTransactionUseCase @Inject constructor(
    private val bitcoinRepository: BitcoinRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
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
        // 1. Registrar el movimiento de Bitcoin (Holding)
        // Por simplicidad, añadimos un nuevo registro de holding cada vez.
        // En un futuro, podríamos consolidar registros o usar UTXOs virtuales.
        // Si es compra, sats positivos. Si es venta, sats negativos.
        val finalSats = if (isBuy) satsAmount else -satsAmount
        
        val holding = BitcoinHolding(
            satsAmount = finalSats,
            lastFiatPrice = price,
            lastUpdate = System.currentTimeMillis()
        )
        bitcoinRepository.insertBitcoinHolding(holding)

        // 2. Afectar la cuenta Fiat (Account)
        val account = accountRepository.getAccountById(accountId)
            ?: throw IllegalArgumentException("Account not found")

        // Si compro BTC, gasto Fiat (resto). Si vendo BTC, recibo Fiat (sumo).
        val newBalance = if (isBuy) {
            account.currentBalance - fiatAmount
        } else {
            account.currentBalance + fiatAmount
        }
        accountRepository.updateAccount(account.copy(currentBalance = newBalance))

        // 3. Registrar la transacción Fiat para historial
        // Usaremos categoryId nulo o una categoría especial "Inversión/Bitcoin" si existiera.
        // Por ahora nulo.
        val defaultNote = if (isBuy) "Compra Bitcoin ($satsAmount sats)" else "Venta Bitcoin ($satsAmount sats)"
        val finalNote = if (!note.isNullOrBlank()) "$defaultNote - $note" else defaultNote

        val transaction = Transaction(
            accountId = accountId,
            categoryId = null,
            amount = if (isBuy) -fiatAmount else fiatAmount,
            date = System.currentTimeMillis(),
            note = finalNote
        )
        // Nota: El AddTransactionUseCase original ya actualiza saldo, pero aquí hemos actualizado
        // manualmente la cuenta para tener control atómico.
        // Así que insertamos la transacción directamente en el repositorio sin pasar por el UseCase
        // para no duplicar el cambio de saldo.
        transactionRepository.insertTransaction(transaction)
    }
}

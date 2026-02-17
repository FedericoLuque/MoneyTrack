package com.federico.moneytrack.domain.usecase.bitcoin

import com.federico.moneytrack.domain.model.BitcoinHolding
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteBitcoinTransactionUseCase @Inject constructor(
    private val bitcoinRepository: BitcoinRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Elimina un holding de Bitcoin y, si tiene transacción fiat vinculada,
     * revierte el saldo de la cuenta y borra la transacción.
     */
    suspend operator fun invoke(holding: BitcoinHolding) {
        val txId = holding.transactionId
        if (txId != null) {
            val transaction = transactionRepository.getTransactionById(txId)
            if (transaction != null) {
                val account = accountRepository.getAccountById(transaction.accountId)
                if (account != null) {
                    // Revertir saldo: transaction.amount es negativo en compra, positivo en venta
                    val newBalance = account.currentBalance - transaction.amount
                    accountRepository.updateAccount(account.copy(currentBalance = newBalance))
                }
                transactionRepository.deleteTransaction(transaction)
            }
        }
        bitcoinRepository.deleteBitcoinHolding(holding)
    }
}

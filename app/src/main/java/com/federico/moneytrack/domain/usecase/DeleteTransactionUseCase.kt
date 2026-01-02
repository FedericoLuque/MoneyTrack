package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Elimina una transacción y revierte el saldo de la cuenta asociada.
     * @param transaction La transacción a eliminar.
     * @param isExpense True si la transacción original era un gasto (se devolverá el dinero), 
     *                  False si era un ingreso (se restará el dinero).
     */
    suspend operator fun invoke(transaction: Transaction, isExpense: Boolean) {
        // 1. Obtener la cuenta actual
        val account = accountRepository.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("Account not found with id: ${transaction.accountId}")

        // 2. Calcular saldo revertido
        // Si era GASTO (restó), ahora debemos SUMAR para devolver el dinero.
        // Si era INGRESO (sumó), ahora debemos RESTAR para quitar el dinero erróneo.
        val newBalance = if (isExpense) {
            account.currentBalance + transaction.amount
        } else {
            account.currentBalance - transaction.amount
        }

        // 3. Actualizar la cuenta
        val updatedAccount = account.copy(currentBalance = newBalance)
        accountRepository.updateAccount(updatedAccount)

        // 4. Eliminar la transacción físicamente
        transactionRepository.deleteTransaction(transaction)
    }
}

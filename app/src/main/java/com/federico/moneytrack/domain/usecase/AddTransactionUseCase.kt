package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Inserta una transacción y actualiza el saldo de la cuenta asociada.
     * @param transaction La transacción a guardar.
     * @param isExpense True si es un gasto (resta), False si es un ingreso (suma).
     */
    suspend operator fun invoke(transaction: Transaction, isExpense: Boolean) {
        // 1. Guardar la transacción
        transactionRepository.insertTransaction(transaction)

        // 2. Obtener la cuenta actual
        val account = accountRepository.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("Account not found with id: ${transaction.accountId}")

        // 3. Calcular nuevo saldo
        val newBalance = if (isExpense) {
            account.currentBalance - transaction.amount
        } else {
            account.currentBalance + transaction.amount
        }

        // 4. Actualizar la cuenta
        val updatedAccount = account.copy(currentBalance = newBalance)
        accountRepository.updateAccount(updatedAccount)
    }
}

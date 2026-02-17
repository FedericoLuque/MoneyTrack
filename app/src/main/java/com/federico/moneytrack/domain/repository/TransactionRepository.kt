package com.federico.moneytrack.domain.repository

import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>
    fun getRecentTransactionsWithCategory(limit: Int): Flow<List<TransactionWithCategory>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(transaction: Transaction)
}

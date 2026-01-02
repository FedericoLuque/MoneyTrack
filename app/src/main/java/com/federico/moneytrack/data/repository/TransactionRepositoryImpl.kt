package com.federico.moneytrack.data.repository

import com.federico.moneytrack.data.local.dao.TransactionDao
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.federico.moneytrack.data.local.entity.Transaction as TransactionEntity

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return dao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction.toEntity())
    }

    // Mappers
    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            date = date,
            note = note
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            date = date,
            note = note
        )
    }
}

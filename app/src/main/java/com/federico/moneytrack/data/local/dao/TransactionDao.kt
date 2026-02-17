package com.federico.moneytrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import com.federico.moneytrack.data.local.entity.Transaction
import com.federico.moneytrack.data.local.pojo.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactionsWithCategory(limit: Int): Flow<List<TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET category_id = :categoryId WHERE category_id IS NULL")
    suspend fun updateNullCategoryTransactions(categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
}
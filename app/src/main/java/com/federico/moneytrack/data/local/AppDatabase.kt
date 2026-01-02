package com.federico.moneytrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.federico.moneytrack.data.local.dao.*
import com.federico.moneytrack.data.local.entity.*

@Database(
    entities = [
        Account::class,
        Category::class,
        Budget::class,
        Transaction::class,
        BitcoinHolding::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun bitcoinHoldingDao(): BitcoinHoldingDao
}

package com.federico.moneytrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun bitcoinHoldingDao(): BitcoinHoldingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bitcoin_holdings ADD COLUMN transaction_id INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bitcoin_holdings ADD COLUMN platform TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bitcoin_holdings ADD COLUMN commission REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}

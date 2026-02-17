package com.federico.moneytrack.data.local

import com.federico.moneytrack.data.local.dao.AccountDao
import com.federico.moneytrack.data.local.dao.CategoryDao
import com.federico.moneytrack.data.local.dao.TransactionDao
import com.federico.moneytrack.data.local.entity.Account
import com.federico.moneytrack.data.local.entity.Category
import javax.inject.Inject

class DataSeeder @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    suspend fun seedIfEmpty() {
        if (accountDao.getAccountCount() == 0) {
            seedAccounts()
        }
        if (categoryDao.getCategoryCount() == 0) {
            seedCategories()
        }
        ensureBitcoinCategory()
    }

    private suspend fun ensureBitcoinCategory() {
        val existing = categoryDao.getCategoryByTransactionType("BITCOIN")
        val categoryId = if (existing != null) {
            existing.id
        } else {
            categoryDao.insertCategoryReturnId(
                Category(
                    name = "Bitcoin",
                    iconName = "ic_bitcoin",
                    colorHex = "#EF6C00",
                    transactionType = "BITCOIN"
                )
            )
        }
        transactionDao.updateNullCategoryTransactions(categoryId)
    }

    private suspend fun seedAccounts() {
        val defaultAccount = Account(
            name = "Efectivo",
            currentBalance = 0.0,
            type = "Efectivo"
        )
        accountDao.insertAccount(defaultAccount)
    }

    private suspend fun seedCategories() {
        val categories = listOf(
            Category(name = "Salario", iconName = "ic_salary", colorHex = "#4CAF50", transactionType = "INCOME"),
            Category(name = "Comida", iconName = "ic_food", colorHex = "#F44336", transactionType = "EXPENSE"),
            Category(name = "Transporte", iconName = "ic_transport", colorHex = "#2196F3", transactionType = "EXPENSE"),
            Category(name = "Ocio", iconName = "ic_leisure", colorHex = "#9C27B0", transactionType = "EXPENSE"),
            Category(name = "Hogar", iconName = "ic_home", colorHex = "#FF9800", transactionType = "EXPENSE"),
            Category(name = "Salud", iconName = "ic_health", colorHex = "#E91E63", transactionType = "EXPENSE")
        )
        categories.forEach { categoryDao.insertCategory(it) }
    }
}

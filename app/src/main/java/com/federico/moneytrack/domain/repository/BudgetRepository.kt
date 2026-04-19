package com.federico.moneytrack.domain.repository

import com.federico.moneytrack.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<Budget>>
    suspend fun insertBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
}

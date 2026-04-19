package com.federico.moneytrack.data.repository

import com.federico.moneytrack.data.local.dao.BudgetDao
import com.federico.moneytrack.data.local.entity.Budget as BudgetEntity
import com.federico.moneytrack.domain.model.Budget
import com.federico.moneytrack.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> =
        budgetDao.getAllBudgets().map { list -> list.map { it.toDomain() } }

    override suspend fun insertBudget(budget: Budget) =
        budgetDao.insertBudget(budget.toEntity())

    override suspend fun deleteBudget(budget: Budget) =
        budgetDao.deleteBudget(budget.toEntity())

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        categoryId = categoryId,
        limitAmount = limitAmount,
        periodMonth = periodMonth,
        periodYear = periodYear
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        categoryId = categoryId,
        limitAmount = limitAmount,
        periodMonth = periodMonth,
        periodYear = periodYear
    )
}

package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.repository.BudgetRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class BudgetAlertChecker @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationHelper: NotificationHelper
) {
    suspend fun checkAndAlert() {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        val startOfMonth = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfMonth = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val budgets = budgetRepository.getAllBudgets().first()
            .filter { it.periodMonth == currentMonth && it.periodYear == currentYear }

        if (budgets.isEmpty()) return

        val transactions = transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth).first()

        budgets.forEach { budget ->
            val categoryName = transactions
                .firstOrNull { it.category?.id == budget.categoryId }
                ?.category?.name ?: return@forEach

            val spending = transactions
                .filter { it.category?.id == budget.categoryId && it.category?.transactionType == "EXPENSE" }
                .sumOf { it.transaction.amount }

            if (budget.limitAmount <= 0.0) return@forEach

            val percentage = (spending / budget.limitAmount * 100).toInt()
            val notificationId = NotificationHelper.BUDGET_NOTIFICATION_ID_BASE + budget.id.toInt()

            when {
                percentage >= 100 -> notificationHelper.showBudgetExceededNotification(categoryName, percentage, notificationId)
                percentage >= 80 -> notificationHelper.showBudgetWarningNotification(categoryName, percentage, notificationId)
            }
        }
    }
}

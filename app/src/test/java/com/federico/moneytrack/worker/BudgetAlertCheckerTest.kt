package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.model.Budget
import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.BudgetRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class BudgetAlertCheckerTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var checker: BudgetAlertChecker

    @Before
    fun setup() {
        budgetRepository = mockk()
        transactionRepository = mockk()
        notificationHelper = mockk(relaxed = true)
        checker = BudgetAlertChecker(budgetRepository, transactionRepository, notificationHelper)
    }

    @Test
    fun `no notifica cuando no hay presupuestos`() = runTest {
        coEvery { budgetRepository.getAllBudgets() } returns flowOf(emptyList())
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(emptyList())

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBudgetWarningNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationHelper.showBudgetExceededNotification(any(), any(), any()) }
    }

    @Test
    fun `notifica advertencia cuando el gasto esta entre 80 y 100 por ciento`() = runTest {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val year = now.get(Calendar.YEAR)

        val category = Category(id = 10L, name = "Comida", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val budget = Budget(id = 1L, categoryId = 10L, limitAmount = 100.0, periodMonth = month, periodYear = year)
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 1L, accountId = 1L, categoryId = 10L, amount = 85.0, date = System.currentTimeMillis(), note = null),
            category = category
        )

        coEvery { budgetRepository.getAllBudgets() } returns flowOf(listOf(budget))
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndAlert()

        verify { notificationHelper.showBudgetWarningNotification("Comida", 85, any()) }
    }

    @Test
    fun `notifica superacion cuando el gasto es igual o mayor al 100 por ciento`() = runTest {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val year = now.get(Calendar.YEAR)

        val category = Category(id = 20L, name = "Ocio", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val budget = Budget(id = 2L, categoryId = 20L, limitAmount = 50.0, periodMonth = month, periodYear = year)
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 2L, accountId = 1L, categoryId = 20L, amount = 60.0, date = System.currentTimeMillis(), note = null),
            category = category
        )

        coEvery { budgetRepository.getAllBudgets() } returns flowOf(listOf(budget))
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndAlert()

        verify { notificationHelper.showBudgetExceededNotification("Ocio", 120, any()) }
    }

    @Test
    fun `no notifica cuando el gasto esta por debajo del 80 por ciento`() = runTest {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val year = now.get(Calendar.YEAR)

        val category = Category(id = 30L, name = "Transporte", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val budget = Budget(id = 3L, categoryId = 30L, limitAmount = 100.0, periodMonth = month, periodYear = year)
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 3L, accountId = 1L, categoryId = 30L, amount = 50.0, date = System.currentTimeMillis(), note = null),
            category = category
        )

        coEvery { budgetRepository.getAllBudgets() } returns flowOf(listOf(budget))
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBudgetWarningNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationHelper.showBudgetExceededNotification(any(), any(), any()) }
    }
}

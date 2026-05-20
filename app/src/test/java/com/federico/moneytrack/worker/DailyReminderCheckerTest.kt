package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DailyReminderCheckerTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var checker: DailyReminderChecker

    @Before
    fun setup() {
        transactionRepository = mockk()
        notificationHelper = mockk(relaxed = true)
        checker = DailyReminderChecker(transactionRepository, notificationHelper)
    }

    @Test
    fun `envia notificacion cuando no hay transacciones en el dia`() = runTest {
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(emptyList())

        checker.checkAndNotify()

        verify { notificationHelper.showDailyReminderNotification() }
    }

    @Test
    fun `no envia notificacion cuando ya hay transacciones en el dia`() = runTest {
        val category = Category(id = 1L, name = "Comida", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 1L, accountId = 1L, categoryId = 1L, amount = 20.0, date = System.currentTimeMillis(), note = null),
            category = category
        )
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndNotify()

        verify(exactly = 0) { notificationHelper.showDailyReminderNotification() }
    }
}

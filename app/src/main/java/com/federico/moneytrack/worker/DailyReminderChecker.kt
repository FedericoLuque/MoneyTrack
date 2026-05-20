package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class DailyReminderChecker @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notificationHelper: NotificationHelper
) {
    suspend fun checkAndNotify() {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val transactions = transactionRepository.getTransactionsByDateRange(startOfDay, endOfDay).first()

        if (transactions.isEmpty()) {
            notificationHelper.showDailyReminderNotification()
        }
    }
}

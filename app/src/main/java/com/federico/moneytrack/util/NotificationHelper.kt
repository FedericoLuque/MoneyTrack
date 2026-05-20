package com.federico.moneytrack.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.federico.moneytrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas MoneyTrack",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alertas de presupuesto, recordatorios y Bitcoin"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showBudgetWarningNotification(categoryName: String, percentage: Int, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Presupuesto al $percentage%")
            .setContentText("Estás al $percentage% de tu presupuesto de $categoryName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun showBudgetExceededNotification(categoryName: String, percentage: Int, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Presupuesto superado")
            .setContentText("Superaste tu presupuesto de $categoryName ($percentage%)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun showDailyReminderNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MoneyTrack")
            .setContentText("¿Ya registraste tus gastos de hoy?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification)
    }

    fun showBitcoinAlertNotification(currentPrice: Double, changePercent: Double) {
        val direction = if (changePercent > 0) "↑" else "↓"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alerta Bitcoin $direction")
            .setContentText("Bitcoin: €${"%.0f".format(currentPrice)} ($direction${"%.1f".format(Math.abs(changePercent))}%)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_BITCOIN, notification)
    }

    companion object {
        const val CHANNEL_ID = "moneytrack_alerts"
        const val NOTIFICATION_ID_REMINDER = 2001
        const val NOTIFICATION_ID_BITCOIN = 3001
        const val BUDGET_NOTIFICATION_ID_BASE = 1000
    }
}

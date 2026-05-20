package com.federico.moneytrack.worker

import android.content.SharedPreferences
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.util.NotificationHelper
import javax.inject.Inject
import kotlin.math.abs

class BitcoinPriceAlertChecker @Inject constructor(
    private val bitcoinRepository: BitcoinRepository,
    private val prefs: SharedPreferences,
    private val notificationHelper: NotificationHelper
) {
    suspend fun checkAndAlert() {
        val currentPrice = try {
            bitcoinRepository.getBitcoinPrice("eur")
        } catch (e: Exception) {
            return
        }

        if (currentPrice <= 0.0) return

        val baselinePrice = Double.fromBits(prefs.getLong(KEY_BASELINE_PRICE, 0L))
        val threshold = prefs.getFloat(KEY_ALERT_THRESHOLD, 5f).toDouble()

        if (baselinePrice <= 0.0) {
            prefs.edit().putLong(KEY_BASELINE_PRICE, currentPrice.toBits()).apply()
            return
        }

        val changePercent = (currentPrice - baselinePrice) / baselinePrice * 100

        if (abs(changePercent) >= threshold) {
            notificationHelper.showBitcoinAlertNotification(currentPrice, changePercent)
            prefs.edit().putLong(KEY_BASELINE_PRICE, currentPrice.toBits()).apply()
        }
    }

    companion object {
        const val KEY_BASELINE_PRICE = "bitcoin_alert_baseline"
        const val KEY_ALERT_THRESHOLD = "bitcoin_alert_threshold"
    }
}

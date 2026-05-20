package com.federico.moneytrack.worker

import android.content.SharedPreferences
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BitcoinPriceAlertCheckerTest {

    private lateinit var bitcoinRepository: BitcoinRepository
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var checker: BitcoinPriceAlertChecker

    @Before
    fun setup() {
        bitcoinRepository = mockk()
        prefs = mockk()
        editor = mockk()
        notificationHelper = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.apply() } just Runs
        checker = BitcoinPriceAlertChecker(bitcoinRepository, prefs, notificationHelper)
    }

    @Test
    fun `guarda baseline en primer uso sin notificar`() = runTest {
        every { prefs.getLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0L) } returns 0L
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 50000.0

        checker.checkAndAlert()

        verify { editor.putLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 50000.0.toBits()) }
        verify(exactly = 0) { notificationHelper.showBitcoinAlertNotification(any(), any()) }
    }

    @Test
    fun `notifica cuando la variacion supera el umbral positivo`() = runTest {
        every { prefs.getLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0L) } returns 50000.0.toBits()
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 53000.0 // +6%

        checker.checkAndAlert()

        verify { notificationHelper.showBitcoinAlertNotification(53000.0, 6.0) }
        verify { editor.putLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 53000.0.toBits()) }
    }

    @Test
    fun `notifica cuando la variacion supera el umbral negativo`() = runTest {
        every { prefs.getLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0L) } returns 50000.0.toBits()
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 46000.0 // -8%

        checker.checkAndAlert()

        verify { notificationHelper.showBitcoinAlertNotification(46000.0, -8.0) }
        verify { editor.putLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 46000.0.toBits()) }
    }

    @Test
    fun `no notifica cuando la variacion esta por debajo del umbral`() = runTest {
        every { prefs.getLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0L) } returns 50000.0.toBits()
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 51000.0 // +2%

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBitcoinAlertNotification(any(), any()) }
    }

    @Test
    fun `no notifica si la API devuelve precio cero`() = runTest {
        every { prefs.getLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0L) } returns 50000.0.toBits()
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 0.0

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBitcoinAlertNotification(any(), any()) }
    }

    @Test
    fun `no hace nada si la API lanza excepcion`() = runTest {
        every { prefs.getLong(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0L) } returns 50000.0.toBits()
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } throws RuntimeException("network error")

        checker.checkAndAlert()

        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { notificationHelper.showBitcoinAlertNotification(any(), any()) }
    }
}

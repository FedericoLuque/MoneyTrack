package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.model.BitcoinHolding
import com.federico.moneytrack.domain.repository.BitcoinRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetBitcoinValueUseCaseTest {

    private lateinit var repository: BitcoinRepository
    private lateinit var useCase: GetBitcoinValueUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetBitcoinValueUseCase(repository)
    }

    @Test
    fun `calcula valor correcto totalSats dividido 100M por precio`() = runTest {
        val holdings = listOf(
            BitcoinHolding(id = 1, satsAmount = 50_000_000, lastFiatPrice = 50000.0, lastUpdate = 0L),
            BitcoinHolding(id = 2, satsAmount = 50_000_000, lastFiatPrice = 50000.0, lastUpdate = 0L)
        )
        // Total = 100_000_000 sats = 1 BTC, precio = 60000 EUR -> valor = 60000.0
        coEvery { repository.getBitcoinPrice("eur") } returns 60000.0
        every { repository.getBitcoinHoldings() } returns flowOf(holdings)

        val result = useCase("eur").first()

        assertEquals(60000.0, result, 0.01)
    }

    @Test
    fun `error de red retorna valor 0`() = runTest {
        val holdings = listOf(
            BitcoinHolding(id = 1, satsAmount = 100_000_000, lastFiatPrice = 50000.0, lastUpdate = 0L)
        )
        coEvery { repository.getBitcoinPrice("eur") } throws Exception("Error de red")
        every { repository.getBitcoinHoldings() } returns flowOf(holdings)

        val result = useCase("eur").first()

        assertEquals(0.0, result, 0.01)
    }
}

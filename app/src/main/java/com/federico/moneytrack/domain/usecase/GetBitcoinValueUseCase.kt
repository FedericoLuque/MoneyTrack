package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.repository.BitcoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetBitcoinValueUseCase @Inject constructor(
    private val repository: BitcoinRepository
) {
    /**
     * Devuelve el valor total de los satoshis en la moneda especificada (por defecto USD).
     * Emite el valor calculado.
     */
    operator fun invoke(currency: String = "usd"): Flow<Double> = flow {
        // 1. Obtener precio actual
        val price = try {
            repository.getBitcoinPrice(currency)
        } catch (e: Exception) {
            0.0 // Si falla la red, asumimos 0 por ahora (o podríamos usar último precio guardado)
        }

        // 2. Obtener total de sats y calcular valor
        repository.getBitcoinHoldings().collect { holdings ->
            val totalSats = holdings.sumOf { it.satsAmount }
            // 1 Bitcoin = 100,000,000 Sats
            val totalBtc = totalSats.toDouble() / 100_000_000.0
            emit(totalBtc * price)
        }
    }
}

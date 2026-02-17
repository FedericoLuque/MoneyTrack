package com.federico.moneytrack.data.repository

import com.federico.moneytrack.data.local.dao.BitcoinHoldingDao
import com.federico.moneytrack.domain.model.BitcoinHolding
import com.federico.moneytrack.domain.repository.BitcoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import com.federico.moneytrack.data.local.entity.BitcoinHolding as BitcoinEntity

class BitcoinRepositoryImpl @Inject constructor(
    private val dao: BitcoinHoldingDao,
    private val api: com.federico.moneytrack.data.remote.CoinGeckoApi
) : BitcoinRepository {

    private val priceMutex = Mutex()
    private var cachedPrice: Double? = null
    private var cachedCurrency: String? = null
    private var cacheTimestamp: Long = 0L

    companion object {
        private const val CACHE_DURATION_MS = 600_000L // 10 minutos
    }

    override fun getBitcoinHoldings(): Flow<List<BitcoinHolding>> {
        return dao.getBitcoinHoldings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBitcoinPrice(currency: String): Double {
        val now = System.currentTimeMillis()
        val currencyLower = currency.lowercase()

        priceMutex.withLock {
            val cached = cachedPrice
            if (cached != null && cachedCurrency == currencyLower && now - cacheTimestamp < CACHE_DURATION_MS) {
                return cached
            }

            val response = api.getBitcoinPrice(vsCurrencies = currencyLower)
            val price = when (currencyLower) {
                "eur" -> response.bitcoin.eur ?: 0.0
                else -> response.bitcoin.usd ?: 0.0
            }

            cachedPrice = price
            cachedCurrency = currencyLower
            cacheTimestamp = now
            return price
        }
    }

    override suspend fun getTotalSats(): Long = dao.getTotalSats()

    override suspend fun insertBitcoinHolding(holding: BitcoinHolding) {
        dao.insertBitcoinHolding(holding.toEntity())
    }

    override suspend fun deleteBitcoinHolding(holding: BitcoinHolding) {
        dao.deleteBitcoinHolding(holding.toEntity())
    }

    override suspend fun getHoldingByTransactionId(transactionId: Long): BitcoinHolding? {
        return dao.getHoldingByTransactionId(transactionId)?.toDomain()
    }

    // Mappers
    private fun BitcoinEntity.toDomain(): BitcoinHolding {
        return BitcoinHolding(
            id = id,
            satsAmount = satsAmount,
            lastFiatPrice = lastFiatPrice,
            lastUpdate = lastUpdate,
            transactionId = transactionId
        )
    }

    private fun BitcoinHolding.toEntity(): BitcoinEntity {
        return BitcoinEntity(
            id = id,
            satsAmount = satsAmount,
            lastFiatPrice = lastFiatPrice,
            lastUpdate = lastUpdate,
            transactionId = transactionId
        )
    }
}

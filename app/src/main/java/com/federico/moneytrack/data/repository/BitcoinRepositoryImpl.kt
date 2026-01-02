package com.federico.moneytrack.data.repository

import com.federico.moneytrack.data.local.dao.BitcoinHoldingDao
import com.federico.moneytrack.domain.model.BitcoinHolding
import com.federico.moneytrack.domain.repository.BitcoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.federico.moneytrack.data.local.entity.BitcoinHolding as BitcoinEntity

class BitcoinRepositoryImpl @Inject constructor(
    private val dao: BitcoinHoldingDao
) : BitcoinRepository {

    override fun getBitcoinHoldings(): Flow<List<BitcoinHolding>> {
        return dao.getBitcoinHoldings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertBitcoinHolding(holding: BitcoinHolding) {
        dao.insertBitcoinHolding(holding.toEntity())
    }

    override suspend fun deleteBitcoinHolding(holding: BitcoinHolding) {
        dao.deleteBitcoinHolding(holding.toEntity())
    }

    // Mappers
    private fun BitcoinEntity.toDomain(): BitcoinHolding {
        return BitcoinHolding(
            id = id,
            satsAmount = satsAmount,
            lastFiatPrice = lastFiatPrice,
            lastUpdate = lastUpdate
        )
    }

    private fun BitcoinHolding.toEntity(): BitcoinEntity {
        return BitcoinEntity(
            id = id,
            satsAmount = satsAmount,
            lastFiatPrice = lastFiatPrice,
            lastUpdate = lastUpdate
        )
    }
}

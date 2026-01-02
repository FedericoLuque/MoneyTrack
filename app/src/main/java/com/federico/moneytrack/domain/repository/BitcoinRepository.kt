package com.federico.moneytrack.domain.repository

import com.federico.moneytrack.domain.model.BitcoinHolding
import kotlinx.coroutines.flow.Flow

interface BitcoinRepository {
    fun getBitcoinHoldings(): Flow<List<BitcoinHolding>>
    suspend fun insertBitcoinHolding(holding: BitcoinHolding)
    suspend fun deleteBitcoinHolding(holding: BitcoinHolding)
}

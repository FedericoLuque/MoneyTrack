package com.federico.moneytrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.federico.moneytrack.data.local.entity.BitcoinHolding
import kotlinx.coroutines.flow.Flow

@Dao
interface BitcoinHoldingDao {
    @Query("SELECT * FROM bitcoin_holdings")
    fun getBitcoinHoldings(): Flow<List<BitcoinHolding>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBitcoinHolding(holding: BitcoinHolding)

    @Update
    suspend fun updateBitcoinHolding(holding: BitcoinHolding)

    @Delete
    suspend fun deleteBitcoinHolding(holding: BitcoinHolding)

    @Query("SELECT COALESCE(SUM(sats_amount), 0) FROM bitcoin_holdings")
    suspend fun getTotalSats(): Long

    @Query("SELECT * FROM bitcoin_holdings WHERE transaction_id = :transactionId")
    suspend fun getHoldingByTransactionId(transactionId: Long): BitcoinHolding?

    @Query("DELETE FROM bitcoin_holdings")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holdings: List<BitcoinHolding>)
}

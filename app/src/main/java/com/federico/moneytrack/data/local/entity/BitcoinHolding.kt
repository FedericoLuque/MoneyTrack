package com.federico.moneytrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "bitcoin_holdings")
data class BitcoinHolding(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sats_amount")
    val satsAmount: Long,
    @ColumnInfo(name = "last_fiat_price")
    val lastFiatPrice: Double,
    @ColumnInfo(name = "last_update")
    val lastUpdate: Long,
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long? = null,
    @ColumnInfo(name = "platform")
    val platform: String? = null,
    @ColumnInfo(name = "commission")
    val commission: Double = 0.0
)

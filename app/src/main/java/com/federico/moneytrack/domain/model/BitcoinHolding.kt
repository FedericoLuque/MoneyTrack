package com.federico.moneytrack.domain.model

data class BitcoinHolding(
    val id: Long = 0,
    val satsAmount: Long,
    val lastFiatPrice: Double,
    val lastUpdate: Long,
    val transactionId: Long? = null
)

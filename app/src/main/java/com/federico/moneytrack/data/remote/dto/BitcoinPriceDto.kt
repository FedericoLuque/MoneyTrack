package com.federico.moneytrack.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BitcoinPriceDto(
    @SerializedName("bitcoin")
    val bitcoin: PriceMap
)

data class PriceMap(
    @SerializedName("usd")
    val usd: Double?,
    @SerializedName("eur")
    val eur: Double?
)

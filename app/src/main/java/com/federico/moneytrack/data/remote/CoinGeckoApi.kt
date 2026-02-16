package com.federico.moneytrack.data.remote

import com.federico.moneytrack.data.remote.dto.BitcoinPriceDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoApi {
    @GET("simple/price")
    suspend fun getBitcoinPrice(
        @Query("ids") ids: String = "bitcoin",
        @Query("vs_currencies") vsCurrencies: String = "usd,eur"
    ): BitcoinPriceDto

    companion object {
        const val BASE_URL = "https://api.coingecko.com/api/v3/"
    }
}

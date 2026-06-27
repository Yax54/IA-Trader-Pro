package com.privateinvest.aitraderpro.network

import com.privateinvest.aitraderpro.BuildConfig
import com.privateinvest.aitraderpro.data.model.GlobalQuoteResponse
import com.privateinvest.aitraderpro.data.model.SymbolSearchResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface MarketApiService {
    @GET("query")
    suspend fun getQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_API_KEY
    ): GlobalQuoteResponse

    @GET("query")
    suspend fun getHistorical(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("outputsize") outputSize: String = "compact",
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_API_KEY
    ): JsonObject

    @GET("query")
    suspend fun searchAsset(
        @Query("function") function: String = "SYMBOL_SEARCH",
        @Query("keywords") keywords: String,
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_API_KEY
    ): SymbolSearchResponse
}

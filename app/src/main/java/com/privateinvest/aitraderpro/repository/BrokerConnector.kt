package com.privateinvest.aitraderpro.repository

interface BrokerConnector {
    suspend fun buy(symbol: String, quantity: Double)
    suspend fun sell(symbol: String, quantity: Double)
    suspend fun getPositions(): List<String>
}

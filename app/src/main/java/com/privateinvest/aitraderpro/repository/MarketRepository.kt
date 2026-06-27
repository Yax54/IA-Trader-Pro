package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.data.model.AssetDetail
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.data.model.AuditSummary
import com.privateinvest.aitraderpro.data.model.LearningStats
import com.privateinvest.aitraderpro.data.model.NotificationHistoryItem
import com.privateinvest.aitraderpro.data.model.PortfolioSummary
import com.privateinvest.aitraderpro.data.model.RiskRule
import com.privateinvest.aitraderpro.data.model.SimulationPositionItem
import com.privateinvest.aitraderpro.data.model.SimulationSummary
import com.privateinvest.aitraderpro.data.model.SimulationTradeItem
import com.privateinvest.aitraderpro.data.model.SymbolSearchMatchDto

interface MarketRepository {
    suspend fun getWatchlist(): List<AssetSignal>
    suspend fun getPortfolio(): PortfolioSummary
    suspend fun getSimulationSummary(): SimulationSummary
    suspend fun getSimulationPositions(): List<SimulationPositionItem>
    suspend fun getSimulationHistory(): List<SimulationTradeItem>
    suspend fun getNotificationHistory(): List<NotificationHistoryItem>
    suspend fun getSignals(): List<AssetSignal>
    suspend fun getMarketData(symbol: String): AssetDetail?
    suspend fun getAuditSummary(): AuditSummary
    suspend fun getLearningStats(): LearningStats
    suspend fun getRiskRules(): List<RiskRule>
    suspend fun getLatestAlert(): AssetSignal?
    suspend fun searchAsset(keywords: String): List<SymbolSearchMatchDto>
    suspend fun confirmSimulationOrder(symbol: String): Boolean
    suspend fun sellSimulationPosition(symbol: String): Boolean
}

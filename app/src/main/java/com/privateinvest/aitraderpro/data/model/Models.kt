package com.privateinvest.aitraderpro.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GlobalQuoteResponse(
    @SerialName("Global Quote") val globalQuote: GlobalQuoteDto? = null
)

@Serializable
data class GlobalQuoteDto(
    @SerialName("01. symbol") val symbol: String? = null,
    @SerialName("05. price") val price: String? = null,
    @SerialName("06. volume") val volume: String? = null,
    @SerialName("09. change") val change: String? = null,
    @SerialName("10. change percent") val changePercent: String? = null
)

@Serializable
data class SymbolSearchResponse(
    @SerialName("bestMatches") val bestMatches: List<SymbolSearchMatchDto> = emptyList()
)

@Serializable
data class SymbolSearchMatchDto(
    @SerialName("1. symbol") val symbol: String? = null,
    @SerialName("2. name") val name: String? = null,
    @SerialName("4. region") val region: String? = null,
    @SerialName("8. currency") val currency: String? = null
)

enum class SignalAction { ACHETER, SURVEILLER, CONSERVER, VENDRE }
enum class AppMode { SIMULATION, REEL }

data class PriceBar(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class TechnicalSnapshot(
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    val ema20: Double,
    val ema50: Double,
    val ema200: Double,
    val atr14: Double,
    val volatility: Double,
    val averageVolume: Double,
    val currentVolume: Double
)

data class SignalExplanation(
    val reasons: List<String>,
    val similarConfigurations: Int = 0,
    val historicalWinRate: Double = 0.0,
    val averageGain: Double = 0.0,
    val averageLoss: Double = 0.0,
    val maxImpactPoints: Int = 0
)

data class ScoringResult(
    val scoreTechnique: Int,
    val scoreRisque: Int,
    val signal: SignalAction,
    val explanation: SignalExplanation
)

data class PortfolioSummary(
    val capital: String,
    val dayPnL: String,
    val monthPnL: String,
    val openPositions: Int,
    val riskLevel: String
)

data class SimulationSummary(
    val initialCapital: String,
    val availableCash: String,
    val investedCapital: String,
    val totalValue: String,
    val totalPerformance: String,
    val openPositions: Int,
    val successRateJ1: String,
    val successRateJ7: String,
    val successRateJ30: String
)

data class SimulationPositionItem(
    val symbol: String,
    val quantity: Double,
    val averagePrice: String,
    val marketPrice: String,
    val marketValue: String,
    val performance: String
)

data class SimulationTradeItem(
    val symbol: String,
    val side: String,
    val quantity: Double,
    val price: String,
    val createdAtLabel: String,
    val followUp: String
)

data class NotificationHistoryItem(
    val title: String,
    val message: String,
    val level: String,
    val symbol: String?,
    val createdAtLabel: String,
    val status: String
)

data class AssetSignal(
    val symbol: String,
    val name: String,
    val score: Int,
    val confidence: Int,
    val target: String,
    val risk: String,
    val action: SignalAction,
    val explanation: List<String>
)

data class RiskRule(
    val title: String,
    val value: String
)

data class TopConfiguration(
    val label: String,
    val occurrences: Int,
    val winRate: Double,
    val averagePerformance: Double
)

data class LearningStats(
    val memorizedConfigurations: Int = 0,
    val successRateJ1: Double = 0.0,
    val successRateJ7: Double = 0.0,
    val successRateJ30: Double = 0.0,
    val topWinningConfigurations: List<TopConfiguration> = emptyList(),
    val topLosingConfigurations: List<TopConfiguration> = emptyList(),
    val weightEvolution: List<String> = emptyList()
)

data class AuditSummary(
    val signalCount: Int,
    val winRate: Double,
    val averageGain: Double,
    val averageLoss: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val currentWeights: Map<String, Double>,
    val learningStats: LearningStats = LearningStats()
)

data class AssetDetail(
    val symbol: String,
    val name: String,
    val quotePrice: Double,
    val scoring: ScoringResult,
    val technicalSnapshot: TechnicalSnapshot
)

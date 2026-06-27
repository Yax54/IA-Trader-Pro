package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.data.model.TechnicalSnapshot
import com.privateinvest.aitraderpro.database.SignalMemoryEntity
import com.privateinvest.aitraderpro.database.SignalOutcomeEntity
import kotlin.math.abs
import kotlin.math.min

data class PatternInsight(
    val similarCount: Int,
    val historicalWinRate: Double,
    val historicalAverageGain: Double,
    val historicalAverageLoss: Double,
    val maxImpactPoints: Int
)

object PatternEvaluator {
    fun findSimilarPatterns(
        snapshot: TechnicalSnapshot,
        memories: List<SignalMemoryEntity>,
        maxDistance: Double = 1.55
    ): List<SignalMemoryEntity> = memories.filter { computeDistance(snapshot, it) <= maxDistance }

    fun computeHistoricalWinRate(
        similarPatterns: List<SignalMemoryEntity>,
        outcomesBySignalId: Map<Long, SignalOutcomeEntity>
    ): Double {
        val successes = similarPatterns.mapNotNull { outcomesBySignalId[it.id]?.preferredSuccess() }
        if (successes.isEmpty()) return 0.0
        return successes.count { it } * 100.0 / successes.size
    }

    fun computeHistoricalAverageGain(
        similarPatterns: List<SignalMemoryEntity>,
        outcomesBySignalId: Map<Long, SignalOutcomeEntity>
    ): Double {
        val gains = similarPatterns.mapNotNull { outcomesBySignalId[it.id]?.preferredPerformance() }
            .filter { it > 0 }
        return if (gains.isEmpty()) 0.0 else gains.average()
    }

    fun computeHistoricalAverageLoss(
        similarPatterns: List<SignalMemoryEntity>,
        outcomesBySignalId: Map<Long, SignalOutcomeEntity>
    ): Double {
        val losses = similarPatterns.mapNotNull { outcomesBySignalId[it.id]?.preferredPerformance() }
            .filter { it < 0 }
        return if (losses.isEmpty()) 0.0 else losses.average()
    }

    fun evaluate(
        snapshot: TechnicalSnapshot,
        memories: List<SignalMemoryEntity>,
        outcomesBySignalId: Map<Long, SignalOutcomeEntity>
    ): PatternInsight {
        val similar = findSimilarPatterns(snapshot, memories)
        val winRate = computeHistoricalWinRate(similar, outcomesBySignalId)
        val avgGain = computeHistoricalAverageGain(similar, outcomesBySignalId)
        val avgLoss = computeHistoricalAverageLoss(similar, outcomesBySignalId)
        val baseImpact = when {
            similar.size >= 15 -> 5
            similar.size >= 10 -> 4
            similar.size >= 6 -> 3
            similar.size >= 3 -> 2
            similar.isNotEmpty() -> 1
            else -> 0
        }
        val signedImpact = when {
            winRate >= 65.0 -> baseImpact
            winRate <= 40.0 -> -baseImpact
            else -> 0
        }
        return PatternInsight(
            similarCount = similar.size,
            historicalWinRate = winRate,
            historicalAverageGain = avgGain,
            historicalAverageLoss = avgLoss,
            maxImpactPoints = signedImpact.coerceIn(-5, 5)
        )
    }

    private fun computeDistance(snapshot: TechnicalSnapshot, memory: SignalMemoryEntity): Double {
        val rsiDistance = abs(snapshot.rsi - memory.rsi) / 100.0
        val macdDistance = normalizedDistance(snapshot.macd, memory.macd)
        val macdSignalDistance = normalizedDistance(snapshot.macdSignal, memory.macdSignal)
        val volatilityDistance = abs(snapshot.volatility - memory.volatility) / 100.0
        val atrDistance = normalizedDistance(snapshot.atr14, memory.atr14)
        val volumeRatioDistance = abs(
            volumeRatio(snapshot.currentVolume, snapshot.averageVolume) -
                volumeRatio(memory.currentVolume, memory.averageVolume)
        )

        val emaTrendDistance = listOf(
            Pair(snapshot.ema20 > snapshot.ema50, memory.ema20 > memory.ema50),
            Pair(snapshot.ema50 > snapshot.ema200, memory.ema50 > memory.ema200)
        ).count { pair -> pair.first != pair.second } * 0.25

        return rsiDistance + macdDistance + macdSignalDistance + volatilityDistance + atrDistance + volumeRatioDistance + emaTrendDistance
    }

    private fun normalizedDistance(a: Double, b: Double): Double {
        val denominator = maxOf(abs(a), abs(b), 1.0)
        return abs(a - b) / denominator
    }

    private fun volumeRatio(current: Double, average: Double): Double =
        if (average <= 0.0) 0.0 else min(current / average, 5.0)

    private fun SignalOutcomeEntity.preferredPerformance(): Double? = performanceJ30 ?: performanceJ7 ?: performanceJ1

    private fun SignalOutcomeEntity.preferredSuccess(): Boolean? = successJ30 ?: successJ7 ?: successJ1
}

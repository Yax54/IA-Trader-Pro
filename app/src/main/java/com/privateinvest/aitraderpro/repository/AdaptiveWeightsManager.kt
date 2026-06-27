package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import com.privateinvest.aitraderpro.database.SignalMemoryEntity
import com.privateinvest.aitraderpro.database.SignalOutcomeEntity
import com.privateinvest.aitraderpro.database.WeightHistoryDao
import com.privateinvest.aitraderpro.database.WeightHistoryEntity

class AdaptiveWeightsManager(
    private val weightHistoryDao: WeightHistoryDao
) {
    private val defaults = mapOf(
        "RSI" to 15.0,
        "MACD" to 15.0,
        "EMA" to 20.0,
        "Volume" to 15.0,
        "Volatilité" to 15.0,
        "Momentum" to 20.0
    )

    suspend fun ensureDefaults() {
        if (weightHistoryDao.getAll().isEmpty()) {
            val now = System.currentTimeMillis()
            weightHistoryDao.insertAll(defaults.map { (factor, value) ->
                WeightHistoryEntity(factor = factor, value = value, recordedAt = now)
            })
        }
    }

    suspend fun currentWeights(): Map<String, Double> {
        ensureDefaults()
        return latestWeights()
    }

    suspend fun updateWeightsFromResults(
        memories: List<SignalMemoryEntity>,
        outcomes: List<SignalOutcomeEntity>
    ): Map<String, Double> {
        ensureDefaults()
        val current = latestWeights().toMutableMap()
        val outcomesById = outcomes.associateBy { it.signalId }
        val matured = memories.mapNotNull { memory ->
            val outcome = outcomesById[memory.id] ?: return@mapNotNull null
            val performance = outcome.performanceJ30 ?: outcome.performanceJ7 ?: outcome.performanceJ1 ?: return@mapNotNull null
            memory to performance
        }
        if (matured.isEmpty()) return current

        val winning = matured.filter { it.second > 0.0 }.map { it.first }
        val losing = matured.filter { it.second < 0.0 }.map { it.first }
        val changes = mutableMapOf<String, Double>()

        val bullishRsiWins = winning.count { it.rsi in 50.0..70.0 }
        val bullishRsiLosses = losing.count { it.rsi in 50.0..70.0 }
        changes["RSI"] = boundedDelta(bullishRsiWins, bullishRsiLosses)

        val positiveMacdWins = winning.count { it.macd > it.macdSignal }
        val positiveMacdLosses = losing.count { it.macd > it.macdSignal }
        changes["MACD"] = boundedDelta(positiveMacdWins, positiveMacdLosses)

        val bullishTrendWins = winning.count { it.ema20 > it.ema50 && it.ema50 > it.ema200 }
        val bullishTrendLosses = losing.count { it.ema20 > it.ema50 && it.ema50 > it.ema200 }
        changes["EMA"] = boundedDelta(bullishTrendWins, bullishTrendLosses)

        val volumeWins = winning.count { it.currentVolume >= it.averageVolume }
        val volumeLosses = losing.count { it.currentVolume >= it.averageVolume }
        changes["Volume"] = boundedDelta(volumeWins, volumeLosses)

        val lowVolWins = winning.count { it.volatility <= 35.0 }
        val highVolLosses = losing.count { it.volatility > 35.0 }
        changes["Volatilité"] = boundedDelta(lowVolWins, highVolLosses)

        val totalWinRate = winning.size.toDouble() / matured.size.toDouble()
        changes["Momentum"] = when {
            totalWinRate >= 0.60 -> 1.0
            totalWinRate <= 0.40 -> -1.0
            else -> 0.0
        }

        val now = System.currentTimeMillis()
        val toInsert = changes.mapNotNull { (factor, delta) ->
            if (delta == 0.0) return@mapNotNull null
            val updated = (current.getValue(factor) + delta).coerceIn(5.0, 35.0)
            if (updated == current.getValue(factor)) return@mapNotNull null
            current[factor] = updated
            WeightHistoryEntity(factor = factor, value = updated, recordedAt = now)
        }
        if (toInsert.isNotEmpty()) {
            weightHistoryDao.insertAll(toInsert)
        }
        return current
    }

    /**
     * Met à jour les poids adaptatifs à partir des pronostics IA non joués.
     *
     * Permet à l'application d'apprendre même lorsque l'utilisateur ne joue pas un signal.
     * Utilise les mêmes facteurs que updateWeightsFromResults() pour cohérence.
     *
     * Les pronostics LT et CT sont traités séparément en amont (AiForecastRepository).
     * Cette fonction ne reçoit que les outcomes déjà filtrés par memoryType.
     */
    suspend fun updateWeightsFromForecastOutcomes(
        forecasts: List<AiForecastEntity>,
        outcomes: List<AiForecastOutcomeEntity>
    ): Map<String, Double> {
        ensureDefaults()
        val current = latestWeights().toMutableMap()
        val outcomesById = outcomes.associateBy { it.forecastId }

        // Associer chaque pronostic à sa performance la plus significative
        val matured = forecasts.mapNotNull { forecast ->
            val outcome = outcomesById[forecast.id] ?: return@mapNotNull null
            // Préférer J30 > J7 > J3 > J1 (cohérent avec signaux classiques)
            val performance = outcome.performanceJ30
                ?: outcome.performanceJ7
                ?: outcome.performanceJ3
                ?: outcome.performanceJ1
                ?: return@mapNotNull null
            // Poids du pronostic : score de confiance normalisé (0.5 → 1.0)
            val weight = (forecast.confidence / 100.0).coerceIn(0.5, 1.0)
            Triple(forecast, outcome, performance * weight)
        }

        if (matured.isEmpty()) return current

        val winning = matured.filter { it.third > 0.0 }
        val losing  = matured.filter { it.third < 0.0 }

        val changes = mutableMapOf<String, Double>()

        // RSI : pronostics sur RSI haussier (50-70) — signal valide ?
        // Proxy : score >= 70 → RSI probablement haussier
        val highScoreWins   = winning.count { it.first.score >= 70 }
        val highScoreLosses = losing.count  { it.first.score >= 70 }
        changes["RSI"] = boundedDelta(highScoreWins, highScoreLosses)

        // MACD : score de confiance élevé → signal momentum fort
        val highConfWins   = winning.count { it.first.confidence >= 75 }
        val highConfLosses = losing.count  { it.first.confidence >= 75 }
        changes["MACD"] = boundedDelta(highConfWins, highConfLosses)

        // EMA (tendance) : stratégies tendancielles CT (SWING, QUICK) vs LT
        val trendStratWins   = winning.count { it.first.strategyType in listOf("SWING", "LONG_TERM", "GROWTH") }
        val trendStratLosses = losing.count  { it.first.strategyType in listOf("SWING", "LONG_TERM", "GROWTH") }
        changes["EMA"] = boundedDelta(trendStratWins, trendStratLosses)

        // Volume : pronostics QUICK avec bonne performance → volume confirmé
        val quickWins   = winning.count { it.first.strategyType == "QUICK" }
        val quickLosses = losing.count  { it.first.strategyType == "QUICK" }
        changes["Volume"] = boundedDelta(quickWins, quickLosses)

        // Volatilité : HIGH_VOLATILITY perd → volatilité mal estimée
        val volWins   = winning.count { it.first.strategyType != "HIGH_VOLATILITY" }
        val volLosses = losing.count  { it.first.strategyType == "HIGH_VOLATILITY" }
        changes["Volatilité"] = boundedDelta(volWins, volLosses)

        // Momentum : taux de réussite global des pronostics non joués
        val totalWinRate = winning.size.toDouble() / matured.size.toDouble()
        changes["Momentum"] = when {
            totalWinRate >= 0.60 -> 1.0
            totalWinRate <= 0.40 -> -1.0
            else -> 0.0
        }

        val now = System.currentTimeMillis()
        val toInsert = changes.mapNotNull { (factor, delta) ->
            if (delta == 0.0) return@mapNotNull null
            val updated = (current.getValue(factor) + delta).coerceIn(5.0, 35.0)
            if (updated == current.getValue(factor)) return@mapNotNull null
            current[factor] = updated
            WeightHistoryEntity(factor = factor, value = updated, recordedAt = now)
        }
        if (toInsert.isNotEmpty()) {
            weightHistoryDao.insertAll(toInsert)
        }
        return current
    }

    suspend fun historyLabels(limit: Int = 24): List<String> {
        ensureDefaults()
        return weightHistoryDao.getAll()
            .sortedByDescending { it.recordedAt }
            .take(limit)
            .map { "${it.factor} → ${String.format("%.2f", it.value)}" }
    }

    private suspend fun latestWeights(): Map<String, Double> =
        weightHistoryDao.getAll()
            .groupBy { it.factor }
            .mapValues { (_, values) -> values.maxByOrNull { it.recordedAt }?.value ?: 0.0 }
            .let { latest ->
                mapOf(
                    "RSI" to (latest["RSI"] ?: defaults.getValue("RSI")),
                    "MACD" to (latest["MACD"] ?: defaults.getValue("MACD")),
                    "EMA" to (latest["EMA"] ?: defaults.getValue("EMA")),
                    "Volume" to (latest["Volume"] ?: defaults.getValue("Volume")),
                    "Volatilité" to (latest["Volatilité"] ?: defaults.getValue("Volatilité")),
                    "Momentum" to (latest["Momentum"] ?: defaults.getValue("Momentum"))
                )
            }

    private fun boundedDelta(positiveSignalCount: Int, negativeSignalCount: Int): Double = when {
        positiveSignalCount > negativeSignalCount -> 1.0
        negativeSignalCount > positiveSignalCount -> -1.0
        else -> 0.0
    }
}

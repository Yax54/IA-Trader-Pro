package com.privateinvest.aitraderpro.repository

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class MacdResult(val macd: Double, val signal: Double)

object TechnicalIndicators {
    fun calculateEMA(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val multiplier = 2.0 / (period + 1)
        return values.drop(1).fold(values.first()) { ema, price ->
            (price - ema) * multiplier + ema
        }
    }

    fun calculateRSI(values: List<Double>, period: Int = 14): Double {
        if (values.size <= period) return 50.0
        val changes = values.zipWithNext { a, b -> b - a }
        val recent = changes.takeLast(period)
        val gains = recent.filter { it > 0 }.sum()
        val losses = recent.filter { it < 0 }.sumOf { -it }
        if (losses == 0.0) return 100.0
        val rs = (gains / period) / (losses / period)
        return 100.0 - (100.0 / (1 + rs))
    }

    fun calculateMACD(values: List<Double>, fastPeriod: Int = 12, slowPeriod: Int = 26, signalPeriod: Int = 9): MacdResult {
        if (values.size < slowPeriod + signalPeriod) return MacdResult(0.0, 0.0)
        val macdSeries = values.indices.mapIndexedNotNull { index, _ ->
            val slice = values.take(index + 1)
            if (slice.size < slowPeriod) null else calculateEMA(slice, fastPeriod) - calculateEMA(slice, slowPeriod)
        }
        val macdValue = macdSeries.lastOrNull() ?: 0.0
        val signalValue = calculateEMA(macdSeries.takeLast(signalPeriod.coerceAtMost(macdSeries.size)), signalPeriod.coerceAtMost(macdSeries.size).coerceAtLeast(1))
        return MacdResult(macdValue, signalValue)
    }

    fun calculateATR(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int = 14): Double {
        if (highs.size != lows.size || lows.size != closes.size || highs.size <= period) return 0.0
        val trueRanges = highs.indices.drop(1).map { i ->
            maxOf(
                highs[i] - lows[i],
                kotlin.math.abs(highs[i] - closes[i - 1]),
                kotlin.math.abs(lows[i] - closes[i - 1])
            )
        }
        return trueRanges.takeLast(period).average()
    }

    /**
     * Volatilité annualisée basée sur l'écart-type des rendements logarithmiques
     * sur la fenêtre disponible (minimum 20 points conseillé).
     */
    fun calculateVolatility(closes: List<Double>): Double {
        if (closes.size < 20) return 0.0
        val returns = closes.zipWithNext { a, b -> ln(b / a) }
        val mean = returns.average()
        val variance = returns.sumOf { (it - mean).pow(2) } / returns.size
        return sqrt(variance) * sqrt(252.0) * 100.0
    }
}

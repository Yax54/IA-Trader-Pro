package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.database.SignalOutcomeEntity

object PerformanceAnalyzer {
    fun computeWinRate(performances: List<Double>): Double {
        if (performances.isEmpty()) return 0.0
        return performances.count { it > 0.0 } * 100.0 / performances.size
    }

    fun computeAverageGain(performances: List<Double>): Double {
        val gains = performances.filter { it > 0.0 }
        return if (gains.isEmpty()) 0.0 else gains.average()
    }

    fun computeAverageLoss(performances: List<Double>): Double {
        val losses = performances.filter { it < 0.0 }
        return if (losses.isEmpty()) 0.0 else losses.average()
    }

    fun computeProfitFactor(performances: List<Double>): Double {
        val gains = performances.filter { it > 0.0 }.sum()
        val losses = performances.filter { it < 0.0 }.sumOf { kotlin.math.abs(it) }
        return if (losses == 0.0) 0.0 else gains / losses
    }

    fun computeMaxDrawdown(performances: List<Double>): Double {
        var peak = 0.0
        var equity = 0.0
        var maxDrawdown = 0.0
        performances.forEach { value ->
            equity += value
            if (equity > peak) peak = equity
            val drawdown = peak - equity
            if (drawdown > maxDrawdown) maxDrawdown = drawdown
        }
        return maxDrawdown
    }

    fun computeSuccessRate(
        outcomes: List<SignalOutcomeEntity>,
        selector: (SignalOutcomeEntity) -> Boolean?
    ): Double {
        val values = outcomes.mapNotNull(selector)
        if (values.isEmpty()) return 0.0
        return values.count { it } * 100.0 / values.size
    }

    fun extractPerformances(
        outcomes: List<SignalOutcomeEntity>,
        selector: (SignalOutcomeEntity) -> Double?
    ): List<Double> = outcomes.mapNotNull(selector)
}

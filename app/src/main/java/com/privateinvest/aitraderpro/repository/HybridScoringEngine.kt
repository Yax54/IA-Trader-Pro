package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.data.model.ScoringResult
import com.privateinvest.aitraderpro.data.model.SignalAction
import com.privateinvest.aitraderpro.data.model.SignalExplanation
import com.privateinvest.aitraderpro.data.model.TechnicalSnapshot
import kotlin.math.roundToInt

data class RiskSettings(
    val maxPositionPercent: Double = 10.0,
    val maxDailyLoss: Double = 2.0,
    val maxOpenPositions: Int = 8,
    val minimumVolumeRatio: Double = 1.0,
    val buyThreshold: Int = 70,
    val acceptableRiskThreshold: Int = 45
)

object HybridScoringEngine {
    fun evaluate(
        snapshot: TechnicalSnapshot,
        weights: Map<String, Double>,
        riskSettings: RiskSettings,
        openPositionsCount: Int = 0
    ): ScoringResult {
        val reasons = mutableListOf<String>()
        var technicalRaw = 0.0

        if (snapshot.rsi in 50.0..70.0) {
            technicalRaw += weights.getValue("RSI")
            reasons += "RSI haussier"
        }
        if (snapshot.macd > snapshot.macdSignal) {
            technicalRaw += weights.getValue("MACD")
            reasons += "MACD positif"
        }
        if (snapshot.ema20 > snapshot.ema50) {
            technicalRaw += weights.getValue("EMA") * 0.5
            reasons += "EMA20 > EMA50"
        }
        if (snapshot.ema50 > snapshot.ema200) {
            technicalRaw += weights.getValue("EMA") * 0.5
            reasons += "EMA50 > EMA200"
        }
        val volumeRatio = if (snapshot.averageVolume > 0.0) snapshot.currentVolume / snapshot.averageVolume else 0.0
        if (volumeRatio >= riskSettings.minimumVolumeRatio) {
            technicalRaw += weights.getValue("Volume")
            reasons += "Volume supérieur à la moyenne"
        }
        if (snapshot.volatility in 0.0..35.0) {
            technicalRaw += weights.getValue("Momentum")
            reasons += "Momentum exploitable"
        }

        val scoreTechnique = technicalRaw.coerceIn(0.0, 100.0).roundToInt()
        var scoreRisque = 0.0
        scoreRisque += if (snapshot.atr14 > 0) (snapshot.atr14 / maxOf(snapshot.ema20, 1.0)) * 100 else 0.0
        scoreRisque += (snapshot.volatility / 2.0)
        if (openPositionsCount >= riskSettings.maxOpenPositions) {
            scoreRisque += 30.0
            reasons += "Nombre max de positions atteint"
        }
        val riskInt = scoreRisque.coerceIn(0.0, 100.0).roundToInt()
        if (riskInt <= riskSettings.acceptableRiskThreshold) {
            reasons += "Volatilité acceptable"
        }

        val signal = when {
            scoreTechnique > riskSettings.buyThreshold && riskInt <= riskSettings.acceptableRiskThreshold && volumeRatio >= riskSettings.minimumVolumeRatio -> SignalAction.ACHETER
            scoreTechnique >= 55 -> SignalAction.SURVEILLER
            scoreTechnique >= 40 -> SignalAction.CONSERVER
            else -> SignalAction.VENDRE
        }

        return ScoringResult(
            scoreTechnique = scoreTechnique,
            scoreRisque = riskInt,
            signal = signal,
            explanation = SignalExplanation(reasons.ifEmpty { listOf("Aucun signal valide") })
        )
    }
}

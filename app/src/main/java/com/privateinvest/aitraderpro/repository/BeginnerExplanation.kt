package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.data.model.AssetDetail
import com.privateinvest.aitraderpro.data.model.SignalAction
import kotlin.math.roundToInt

data class BeginnerSignalSummary(
    val actionLabel: String,
    val headline: String,
    val riskLabel: String,
    val confidenceLabel: String,
    val confidencePercent: Int,
    val confidenceVisualLabel: String,
    val maxInvestmentPercent: Int,
    val stopLossPercent: Double,
    val whyText: List<String>,
    val similarSignalsText: String,
    val historicalEdgeText: String,
    val warningText: String
)

object BeginnerExplanation {
    fun fromAssetDetail(detail: AssetDetail): BeginnerSignalSummary {
        val signal = detail.scoring.signal
        val riskScore = detail.scoring.scoreRisque
        val explanation = detail.scoring.explanation
        val confidencePercent = (detail.scoring.scoreTechnique - (riskScore * 0.35)).roundToInt().coerceIn(0, 100)
        val maxInvestmentPercent = when {
            riskScore <= 25 && signal == SignalAction.ACHETER -> 10
            riskScore <= 45 -> 8
            riskScore <= 60 -> 6
            else -> 4
        }

        val actionLabel = when (signal) {
            SignalAction.ACHETER -> "ACHETER"
            SignalAction.SURVEILLER -> "ATTENDRE / SURVEILLER"
            SignalAction.CONSERVER -> "CONSERVER"
            SignalAction.VENDRE -> "VENDRE"
        }

        val headline = when (signal) {
            SignalAction.ACHETER -> "Le signal est favorable, mais l'achat reste à valider manuellement."
            SignalAction.SURVEILLER -> "Le dossier est intéressant, mais il manque encore une confirmation."
            SignalAction.CONSERVER -> "La position peut être gardée sans renforcer agressivement."
            SignalAction.VENDRE -> "Le risque domine actuellement, mieux vaut réduire l'exposition."
        }

        val riskLabel = when {
            riskScore <= 25 -> "Faible"
            riskScore <= 45 -> "Modéré"
            riskScore <= 65 -> "Élevé"
            else -> "Très élevé"
        }

        val confidenceLabel = when {
            detail.scoring.scoreTechnique >= 75 && riskScore <= 45 -> "Bonne lisibilité"
            detail.scoring.scoreTechnique >= 60 -> "Lisibilité moyenne"
            else -> "Faible lisibilité"
        }

        val confidenceVisualLabel = when {
            confidencePercent >= 75 -> "🟢 Forte"
            confidencePercent >= 55 -> "🟠 Moyenne"
            else -> "🔴 Faible"
        }

        val similarSignalsText = if (explanation.similarConfigurations > 0) {
            "${explanation.similarConfigurations} configurations proches déjà mémorisées"
        } else {
            "Pas encore assez d'historique comparable"
        }

        val historicalEdgeText = if (explanation.similarConfigurations > 0) {
            "Réussite historique ${formatPercent(explanation.historicalWinRate)} · Gain moyen ${formatPercent(explanation.averageGain)}"
        } else {
            "L'historique IA s'enrichira au fil des validations et résultats J+1 / J+7 / J+30"
        }

        return BeginnerSignalSummary(
            actionLabel = actionLabel,
            headline = headline,
            riskLabel = riskLabel,
            confidenceLabel = confidenceLabel,
            confidencePercent = confidencePercent,
            confidenceVisualLabel = confidenceVisualLabel,
            maxInvestmentPercent = maxInvestmentPercent,
            stopLossPercent = calculateStopLossPercent(detail),
            whyText = buildWhyText(detail),
            similarSignalsText = similarSignalsText,
            historicalEdgeText = historicalEdgeText,
            warningText = "Rappel : l'application assiste la décision, elle ne remplace ni votre validation ni une gestion du risque disciplinée."
        )
    }

    fun buildWhyText(detail: AssetDetail): List<String> {
        val snapshot = detail.technicalSnapshot
        val items = mutableListOf<String>()
        items += beginnerLabelForIndicator("RSI", snapshot.rsi)
        items += if (snapshot.macd > snapshot.macdSignal) {
            "Le momentum reste haussier : le MACD est au-dessus de son signal."
        } else {
            "Le momentum reste fragile : le MACD ne confirme pas encore franchement."
        }
        items += if (snapshot.ema20 > snapshot.ema50) {
            "La tendance courte reste au-dessus de la tendance intermédiaire (EMA20 > EMA50)."
        } else {
            "La tendance courte n'est pas encore au-dessus de la tendance intermédiaire."
        }
        items += if (snapshot.ema50 > snapshot.ema200) {
            "La structure de fond reste positive (EMA50 > EMA200)."
        } else {
            "La structure de fond n'est pas encore solide (EMA50 ≤ EMA200)."
        }
        items += beginnerLabelForIndicator("Volatilité", snapshot.volatility)
        if (detail.scoring.explanation.reasons.isNotEmpty()) {
            items += detail.scoring.explanation.reasons.take(2).map { "Raison IA : $it" }
        }
        return items.distinct()
    }

    fun calculateStopLossPercent(detail: AssetDetail): Double {
        val price = detail.quotePrice.coerceAtLeast(0.01)
        val atrBased = (detail.technicalSnapshot.atr14 / price) * 100.0
        return atrBased.coerceIn(2.0, 8.0)
    }

    fun beginnerLabelForIndicator(indicator: String, value: Double): String {
        return when (indicator) {
            "RSI" -> when {
                value in 50.0..70.0 -> "Le RSI est dans une zone favorable sans signal de surchauffe majeure."
                value < 40.0 -> "Le RSI reste faible : le marché n'a pas encore repris de force nette."
                value > 70.0 -> "Le RSI est élevé : le potentiel existe mais le risque de surchauffe augmente."
                else -> "Le RSI est neutre : il faut d'autres confirmations."
            }
            "Volatilité" -> when {
                value <= 20.0 -> "La volatilité reste contenue, le mouvement est plus lisible."
                value <= 35.0 -> "La volatilité est correcte, mais il faut garder une taille de position mesurée."
                else -> "La volatilité est forte : il faut réduire l'exposition et protéger davantage la position."
            }
            else -> "Indicateur $indicator : ${formatPercent(value)}"
        }
    }

    private fun formatPercent(value: Double): String = String.format("%.1f %%", value)
}

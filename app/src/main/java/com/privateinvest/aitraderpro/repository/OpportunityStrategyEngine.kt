package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.data.model.AssetDetail
import com.privateinvest.aitraderpro.data.model.SignalAction
import kotlin.math.abs
import kotlin.math.roundToInt

// DIVIDEND supprimé : stratégie non calculée, remplacée par DEFENSIVE existant.
enum class OpportunityStrategyType(
    val emoji: String,
    val label: String,
    val shortLabel: String
) {
    QUICK("⚡", "Opportunité rapide", "Rapide"),
    SWING("📈", "Swing trading", "Swing"),
    HIGH_VOLATILITY("🔥", "Forte volatilité", "Volatile"),
    LONG_TERM("🏆", "Long terme", "Long terme"),
    GROWTH("🌱", "Croissance", "Croissance"),
    DEFENSIVE("🛡️", "Défensif", "Défensif")
}

data class OpportunityStrategy(
    val type: OpportunityStrategyType,
    val compatibility: Int,
    val horizon: String,
    val targetGainPercent: Double,
    val stopLossPercent: Double,
    val maxHoldingDays: Int,
    val title: String,
    val beginnerExplanation: String,
    val whatAppDoes: String,
    val reasons: List<String>
)

object OpportunityStrategyEngine {

    fun classify(detail: AssetDetail): List<OpportunityStrategy> {
        val snapshot = detail.technicalSnapshot
        val scoring = detail.scoring
        val list = mutableListOf<OpportunityStrategy>()

        val rsi = snapshot.rsi
        val macdPositive = snapshot.macd > snapshot.macdSignal
        val trendShort = snapshot.ema20 > snapshot.ema50
        val trendLong = snapshot.ema50 > snapshot.ema200
        val volumeRatio = if (snapshot.averageVolume > 0) snapshot.currentVolume / snapshot.averageVolume else 0.0
        val volatility = snapshot.volatility
        val atrPct = if (detail.quotePrice > 0) (snapshot.atr14 / detail.quotePrice) * 100.0 else 0.0

        // ⚡ Opportunité rapide
        if (
            scoring.signal == SignalAction.ACHETER &&
            (rsi <= 35.0 || (rsi in 35.0..55.0 && macdPositive)) &&
            volumeRatio >= 1.0 &&
            volatility >= 18.0
        ) {
            list += OpportunityStrategy(
                type = OpportunityStrategyType.QUICK,
                compatibility = computeScore(
                    base = 62,
                    bonuses = listOf(
                        if (rsi <= 35.0) 14 else 6,
                        if (macdPositive) 10 else 0,
                        if (volumeRatio >= 1.2) 8 else 3,
                        if (volatility >= 22.0) 6 else 0
                    )
                ),
                horizon = "1 à 5 jours",
                targetGainPercent = when {
                    volatility >= 35 -> 5.0
                    volatility >= 25 -> 4.0
                    else -> 3.0
                },
                stopLossPercent = maxOf(2.0, minOf(4.5, atrPct * 1.4)),
                maxHoldingDays = 5,
                title = "Rebond rapide possible",
                beginnerExplanation = "L'action semble pouvoir faire un rebond court. L'objectif n'est pas de la garder longtemps, mais de surveiller une sortie rapide si le petit gain attendu arrive.",
                whatAppDoes = "L'application place l'action en suivi intelligent et t'alerte si l'objectif, le stop de protection ou la durée maximale sont atteints.",
                reasons = listOfNotNull(
                    if (rsi <= 35.0) "RSI bas : l'action paraît survendue" else "RSI proche d'une zone de reprise",
                    if (macdPositive) "MACD favorable : la tendance courte se réveille" else null,
                    if (volumeRatio >= 1.0) "Volume suffisant : le mouvement est confirmé" else null,
                    if (volatility >= 18.0) "Volatilité exploitable pour un mouvement court" else null
                )
            )
        }

        // 📈 Swing trading
        if (
            scoring.signal == SignalAction.ACHETER &&
            trendShort &&
            macdPositive &&
            scoring.scoreTechnique >= 65
        ) {
            list += OpportunityStrategy(
                type = OpportunityStrategyType.SWING,
                compatibility = computeScore(
                    58,
                    listOf(
                        if (trendShort) 12 else 0,
                        if (trendLong) 10 else 0,
                        if (macdPositive) 10 else 0,
                        if (scoring.scoreTechnique >= 80) 8 else 4
                    )
                ),
                horizon = "2 à 6 semaines",
                targetGainPercent = when {
                    scoring.scoreTechnique >= 85 -> 10.0
                    else -> 7.0
                },
                stopLossPercent = maxOf(5.0, minOf(8.0, atrPct * 2.0)),
                maxHoldingDays = 35,
                title = "Tendance exploitable sur plusieurs jours",
                beginnerExplanation = "Le swing trading consiste à profiter d'une tendance pendant quelques jours ou semaines, sans chercher à garder l'action très longtemps.",
                whatAppDoes = "L'application surveille si la tendance continue, si l'objectif est atteint ou si les indicateurs commencent à se retourner.",
                reasons = listOfNotNull(
                    if (trendShort) "EMA20 au-dessus de EMA50 : tendance courte positive" else null,
                    if (trendLong) "EMA50 au-dessus de EMA200 : tendance de fond favorable" else null,
                    if (macdPositive) "MACD positif : dynamique haussière" else null
                )
            )
        }

        // 🔥 Forte volatilité
        if (volatility >= 32.0 || atrPct >= 4.0) {
            list += OpportunityStrategy(
                type = OpportunityStrategyType.HIGH_VOLATILITY,
                compatibility = computeScore(
                    55,
                    listOf(
                        if (volatility >= 40.0) 22 else 12,
                        if (atrPct >= 4.0) 12 else 0,
                        if (volumeRatio >= 1.5) 8 else 0
                    )
                ),
                horizon = "Très court à court terme",
                targetGainPercent = 4.0,
                stopLossPercent = maxOf(3.0, minOf(6.0, atrPct * 1.3)),
                maxHoldingDays = 3,
                title = "Action très nerveuse",
                beginnerExplanation = "Cette action bouge beaucoup. Elle peut offrir de belles opportunités, mais elle peut aussi se retourner vite. Elle demande une surveillance stricte.",
                whatAppDoes = "L'application t'alerte plus vite en cas de gain, de perte ou de changement de tendance.",
                reasons = listOf(
                    "Volatilité élevée : mouvements rapides possibles",
                    "Stop de protection recommandé",
                    "À éviter si tu ne veux pas de variations fortes"
                )
            )
        }

        // 🏆 Long terme
        if (trendLong && scoring.scoreRisque <= 45 && volatility <= 25.0 && scoring.scoreTechnique >= 60) {
            list += OpportunityStrategy(
                type = OpportunityStrategyType.LONG_TERM,
                compatibility = computeScore(
                    60,
                    listOf(
                        if (trendLong) 15 else 0,
                        if (volatility <= 20.0) 10 else 4,
                        if (scoring.scoreRisque <= 30) 8 else 2
                    )
                ),
                horizon = "Plusieurs mois",
                targetGainPercent = 12.0,
                stopLossPercent = maxOf(8.0, minOf(12.0, atrPct * 3.0)),
                maxHoldingDays = 120,
                title = "Profil plus stable",
                beginnerExplanation = "L'action semble plus adaptée à une conservation longue qu'à un aller-retour rapide.",
                whatAppDoes = "L'application surveille surtout la cassure de tendance et les alertes de risque.",
                reasons = listOf(
                    "Tendance longue favorable",
                    "Risque maîtrisé",
                    "Volatilité relativement raisonnable"
                )
            )
        }

        // 🌱 Croissance
        if (scoring.scoreTechnique >= 72 && trendLong && volumeRatio >= 1.0) {
            list += OpportunityStrategy(
                type = OpportunityStrategyType.GROWTH,
                compatibility = computeScore(
                    55,
                    listOf(
                        if (scoring.scoreTechnique >= 80) 14 else 6,
                        if (trendLong) 10 else 0,
                        if (volumeRatio >= 1.2) 6 else 2
                    )
                ),
                horizon = "Semaines à mois",
                targetGainPercent = 10.0,
                stopLossPercent = 7.0,
                maxHoldingDays = 60,
                title = "Dynamique de croissance",
                beginnerExplanation = "Le marché semble favoriser cette action. Elle peut être intéressante si tu acceptes de laisser respirer la position.",
                whatAppDoes = "L'application surveille la poursuite de tendance et le risque de retournement.",
                reasons = listOf("Score technique élevé", "Tendance de fond favorable", "Volume cohérent")
            )
        }

        return list
            .sortedByDescending { it.compatibility }
            .take(4)
            .ifEmpty {
                listOf(
                    OpportunityStrategy(
                        type = OpportunityStrategyType.DEFENSIVE,
                        compatibility = 45,
                        horizon = "Attente",
                        targetGainPercent = 0.0,
                        stopLossPercent = 0.0,
                        maxHoldingDays = 0,
                        title = "Pas de stratégie claire",
                        beginnerExplanation = "L'IA ne détecte pas encore une opportunité suffisamment nette. Il vaut mieux attendre ou surveiller.",
                        whatAppDoes = "L'application peut garder l'actif en observation sans déclencher d'action.",
                        reasons = listOf("Signal trop faible ou données insuffisantes")
                    )
                )
            }
    }

    fun explanationFor(type: OpportunityStrategyType): String = when (type) {
        OpportunityStrategyType.QUICK ->
            "Une opportunité rapide vise un petit mouvement court, souvent après une baisse ou un rebond détecté. L'application surveille l'objectif et t'alerte si une vente devient intéressante."
        OpportunityStrategyType.SWING ->
            "Le swing trading vise une tendance sur plusieurs jours ou semaines. L'application surveille si la tendance reste valide ou si elle commence à se retourner."
        OpportunityStrategyType.HIGH_VOLATILITY ->
            "Forte volatilité signifie que l'action peut beaucoup bouger. C'est intéressant pour des mouvements rapides, mais le risque est plus élevé."
        OpportunityStrategyType.LONG_TERM ->
            "Long terme signifie que l'action semble plus adaptée à une conservation longue, avec moins d'allers-retours."
        OpportunityStrategyType.GROWTH ->
            "Croissance signifie que l'action est surtout intéressante pour sa dynamique de progression et sa tendance."
        OpportunityStrategyType.DEFENSIVE ->
            "Défensif signifie que l'application ne détecte pas de stratégie claire pour le moment. Il vaut mieux surveiller et attendre un signal plus net."
    }

    private fun computeScore(base: Int, bonuses: List<Int>): Int {
        return (base + bonuses.sum()).coerceIn(0, 100)
    }
}

fun Double.formatPercent1(): String = "${(this * 10.0).roundToInt() / 10.0} %"

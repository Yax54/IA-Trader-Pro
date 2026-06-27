package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.database.AppDatabase
import com.privateinvest.aitraderpro.database.StrategyFollowUpEntity
import com.privateinvest.aitraderpro.notifications.TraderNotificationManager
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class StrategyFollowUpItem(
    val id: Long,
    val symbol: String,
    val name: String,
    val strategyLabel: String,
    val strategyEmoji: String,
    val mode: String,
    val entryPrice: Double,
    val currentPrice: Double,
    val performancePercent: Double,
    val targetPercent: Double,
    val stopPercent: Double,
    val maxHoldingDays: Int,
    val daysOpen: Long,
    val status: String,
    val advice: String,
    val alertReason: String?
)

class StrategyMonitoringRepository(
    private val db: AppDatabase,
    private val marketRepository: MarketRepository,
    private val notificationManager: TraderNotificationManager
) {
    suspend fun createFollowUp(
        symbol: String,
        name: String,
        mode: String = "SIMULATION",
        preferredType: OpportunityStrategyType? = null
    ): Boolean {
        // V1.3 DataFreshnessGuard — bloquer si données absentes/périmées
        if (!DataFreshnessGuard.canGenerateSignal(symbol)) return false

        val detail = marketRepository.getMarketData(symbol) ?: return false
        val strategies = OpportunityStrategyEngine.classify(detail)
        val selected = preferredType?.let { type -> strategies.firstOrNull { it.type == type } } ?: strategies.first()
        if (selected.type == OpportunityStrategyType.DEFENSIVE && selected.maxHoldingDays == 0) return false

        val now = System.currentTimeMillis()
        db.strategyFollowUpDao().insert(
            StrategyFollowUpEntity(
                symbol = symbol,
                name = name,
                strategyType = selected.type.name,
                strategyLabel = selected.type.label,
                mode = mode,
                entryPrice = detail.quotePrice,
                currentPrice = detail.quotePrice,
                targetPercent = selected.targetGainPercent,
                stopPercent = selected.stopLossPercent,
                maxHoldingDays = selected.maxHoldingDays,
                openedAt = now,
                lastCheckedAt = now,
                status = "ACTIVE",
                alertReason = null
            )
        )
        return true
    }

    suspend fun getFollowUps(): List<StrategyFollowUpItem> {
        refreshFollowUps()
        return db.strategyFollowUpDao().getAll().map { it.toItem() }
    }

    suspend fun closeFollowUp(id: Long) {
        db.strategyFollowUpDao().findById(id)?.let {
            db.strategyFollowUpDao().update(it.copy(status = "CLOSED", alertReason = "Suivi clôturé manuellement"))
        }
    }

    suspend fun extendFollowUp(id: Long, extraDays: Int = 3) {
        db.strategyFollowUpDao().findById(id)?.let {
            db.strategyFollowUpDao().update(
                it.copy(
                    maxHoldingDays = it.maxHoldingDays + extraDays,
                    status = "ACTIVE",
                    alertReason = null,
                    lastCheckedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun refreshFollowUps() {
        // V1.3 DataFreshnessGuard — si aucune donnée réelle, mise à jour des prix bloquée
        val dataAvailable = DataFreshnessGuard.canGenerateSignal()

        val active = db.strategyFollowUpDao().getActive()
        val now = System.currentTimeMillis()
        active.forEach { item ->
            // Si données indisponibles, on ne met pas à jour le prix mais on continue le suivi
            val detail = if (dataAvailable) marketRepository.getMarketData(item.symbol) else null
            val currentPrice = detail?.quotePrice ?: item.currentPrice
            val perf = if (item.entryPrice > 0) ((currentPrice - item.entryPrice) / item.entryPrice) * 100.0 else 0.0
            val daysOpen = TimeUnit.MILLISECONDS.toDays(now - item.openedAt)
            val reason = when {
                perf >= item.targetPercent -> "Objectif atteint : ${perf.formatSignedPercent()}"
                perf <= -item.stopPercent -> "Stop de protection atteint : ${perf.formatSignedPercent()}"
                daysOpen >= item.maxHoldingDays -> "Durée maximale atteinte : ${daysOpen}j"
                detail != null && detail.scoring.signal.name == "VENDRE" -> "Signal IA de vente détecté"
                else -> null
            }
            val nextStatus = if (reason != null) "SELL_ALERT" else "ACTIVE"
            val updated = item.copy(
                currentPrice = currentPrice,
                lastCheckedAt = now,
                status = nextStatus,
                alertReason = reason
            )
            db.strategyFollowUpDao().update(updated)

            if (reason != null && now - item.lastAlertAt > TimeUnit.HOURS.toMillis(6)) {
                val status = notificationManager.publishNotification(
                    title = "Vente à envisager : ${item.symbol}",
                    message = "${item.strategyLabel} — $reason. Ouvre AI Trader Pro pour valider manuellement.",
                    notificationId = ("strategy_${item.symbol}_${item.id}").hashCode()
                )
                db.strategyFollowUpDao().update(updated.copy(lastAlertAt = now, alertStatus = status))
            }
        }
    }

    private fun StrategyFollowUpEntity.toItem(): StrategyFollowUpItem {
        val perf = if (entryPrice > 0) ((currentPrice - entryPrice) / entryPrice) * 100.0 else 0.0
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - openedAt)
        val type = runCatching { OpportunityStrategyType.valueOf(strategyType) }.getOrDefault(OpportunityStrategyType.DEFENSIVE)
        val advice = when (status) {
            "SELL_ALERT" -> "Vente à envisager. L'application t'alerte, mais ne vend jamais seule."
            "CLOSED" -> "Suivi clôturé."
            else -> when (type) {
                OpportunityStrategyType.QUICK -> "Surveillance courte : objectif rapide ou sortie si retournement."
                OpportunityStrategyType.SWING -> "Surveillance tendance : garder tant que les indicateurs restent favorables."
                OpportunityStrategyType.HIGH_VOLATILITY -> "Surveillance stricte : action nerveuse, réaction rapide conseillée."
                else -> "Surveillance active sans action automatique."
            }
        }
        return StrategyFollowUpItem(
            id = id,
            symbol = symbol,
            name = name,
            strategyLabel = strategyLabel,
            strategyEmoji = type.emoji,
            mode = mode,
            entryPrice = entryPrice,
            currentPrice = currentPrice,
            performancePercent = perf,
            targetPercent = targetPercent,
            stopPercent = stopPercent,
            maxHoldingDays = maxHoldingDays,
            daysOpen = days,
            status = status,
            advice = advice,
            alertReason = alertReason
        )
    }
}

private fun Double.formatSignedPercent(): String {
    val rounded = (this * 10.0).roundToInt() / 10.0
    val sign = if (rounded > 0) "+" else ""
    return "$sign$rounded %"
}

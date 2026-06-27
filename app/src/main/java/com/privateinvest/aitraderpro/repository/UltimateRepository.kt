package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.database.FavoriteAssetEntity
import com.privateinvest.aitraderpro.database.PortfolioGoalEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class JournalItem(
    val dateLabel: String,
    val symbol: String,
    val decision: String,
    val score: Int,
    val detail: String
)

data class CalendarDayItem(
    val dateLabel: String,
    val status: String,
    val signalCount: Int,
    val performance: Double
)

data class HealthCheckItem(
    val title: String,
    val status: String,
    val detail: String
)

data class GoalSummary(
    val annualTargetPercent: Double,
    val currentPerformancePercent: Double,
    val progressPercent: Int,
    val label: String
)

data class BackupSummary(
    val jsonPreview: String,
    val signalCount: Int,
    val memoryCount: Int,
    val tradeCount: Int,
    val favoriteCount: Int
)

class UltimateRepository(
    private val marketRepository: MarketRepository = ServiceLocator.marketRepository
) {
    private val db get() = ServiceLocator.database

    suspend fun topOpportunities(): List<AssetSignal> = marketRepository.getSignals()
        .sortedWith(compareByDescending<AssetSignal> { it.score }.thenByDescending { it.confidence })
        .take(20)

    suspend fun journal(): List<JournalItem> {
        val decisions = db.decisionLogDao().getAll().map {
            JournalItem(
                dateLabel = formatDateTime(it.date),
                symbol = it.symbol,
                decision = it.decision,
                score = it.score,
                detail = "Poids utilisés : ${it.weightsUsed.ifBlank { "non disponible" }}"
            )
        }
        val trades = db.tradeDao().getAll().map {
            JournalItem(
                dateLabel = formatDateTime(it.createdAt),
                symbol = it.symbol,
                decision = it.side,
                score = 0,
                detail = "Simulation · quantité ${String.format(Locale.FRANCE, "%.0f", it.quantity)} · prix ${String.format(Locale.FRANCE, "%.2f €", it.price)}"
            )
        }
        val notifications = db.notificationHistoryDao().getAll().map {
            JournalItem(
                dateLabel = formatDateTime(it.createdAt),
                symbol = it.symbol ?: "APP",
                decision = it.level,
                score = 0,
                detail = "${it.title} — ${it.message}"
            )
        }
        return (decisions + trades + notifications).sortedByDescending { parseSortKey(it.dateLabel) }.take(200)
    }

    suspend fun calendar(): List<CalendarDayItem> {
        val results = db.signalResultDao().getAll()
        val grouped = results.groupBy { formatDay(it.dateCreated) }
        if (grouped.isEmpty()) return listOf(
            CalendarDayItem(formatDay(System.currentTimeMillis()), "Pas assez de données", 0, 0.0)
        )
        return grouped.map { (day, items) ->
            val perf = items.sumOf { it.profitPercent }
            CalendarDayItem(
                dateLabel = day,
                status = when {
                    perf > 0.2 -> "Positif"
                    perf < -0.2 -> "Négatif"
                    else -> "Neutre"
                },
                signalCount = items.size,
                performance = perf
            )
        }.sortedByDescending { it.dateLabel }.take(60)
    }

    suspend fun favorites(): List<FavoriteAssetEntity> = db.favoriteAssetDao().getAll()

    suspend fun toggleFavorite(symbol: String, name: String): Boolean {
        val existing = db.favoriteAssetDao().findBySymbol(symbol)
        return if (existing == null) {
            db.favoriteAssetDao().insert(FavoriteAssetEntity(symbol = symbol, name = name, createdAt = System.currentTimeMillis()))
            true
        } else {
            db.favoriteAssetDao().delete(existing)
            false
        }
    }

    suspend fun health(): List<HealthCheckItem> {
        val signals = db.signalDao().getAllSignals().size
        val memories = db.signalMemoryDao().getAll().size
        val notifications = db.notificationHistoryDao().getAll().size
        val account = db.simulationAccountDao().getAccount()
        val audit = runCatching { marketRepository.getAuditSummary() }.getOrNull()
        return listOf(
            HealthCheckItem("Base Room", "OK", "Lecture locale réussie"),
            HealthCheckItem("Signaux", if (signals > 0) "OK" else "Attention", "$signals signaux enregistrés"),
            HealthCheckItem("Mémoire IA", if (memories > 0) "OK" else "Attention", "$memories configurations mémorisées"),
            HealthCheckItem("Audit IA", if (audit != null) "OK" else "Attention", audit?.let { "${it.signalCount} signaux audités" } ?: "Audit non disponible"),
            HealthCheckItem("Simulation", if (account != null) "OK" else "Attention", account?.let { "Cash ${String.format(Locale.FRANCE, "%.2f €", it.availableCash)}" } ?: "Compte virtuel non initialisé"),
            HealthCheckItem("Notifications", if (notifications > 0) "OK" else "Attention", "$notifications notifications journalisées")
        )
    }

    suspend fun goalSummary(): GoalSummary {
        val goal = db.portfolioGoalDao().getCurrent() ?: PortfolioGoalEntity(targetPercent = 10.0, createdAt = System.currentTimeMillis()).also {
            db.portfolioGoalDao().upsert(it)
        }
        val simulation = marketRepository.getSimulationSummary()
        val current = extractPercent(simulation.totalPerformance)
        val progress = if (goal.targetPercent > 0) ((current / goal.targetPercent) * 100.0).toInt().coerceIn(0, 100) else 0
        return GoalSummary(
            annualTargetPercent = goal.targetPercent,
            currentPerformancePercent = current,
            progressPercent = progress,
            label = when {
                progress >= 100 -> "Objectif atteint"
                progress >= 60 -> "Bonne progression"
                progress > 0 -> "En progression"
                else -> "À construire"
            }
        )
    }

    suspend fun setGoal(percent: Double) {
        db.portfolioGoalDao().upsert(PortfolioGoalEntity(targetPercent = percent.coerceIn(1.0, 100.0), createdAt = System.currentTimeMillis()))
    }

    suspend fun backupSummary(): BackupSummary {
        val signals = db.signalDao().getAllSignals()
        val memories = db.signalMemoryDao().getAll()
        val trades = db.tradeDao().getAll()
        val favorites = db.favoriteAssetDao().getAll()
        val preview = buildString {
            appendLine("{")
            appendLine("  \"app\": \"AI Trader Pro\",")
            appendLine("  \"exportedAt\": \"${formatDateTime(System.currentTimeMillis())}\",")
            appendLine("  \"signals\": ${signals.size},")
            appendLine("  \"memories\": ${memories.size},")
            appendLine("  \"trades\": ${trades.size},")
            appendLine("  \"favorites\": ${favorites.map { it.symbol }}")
            appendLine("}")
        }
        return BackupSummary(preview, signals.size, memories.size, trades.size, favorites.size)
    }

    suspend fun exportTextReport(): String {
        val audit = marketRepository.getAuditSummary()
        val simulation = marketRepository.getSimulationSummary()
        return buildString {
            appendLine("AI TRADER PRO — RAPPORT")
            appendLine("Date : ${formatDateTime(System.currentTimeMillis())}")
            appendLine("Capital simulation : ${simulation.totalValue}")
            appendLine("Performance simulation : ${simulation.totalPerformance}")
            appendLine("Signaux audités : ${audit.signalCount}")
            appendLine("Taux de réussite : ${String.format(Locale.FRANCE, "%.1f %%", audit.winRate)}")
            appendLine("Profit factor : ${String.format(Locale.FRANCE, "%.2f", audit.profitFactor)}")
            appendLine("Drawdown max : ${String.format(Locale.FRANCE, "%.2f", audit.maxDrawdown)}")
        }
    }

    private fun extractPercent(text: String): Double {
        val regex = Regex("[-+]?\\d+[,.]?\\d*")
        val matches = regex.findAll(text).map { it.value.replace(',', '.').toDoubleOrNull() }.filterNotNull().toList()
        return matches.lastOrNull() ?: 0.0
    }

    private fun formatDateTime(timestamp: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(timestamp))
    private fun formatDay(timestamp: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(timestamp))
    private fun parseSortKey(label: String): String = label.reversed()
}

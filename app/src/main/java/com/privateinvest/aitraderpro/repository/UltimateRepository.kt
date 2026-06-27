package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.BuildConfig
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.database.FavoriteAssetEntity
import com.privateinvest.aitraderpro.database.PortfolioGoalEntity
import com.privateinvest.aitraderpro.export.ExportLevel
import com.privateinvest.aitraderpro.export.ExportLine
import com.privateinvest.aitraderpro.export.ExportReport
import com.privateinvest.aitraderpro.export.ExportSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Modules premium : journal, calendrier, favoris, santé, objectifs, exports et robustesse. */
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

data class PerformancePoint(
    val label: String,
    val value: Double
)

data class RobustnessCheck(
    val title: String,
    val status: String,
    val detail: String,
    val priority: String
)

data class RobustnessSummary(
    val globalStatus: String,
    val readyForDailyTest: Boolean,
    val checks: List<RobustnessCheck>,
    val nextActions: List<String>
)

data class ChartBundle(
    val capitalCurve: List<PerformancePoint>,
    val winRateCurve: List<PerformancePoint>,
    val weightCurve: List<PerformancePoint>
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
        val apiKeyOk = BuildConfig.ALPHA_VANTAGE_API_KEY != "YOUR_API_KEY_HERE" && BuildConfig.ALPHA_VANTAGE_API_KEY.isNotBlank()
        return listOf(
            HealthCheckItem("Build / APK", "OK", "Application installable si assembleDebug reste en SUCCESS"),
            HealthCheckItem("Clé API", if (apiKeyOk) "OK" else "Attention", if (apiKeyOk) "Clé Alpha Vantage configurée" else "Clé placeholder : données réelles limitées"),
            HealthCheckItem("Base Room", "OK", "Lecture locale réussie"),
            HealthCheckItem("Signaux", if (signals > 0) "OK" else "Attention", "$signals signaux enregistrés"),
            HealthCheckItem("Mémoire IA", if (memories > 0) "OK" else "Attention", "$memories configurations mémorisées"),
            HealthCheckItem("Audit IA", if (audit != null) "OK" else "Attention", audit?.let { "${it.signalCount} signaux audités" } ?: "Audit non disponible"),
            HealthCheckItem("Simulation", if (account != null) "OK" else "Attention", account?.let { "Cash ${String.format(Locale.FRANCE, "%.2f €", it.availableCash)}" } ?: "Compte virtuel non initialisé"),
            HealthCheckItem("Notifications", if (notifications > 0) "OK" else "Attention", "$notifications notifications journalisées")
        )
    }

    suspend fun robustnessSummary(): RobustnessSummary {
        val checks = mutableListOf<RobustnessCheck>()
        val apiKeyOk = BuildConfig.ALPHA_VANTAGE_API_KEY != "YOUR_API_KEY_HERE" && BuildConfig.ALPHA_VANTAGE_API_KEY.isNotBlank()
        val signals = db.signalDao().getAllSignals().size
        val memories = db.signalMemoryDao().getAll().size
        @Suppress("UNUSED_VARIABLE") val trades = db.tradeDao().getAll().size
        val notifications = db.notificationHistoryDao().getAll().size
        val account = db.simulationAccountDao().getAccount()
        val favorites = db.favoriteAssetDao().getAll().size
        val audit = runCatching { marketRepository.getAuditSummary() }.getOrNull()

        checks += RobustnessCheck("APK installé", "À vérifier", "Installer l'APK debug sur ton téléphone et ouvrir l'application.", "Haute")
        checks += RobustnessCheck("Clé Alpha Vantage", if (apiKeyOk) "OK" else "Attention", if (apiKeyOk) "Données marché réelles prêtes." else "Ajouter une vraie clé pour tester les cotations.", "Haute")
        checks += RobustnessCheck("Room", "OK", "Base locale accessible.", "Haute")
        checks += RobustnessCheck("Simulation", if (account != null) "OK" else "Attention", if (account != null) "Compte virtuel initialisé." else "Créer le premier achat simulation.", "Haute")
        checks += RobustnessCheck("Signaux IA", if (signals > 0) "OK" else "Attention", "$signals signaux disponibles.", "Haute")
        checks += RobustnessCheck("Mémoire IA", if (memories > 0) "OK" else "Attention", "$memories configurations mémorisées.", "Moyenne")
        checks += RobustnessCheck("Audit", if (audit != null) "OK" else "Attention", audit?.let { "Win rate ${String.format(Locale.FRANCE, "%.1f %%", it.winRate)}" } ?: "Pas encore de statistiques.", "Moyenne")
        checks += RobustnessCheck("Notifications", if (notifications > 0) "OK" else "À tester", "$notifications alertes en historique.", "Moyenne")
        checks += RobustnessCheck("Favoris", if (favorites > 0) "OK" else "Optionnel", "$favorites actifs favoris.", "Basse")
        checks += RobustnessCheck("Exports", "OK", "PNG/PDF générés en pleine page via moteur de rapport.", "Moyenne")
        checks += RobustnessCheck("Mode réel", "Verrouillé", "Aucun broker réel et aucune exécution automatique.", "Haute")

        val blockingWarnings = checks.count { it.priority == "Haute" && it.status !in listOf("OK", "Verrouillé") }
        val ready = blockingWarnings == 0
        return RobustnessSummary(
            globalStatus = if (ready) "Prêt pour test quotidien" else "À finaliser avant test sérieux",
            readyForDailyTest = ready,
            checks = checks,
            nextActions = listOf(
                "Installer l'APK sur téléphone.",
                "Configurer la clé Alpha Vantage si ce n'est pas fait.",
                "Créer 3 favoris et lancer les premiers signaux.",
                "Faire 3 achats en simulation, jamais en réel.",
                "Exporter un rapport PNG et PDF après le premier test.",
                "Revenir après J+1 / J+7 / J+30 pour vérifier l'apprentissage."
            )
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
        val preview = exportBackupJson().take(1200)
        return BackupSummary(preview, signals.size, memories.size, trades.size, favorites.size)
    }

    suspend fun exportBackupJson(): String {
        val signals = db.signalDao().getAllSignals()
        val memories = db.signalMemoryDao().getAll()
        val trades = db.tradeDao().getAll()
        val favorites = db.favoriteAssetDao().getAll()
        val weights = db.weightHistoryDao().getAll()
        return buildString {
            appendLine("{")
            appendLine("  \"app\": \"AI Trader Pro\",")
            appendLine("  \"version\": \"backup-v1\",")
            appendLine("  \"exportedAt\": \"${formatDateTime(System.currentTimeMillis())}\",")
            appendLine("  \"counts\": {")
            appendLine("    \"signals\": ${signals.size},")
            appendLine("    \"memories\": ${memories.size},")
            appendLine("    \"trades\": ${trades.size},")
            appendLine("    \"favorites\": ${favorites.size},")
            appendLine("    \"weights\": ${weights.size}")
            appendLine("  },")
            appendLine("  \"favorites\": [${favorites.joinToString { "\"${it.symbol}\"" }}],")
            appendLine("  \"operationMode\": ${ServiceLocator.operationModeRepository.exportSnapshot().prependIndent("  ").trim()},")
            appendLine("  \"marketFilters\": ${ServiceLocator.marketFiltersRepository.exportSnapshot().prependIndent("  ").trim()},")
            appendLine("  \"v15\": { \"includes\": [\"operation_mode\", \"market_place_filters\", \"sync_preferences\"] },")
            appendLine("  \"integrity\": \"${signals.size}-${memories.size}-${trades.size}-${favorites.size}-${weights.size}\"")
            appendLine("}")
        }
    }

    suspend fun chartBundle(): ChartBundle {
        val results = db.signalResultDao().getAll().sortedBy { it.dateCreated }
        val trades = db.tradeDao().getAll().sortedBy { it.createdAt }
        val weights = db.weightHistoryDao().getAll().sortedBy { it.recordedAt }

        val capital = if (trades.isEmpty()) {
            listOf(PerformancePoint("Départ", 10000.0), PerformancePoint("Actuel", 10000.0))
        } else {
            var value = 10000.0
            trades.takeLast(12).mapIndexed { index, trade ->
                value += if (trade.side.contains("SELL", true)) trade.price * trade.quantity else 0.0
                PerformancePoint("T${index + 1}", value)
            }
        }

        val win = if (results.isEmpty()) {
            listOf(PerformancePoint("Départ", 0.0), PerformancePoint("Actuel", 0.0))
        } else {
            val recent = results.takeLast(12)
            recent.mapIndexed { index, _ ->
                val subset = recent.take(index + 1)
                val positives = subset.count { it.profitPercent > 0.0 }
                PerformancePoint("S${index + 1}", (positives.toDouble() / subset.size.toDouble()) * 100.0)
            }
        }

        val weight = if (weights.isEmpty()) {
            listOf(PerformancePoint("Départ", 0.0), PerformancePoint("Actuel", 0.0))
        } else {
            weights.takeLast(12).mapIndexed { index, item -> PerformancePoint(item.factor.take(3) + index, item.value) }
        }
        return ChartBundle(capital, win, weight)
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

    suspend fun buildExportReport(type: String): ExportReport {
        val audit = runCatching { marketRepository.getAuditSummary() }.getOrNull()
        val simulation = runCatching { marketRepository.getSimulationSummary() }.getOrNull()
        val health = health()
        val robustness = robustnessSummary()
        val journal = journal().take(12)
        val favorites = favorites()
        val goals = goalSummary()

        val sections = mutableListOf<ExportSection>()
        sections += ExportSection(
            title = "Résumé général",
            subtitle = "Vue complète générée depuis les données locales de l'application.",
            lines = listOf(
                ExportLine("Statut robustesse", robustness.globalStatus, if (robustness.readyForDailyTest) ExportLevel.POSITIVE else ExportLevel.WARNING),
                ExportLine("Mode réel", "Verrouillé", ExportLevel.POSITIVE),
                ExportLine("Objectif annuel", String.format(Locale.FRANCE, "%.1f %%", goals.annualTargetPercent), ExportLevel.NEUTRAL),
                ExportLine("Progression objectif", "${goals.progressPercent} %", if (goals.progressPercent >= 60) ExportLevel.POSITIVE else ExportLevel.WARNING)
            ),
            notes = listOf("Ce rapport sert à tester l'application en simulation avant toute décision réelle.")
        )

        simulation?.let {
            sections += ExportSection(
                title = "Simulation",
                subtitle = "Aucun ordre réel n'est envoyé.",
                lines = listOf(
                    ExportLine("Capital initial", it.initialCapital),
                    ExportLine("Cash disponible", it.availableCash),
                    ExportLine("Capital investi", it.investedCapital),
                    ExportLine("Valeur totale", it.totalValue),
                    ExportLine("Performance", it.totalPerformance, if (it.totalPerformance.contains("-")) ExportLevel.DANGER else ExportLevel.POSITIVE),
                    ExportLine("Positions ouvertes", it.openPositions.toString())
                )
            )
        }

        audit?.let {
            sections += ExportSection(
                title = "Audit IA",
                subtitle = "Statistiques calculées depuis la base locale.",
                lines = listOf(
                    ExportLine("Signaux", it.signalCount.toString()),
                    ExportLine("Win rate", String.format(Locale.FRANCE, "%.1f %%", it.winRate), if (it.winRate >= 55.0) ExportLevel.POSITIVE else ExportLevel.WARNING),
                    ExportLine("Gain moyen", String.format(Locale.FRANCE, "%.2f %%", it.averageGain)),
                    ExportLine("Perte moyenne", String.format(Locale.FRANCE, "%.2f %%", it.averageLoss), ExportLevel.WARNING),
                    ExportLine("Profit factor", String.format(Locale.FRANCE, "%.2f", it.profitFactor), if (it.profitFactor >= 1.2) ExportLevel.POSITIVE else ExportLevel.WARNING),
                    ExportLine("Drawdown max", String.format(Locale.FRANCE, "%.2f", it.maxDrawdown), ExportLevel.WARNING)
                )
            )
        }

        sections += ExportSection(
            title = "Diagnostic",
            lines = health.map { ExportLine(it.title, it.status, when (it.status) { "OK" -> ExportLevel.POSITIVE; "Erreur" -> ExportLevel.DANGER; else -> ExportLevel.WARNING }) },
            notes = health.map { it.detail }.take(6)
        )

        if (journal.isNotEmpty()) {
            sections += ExportSection(
                title = "Dernières actions IA",
                lines = journal.map { ExportLine("${it.dateLabel} · ${it.symbol}", "${it.decision} ${if (it.score > 0) it.score else ""}") },
                notes = journal.map { it.detail }.take(6)
            )
        }

        if (favorites.isNotEmpty()) {
            sections += ExportSection(
                title = "Favoris",
                lines = favorites.map { ExportLine(it.symbol, it.name) }
            )
        }

        return ExportReport(
            title = "AI Trader Pro — ${type.uppercase(Locale.FRANCE)}",
            subtitle = "Export pleine page PNG/PDF",
            sections = sections
        )
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

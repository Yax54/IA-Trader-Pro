package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.database.SecurityLogEntity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class AdvancedStatsSummary(
    val signalCount: Int,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val averageGain: Double,
    val averageLoss: Double,
    val bestSymbol: String,
    val worstSymbol: String,
    val byMonth: List<StatsLine>,
    val bySymbol: List<StatsLine>,
    val byStrategy: List<StatsLine>
)

data class StatsLine(val label: String, val value: String, val score: Int)

data class PreferencesCenterState(
    val beginnerMode: Boolean,
    val currency: String = "€",
    val language: String = "Français",
    val syncFrequency: String = "Manuelle + ouverture application",
    val notifications: String = "Alertes fortes uniquement",
    val fontSize: String = "Grand",
    val theme: String = "Sombre premium"
)

data class PortfolioProfile(
    val name: String,
    val type: String,
    val value: String,
    val risk: String,
    val active: Boolean
)

data class DailyReportSummary(
    val title: String,
    val signalCount: Int,
    val simulatedTrades: Int,
    val globalPerformance: String,
    val winRate: String,
    val bestSignal: String,
    val worstSignal: String,
    val advice: String
)

data class OnboardingStep(val title: String, val description: String, val status: String)

class FinalModulesRepository {
    private val db get() = ServiceLocator.database
    private val marketRepository get() = ServiceLocator.marketRepository
    private val userPreferencesRepository get() = ServiceLocator.userPreferencesRepository

    suspend fun advancedStats(): AdvancedStatsSummary {
        val audit = marketRepository.getAuditSummary()
        val results = db.signalResultDao().getAll()
        val trades = db.tradeDao().getAll()
        val bySymbol = results
            .groupBy { it.symbol }
            .map { (symbol, items) ->
                val avg = items.map { it.profitPercent }.averageOrZero()
                StatsLine(symbol, formatPercent(avg), scoreFromPerformance(avg))
            }
            .sortedByDescending { it.score }
            .take(8)
        val best = bySymbol.firstOrNull()?.label ?: "Pas encore assez de données"
        val worst = bySymbol.lastOrNull()?.label ?: "Pas encore assez de données"
        val monthFormat = SimpleDateFormat("MM/yyyy", Locale.FRANCE)
        val byMonth = results
            .groupBy { monthFormat.format(Date(it.dateCreated)) }
            .map { (month, items) ->
                val avg = items.map { it.profitPercent }.averageOrZero()
                StatsLine(month, formatPercent(avg), scoreFromPerformance(avg))
            }
            .sortedBy { it.label }
            .takeLast(6)
        val byStrategy = listOf(
            StatsLine("Signaux ACHETER", "${results.count { it.signal.contains("ACHETER", true) }} cas", audit.winRate.toInt().coerceIn(0, 100)),
            StatsLine("Mémoire IA", "${db.signalMemoryDao().getAll().size} configurations", 75),
            StatsLine("Simulation", "${trades.size} opérations", 70)
        )
        return AdvancedStatsSummary(
            signalCount = audit.signalCount,
            winRate = audit.winRate,
            profitFactor = audit.profitFactor,
            maxDrawdown = audit.maxDrawdown,
            averageGain = audit.averageGain,
            averageLoss = audit.averageLoss,
            bestSymbol = best,
            worstSymbol = worst,
            byMonth = byMonth,
            bySymbol = bySymbol,
            byStrategy = byStrategy
        )
    }

    suspend fun preferences(): PreferencesCenterState {
        val beginner = userPreferencesRepository.beginnerModeEnabled.first()
        return PreferencesCenterState(beginnerMode = beginner)
    }

    suspend fun setBeginnerMode(enabled: Boolean) {
        userPreferencesRepository.setBeginnerModeEnabled(enabled)
        db.securityLogDao().insert(SecurityLogEntity(event = "PREFERENCES", level = "INFO", details = "Mode ${if (enabled) "débutant" else "expert"} activé", createdAt = System.currentTimeMillis()))
    }

    suspend fun portfolios(): List<PortfolioProfile> {
        val simulation = marketRepository.getSimulationSummary()
        return listOf(
            PortfolioProfile("Simulation IA", "Virtuel", simulation.totalValue, "Sécurisé", true),
            PortfolioProfile("Portefeuille réel", "Broker", "À connecter", "Protégé par PIN", false),
            PortfolioProfile("Long terme", "Suivi", "À créer", "Modéré", false),
            PortfolioProfile("Dividendes", "Suivi", "À créer", "Prudent", false)
        )
    }

    suspend fun dailyReport(): DailyReportSummary {
        val audit = marketRepository.getAuditSummary()
        val signals = marketRepository.getSignals()
        val trades = db.tradeDao().getAll()
        val best = signals.maxByOrNull { it.score }
        val worst = signals.minByOrNull { it.score }
        return DailyReportSummary(
            title = "Rapport du jour",
            signalCount = signals.size,
            simulatedTrades = trades.size,
            globalPerformance = formatPercent(audit.averageGain - abs(audit.averageLoss) / 2.0),
            winRate = String.format(Locale.FRANCE, "%.1f %%", audit.winRate),
            bestSignal = best?.let { "${it.symbol} · ${it.action.name} · ${it.score}/100" } ?: "Aucun signal disponible",
            worstSignal = worst?.let { "${it.symbol} · ${it.action.name} · ${it.score}/100" } ?: "Aucun signal disponible",
            advice = when {
                audit.signalCount < 30 -> "Continue la simulation : il faut plus d'historique avant de juger l'IA."
                audit.profitFactor >= 1.5 -> "L'IA montre des signes intéressants, à confirmer sur plusieurs semaines."
                else -> "Reste prudent : la performance historique n'est pas encore assez solide."
            }
        )
    }

    suspend fun onboarding(): List<OnboardingStep> {
        val favorites = db.favoriteAssetDao().getAll().size
        val memories = db.signalMemoryDao().getAll().size
        val securityLogs = db.securityLogDao().getLatest(5).size
        return listOf(
            OnboardingStep("Profil", "Mode débutant actif par défaut, lisible et pédagogique.", "OK"),
            OnboardingStep("Simulation", "Capital virtuel prêt pour tester sans argent réel.", "OK"),
            OnboardingStep("Favoris", "$favorites actifs favoris enregistrés.", if (favorites > 0) "OK" else "À faire"),
            OnboardingStep("Mémoire IA", "$memories configurations mémorisées.", if (memories > 0) "OK" else "À alimenter"),
            OnboardingStep("Sécurité", "PIN/empreinte uniquement pour les actions sensibles.", if (securityLogs > 0) "OK" else "À configurer")
        )
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
    private fun scoreFromPerformance(value: Double): Int = (50 + value * 8).toInt().coerceIn(0, 100)
    private fun formatPercent(value: Double): String = if (value >= 0) String.format(Locale.FRANCE, "+%.2f %%", value) else String.format(Locale.FRANCE, "%.2f %%", value)
}

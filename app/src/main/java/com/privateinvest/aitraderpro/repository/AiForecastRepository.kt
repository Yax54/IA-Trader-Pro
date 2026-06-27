package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.database.AiForecastDao
import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeDao
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import com.privateinvest.aitraderpro.database.AppDatabase
import com.privateinvest.aitraderpro.notifications.TraderNotificationManager
import kotlin.math.abs
import kotlin.math.exp

// ─── CONSTANTES DE MÉMOIRE ───────────────────────────────────────────────────

/** Identifiant des types de mémoire — totalement indépendants. */
object MemoryType {
    const val CT = "CT"   // Court Terme : QUICK, SWING, HIGH_VOLATILITY
    const val LT = "LT"   // Long Terme  : LONG_TERM, GROWTH, DEFENSIVE
}

/** Buckets mémoire disponibles. */
object MemoryBucket {
    const val J7    = "7J"
    const val J30   = "30J"
    const val J90   = "90J"
    const val M3    = "3M"
    const val M6    = "6M"
    const val M12   = "12M"
    const val GLOBAL = "GLOBAL"
}

/** Mapping stratégie → type de mémoire. */
fun strategyMemoryType(strategyType: String): String = when (strategyType) {
    "QUICK", "SWING", "HIGH_VOLATILITY" -> MemoryType.CT
    "LONG_TERM", "GROWTH", "DEFENSIVE"  -> MemoryType.LT
    else -> MemoryType.CT
}

/** Mapping stratégie → bucket mémoire par défaut. */
fun strategyDefaultBucket(strategyType: String): String = when (strategyType) {
    "QUICK"          -> MemoryBucket.J7
    "SWING"          -> MemoryBucket.J30
    "HIGH_VOLATILITY"-> MemoryBucket.J30
    "LONG_TERM"      -> MemoryBucket.M6
    "GROWTH"         -> MemoryBucket.M3
    "DEFENSIVE"      -> MemoryBucket.M6
    else -> MemoryBucket.J30
}

/** Durée en jours correspondant à un bucket mémoire. */
fun bucketDays(bucket: String): Long = when (bucket) {
    MemoryBucket.J7   -> 7L
    MemoryBucket.J30  -> 30L
    MemoryBucket.J90  -> 90L
    MemoryBucket.M3   -> 90L
    MemoryBucket.M6   -> 180L
    MemoryBucket.M12  -> 365L
    else              -> 365L
}

// ─── MODÈLES DE DONNÉES ──────────────────────────────────────────────────────

/** Résultat de confiance calculé par bucket. */
data class BucketConfidence(
    val bucket: String,
    val count: Int,
    val winRate: Double,        // 0.0 – 1.0
    val avgPerformance: Double, // en %
    val confidenceScore: Double // 0.0 – 100.0
)

/** Résultats statistiques par stratégie. */
data class StrategyStats(
    val strategyType: String,
    val strategyLabel: String,
    val memoryType: String,
    val count: Int,
    val winRate: Double,
    val avgGain: Double,
    val avgLoss: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val isReliable: Boolean      // true si count >= MIN_FORECASTS_FOR_STATS
)

/** Stats par secteur. */
data class SectorStats(
    val sector: String,
    val count: Int,
    val winRate: Double,
    val avgPerformance: Double
)

/** Comparaison pronostic IA vs signal joué. */
data class ForecastVsPlayedResult(
    val symbol: String,
    val forecastPerformance: Double?,
    val playedPerformance: Double?,
    val delta: Double?,             // forecastPerf - playedPerf
    val forecastWonOverPlayed: Boolean?
)

/** Résumé d'un pronostic remarquable non joué. */
data class RemarkableForecast(
    val forecast: AiForecastEntity,
    val outcome: AiForecastOutcomeEntity,
    val bestPerformance: Double,
    val horizon: String             // "J+1", "J+3", "J+7", "J+30", "J+90"
)

// ─── REPOSITORY PRINCIPAL ────────────────────────────────────────────────────

/**
 * Repository central pour le module Pronostics IA + Mémoire Glissante.
 *
 * Deux mémoires totalement indépendantes :
 * - CT (Court Terme) : QUICK / SWING / HIGH_VOLATILITY — buckets 7J / 30J / 90J
 * - LT (Long Terme)  : LONG_TERM / GROWTH / DEFENSIVE — buckets 3M / 6M / 12M
 *
 * Les résultats LT n'influencent JAMAIS les statistiques CT et vice-versa.
 */
class AiForecastRepository(
    private val db: AppDatabase,
    private val marketRepository: MarketRepository,
    private val notificationManager: TraderNotificationManager
) {

    private val forecastDao: AiForecastDao get() = db.aiForecastDao()
    private val outcomeDao: AiForecastOutcomeDao get() = db.aiForecastOutcomeDao()

    companion object {
        /** Nombre minimum de pronostics avant affichage des statistiques. */
        const val MIN_FORECASTS_FOR_STATS = 10

        /** Seuil de performance pour une alerte "remarquable" (en %). */
        const val REMARKABLE_THRESHOLD = 5.0

        /** Quotas par défaut par stratégie. */
        val DEFAULT_DAILY_QUOTA = mapOf(
            "QUICK"           to 5,
            "SWING"           to 5,
            "HIGH_VOLATILITY" to 2
        )
        val DEFAULT_WEEKLY_QUOTA = mapOf(
            "LONG_TERM"  to 2,
            "GROWTH"     to 2,
            "DEFENSIVE"  to 2
        )
    }

    // ─── QUOTAS INTELLIGENTS ─────────────────────────────────────────────────

    /**
     * Vérifie si le quota de la stratégie est atteint.
     * CT : quota journalier | LT : quota hebdomadaire.
     */
    suspend fun isQuotaReached(
        strategyType: String,
        dailyQuota: Int = DEFAULT_DAILY_QUOTA[strategyType] ?: 5,
        weeklyQuota: Int = DEFAULT_WEEKLY_QUOTA[strategyType] ?: 2
    ): Boolean {
        val memType = strategyMemoryType(strategyType)
        val now = System.currentTimeMillis()
        return if (memType == MemoryType.CT) {
            val since = now - 24 * 60 * 60 * 1000L
            forecastDao.countSince(strategyType, since) >= dailyQuota
        } else {
            val since = now - 7 * 24 * 60 * 60 * 1000L
            forecastDao.countSince(strategyType, since) >= weeklyQuota
        }
    }

    /**
     * Sélectionne les meilleurs pronostics à enregistrer selon les quotas.
     * Filtre par score décroissant, respecte le quota de chaque stratégie.
     */
    suspend fun selectBestForecasts(
        candidates: List<AiForecastEntity>,
        dailyQuotas: Map<String, Int> = DEFAULT_DAILY_QUOTA,
        weeklyQuotas: Map<String, Int> = DEFAULT_WEEKLY_QUOTA
    ): List<AiForecastEntity> {
        val selected = mutableListOf<AiForecastEntity>()
        val countByStrategy = mutableMapOf<String, Int>()

        // Tri par score décroissant — seuls les meilleurs passent
        val sorted = candidates.sortedByDescending { it.score }

        for (candidate in sorted) {
            val strategy = candidate.strategyType
            val memType = strategyMemoryType(strategy)
            val currentCount = countByStrategy.getOrDefault(strategy, 0)

            val quota = if (memType == MemoryType.CT) {
                dailyQuotas[strategy] ?: DEFAULT_DAILY_QUOTA[strategy] ?: 5
            } else {
                weeklyQuotas[strategy] ?: DEFAULT_WEEKLY_QUOTA[strategy] ?: 2
            }

            // Vérifier aussi le quota en base (persistant entre runs)
            val alreadyStored = if (memType == MemoryType.CT) {
                val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                forecastDao.countSince(strategy, since)
            } else {
                val since = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                forecastDao.countSince(strategy, since)
            }

            if ((currentCount + alreadyStored) < quota) {
                selected.add(candidate)
                countByStrategy[strategy] = currentCount + 1
            }
        }
        return selected
    }

    // ─── ENREGISTREMENT D'UN PRONOSTIC ───────────────────────────────────────

    /**
     * Insère un pronostic IA après vérification du quota.
     * Retourne l'id inséré, ou -1 si quota atteint.
     */
    suspend fun recordForecast(
        forecast: AiForecastEntity,
        dailyQuota: Int = DEFAULT_DAILY_QUOTA[forecast.strategyType] ?: 5,
        weeklyQuota: Int = DEFAULT_WEEKLY_QUOTA[forecast.strategyType] ?: 2
    ): Long {
        if (isQuotaReached(forecast.strategyType, dailyQuota, weeklyQuota)) return -1L
        return forecastDao.insert(forecast)
    }

    /** Marque un pronostic comme joué par l'utilisateur. */
    suspend fun markAsPlayed(forecastId: Long) {
        val forecast = forecastDao.getAll().find { it.id == forecastId } ?: return
        forecastDao.update(forecast.copy(userAction = "PLAYED"))
    }

    /** Marque un pronostic comme ignoré par l'utilisateur. */
    suspend fun markAsIgnored(forecastId: Long) {
        val forecast = forecastDao.getAll().find { it.id == forecastId } ?: return
        forecastDao.update(forecast.copy(userAction = "IGNORED"))
    }

    // ─── COEFFICIENT DE DÉCROISSANCE ─────────────────────────────────────────

    /**
     * Calcule le coefficient de décroissance d'un pronostic selon son ancienneté
     * et l'horizon défini (horizonDays).
     *
     * Formule : decay = exp(-λ * age_ratio)
     * où age_ratio = ageJours / horizonDays
     * λ = 1.5 pour CT (décroissance rapide), 0.8 pour LT (décroissance lente)
     *
     * Retourne entre 0.0 (complètement dégradé) et 1.0 (frais).
     */
    fun decayScore(forecast: AiForecastEntity): Double {
        val now = System.currentTimeMillis()
        val ageMs = now - forecast.createdAt
        val ageDays = ageMs / (24 * 60 * 60 * 1000.0)
        val horizon = forecast.horizonDays.coerceAtLeast(1).toDouble()
        val ageRatio = ageDays / horizon
        val lambda = if (strategyMemoryType(forecast.strategyType) == MemoryType.CT) 1.5 else 0.8
        return exp(-lambda * ageRatio).coerceIn(0.0, 1.0)
    }

    /** Score ajusté par le coefficient de décroissance. */
    fun adjustedScore(forecast: AiForecastEntity): Double =
        forecast.score * decayScore(forecast)

    // ─── SCORE DE CONFIANCE PAR BUCKET ───────────────────────────────────────

    /**
     * Calcule le score de confiance de l'IA par bucket mémoire.
     * Basé sur : winRate, avgPerformance et nombre de pronostics.
     * Retourne null si < MIN_FORECASTS_FOR_STATS.
     */
    suspend fun confidenceByBucket(memoryType: String): List<BucketConfidence> {
        val buckets = if (memoryType == MemoryType.CT)
            listOf(MemoryBucket.J7, MemoryBucket.J30, MemoryBucket.J90)
        else
            listOf(MemoryBucket.M3, MemoryBucket.M6, MemoryBucket.M12)

        return buckets.mapNotNull { bucket ->
            val forecasts = forecastDao.getByBucket(bucket)
                .filter { it.memoryType == memoryType && it.finalPerformancePercent != null }
            if (forecasts.size < MIN_FORECASTS_FOR_STATS) return@mapNotNull null

            val perfs = forecasts.mapNotNull { it.finalPerformancePercent }
            val wins = perfs.count { it > 0 }
            val winRate = wins.toDouble() / perfs.size
            val avgPerf = if (perfs.isEmpty()) 0.0 else perfs.average()

            // Score de confiance composite : 60% winRate + 40% performance normalisée
            val perfNorm = (avgPerf / 10.0).coerceIn(-1.0, 1.0) // normalise ±10% → ±1
            val confidence = ((winRate * 60.0) + ((perfNorm + 1.0) / 2.0 * 40.0)).coerceIn(0.0, 100.0)

            BucketConfidence(
                bucket = bucket,
                count = forecasts.size,
                winRate = winRate,
                avgPerformance = avgPerf,
                confidenceScore = confidence
            )
        }
    }

    // ─── STATISTIQUES PAR STRATÉGIE ──────────────────────────────────────────

    /**
     * Calcule les statistiques par stratégie pour une mémoire donnée (CT ou LT).
     * Uniquement si count >= MIN_FORECASTS_FOR_STATS.
     */
    suspend fun statsByStrategy(memoryType: String): List<StrategyStats> {
        val strategies = if (memoryType == MemoryType.CT)
            listOf("QUICK", "SWING", "HIGH_VOLATILITY")
        else
            listOf("LONG_TERM", "GROWTH", "DEFENSIVE")

        return strategies.mapNotNull { strategy ->
            val forecasts = forecastDao.getClosedWithPerformance(memoryType)
                .filter { it.strategyType == strategy }
            val label = forecasts.firstOrNull()?.strategyLabel ?: strategyDisplayLabel(strategy)
            val perfs = forecasts.mapNotNull { it.finalPerformancePercent }

            StrategyStats(
                strategyType = strategy,
                strategyLabel = label,
                memoryType = memoryType,
                count = perfs.size,
                winRate = PerformanceAnalyzer.computeWinRate(perfs),
                avgGain = PerformanceAnalyzer.computeAverageGain(perfs),
                avgLoss = PerformanceAnalyzer.computeAverageLoss(perfs),
                profitFactor = PerformanceAnalyzer.computeProfitFactor(perfs),
                maxDrawdown = PerformanceAnalyzer.computeMaxDrawdown(perfs),
                isReliable = perfs.size >= MIN_FORECASTS_FOR_STATS
            )
        }
    }

    // ─── STATISTIQUES PAR SECTEUR ────────────────────────────────────────────

    /**
     * Statistiques par secteur (basées sur les 4 premières lettres du symbole
     * comme proxy secteur, ou via le champ notes si encodé).
     * NOTE : améliorer avec un champ sector dédié si disponible.
     */
    suspend fun statsBySector(memoryType: String): List<SectorStats> {
        val forecasts = forecastDao.getClosedWithPerformance(memoryType)
        // Regroupement par secteur via le préfixe du symbole (proxy)
        val bySector = forecasts.groupBy { extractSector(it.symbol, it.notes) }

        return bySector.mapNotNull { (sector, items) ->
            if (sector == "UNKNOWN") return@mapNotNull null
            val perfs = items.mapNotNull { it.finalPerformancePercent }
            if (perfs.isEmpty()) return@mapNotNull null
            val wins = perfs.count { it > 0 }
            SectorStats(
                sector = sector,
                count = perfs.size,
                winRate = wins.toDouble() / perfs.size,
                avgPerformance = perfs.average()
            )
        }.sortedByDescending { it.avgPerformance }
    }

    /**
     * Extrait le secteur depuis les notes du pronostic (format "sector:TECH")
     * ou retourne un proxy basé sur le symbole.
     */
    private fun extractSector(symbol: String, notes: String): String {
        val sectorRegex = Regex("sector:([A-Z_]+)")
        val match = sectorRegex.find(notes)
        if (match != null) return match.groupValues[1]
        // Proxy basé sur le symbole : insuffisant pour la production mais fonctionnel
        return when {
            symbol.startsWith("AAPL") || symbol.startsWith("MSFT") || symbol.startsWith("GOOGL") -> "TECH"
            symbol.startsWith("JPM") || symbol.startsWith("GS") || symbol.startsWith("BAC") -> "FINANCE"
            symbol.startsWith("XOM") || symbol.startsWith("CVX") -> "ENERGIE"
            symbol.startsWith("JNJ") || symbol.startsWith("PFE") -> "SANTE"
            else -> "DIVERS"
        }
    }

    // ─── PRONOSTICS NON JOUÉS — ALERTES REMARQUABLES ─────────────────────────

    /**
     * Détecte les pronostics non joués qui ont atteint une performance > REMARKABLE_THRESHOLD.
     * Envoie une notification pour chacun si non encore alerté.
     */
    suspend fun detectAndAlertRemarkableUnplayed(): List<RemarkableForecast> {
        val remarkable = forecastDao.getRemarkableUnplayed(REMARKABLE_THRESHOLD)
        val result = mutableListOf<RemarkableForecast>()

        for (forecast in remarkable) {
            val outcome = outcomeDao.findByForecastId(forecast.id) ?: continue
            val (bestPerf, horizon) = bestOutcomeValue(outcome) ?: continue

            if (bestPerf >= REMARKABLE_THRESHOLD) {
                result.add(RemarkableForecast(forecast, outcome, bestPerf, horizon))

                // Notification push
                notificationManager.publishNotification(
                    title = "🧠 Pronostic IA non joué : +${String.format("%.1f", bestPerf)}%",
                    message = "${forecast.name} (${forecast.symbol}) — ${forecast.strategyLabel} — Non joué",
                    notificationId = (forecast.id % Int.MAX_VALUE).toInt()
                )
            }
        }
        return result
    }

    // ─── COMPARAISON PRONOSTIC IA vs SIGNAL JOUÉ ─────────────────────────────

    /**
     * Compare les pronostics IA vs les signaux joués pour le même symbole.
     * Retourne la différence de performance (IA - joué).
     */
    suspend fun compareForecastVsPlayed(): List<ForecastVsPlayedResult> {
        val played = forecastDao.getAllPlayed()
        val results = mutableListOf<ForecastVsPlayedResult>()

        for (forecast in played) {
            val forecastOutcome = outcomeDao.findByForecastId(forecast.id)
            val forecastPerf = forecastOutcome?.let {
                it.performanceJ30 ?: it.performanceJ7 ?: it.performanceJ1
            }

            // Récupérer le signal joué correspondant dans la mémoire des signaux
            val playedSignals = db.signalOutcomeDao().getAll().filter { outcome ->
                db.signalMemoryDao().findById(outcome.signalId)?.symbol == forecast.symbol
            }
            val playedPerf = playedSignals.firstOrNull()?.let {
                it.performanceJ30 ?: it.performanceJ7 ?: it.performanceJ1
            }

            val delta = if (forecastPerf != null && playedPerf != null)
                forecastPerf - playedPerf else null

            results.add(ForecastVsPlayedResult(
                symbol = forecast.symbol,
                forecastPerformance = forecastPerf,
                playedPerformance = playedPerf,
                delta = delta,
                forecastWonOverPlayed = if (delta != null) delta > 0 else null
            ))
        }
        return results
    }

    // ─── MISE À JOUR DES OUTCOMES (J+1 / J+3 / J+7 / J+30 / J+90) ──────────

    /**
     * Rafraîchit les outcomes des pronostics actifs.
     * Appelé par le WorkManager toutes les 6h.
     *
     * Pour chaque pronostic ACTIVE, tente de récupérer le prix actuel
     * et calcule la performance selon les horizons écoulés.
     */
    suspend fun refreshOutcomes() {
        val active = forecastDao.getAllPending() + forecastDao.getAllPlayed()
            .filter { it.status == "ACTIVE" }

        for (forecast in active) {
            refreshOutcomeForForecast(forecast)
        }

        // Mettre à jour les statuts des pronostics expirés
        val all = forecastDao.getAll()
        val now = System.currentTimeMillis()
        for (forecast in all) {
            if (forecast.status == "ACTIVE" && forecast.dueAt < now) {
                val outcome = outcomeDao.findByForecastId(forecast.id)
                val finalPerf = outcome?.let {
                    it.performanceJ90 ?: it.performanceJ30 ?: it.performanceJ7 ?: it.performanceJ1
                }
                forecastDao.update(forecast.copy(
                    status = "EXPIRED",
                    closedAt = now,
                    finalPerformancePercent = finalPerf,
                    closeReason = "Horizon atteint"
                ))
            }
        }

        // Purger les pronostics expirés de plus de 12 mois
        val cutoff = now - 365L * 24 * 60 * 60 * 1000L
        forecastDao.purgeExpired(cutoff)
    }

    private suspend fun refreshOutcomeForForecast(forecast: AiForecastEntity) {
        val now = System.currentTimeMillis()
        val ageMs = now - forecast.createdAt
        val ageDays = ageMs / (24 * 60 * 60 * 1000.0)

        // Tenter de récupérer le prix actuel via le MarketRepository
        // Tenter de récupérer les données de marché via le repository
        val assetDetail = try {
            marketRepository.getMarketData(forecast.symbol)
        } catch (e: Exception) { null }
        val currentPrice: Double? = assetDetail?.quotePrice?.takeIf { it > 0.0 }

        if (currentPrice == null || currentPrice <= 0.0) return
        val entry = forecast.entryPrice
        if (entry <= 0.0) return

        val existing = outcomeDao.findByForecastId(forecast.id)
        val perf = ((currentPrice - entry) / entry) * 100.0

        val updated = (existing ?: AiForecastOutcomeEntity(
            forecastId = forecast.id,
            symbol = forecast.symbol,
            updatedAt = now
        )).copy(
            priceJ1   = if (ageDays >= 1.0)  currentPrice else existing?.priceJ1,
            priceJ3   = if (ageDays >= 3.0)  currentPrice else existing?.priceJ3,
            priceJ7   = if (ageDays >= 7.0)  currentPrice else existing?.priceJ7,
            priceJ30  = if (ageDays >= 30.0) currentPrice else existing?.priceJ30,
            priceJ90  = if (ageDays >= 90.0) currentPrice else existing?.priceJ90,
            performanceJ1   = if (ageDays >= 1.0)  ((currentPrice - entry) / entry * 100.0) else existing?.performanceJ1,
            performanceJ3   = if (ageDays >= 3.0)  perf else existing?.performanceJ3,
            performanceJ7   = if (ageDays >= 7.0)  perf else existing?.performanceJ7,
            performanceJ30  = if (ageDays >= 30.0) perf else existing?.performanceJ30,
            performanceJ90  = if (ageDays >= 90.0) perf else existing?.performanceJ90,
            successJ1  = if (ageDays >= 1.0) perf > 0.0 else existing?.successJ1,
            successJ3  = if (ageDays >= 3.0) perf > 0.0 else existing?.successJ3,
            successJ7  = if (ageDays >= 7.0) perf > 0.0 else existing?.successJ7,
            successJ30 = if (ageDays >= 30.0) perf > 0.0 else existing?.successJ30,
            successJ90 = if (ageDays >= 90.0) perf > 0.0 else existing?.successJ90,
            updatedAt = now
        )

        if (existing == null) outcomeDao.insert(updated) else outcomeDao.update(updated)

        // Vérifier si target ou stop atteint
        val targetPrice = entry * (1.0 + forecast.targetPercent / 100.0)
        val stopPrice   = entry * (1.0 - forecast.stopPercent  / 100.0)
        if (currentPrice >= targetPrice) {
            forecastDao.update(forecast.copy(
                status = "HIT_TARGET",
                closedAt = now,
                finalPerformancePercent = perf,
                closeReason = "Objectif atteint (+${String.format("%.1f", forecast.targetPercent)}%)"
            ))
        } else if (currentPrice <= stopPrice) {
            forecastDao.update(forecast.copy(
                status = "HIT_STOP",
                closedAt = now,
                finalPerformancePercent = perf,
                closeReason = "Stop-loss déclenché (-${String.format("%.1f", abs(perf))}%)"
            ))
        }
    }

    // ─── ALIMENTATION DE L'APPRENTISSAGE ADAPTATIF ───────────────────────────

    /**
     * Alimente AdaptiveWeightsManager avec les outcomes des pronostics non joués.
     * C'est la fonction qui permet à l'application d'apprendre même sans signaux joués.
     *
     * Clé architecturale : séparation stricte CT/LT pour ne pas contaminer les poids.
     */
    suspend fun feedAdaptiveWeightsFromForecasts(
        adaptiveWeightsManager: AdaptiveWeightsManager,
        memoryType: String
    ) {
        val forecastsWithOutcomes = forecastDao.getByMemoryType(memoryType)
            .filter { it.userAction == "NONE" || it.userAction == "IGNORED" }
            .filter { it.finalPerformancePercent != null || it.status in listOf("EXPIRED", "HIT_TARGET", "HIT_STOP") }

        if (forecastsWithOutcomes.isEmpty()) return

        val outcomesUnplayed = outcomeDao.getOutcomesForUnplayedByMemoryType(memoryType)
        if (outcomesUnplayed.isEmpty()) return

        adaptiveWeightsManager.updateWeightsFromForecastOutcomes(
            forecasts = forecastsWithOutcomes,
            outcomes = outcomesUnplayed
        )
    }

    // ─── LECTURE DES DONNÉES ─────────────────────────────────────────────────

    /** Pronostics actifs CT (non joués, triés par score décroissant). */
    suspend fun getActiveCT(): List<AiForecastEntity> =
        forecastDao.getActivePendingByMemoryType(MemoryType.CT)
            .sortedByDescending { adjustedScore(it) }

    /** Pronostics actifs LT (non joués, triés par score décroissant). */
    suspend fun getActiveLT(): List<AiForecastEntity> =
        forecastDao.getActivePendingByMemoryType(MemoryType.LT)
            .sortedByDescending { adjustedScore(it) }

    /** Tous les pronostics CT (actifs + historique). */
    suspend fun getAllCT(): List<AiForecastEntity> =
        forecastDao.getByMemoryType(MemoryType.CT)

    /** Tous les pronostics LT (actifs + historique). */
    suspend fun getAllLT(): List<AiForecastEntity> =
        forecastDao.getByMemoryType(MemoryType.LT)

    /** Outcome d'un pronostic. */
    suspend fun getOutcome(forecastId: Long): AiForecastOutcomeEntity? =
        outcomeDao.findByForecastId(forecastId)

    /** Tous les outcomes. */
    suspend fun getAllOutcomes(): List<AiForecastOutcomeEntity> =
        outcomeDao.getAll()

    // ─── GRAPHIQUE DE PRÉCISION IA ───────────────────────────────────────────

    /**
     * Construit les données pour le graphique de précision de l'IA dans le temps.
     * Retourne une liste de (timestamp, winRate) triée chronologiquement.
     * Fenêtre glissante de 30 pronostics.
     */
    suspend fun precisionOverTime(memoryType: String): List<Pair<Long, Double>> {
        val closed = forecastDao.getClosedWithPerformance(memoryType)
            .sortedBy { it.closedAt ?: it.createdAt }

        if (closed.size < MIN_FORECASTS_FOR_STATS) return emptyList()

        val windowSize = 10
        val result = mutableListOf<Pair<Long, Double>>()

        for (i in windowSize until closed.size + 1) {
            val window = closed.subList(maxOf(0, i - windowSize), i)
            val perfs = window.mapNotNull { it.finalPerformancePercent }
            if (perfs.isEmpty()) continue
            val winRate = perfs.count { it > 0 }.toDouble() / perfs.size
            val ts = closed[i - 1].closedAt ?: closed[i - 1].createdAt
            result.add(ts to winRate)
        }
        return result
    }

    // ─── EXPORT CSV ──────────────────────────────────────────────────────────

    /**
     * Génère le contenu CSV de tous les pronostics.
     * Format : id, symbol, name, strategyType, memoryType, bucket, score,
     *          confidence, entryPrice, target%, stop%, horizonDays, status,
     *          userAction, createdAt, closedAt, finalPerf%, closeReason
     */
    suspend fun exportToCsv(memoryType: String? = null): String {
        val forecasts = if (memoryType != null)
            forecastDao.getByMemoryType(memoryType)
        else
            forecastDao.getAll()

        val sb = StringBuilder()
        sb.appendLine("id,symbol,name,strategyType,memoryType,memoryBucket,score,confidence,entryPrice,targetPercent,stopPercent,horizonDays,status,userAction,createdAt,closedAt,finalPerformancePercent,closeReason")

        for (f in forecasts) {
            val closedDate = if (f.closedAt != null) formatDate(f.closedAt) else ""
            sb.appendLine(
                "${f.id},${f.symbol},\"${f.name}\",${f.strategyType},${f.memoryType},${f.memoryBucket}," +
                "${f.score},${f.confidence},${f.entryPrice},${f.targetPercent},${f.stopPercent}," +
                "${f.horizonDays},${f.status},${f.userAction}," +
                "${formatDate(f.createdAt)},${closedDate}," +
                "${f.finalPerformancePercent ?: ""},\"${f.closeReason ?: ""}\""
            )
        }
        return sb.toString()
    }

    private fun formatDate(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }

    // ─── UTILITAIRES ─────────────────────────────────────────────────────────

    private fun bestOutcomeValue(outcome: AiForecastOutcomeEntity): Pair<Double, String>? {
        val candidates = listOf(
            outcome.performanceJ90 to "J+90",
            outcome.performanceJ30 to "J+30",
            outcome.performanceJ7  to "J+7",
            outcome.performanceJ3  to "J+3",
            outcome.performanceJ1  to "J+1"
        ).mapNotNull { (perf, horizon) -> if (perf != null) perf to horizon else null }

        return candidates.maxByOrNull { it.first }
    }

    private fun strategyDisplayLabel(strategyType: String): String = when (strategyType) {
        "QUICK"           -> "Opportunité rapide"
        "SWING"           -> "Swing"
        "HIGH_VOLATILITY" -> "Forte volatilité"
        "LONG_TERM"       -> "Long terme"
        "GROWTH"          -> "Croissance"
        "DEFENSIVE"       -> "Défensif"
        else -> strategyType
    }
}

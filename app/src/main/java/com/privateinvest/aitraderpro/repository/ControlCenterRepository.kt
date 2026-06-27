package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.BuildConfig
import com.privateinvest.aitraderpro.ServiceLocator
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ─── Modèles de données ────────────────────────────────────────────────────────

data class ControlModuleStatus(
    val title: String,
    val icon: String,
    val status: String,
    val score: Int,
    val summary: String,
    val details: List<Pair<String, String>>,
    val route: String? = null,
    val blocking: Boolean = false
)

data class ControlCenterState(
    val globalScore: Int,
    val globalLabel: String,
    val globalMessage: String,
    val modules: List<ControlModuleStatus>,
    val latestEvents: List<String>,
    val checkedAtLabel: String
)

data class ValidationCheckItem(
    val title: String,
    val status: String,
    val detail: String,
    val blocking: Boolean
)

data class RealValidationState(
    val score: Int,
    val statusLabel: String,
    val canUseRealMode: Boolean,
    val hardBlocks: List<ValidationCheckItem>,
    val warnings: List<ValidationCheckItem>,
    val brokerDiagnostic: List<ValidationCheckItem>,
    val latestBackupLabel: String,
    val checkedAtLabel: String
)

// ─── Repository — agrégateur pur ───────────────────────────────────────────────
/**
 * ControlCenterRepository est un AGRÉGATEUR PUR.
 *
 * Règle architecturale V1.4 :
 * Chaque carte lit les repositories existants via ServiceLocator sans dupliquer
 * la logique métier. Les calculs restent dans leurs modules d'origine :
 *   📊 Santé IA       → MarketRepository.getAuditSummary()
 *   📈 Flux Marché    → MarketDataConnectionRepository.inspect()
 *   💰 Broker         → MarketDataConnectionRepository.inspect() (données broker)
 *   🔒 Sécurité       → SecurityRepository.securityState
 *   📦 Sauvegarde     → BackupSafetyRepository.latestLocalSnapshot()
 *   🤖 Pronostics IA  → AiForecastDao / AiForecastOutcomeDao
 *   📉 Stratégies     → StrategyFollowUpDao
 */
class ControlCenterRepository {
    private val db get() = ServiceLocator.database
    private val marketRepository get() = ServiceLocator.marketRepository
    private val marketDataRepository get() = ServiceLocator.marketDataConnectionRepository
    private val securityRepository get() = ServiceLocator.securityRepository
    private val backupSafetyRepository get() = ServiceLocator.backupSafetyRepository

    suspend fun cockpit(symbol: String = "NVDA"): ControlCenterState {
        // ── Lecture des modules existants (agrégateur pur) ──
        val market = runCatching { marketDataRepository.inspect(symbol) }.getOrNull()
        val security = securityRepository.securityState.first()
        val audit = runCatching { marketRepository.getAuditSummary() }.getOrNull()
        val simulation = runCatching { marketRepository.getSimulationSummary() }.getOrNull()
        val forecastCount = runCatching { db.aiForecastDao().getAll().size }.getOrDefault(0)
        val forecastOutcomes = runCatching { db.aiForecastOutcomeDao().getAll().size }.getOrDefault(0)
        val followUps = runCatching { db.strategyFollowUpDao().getActive().size }.getOrDefault(0)
        val sellAlerts = runCatching { db.strategyFollowUpDao().countAlerts() }.getOrDefault(0)
        val memories = runCatching { db.signalMemoryDao().getAll().size }.getOrDefault(0)
        val notifications = runCatching { db.notificationHistoryDao().getAll().size }.getOrDefault(0)
        val events = runCatching {
            db.securityLogDao().getLatest(6).map { "${time(it.createdAt)} · ${it.event} · ${it.details}" }
        }.getOrDefault(emptyList())
        val latestBackup = backupSafetyRepository.latestLocalSnapshot()
        val apiKeyOk = BuildConfig.ALPHA_VANTAGE_API_KEY.isNotBlank() &&
                BuildConfig.ALPHA_VANTAGE_API_KEY != "YOUR_API_KEY_HERE"

        // ── Calcul de scores — délégués depuis l'état des modules ──
        val dataScore = when {
            market?.usableForRealOrders == true -> 100
            market?.usableForSimulation == true -> 85
            apiKeyOk -> 60
            else -> 25
        }
        val brokerScore = when {
            market?.broker?.health == MarketFeedHealth.OK -> 100
            market?.broker?.health == MarketFeedHealth.ATTENTION -> 65
            else -> 35
        }
        val iaScore = when {
            audit == null -> 45
            audit.signalCount >= 100 -> audit.winRate.roundToInt().coerceIn(55, 95)
            audit.signalCount >= 30 -> 65
            else -> 50
        }
        val forecastScore = when {
            forecastOutcomes >= 100 -> 90
            forecastOutcomes >= 30 -> 75
            forecastCount >= 10 -> 62
            else -> 40
        }
        val securityScore = when {
            security.pinConfigured && security.fingerprintEnabled -> 100
            security.pinConfigured -> 90
            else -> 45
        }
        val backupScore = when {
            latestBackup != null &&
                    System.currentTimeMillis() - latestBackup.createdAt < 3L * 24L * 3600L * 1000L -> 95
            latestBackup != null -> 80
            else -> 60
        }
        val strategyScore = when {
            followUps > 0 && sellAlerts == 0 -> 85
            sellAlerts > 0 -> 70
            else -> 55
        }

        // ── CORRECTION V1.4 : realValidation() calculé UNE SEULE FOIS ──
        // puis réutilisé pour toutes les cartes — pas 3 appels réseau/DB séparés
        val validation = realValidation(symbol)

        val modules = listOf(
            // 📊 Santé IA → lit MarketRepository.getAuditSummary()
            ControlModuleStatus(
                "Santé de l'IA", "📊", statusFrom(iaScore), iaScore,
                "Moteur IA et apprentissage adaptatif",
                listOf(
                    "Signaux audités" to (audit?.signalCount?.toString() ?: "Non disponible"),
                    "Win rate" to (audit?.let { String.format(Locale.FRANCE, "%.1f %%", it.winRate) } ?: "En apprentissage"),
                    "Profit factor" to (audit?.let { String.format(Locale.FRANCE, "%.2f", it.profitFactor) } ?: "—"),
                    "Mémoire IA" to "$memories configurations"
                ), route = "stats_center"
            ),
            // 📈 Flux Marché → lit MarketDataConnectionRepository.inspect()
            ControlModuleStatus(
                "Flux Marché", "📈", statusFrom(dataScore), dataScore,
                market?.globalWarning ?: "Sources de données contrôlées",
                listOf(
                    "Source principale" to (market?.primarySource?.name ?: "Inconnue"),
                    "Alpha Vantage" to (market?.alpha?.label ?: "Non testé"),
                    "Broker" to (market?.broker?.label ?: "Non testé"),
                    "Comparaison" to (market?.comparison?.statusLabel ?: "Non calculée")
                ), route = "market_data_center", blocking = dataScore < 50
            ),
            // 🤖 Pronostics IA → lit AiForecastDao / AiForecastOutcomeDao
            ControlModuleStatus(
                "Pronostics IA", "🤖", statusFrom(forecastScore), forecastScore,
                "Pronostics joués et non joués",
                listOf(
                    "Pronostics" to forecastCount.toString(),
                    "Résultats suivis" to forecastOutcomes.toString(),
                    "Mémoire courte" to "7j / 30j / 90j",
                    "Mémoire longue" to "3m / 6m / 12m"
                ), route = "ai_forecasts"
            ),
            // 🧠 Mémoire glissante → lit AiForecastDao
            ControlModuleStatus(
                "Mémoire glissante", "🧠", statusFrom(forecastScore), forecastScore,
                "Évalue la fiabilité récente sans attendre le long terme",
                listOf(
                    "Court terme" to "Rapide / Swing / Volatilité",
                    "Long terme" to "Croissance / Défensif / Long terme",
                    "Seuil stats" to "10 pronostics minimum"
                ), route = "ai_forecasts"
            ),
            // 💰 Broker → lit MarketDataConnectionRepository.inspect() (données broker)
            ControlModuleStatus(
                "Broker", "💰", statusFrom(brokerScore), brokerScore,
                "Connexion et capacités broker",
                listOf(
                    "Compte" to (market?.broker?.label ?: "Non connecté"),
                    "Prix broker" to (market?.broker?.price?.let {
                        String.format(Locale.FRANCE, "%.2f", it)
                    } ?: "Non disponible"),
                    "Ordres réels" to if (market?.usableForRealOrders == true) "Autorisables après PIN" else "Non prêts",
                    "Simulation" to (simulation?.totalValue ?: "Disponible")
                ), route = "broker_assistant", blocking = brokerScore < 50
            ),
            // 🔒 Sécurité → lit SecurityRepository.securityState
            ControlModuleStatus(
                "Sécurité", "🔒", statusFrom(securityScore), securityScore,
                "PIN uniquement pour les actions sensibles",
                listOf(
                    "PIN" to if (security.pinConfigured) "OK" else "À configurer",
                    "Empreinte" to if (security.fingerprintEnabled) "Activée" else "Optionnelle",
                    "Mode actif" to security.tradingMode.label,
                    "Verrouillage réel" to if (security.realModeLocked) "Actif" else "OK"
                ), route = "security_center", blocking = !security.pinConfigured
            ),
            // ⚡ Validation Réelle → lit validation calculé UNE SEULE FOIS ci-dessus
            ControlModuleStatus(
                "Validation Réelle", "⚡",
                statusFrom(realValidationScore(market, security)),
                realValidationScore(market, security),
                "Check avant ordre réel",
                listOf(
                    "Blocages" to validation.hardBlocks.size.toString(),
                    "Avertissements" to validation.warnings.size.toString(),
                    "Décision" to validation.statusLabel
                ), route = "real_validation"
            ),
            // 📋 Journal → lit SecurityLogDao
            ControlModuleStatus(
                "Journal des événements", "📋",
                statusFrom(if (events.isNotEmpty()) 80 else 55),
                if (events.isNotEmpty()) 80 else 55,
                "Derniers événements de sécurité et synchronisation",
                listOf(
                    "Événements" to events.size.toString(),
                    "Dernier" to (events.firstOrNull() ?: "Aucun événement récent")
                ), route = "journal_ia"
            ),
            // 📦 Sauvegarde → lit BackupSafetyRepository.latestLocalSnapshot()
            ControlModuleStatus(
                "Sauvegarde", "📦", statusFrom(backupScore), backupScore,
                "Recommandée, jamais bloquante",
                listOf(
                    "Sauvegarde locale" to (latestBackup?.fileName ?: "Aucune"),
                    "Statut" to if (latestBackup != null) "Disponible" else "Recommandée",
                    "Règle" to "Ne bloque jamais le trading"
                ), route = "backup"
            ),
            // 🔄 Synchronisation → lit MarketDataConnectionRepository + NotificationHistoryDao
            ControlModuleStatus(
                "Synchronisation", "🔄", statusFrom(dataScore), dataScore,
                "Marchés, broker, mémoire et notifications",
                listOf(
                    "Données" to if (market?.usableForSimulation == true) "OK" else "À corriger",
                    "Notifications" to "$notifications historiques",
                    "Dernier contrôle" to time(System.currentTimeMillis())
                ), route = "market_data_center"
            ),
            // 📉 Stratégies → lit StrategyFollowUpDao
            ControlModuleStatus(
                "Performance des stratégies", "📉", statusFrom(strategyScore), strategyScore,
                "Suivi automatique sans vente automatique",
                listOf(
                    "Suivis actifs" to followUps.toString(),
                    "Alertes vente" to sellAlerts.toString(),
                    "Règle" to "Notification puis validation manuelle"
                ), route = "strategy_monitoring"
            )
        )

        val weighted = modules.map { it.score }.average().roundToInt().coerceIn(0, 100)
        return ControlCenterState(
            globalScore = weighted,
            globalLabel = statusFrom(weighted),
            globalMessage = when {
                weighted >= 90 -> "Plateforme opérationnelle. Tu peux analyser et tester dans de bonnes conditions."
                weighted >= 70 -> "Plateforme utilisable, avec quelques points à surveiller."
                else -> "Plusieurs points critiques doivent être corrigés avant le réel."
            },
            modules = modules,
            latestEvents = events,
            checkedAtLabel = time(System.currentTimeMillis())
        )
    }

    /**
     * Validation complète avant passage au mode réel.
     * Cette méthode est la source de vérité — appelée UNE SEULE FOIS dans cockpit().
     */
    suspend fun realValidation(symbol: String = "NVDA"): RealValidationState {
        val market = runCatching { marketDataRepository.inspect(symbol) }.getOrNull()
        val security = securityRepository.securityState.first()
        val latestBackup = backupSafetyRepository.latestLocalSnapshot()
        val audit = runCatching { marketRepository.getAuditSummary() }.getOrNull()

        val hard = mutableListOf<ValidationCheckItem>()
        val warnings = mutableListOf<ValidationCheckItem>()
        val brokerChecks = mutableListOf<ValidationCheckItem>()

        fun hardCheck(title: String, ok: Boolean, detailOk: String, detailKo: String) {
            val item = ValidationCheckItem(title, if (ok) "OK" else "BLOQUÉ", if (ok) detailOk else detailKo, blocking = !ok)
            if (!ok) hard += item
            brokerChecks += item
        }

        fun warning(title: String, ok: Boolean, detailOk: String, detailWarn: String) {
            val item = ValidationCheckItem(title, if (ok) "OK" else "À surveiller", if (ok) detailOk else detailWarn, blocking = false)
            if (!ok) warnings += item
        }

        // Blocages durs — vrais dangers, pas des recommandations
        hardCheck("Broker connecté", market?.broker?.health == MarketFeedHealth.OK,
            "Broker opérationnel", "Broker non connecté ou prix broker indisponible")
        hardCheck("Flux marché", market?.usableForSimulation == true,
            "Données marché exploitables", "Données marché absentes ou trop anciennes")
        hardCheck("Prix cohérents", market?.comparison?.warning == null,
            "Broker et Alpha Vantage cohérents", market?.comparison?.warning ?: "Comparaison impossible")
        hardCheck("PIN sécurité", security.pinConfigured,
            "PIN configuré", "Créer un PIN dans le Centre Sécurité")
        hardCheck("Mode réel non verrouillé", !security.realModeLocked,
            "Mode réel déverrouillé", "Mode réel verrouillé après erreurs PIN")
        hardCheck("Ordres réels", market?.usableForRealOrders == true,
            "Prix broker exploitable pour ordre réel", "Aucun ordre réel sans broker et prix broker valide")

        // Avertissements — sauvegarde = recommandation, jamais blocage
        warning("Sauvegarde", latestBackup != null,
            "Sauvegarde locale disponible", "Sauvegarde recommandée, mais non bloquante")
        warning("Empreinte", security.fingerprintEnabled,
            "Empreinte activée", "Empreinte optionnelle : le PIN suffit")
        warning("Historique IA", (audit?.signalCount ?: 0) >= 30,
            "Historique suffisant pour premières statistiques", "IA encore en apprentissage : privilégier petit capital")
        warning("Notifications",
            runCatching { db.notificationHistoryDao().getAll().isNotEmpty() }.getOrDefault(false),
            "Notifications déjà testées", "Tester les notifications avant usage intensif")

        val hardOk = hard.isEmpty()
        val warningPenalty = warnings.size * 4
        val score = if (hardOk) (100 - warningPenalty).coerceIn(70, 100)
        else (60 - hard.size * 10).coerceIn(0, 65)

        return RealValidationState(
            score = score,
            statusLabel = if (hardOk) "Prêt pour petit capital réel" else "Mode réel bloqué",
            canUseRealMode = hardOk,
            hardBlocks = hard,
            warnings = warnings,
            brokerDiagnostic = brokerChecks,
            latestBackupLabel = latestBackup?.let { "${it.fileName} · ${formatBytes(it.sizeBytes)}" }
                ?: "Aucune sauvegarde locale",
            checkedAtLabel = time(System.currentTimeMillis())
        )
    }

    /** Crée une sauvegarde de sécurité via le moteur unique BackupSafetyRepository. */
    suspend fun createSafetyBackup(reason: String): LocalBackupSnapshot =
        backupSafetyRepository.createLocalSnapshot(reason)

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun statusFrom(score: Int): String = when {
        score >= 85 -> "OK"
        score >= 65 -> "Attention"
        else -> "À corriger"
    }

    private fun realValidationScore(market: MarketDataCenterState?, security: SecurityState): Int {
        val blockers = listOf(
            market?.broker?.health == MarketFeedHealth.OK,
            market?.usableForSimulation == true,
            market?.comparison?.warning == null,
            security.pinConfigured,
            !security.realModeLocked,
            market?.usableForRealOrders == true
        ).count { !it }
        return if (blockers == 0) 96 else (60 - blockers * 8).coerceIn(0, 80)
    }

    private fun time(ms: Long): String = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date(ms))

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format(Locale.FRANCE, "%.1f Mo", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> String.format(Locale.FRANCE, "%.1f Ko", bytes / 1024.0)
        else -> "$bytes o"
    }
}

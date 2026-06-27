package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.BuildConfig
import com.privateinvest.aitraderpro.data.model.AssetDetail
import kotlin.math.abs

/**
 * Centre Données Marché — V1.3
 *
 * Rôle : garantir que Simulation, Pronostics IA, Suivi intelligent et Broker utilisent
 * des données de marché réelles, avec un état clair des flux.
 *
 * - Alpha Vantage = source marché réelle + historique + secours.
 * - Broker = source prioritaire quand il fournit une cotation exploitable.
 * - Cache = dernier recours visuel uniquement, jamais présenté comme temps réel.
 *
 * Colonne vertébrale :
 * - getPrice(symbol) → Triple<prix?, source, isFresh>
 * - inspect(symbol) → MarketDataCenterState complet pour l'écran dédié
 */
enum class MarketFeedSource { ALPHA_VANTAGE, BROKER, CACHE, NONE }
enum class MarketFeedHealth { OK, ATTENTION, ERREUR, NON_CONFIGURE }

data class MarketFeedStatus(
    val source: MarketFeedSource,
    val health: MarketFeedHealth,
    val label: String,
    val detail: String,
    val lastSyncLabel: String = "Jamais",
    val price: Double? = null,
    val isFresh: Boolean = false
)

data class MarketPriceComparison(
    val symbol: String,
    val alphaPrice: Double?,
    val brokerPrice: Double?,
    val differencePercent: Double?,
    val statusLabel: String,
    val warning: String?
)

data class MarketDataCenterState(
    val symbol: String,
    val primarySource: MarketFeedSource,
    val alpha: MarketFeedStatus,
    val broker: MarketFeedStatus,
    val comparison: MarketPriceComparison,
    val usableForSimulation: Boolean,
    val usableForForecasts: Boolean,
    val usableForRealOrders: Boolean,
    val globalWarning: String?,
    val checkedAt: Long = System.currentTimeMillis()
)

/**
 * Interface volontairement simple pour relier plus tard un vrai connecteur broker.
 * Tant qu'aucun broker n'est réellement configuré, elle retourne broker non configuré.
 */
interface BrokerMarketDataBridge {
    suspend fun getBrokerQuote(symbol: String): BrokerQuoteSnapshot?
    suspend fun getBrokerAccount(): BrokerAccountSnapshot?
}

data class BrokerQuoteSnapshot(
    val symbol: String,
    val price: Double,
    val lastSyncMillis: Long = System.currentTimeMillis(),
    val providerLabel: String = "Broker"
)

data class BrokerAccountSnapshot(
    val brokerName: String,
    val modeLabel: String,
    val cash: Double,
    val portfolioValue: Double,
    val buyingPower: Double,
    val connected: Boolean,
    val lastSyncMillis: Long = System.currentTimeMillis()
)

object NoBrokerMarketDataBridge : BrokerMarketDataBridge {
    override suspend fun getBrokerQuote(symbol: String): BrokerQuoteSnapshot? = null
    override suspend fun getBrokerAccount(): BrokerAccountSnapshot? = null
}

class MarketDataConnectionRepository(
    private val marketRepository: MarketRepository,
    private val brokerBridge: BrokerMarketDataBridge = NoBrokerMarketDataBridge
) {

    // ─── SOURCE UNIQUE DE PRIX ────────────────────────────────────────────────
    /**
     * Source unique de prix pour toute l'application.
     *
     * Priorité : Broker (connecté + prix réel) → Alpha Vantage → null
     *
     * @return Triple<prix?, MarketFeedSource, isFresh>
     *   - prix null = aucune donnée exploitable → DataFreshnessGuard bloquera les actions
     *   - isFresh true = donnée reçue en temps réel (pas cache)
     */
    suspend fun getPrice(symbol: String): Triple<Double?, MarketFeedSource, Boolean> {
        val normalizedSymbol = symbol.ifBlank { "NVDA" }.uppercase()

        // 1. Essayer le broker en priorité
        val brokerAccount = runCatching { brokerBridge.getBrokerAccount() }.getOrNull()
        if (brokerAccount?.connected == true) {
            val brokerQuote = runCatching { brokerBridge.getBrokerQuote(normalizedSymbol) }.getOrNull()
            if (brokerQuote?.price != null && brokerQuote.price > 0.0) {
                return Triple(brokerQuote.price, MarketFeedSource.BROKER, true)
            }
        }

        // 2. Alpha Vantage si clé configurée
        val alphaConfigured = BuildConfig.ALPHA_VANTAGE_API_KEY.isNotBlank() &&
            BuildConfig.ALPHA_VANTAGE_API_KEY != "YOUR_API_KEY_HERE"
        if (alphaConfigured) {
            val alphaDetail = runCatching { marketRepository.getMarketData(normalizedSymbol) }.getOrNull()
            val alphaPrice = alphaDetail?.quotePrice?.takeIf { it > 0.0 }
            if (alphaPrice != null) {
                return Triple(alphaPrice, MarketFeedSource.ALPHA_VANTAGE, true)
            }
        }

        // 3. Aucune source réelle disponible
        return Triple(null, MarketFeedSource.NONE, false)
    }

    // ─── INSPECTION COMPLÈTE ──────────────────────────────────────────────────
    /**
     * Inspection complète pour l'écran Centre Données Marché.
     */
    suspend fun inspect(symbol: String): MarketDataCenterState {
        val normalizedSymbol = symbol.ifBlank { "NVDA" }.uppercase()
        val alphaDetail = runCatching { marketRepository.getMarketData(normalizedSymbol) }.getOrNull()
        val alphaPrice = alphaDetail?.quotePrice?.takeIf { it > 0.0 }
        val brokerQuote = runCatching { brokerBridge.getBrokerQuote(normalizedSymbol) }.getOrNull()
        val brokerAccount = runCatching { brokerBridge.getBrokerAccount() }.getOrNull()

        val alphaConfigured = BuildConfig.ALPHA_VANTAGE_API_KEY.isNotBlank() &&
            BuildConfig.ALPHA_VANTAGE_API_KEY != "YOUR_API_KEY_HERE"

        val alphaStatus = buildAlphaStatus(alphaConfigured, alphaDetail, alphaPrice)
        val brokerStatus = buildBrokerStatus(brokerQuote, brokerAccount)
        val comparison = comparePrices(normalizedSymbol, alphaPrice, brokerQuote?.price)

        val primary = when {
            brokerQuote?.price != null && brokerAccount?.connected == true -> MarketFeedSource.BROKER
            alphaPrice != null -> MarketFeedSource.ALPHA_VANTAGE
            else -> MarketFeedSource.NONE
        }

        val usableMarketData = alphaPrice != null || brokerQuote?.price != null
        val globalWarning = when {
            !alphaConfigured && brokerQuote == null -> "Aucune clé Alpha Vantage active et aucun prix broker disponible. Les stratégies ne peuvent pas être fiables."
            !usableMarketData -> "Aucun cours réel disponible pour $normalizedSymbol. Simulation, pronostics et réel doivent attendre une donnée valide."
            comparison.warning != null -> comparison.warning
            brokerAccount?.connected == true && brokerQuote == null -> "Broker connecté, mais il ne fournit pas encore de prix exploitable pour $normalizedSymbol. Alpha Vantage reste utilisé pour l'analyse."
            else -> null
        }

        return MarketDataCenterState(
            symbol = normalizedSymbol,
            primarySource = primary,
            alpha = alphaStatus,
            broker = brokerStatus,
            comparison = comparison,
            usableForSimulation = usableMarketData,
            usableForForecasts = usableMarketData,
            usableForRealOrders = brokerAccount?.connected == true && brokerQuote?.price != null,
            globalWarning = globalWarning
        )
    }

    // ─── BUILDERS INTERNES ───────────────────────────────────────────────────

    private fun buildAlphaStatus(configured: Boolean, detail: AssetDetail?, price: Double?): MarketFeedStatus {
        return when {
            !configured -> MarketFeedStatus(
                source = MarketFeedSource.ALPHA_VANTAGE,
                health = MarketFeedHealth.NON_CONFIGURE,
                label = "Alpha Vantage non configuré",
                detail = "Ajoute une clé API dans local.properties / BuildConfig pour activer les cours et historiques.",
                isFresh = false
            )
            price != null -> MarketFeedStatus(
                source = MarketFeedSource.ALPHA_VANTAGE,
                health = MarketFeedHealth.OK,
                label = "Alpha Vantage connecté",
                detail = "Cours réel reçu. Utilisable pour indicateurs, simulation, pronostics et historique.",
                lastSyncLabel = "Maintenant",
                price = price,
                isFresh = true
            )
            detail != null -> MarketFeedStatus(
                source = MarketFeedSource.ALPHA_VANTAGE,
                health = MarketFeedHealth.ATTENTION,
                label = "Alpha Vantage partiel",
                detail = "Réponse reçue mais prix inexploitable. Vérifier symbole, quota ou format API.",
                lastSyncLabel = "Maintenant",
                isFresh = false
            )
            else -> MarketFeedStatus(
                source = MarketFeedSource.ALPHA_VANTAGE,
                health = MarketFeedHealth.ERREUR,
                label = "Alpha Vantage indisponible",
                detail = "Impossible de récupérer un cours réel. L'application doit utiliser le cache ou attendre la prochaine synchronisation.",
                isFresh = false
            )
        }
    }

    private fun buildBrokerStatus(quote: BrokerQuoteSnapshot?, account: BrokerAccountSnapshot?): MarketFeedStatus {
        return when {
            account?.connected == true && quote != null -> MarketFeedStatus(
                source = MarketFeedSource.BROKER,
                health = MarketFeedHealth.OK,
                label = "${account.brokerName} connecté",
                detail = "Compte ${account.modeLabel}. Prix broker exploitable pour comparaison et ordres.",
                lastSyncLabel = "Maintenant",
                price = quote.price,
                isFresh = true
            )
            account?.connected == true -> MarketFeedStatus(
                source = MarketFeedSource.BROKER,
                health = MarketFeedHealth.ATTENTION,
                label = "${account.brokerName} connecté",
                detail = "Compte ${account.modeLabel}, mais aucun prix broker exploitable pour ce symbole.",
                lastSyncLabel = "Maintenant",
                isFresh = false
            )
            else -> MarketFeedStatus(
                source = MarketFeedSource.BROKER,
                health = MarketFeedHealth.NON_CONFIGURE,
                label = "Broker non connecté",
                detail = "Connecte ton broker pour voir liquidités, positions, pouvoir d'achat et prix broker.",
                isFresh = false
            )
        }
    }

    private fun comparePrices(symbol: String, alphaPrice: Double?, brokerPrice: Double?): MarketPriceComparison {
        if (alphaPrice == null && brokerPrice == null) {
            return MarketPriceComparison(symbol, null, null, null, "Aucune comparaison", "Aucune source de prix réelle disponible.")
        }
        if (alphaPrice == null) {
            return MarketPriceComparison(symbol, null, brokerPrice, null, "Broker seul", "Alpha Vantage ne fournit pas de prix exploitable.")
        }
        if (brokerPrice == null) {
            return MarketPriceComparison(symbol, alphaPrice, null, null, "Alpha Vantage seul", null)
        }
        val diff = abs(alphaPrice - brokerPrice) / maxOf(alphaPrice, brokerPrice) * 100.0
        val warning = when {
            diff > 1.0 -> "Écart important entre broker et Alpha Vantage : vérifier avant ordre réel."
            diff > 0.3 -> "Écart léger entre les sources. Utiliser le prix broker pour l'ordre réel."
            else -> null
        }
        val status = when {
            diff > 1.0 -> "Écart important"
            diff > 0.3 -> "Écart léger"
            else -> "Prix cohérents"
        }
        return MarketPriceComparison(symbol, alphaPrice, brokerPrice, diff, status, warning)
    }
}

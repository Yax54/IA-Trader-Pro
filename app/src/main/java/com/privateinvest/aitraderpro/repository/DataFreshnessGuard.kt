package com.privateinvest.aitraderpro.repository

/**
 * DataFreshnessGuard — V1.3 Colonne vertébrale données marché.
 *
 * Singleton central qui maintient l'état de fraîcheur des données de marché
 * et expose des méthodes de blocage pour toutes les fonctionnalités critiques.
 *
 * Règles :
 *  - canGenerateSignal()   → Simulation + signaux IA nécessitent un cours réel (Alpha Vantage ou Broker)
 *  - canGenerateForecast() → Pronostics IA nécessitent un cours réel (Alpha Vantage ou Broker)
 *  - canPlaceRealOrder()   → Ordre réel nécessite Broker connecté + prix broker valide
 *
 * Usage :
 *  DataFreshnessGuard.updateState(state)          // appelé après chaque inspect()
 *  DataFreshnessGuard.canGenerateSignal(symbol)   // avant génération signaux
 *  DataFreshnessGuard.canGenerateForecast(symbol) // avant pronostics
 *  DataFreshnessGuard.canPlaceRealOrder(symbol)   // avant ordre REEL
 */
object DataFreshnessGuard {

    // Cache de l'état le plus récent par symbole
    private val stateCache = mutableMapOf<String, MarketDataCenterState>()

    // État global (tous symboles confondus — pour les bannières globales)
    @Volatile
    private var globalState: MarketDataCenterState? = null

    /**
     * Met à jour le cache interne avec le dernier état inspecté.
     * Appelé automatiquement depuis MarketDataConnectionRepository.inspect().
     */
    fun updateState(state: MarketDataCenterState) {
        stateCache[state.symbol.uppercase()] = state
        globalState = state
    }

    /**
     * Réinitialise le cache — utile lors d'un changement de clé API ou déconnexion broker.
     */
    fun clearCache() {
        stateCache.clear()
        globalState = null
    }

    // ─── CONTRÔLES DE BLOCAGE ────────────────────────────────────────────────

    /**
     * Peut-on générer des signaux IA pour ce symbole ?
     *
     * Autorisé si :
     * - Clé Alpha Vantage configurée ET cours reçu (prix > 0), OU
     * - Broker connecté avec prix valide
     *
     * Bloqué si aucune source réelle n'est disponible.
     */
    fun canGenerateSignal(symbol: String = ""): Boolean {
        val state = getStateFor(symbol) ?: return true // Pas encore inspecté → autoriser (clé pas encore testée)
        return state.usableForSimulation
    }

    /**
     * Raison du blocage pour les signaux IA.
     * Retourne null si pas de blocage.
     */
    fun signalBlockReason(symbol: String = ""): String? {
        val state = getStateFor(symbol) ?: return null
        return if (!state.usableForSimulation) {
            state.globalWarning ?: "Aucun cours réel disponible pour ${state.symbol}. Les signaux IA nécessitent des données fraîches."
        } else null
    }

    /**
     * Peut-on générer des pronostics IA pour ce symbole ?
     *
     * Mêmes conditions que canGenerateSignal — pronostics basés sur données réelles.
     */
    fun canGenerateForecast(symbol: String = ""): Boolean {
        val state = getStateFor(symbol) ?: return true
        return state.usableForForecasts
    }

    /**
     * Raison du blocage pour les pronostics IA.
     * Retourne null si pas de blocage.
     */
    fun forecastBlockReason(symbol: String = ""): String? {
        val state = getStateFor(symbol) ?: return null
        return if (!state.usableForForecasts) {
            state.globalWarning ?: "Aucun cours réel disponible pour ${state.symbol}. Les pronostics IA nécessitent des données fraîches."
        } else null
    }

    /**
     * Peut-on placer un ordre réel pour ce symbole ?
     *
     * Autorisé UNIQUEMENT si :
     * - Broker connecté (brokerAccount.connected == true), ET
     * - Broker fournit un prix valide pour ce symbole
     *
     * Bloqué sinon — ordres réels sans données broker = trop risqué.
     */
    fun canPlaceRealOrder(symbol: String = ""): Boolean {
        val state = getStateFor(symbol) ?: return false // Pas encore inspecté → bloquer par sécurité
        return state.usableForRealOrders
    }

    /**
     * Raison du blocage pour les ordres réels.
     * Retourne null si pas de blocage.
     */
    fun realOrderBlockReason(symbol: String = ""): String? {
        val state = getStateFor(symbol) ?: return "Données broker non vérifiées. Connecte un broker avant de placer un ordre réel."
        return if (!state.usableForRealOrders) {
            when {
                state.broker.health == MarketFeedHealth.NON_CONFIGURE ->
                    "Aucun broker connecté. Ordres réels impossibles sans connexion broker."
                state.broker.health == MarketFeedHealth.ERREUR ->
                    "Erreur broker : ${state.broker.detail}"
                state.broker.health == MarketFeedHealth.ATTENTION ->
                    "Broker connecté mais sans prix valide pour ${state.symbol}. Ordre réel bloqué par précaution."
                else -> state.globalWarning ?: "Ordre réel bloqué : données broker insuffisantes."
            }
        } else null
    }

    // ─── ÉTAT GLOBAL POUR BANNIÈRES ──────────────────────────────────────────

    /**
     * Les données sont-elles utilisables pour au moins une fonctionnalité ?
     * Utilisé pour les bannières globales dans Dashboard et TopOpportunities.
     */
    fun isAnyDataUsable(): Boolean {
        val state = globalState ?: return true // Pas encore inspecté → ne pas afficher bannière
        return state.usableForSimulation || state.usableForForecasts
    }

    /**
     * Message global de bannière (null si tout est OK).
     */
    fun globalBannerMessage(): String? {
        val state = globalState ?: return null
        return if (!isAnyDataUsable()) state.globalWarning else null
    }

    /**
     * Source principale utilisée actuellement.
     */
    fun primarySource(): MarketFeedSource {
        return globalState?.primarySource ?: MarketFeedSource.NONE
    }

    // ─── HELPERS INTERNES ────────────────────────────────────────────────────

    private fun getStateFor(symbol: String): MarketDataCenterState? {
        return if (symbol.isBlank()) globalState
        else stateCache[symbol.uppercase()] ?: globalState
    }
}

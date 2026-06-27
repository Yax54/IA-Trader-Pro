package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.data.model.AssetDetail
import com.privateinvest.aitraderpro.navigation.SelectedAssetStore
import com.privateinvest.aitraderpro.repository.DataFreshnessGuard
import com.privateinvest.aitraderpro.repository.MarketRepository
import com.privateinvest.aitraderpro.repository.SecurityRepository
import com.privateinvest.aitraderpro.repository.TradingExecutionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.floor

/** Mode broker sélectionné par l'utilisateur */
enum class BrokerMode {
    SIMULATION,    // Mode par défaut — portefeuille virtuel interne
    PAPER_TRADING, // Paper trading broker (broker réel, argent fictif)
    REEL           // Ordre réel — nécessite double confirmation
}

/**
 * État complet de l'assistant d'investissement broker.
 * Toutes les valeurs financières sont en euros/devise de l'actif.
 */
data class BrokerUiState(
    val symbol: String = "",
    val name: String = "",
    val currentPrice: Double = 0.0,

    // Données IA
    val actionLabel: String = "SURVEILLER",
    val confidencePercent: Int = 0,
    val riskLabel: String = "Faible",
    val scoreTechnique: Int = 0,

    // Liquidités et portefeuille (simulé tant qu'aucune API broker n'est connectée)
    val availableCash: Double = 10_000.0,
    val portfolioValue: Double = 10_000.0,
    val buyingPower: Double = 10_000.0,

    // Montant sélectionné par l'utilisateur
    val selectedAmount: Double = 500.0,

    // Calculs automatiques
    val estimatedQuantity: Double = 0.0,
    val totalAmount: Double = 0.0,
    val portfolioImpactPercent: Double = 0.0,
    val assetExposurePercent: Double = 0.0,
    val remainingCash: Double = 0.0,
    val riskAfterBuy: String = "Faible",

    // Montant recommandé par l'IA (% du capital selon confiance)
    val aiRecommendedAmount: Double = 0.0,

    // Mode sélectionné
    val brokerMode: BrokerMode = BrokerMode.SIMULATION,

    // UI
    val loading: Boolean = true,
    val error: String? = null,
    val orderResult: String? = null
)

class BrokerViewModel(
    private val repository: MarketRepository,
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val strategyRepo get() = ServiceLocator.strategyMonitoringRepository

    private val _uiState = MutableStateFlow(BrokerUiState())
    val uiState: StateFlow<BrokerUiState> = _uiState.asStateFlow()

    init {
        loadAssetData()
    }

    fun loadAssetData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching {
                repository.getMarketData(SelectedAssetStore.currentSymbol)
            }.onSuccess { detail ->
                val portfolio = repository.getSimulationSummary()
                val cash = parseMoneyString(portfolio.availableCash)
                val portValue = parseMoneyString(portfolio.totalValue)

                val state = _uiState.value.copy(
                    symbol = SelectedAssetStore.currentSymbol,
                    name = SelectedAssetStore.currentName,
                    currentPrice = detail?.quotePrice ?: 0.0,
                    actionLabel = detail?.scoring?.signal?.name ?: "SURVEILLER",
                    confidencePercent = detail?.let { computeConfidence(it) } ?: 50,
                    riskLabel = computeRiskLabel(detail),
                    scoreTechnique = detail?.scoring?.scoreTechnique ?: 0,
                    availableCash = cash,
                    portfolioValue = portValue,
                    buyingPower = cash,
                    aiRecommendedAmount = computeAiRecommendedAmount(cash, detail),
                    loading = false
                )
                _uiState.value = state
                recalculate(state.selectedAmount)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    symbol = SelectedAssetStore.currentSymbol,
                    name = SelectedAssetStore.currentName,
                    loading = false,
                    error = "Données de marché indisponibles : ${e.message}"
                )
                recalculate(_uiState.value.selectedAmount)
            }
        }
    }

    /** Met à jour le montant sélectionné et recalcule tous les indicateurs */
    fun setAmount(amount: Double) {
        val clamped = amount.coerceAtLeast(0.0)
        _uiState.value = _uiState.value.copy(selectedAmount = clamped)
        recalculate(clamped)
    }

    /**
     * Correction 1 : met à jour le BrokerMode local ET synchronise TradingExecutionMode
     * dans SecurityRepository pour que le bandeau global reflète le mode réellement actif.
     *   BrokerMode.SIMULATION    → TradingExecutionMode.SIMULATION
     *   BrokerMode.PAPER_TRADING → TradingExecutionMode.PAPER_TRADING
     *   BrokerMode.REEL          → TradingExecutionMode.REAL
     */
    fun setBrokerMode(mode: BrokerMode) {
        _uiState.value = _uiState.value.copy(brokerMode = mode)
        val execMode = when (mode) {
            BrokerMode.SIMULATION    -> TradingExecutionMode.SIMULATION
            BrokerMode.PAPER_TRADING -> TradingExecutionMode.PAPER_TRADING
            BrokerMode.REEL          -> TradingExecutionMode.REAL
        }
        viewModelScope.launch { securityRepository.setTradingMode(execMode) }
    }

    /**
     * Soumet l'ordre après la double confirmation UI.
     * En simulation : enregistre en base. En paper/réel : stub (à brancher sur API broker).
     */
    fun submitOrder() {
        val state = _uiState.value
        if (state.selectedAmount <= 0 || state.currentPrice <= 0) return

        viewModelScope.launch {
            when (state.brokerMode) {
                BrokerMode.SIMULATION -> {
                    val success = repository.confirmSimulationOrder(state.symbol)
                    if (success) {
                        // Suivi stratégique automatique après achat simulation
                        runCatching {
                            strategyRepo.createFollowUp(
                                symbol = state.symbol,
                                name = state.name,
                                mode = "SIMULATION"
                            )
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        orderResult = if (success)
                            "Ordre simulé enregistré. Suivi J+1/J+7/J+30 démarré. Suivi stratégique actif : l'application t'alertera sans jamais vendre automatiquement."
                        else
                            "Impossible d'enregistrer l'ordre simulé (données insuffisantes)."
                    )
                }
                BrokerMode.PAPER_TRADING -> {
                    // Suivi stratégique automatique après achat paper trading
                    runCatching {
                        strategyRepo.createFollowUp(
                            symbol = state.symbol,
                            name = state.name,
                            mode = "PAPER"
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        orderResult = "Ordre paper trading envoyé : ${String.format("%.0f", state.estimatedQuantity)} × ${state.symbol} pour ${formatMoney(state.totalAmount)}. Suivi stratégique activé. (Connexion broker non encore configurée)"
                    )
                }
                BrokerMode.REEL -> {
                    // V1.3 DataFreshnessGuard — bloquer si broker/données invalides
                    val blockReason = DataFreshnessGuard.realOrderBlockReason(state.symbol)
                    if (blockReason != null) {
                        _uiState.value = _uiState.value.copy(
                            error = "⚠️ Ordre réel bloqué : $blockReason"
                        )
                        return@launch
                    }

                    // Suivi stratégique automatique après achat réel — PIN/empreinte déjà validés par l'UI
                    runCatching {
                        strategyRepo.createFollowUp(
                            symbol = state.symbol,
                            name = state.name,
                            mode = "REEL"
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        orderResult = "Ordre RÉEL soumis : ${String.format("%.0f", state.estimatedQuantity)} × ${state.symbol} pour ${formatMoney(state.totalAmount)}. Suivi stratégique activé. Alerte manuelle garantie. (Connexion broker non encore configurée)"
                    )
                }
            }
        }
    }

    fun clearOrderResult() {
        _uiState.value = _uiState.value.copy(orderResult = null)
    }

    /** Correction 3 : permet à l'UI d'afficher un message d'erreur sécurité */
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ─── Calculs internes ───────────────────────────────────────────────────

    private fun recalculate(amount: Double) {
        val s = _uiState.value
        if (s.currentPrice <= 0.0) {
            _uiState.value = s.copy(
                estimatedQuantity = 0.0,
                totalAmount = amount,
                portfolioImpactPercent = 0.0,
                assetExposurePercent = 0.0,
                remainingCash = s.availableCash - amount
            )
            return
        }

        val qty = floor(amount / s.currentPrice)
        val total = qty * s.currentPrice
        val portImpact = if (s.portfolioValue > 0) (total / s.portfolioValue) * 100 else 0.0
        val exposure = portImpact  // Exposition = impact portefeuille (simplifié)
        val remaining = s.availableCash - total
        val riskAfter = when {
            exposure >= 30.0 -> "Élevé"
            exposure >= 15.0 -> "Modéré"
            else -> "Faible"
        }

        _uiState.value = s.copy(
            estimatedQuantity = qty,
            totalAmount = total,
            portfolioImpactPercent = portImpact,
            assetExposurePercent = exposure,
            remainingCash = remaining,
            riskAfterBuy = riskAfter
        )
    }

    private fun computeAiRecommendedAmount(cash: Double, detail: AssetDetail?): Double {
        val confidence = detail?.let { computeConfidence(it) } ?: 50
        val pct = when {
            confidence >= 75 -> 0.10  // 10% du capital
            confidence >= 60 -> 0.05  // 5% du capital
            confidence >= 50 -> 0.025 // 2.5%
            else -> 0.01              // 1% (signal faible)
        }
        return (cash * pct).let { floor(it / 50.0) * 50.0 } // arrondi au 50€ inférieur
    }

    private fun computeConfidence(detail: AssetDetail): Int {
        return detail.scoring.scoreTechnique.coerceIn(0, 100)
    }

    private fun computeRiskLabel(detail: AssetDetail?): String {
        val score = detail?.scoring?.scoreRisque ?: 50
        return when {
            score >= 75 -> "Faible"
            score >= 50 -> "Modéré"
            else -> "Élevé"
        }
    }

    private fun parseMoneyString(value: String): Double {
        return value.replace("[^0-9.,]".toRegex(), "").replace(",", ".").toDoubleOrNull() ?: 10_000.0
    }

    private fun formatMoney(value: Double): String = String.format("%.2f €", value)
}

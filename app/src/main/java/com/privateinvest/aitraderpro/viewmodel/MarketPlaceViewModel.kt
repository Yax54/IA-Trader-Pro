package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.repository.MarketFiltersRepository
import com.privateinvest.aitraderpro.repository.MarketFiltersState
import com.privateinvest.aitraderpro.repository.MarketSortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MarketPlaceUiState(
    val loading: Boolean = true,
    val filters: MarketFiltersState = MarketFiltersState(),
    val allItems: List<AssetSignal> = emptyList(),
    val filteredItems: List<AssetSignal> = emptyList(),
    val error: String? = null
)

class MarketPlaceViewModel(
    private val filtersRepository: MarketFiltersRepository
) : ViewModel() {
    private val items = MutableStateFlow<List<AssetSignal>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MarketPlaceUiState> = combine(filtersRepository.filters, items, loading, error) { filters, raw, isLoading, err ->
        MarketPlaceUiState(
            loading = isLoading,
            filters = filters,
            allItems = raw,
            filteredItems = applyFilters(raw, filters),
            error = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarketPlaceUiState())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        loading.value = true
        runCatching { ServiceLocator.marketRepository.getSignals() }
            .onSuccess { items.value = it; error.value = null }
            .onFailure { error.value = it.message ?: "Erreur chargement Place du marché" }
        loading.value = false
    }

    fun updateFilters(transform: (MarketFiltersState) -> MarketFiltersState) = viewModelScope.launch {
        filtersRepository.save(transform(uiState.value.filters))
    }

    fun resetFilters() = viewModelScope.launch { filtersRepository.reset() }

    private fun applyFilters(raw: List<AssetSignal>, f: MarketFiltersState): List<AssetSignal> {
        val query = f.query.trim().lowercase()
        return raw.asSequence()
            .filter { signal -> query.isBlank() || signal.symbol.lowercase().contains(query) || signal.name.lowercase().contains(query) }
            .filter { it.score >= f.minScore }
            .filter { it.confidence >= f.minConfidence }
            .filter { riskToScore(it.risk) <= f.maxRisk }
            .filter { categoryAllowed(it, f) }
            .let { seq ->
                when (f.sortMode) {
                    MarketSortMode.SCORE -> seq.sortedByDescending { it.score }
                    MarketSortMode.POTENTIAL -> seq.sortedByDescending { targetToNumber(it.target) }
                    MarketSortMode.RISK -> seq.sortedBy { riskToScore(it.risk) }
                    MarketSortMode.VOLUME -> seq.sortedByDescending { it.confidence }
                    MarketSortMode.VARIATION -> seq.sortedByDescending { targetToNumber(it.target) }
                    MarketSortMode.NAME -> seq.sortedBy { it.name }
                }
            }
            .take(100)
            .toList()
    }

    private fun categoryAllowed(signal: AssetSignal, f: MarketFiltersState): Boolean {
        val quick = signal.score >= 85 && signal.risk.contains("faible", true)
        val swing = signal.score >= 78 && signal.confidence >= 70
        val long = signal.score >= 75 && signal.confidence >= 75 && signal.risk.contains("faible", true)
        val volatile = signal.risk.contains("élev", true) || signal.risk.contains("fort", true)
        val growth = signal.name.contains("tech", true) || signal.symbol in setOf("NVDA", "AMD", "MSFT", "AAPL", "ASML")
        val defensive = signal.risk.contains("faible", true) && signal.score >= 70
        val dividend = signal.name.contains("dividend", true) || signal.name.contains("dividende", true)
        return (f.opportunityQuick && quick) || (f.swing && swing) || (f.longTerm && long) || (f.volatility && volatile) || (f.growth && growth) || (f.defensive && defensive) || (f.dividend && dividend)
    }

    private fun riskToScore(risk: String): Int = when {
        risk.contains("faible", true) -> 25
        risk.contains("mod", true) || risk.contains("moy", true) -> 55
        risk.contains("élev", true) || risk.contains("fort", true) -> 85
        else -> 60
    }

    private fun targetToNumber(target: String): Double = Regex("""[-+]?\d+(?:[,.]\d+)?""").find(target)?.value?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
}

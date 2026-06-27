package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.SimulationPositionItem
import com.privateinvest.aitraderpro.data.model.SimulationSummary
import com.privateinvest.aitraderpro.data.model.SimulationTradeItem
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimulationPortfolioUiState(
    val summary: SimulationSummary = SimulationSummary(
        initialCapital = "0.00 €",
        availableCash = "0.00 €",
        investedCapital = "0.00 €",
        totalValue = "0.00 €",
        totalPerformance = "0.00 €",
        openPositions = 0,
        successRateJ1 = "0 %",
        successRateJ7 = "0 %",
        successRateJ30 = "0 %"
    ),
    val positions: List<SimulationPositionItem> = emptyList(),
    val history: List<SimulationTradeItem> = emptyList(),
    val statusMessage: String? = null
)

class PortfolioViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SimulationPortfolioUiState())
    val uiState: StateFlow<SimulationPortfolioUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                summary = repository.getSimulationSummary(),
                positions = repository.getSimulationPositions(),
                history = repository.getSimulationHistory()
            )
        }
    }

    fun sellPosition(symbol: String) {
        viewModelScope.launch {
            val success = repository.sellSimulationPosition(symbol)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (success) {
                    "Vente simulée exécutée sur $symbol."
                } else {
                    "Impossible de clôturer la position sur $symbol."
                }
            )
            refresh()
        }
    }
}

package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.data.model.PortfolioSummary
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val summary: PortfolioSummary = PortfolioSummary("0.00 €", "0.00 €", "0.00 €", 0, "Faible"),
    val signals: List<AssetSignal> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                DashboardUiState(
                    loading = false,
                    summary = repository.getPortfolio(),
                    signals = repository.getSignals()
                )
            }.onSuccess { _uiState.value = it }
                .onFailure { _uiState.value = DashboardUiState(loading = false, error = it.message) }
        }
    }
}

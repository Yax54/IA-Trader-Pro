package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import com.privateinvest.aitraderpro.repository.AiForecastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ForecastTab { ACTIVE, SUCCESS, FAILED, MEMORY }

data class AiForecastUiState(
    val selectedTab: ForecastTab = ForecastTab.ACTIVE,
    val loading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val selectedStrategy: String = "TOUTES",
    val active: List<AiForecastEntity> = emptyList(),
    val success: List<AiForecastEntity> = emptyList(),
    val failed: List<AiForecastEntity> = emptyList(),
    val memory: List<AiForecastEntity> = emptyList(),
    val outcomes: Map<Long, AiForecastOutcomeEntity> = emptyMap(),
    val csvContent: String? = null
)

class AiForecastViewModel : ViewModel() {
    private val repo: AiForecastRepository get() = ServiceLocator.aiForecastRepository

    private val _uiState = MutableStateFlow(AiForecastUiState())
    val uiState: StateFlow<AiForecastUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun selectTab(tab: ForecastTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun updateStrategy(strategy: String) {
        _uiState.value = _uiState.value.copy(selectedStrategy = strategy)
    }

    fun clearCsv() {
        _uiState.value = _uiState.value.copy(csvContent = null)
    }

    fun exportCsv() {
        viewModelScope.launch {
            runCatching { repo.exportToCsv(null) }
                .onSuccess { _uiState.value = _uiState.value.copy(csvContent = it) }
                .onFailure { _uiState.value = _uiState.value.copy(error = "Export impossible : ${it.message}") }
        }
    }

    fun markAsPlayed(id: Long) {
        viewModelScope.launch {
            repo.markAsPlayed(id)
            refresh()
        }
    }

    fun markAsIgnored(id: Long) {
        viewModelScope.launch {
            repo.markAsIgnored(id)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                repo.refreshOutcomes()
                val forecasts = (repo.getAllCT() + repo.getAllLT()).distinctBy { it.id }.sortedByDescending { it.createdAt }
                val outcomes = repo.getAllOutcomes().associateBy { it.forecastId }

                val active = forecasts.filter { it.status.equals("ACTIVE", true) || it.status.equals("PENDING", true) }
                val success = forecasts.filter { forecastSucceeded(it, outcomes[it.id]) }
                val failed = forecasts.filter { forecastFailed(it, outcomes[it.id]) }

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    active = active,
                    success = success,
                    failed = failed,
                    memory = forecasts,
                    outcomes = outcomes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Erreur Pronostics IA : ${e.message}"
                )
            }
        }
    }

    fun filtered(list: List<AiForecastEntity>): List<AiForecastEntity> {
        val state = _uiState.value
        val q = state.query.trim().uppercase()
        return list.filter { item ->
            val matchesQuery = q.isBlank() || item.symbol.uppercase().contains(q) || item.name.uppercase().contains(q)
            val matchesStrategy = state.selectedStrategy == "TOUTES" || item.strategyType == state.selectedStrategy
            matchesQuery && matchesStrategy
        }
    }

    private fun forecastSucceeded(f: AiForecastEntity, outcome: AiForecastOutcomeEntity?): Boolean {
        if (f.status.equals("HIT_TARGET", true)) return true
        if ((f.finalPerformancePercent ?: Double.NaN) > 0.0 && f.status.equals("EXPIRED", true)) return true
        return listOf(outcome?.successJ1, outcome?.successJ3, outcome?.successJ7, outcome?.successJ30, outcome?.successJ90).any { it == true }
    }

    private fun forecastFailed(f: AiForecastEntity, outcome: AiForecastOutcomeEntity?): Boolean {
        if (f.status.equals("HIT_STOP", true) || f.status.equals("CANCELLED", true)) return true
        if (f.status.equals("EXPIRED", true) && (f.finalPerformancePercent ?: 0.0) <= 0.0) return true
        val known = listOf(outcome?.successJ1, outcome?.successJ3, outcome?.successJ7, outcome?.successJ30, outcome?.successJ90).filterNotNull()
        return known.isNotEmpty() && known.all { !it }
    }
}

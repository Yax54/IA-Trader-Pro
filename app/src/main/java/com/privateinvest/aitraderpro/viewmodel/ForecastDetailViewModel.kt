package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForecastDetailUiState(
    val loading: Boolean = true,
    val forecast: AiForecastEntity? = null,
    val outcome: AiForecastOutcomeEntity? = null,
    val error: String? = null
)

class ForecastDetailViewModel : ViewModel() {
    private val repo get() = ServiceLocator.aiForecastRepository
    private val _uiState = MutableStateFlow(ForecastDetailUiState())
    val uiState: StateFlow<ForecastDetailUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = ForecastDetailUiState(loading = true)
            try {
                val forecast = (repo.getAllCT() + repo.getAllLT()).firstOrNull { it.id == id }
                val outcome = if (forecast != null) repo.getOutcome(forecast.id) else null
                _uiState.value = ForecastDetailUiState(
                    loading = false,
                    forecast = forecast,
                    outcome = outcome,
                    error = if (forecast == null) "Pronostic introuvable" else null
                )
            } catch (e: Exception) {
                _uiState.value = ForecastDetailUiState(loading = false, error = e.message)
            }
        }
    }
}

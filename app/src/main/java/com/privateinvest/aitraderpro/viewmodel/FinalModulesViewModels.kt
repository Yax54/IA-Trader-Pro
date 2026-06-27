package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.repository.AdvancedStatsSummary
import com.privateinvest.aitraderpro.repository.DailyReportSummary
import com.privateinvest.aitraderpro.repository.FinalModulesRepository
import com.privateinvest.aitraderpro.repository.OnboardingStep
import com.privateinvest.aitraderpro.repository.PortfolioProfile
import com.privateinvest.aitraderpro.repository.PreferencesCenterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsCenterUiState(val loading: Boolean = true, val summary: AdvancedStatsSummary? = null, val error: String? = null)
data class PreferencesCenterUiState(val loading: Boolean = true, val preferences: PreferencesCenterState? = null, val message: String? = null)
data class MultiPortfolioUiState(val loading: Boolean = true, val items: List<PortfolioProfile> = emptyList())
data class DailyReportUiState(val loading: Boolean = true, val report: DailyReportSummary? = null, val error: String? = null)
data class OnboardingUiState(val loading: Boolean = true, val steps: List<OnboardingStep> = emptyList())

class StatsCenterViewModel : ViewModel() {
    private val repo = FinalModulesRepository()
    private val _uiState = MutableStateFlow(StatsCenterUiState())
    val uiState: StateFlow<StatsCenterUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        runCatching { repo.advancedStats() }
            .onSuccess { _uiState.value = StatsCenterUiState(false, it) }
            .onFailure { _uiState.value = StatsCenterUiState(false, error = it.message) }
    }
}

class PreferencesCenterViewModel : ViewModel() {
    private val repo = FinalModulesRepository()
    private val _uiState = MutableStateFlow(PreferencesCenterUiState())
    val uiState: StateFlow<PreferencesCenterUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { _uiState.value = PreferencesCenterUiState(false, repo.preferences()) }
    fun setBeginnerMode(enabled: Boolean) = viewModelScope.launch {
        repo.setBeginnerMode(enabled)
        _uiState.value = PreferencesCenterUiState(false, repo.preferences(), "Préférence enregistrée")
    }
}

class MultiPortfolioViewModel : ViewModel() {
    private val repo = FinalModulesRepository()
    private val _uiState = MutableStateFlow(MultiPortfolioUiState())
    val uiState: StateFlow<MultiPortfolioUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { _uiState.value = MultiPortfolioUiState(false, repo.portfolios()) }
}

class DailyReportViewModel : ViewModel() {
    private val repo = FinalModulesRepository()
    private val _uiState = MutableStateFlow(DailyReportUiState())
    val uiState: StateFlow<DailyReportUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        runCatching { repo.dailyReport() }
            .onSuccess { _uiState.value = DailyReportUiState(false, it) }
            .onFailure { _uiState.value = DailyReportUiState(false, error = it.message) }
    }
}

class OnboardingViewModel : ViewModel() {
    private val repo = FinalModulesRepository()
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { _uiState.value = OnboardingUiState(false, repo.onboarding()) }
}

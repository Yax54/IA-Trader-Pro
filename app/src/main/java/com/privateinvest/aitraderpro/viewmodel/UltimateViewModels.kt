package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.database.FavoriteAssetEntity
import com.privateinvest.aitraderpro.export.ExportReport
import com.privateinvest.aitraderpro.repository.BackupSummary
import com.privateinvest.aitraderpro.repository.CalendarDayItem
import com.privateinvest.aitraderpro.repository.ChartBundle
import com.privateinvest.aitraderpro.repository.GoalSummary
import com.privateinvest.aitraderpro.repository.HealthCheckItem
import com.privateinvest.aitraderpro.repository.JournalItem
import com.privateinvest.aitraderpro.repository.RobustnessSummary
import com.privateinvest.aitraderpro.repository.UltimateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopOpportunitiesUiState(val loading: Boolean = true, val items: List<AssetSignal> = emptyList(), val error: String? = null)
data class JournalUiState(val loading: Boolean = true, val items: List<JournalItem> = emptyList(), val error: String? = null)
data class CalendarUiState(val loading: Boolean = true, val days: List<CalendarDayItem> = emptyList(), val error: String? = null)
data class FavoritesUiState(val loading: Boolean = true, val items: List<FavoriteAssetEntity> = emptyList(), val message: String? = null)
data class HealthUiState(val loading: Boolean = true, val checks: List<HealthCheckItem> = emptyList(), val error: String? = null, val chartBundle: ChartBundle? = null)
data class GoalUiState(val loading: Boolean = true, val summary: GoalSummary? = null, val message: String? = null, val chartBundle: ChartBundle? = null)
data class BackupUiState(val loading: Boolean = true, val summary: BackupSummary? = null, val report: String = "", val backupJson: String = "")
data class RobustnessUiState(val loading: Boolean = true, val summary: RobustnessSummary? = null, val chartBundle: ChartBundle? = null, val error: String? = null)
data class ExportUiState(val loading: Boolean = true, val report: ExportReport? = null, val message: String? = null)

class TopOpportunitiesViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(TopOpportunitiesUiState())
    val uiState: StateFlow<TopOpportunitiesUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        runCatching { repo.topOpportunities() }
            .onSuccess { _uiState.value = TopOpportunitiesUiState(false, it) }
            .onFailure { _uiState.value = TopOpportunitiesUiState(false, error = it.message) }
    }
}

class JournalViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        runCatching { repo.journal() }
            .onSuccess { _uiState.value = JournalUiState(false, it) }
            .onFailure { _uiState.value = JournalUiState(false, error = it.message) }
    }
}

class CalendarIaViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        runCatching { repo.calendar() }
            .onSuccess { _uiState.value = CalendarUiState(false, it) }
            .onFailure { _uiState.value = CalendarUiState(false, error = it.message) }
    }
}

class FavoritesViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { _uiState.value = FavoritesUiState(false, repo.favorites()) }
    fun addExampleFavorite(symbol: String, name: String) = viewModelScope.launch {
        val added = repo.toggleFavorite(symbol, name)
        _uiState.value = FavoritesUiState(false, repo.favorites(), if (added) "$symbol ajouté aux favoris" else "$symbol retiré des favoris")
    }
}

class HealthViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        runCatching { repo.health() to repo.chartBundle() }
            .onSuccess { _uiState.value = HealthUiState(false, it.first, chartBundle = it.second) }
            .onFailure { _uiState.value = HealthUiState(false, error = it.message) }
    }
}

class GoalViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { _uiState.value = GoalUiState(false, repo.goalSummary(), chartBundle = repo.chartBundle()) }
    fun setGoal(percent: Double) = viewModelScope.launch {
        repo.setGoal(percent)
        _uiState.value = GoalUiState(false, repo.goalSummary(), "Objectif mis à jour", repo.chartBundle())
    }
}

class BackupViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _uiState.value = BackupUiState(false, repo.backupSummary(), repo.exportTextReport(), repo.exportBackupJson())
    }
}

class RobustnessViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(RobustnessUiState())
    val uiState: StateFlow<RobustnessUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        runCatching { repo.robustnessSummary() to repo.chartBundle() }
            .onSuccess { _uiState.value = RobustnessUiState(false, it.first, it.second) }
            .onFailure { _uiState.value = RobustnessUiState(false, error = it.message) }
    }
}

class ExportCenterViewModel : ViewModel() {
    private val repo = UltimateRepository()
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    init { loadReport("complet") }
    fun loadReport(type: String) = viewModelScope.launch {
        runCatching { repo.buildExportReport(type) }
            .onSuccess { _uiState.value = ExportUiState(false, it) }
            .onFailure { _uiState.value = ExportUiState(false, message = it.message) }
    }
}

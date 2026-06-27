package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import com.privateinvest.aitraderpro.repository.AiForecastRepository
import com.privateinvest.aitraderpro.repository.BucketConfidence
import com.privateinvest.aitraderpro.repository.ForecastVsPlayedResult
import com.privateinvest.aitraderpro.repository.MemoryType
import com.privateinvest.aitraderpro.repository.RemarkableForecast
import com.privateinvest.aitraderpro.repository.StrategyStats
import com.privateinvest.aitraderpro.repository.SectorStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── ÉTAT UI ─────────────────────────────────────────────────────────────────

data class AiForecastUiState(
    // ── Court Terme (CT) ──────────────────────────────────────────────────────
    val activeForecastsCT: List<AiForecastEntity> = emptyList(),
    val allForecastsCT: List<AiForecastEntity> = emptyList(),
    val stratStatsCT: List<StrategyStats> = emptyList(),
    val bucketConfidenceCT: List<BucketConfidence> = emptyList(),
    val precisionOverTimeCT: List<Pair<Long, Double>> = emptyList(),
    val sectorStatsCT: List<SectorStats> = emptyList(),

    // ── Long Terme (LT) ───────────────────────────────────────────────────────
    val activeForecastsLT: List<AiForecastEntity> = emptyList(),
    val allForecastsLT: List<AiForecastEntity> = emptyList(),
    val stratStatsLT: List<StrategyStats> = emptyList(),
    val bucketConfidenceLT: List<BucketConfidence> = emptyList(),
    val precisionOverTimeLT: List<Pair<Long, Double>> = emptyList(),
    val sectorStatsLT: List<SectorStats> = emptyList(),

    // ── Global ────────────────────────────────────────────────────────────────
    val outcomes: Map<Long, AiForecastOutcomeEntity> = emptyMap(),
    val remarkableUnplayed: List<RemarkableForecast> = emptyList(),
    val forecastVsPlayed: List<ForecastVsPlayedResult> = emptyList(),

    // ── UI ────────────────────────────────────────────────────────────────────
    val selectedTab: ForecastTab = ForecastTab.CT,
    val isLoading: Boolean = false,
    val exportCsvContent: String? = null,
    val errorMessage: String? = null,

    // ── Quotas (affichage) ───────────────────────────────────────────────────
    val quotaQuick: Int = 5,
    val quotaSwing: Int = 5,
    val quotaHighVol: Int = 2,
    val quotaLt: Int = 2
)

enum class ForecastTab { CT, LT, COMPARE, STATS }

// ─── VIEWMODEL ───────────────────────────────────────────────────────────────

class AiForecastViewModel : ViewModel() {

    private val repo: AiForecastRepository get() = ServiceLocator.aiForecastRepository
    private val adaptiveWeights get() = ServiceLocator.adaptiveWeightsManager
    private val userPrefs get() = ServiceLocator.userPreferencesRepository

    private val _uiState = MutableStateFlow(AiForecastUiState())
    val uiState: StateFlow<AiForecastUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    // ─── CHARGEMENT COMPLET ──────────────────────────────────────────────────

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // CT
                val activeCT  = repo.getActiveCT()
                val allCT     = repo.getAllCT()
                val statsCT   = repo.statsByStrategy(MemoryType.CT)
                val bucketsCT = repo.confidenceByBucket(MemoryType.CT)
                val precCT    = repo.precisionOverTime(MemoryType.CT)
                val sectorCT  = repo.statsBySector(MemoryType.CT)

                // LT (complètement indépendant)
                val activeLT  = repo.getActiveLT()
                val allLT     = repo.getAllLT()
                val statsLT   = repo.statsByStrategy(MemoryType.LT)
                val bucketsLT = repo.confidenceByBucket(MemoryType.LT)
                val precLT    = repo.precisionOverTime(MemoryType.LT)
                val sectorLT  = repo.statsBySector(MemoryType.LT)

                // Outcomes (map forecastId → outcome)
                val allOutcomes = repo.getAllOutcomes().associateBy { it.forecastId }

                // Comparaison
                val comparison = repo.compareForecastVsPlayed()

                // Pronostics remarquables (sans notification dans loadAll — seulement depuis Worker)
                val remarkable = (activeCT + activeLT).mapNotNull { f ->
                    val outcome = allOutcomes[f.id] ?: return@mapNotNull null
                    val perfs = listOfNotNull(
                        outcome.performanceJ90, outcome.performanceJ30,
                        outcome.performanceJ7, outcome.performanceJ1
                    )
                    val best = perfs.maxOrNull() ?: return@mapNotNull null
                    if (best >= AiForecastRepository.REMARKABLE_THRESHOLD && f.userAction == "NONE") {
                        val horizon = when {
                            outcome.performanceJ90 == best -> "J+90"
                            outcome.performanceJ30 == best -> "J+30"
                            outcome.performanceJ7  == best -> "J+7"
                            else -> "J+1"
                        }
                        RemarkableForecast(f, outcome, best, horizon)
                    } else null
                }

                _uiState.value = _uiState.value.copy(
                    activeForecastsCT = activeCT,
                    allForecastsCT    = allCT,
                    stratStatsCT      = statsCT,
                    bucketConfidenceCT = bucketsCT,
                    precisionOverTimeCT = precCT,
                    sectorStatsCT     = sectorCT,

                    activeForecastsLT = activeLT,
                    allForecastsLT    = allLT,
                    stratStatsLT      = statsLT,
                    bucketConfidenceLT = bucketsLT,
                    precisionOverTimeLT = precLT,
                    sectorStatsLT     = sectorLT,

                    outcomes          = allOutcomes,
                    forecastVsPlayed  = comparison,
                    remarkableUnplayed = remarkable,
                    isLoading         = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Erreur de chargement : ${e.message}"
                )
            }
        }
    }

    // ─── ACTIONS UTILISATEUR ─────────────────────────────────────────────────

    fun onTabSelected(tab: ForecastTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun markAsPlayed(forecastId: Long) {
        viewModelScope.launch {
            repo.markAsPlayed(forecastId)
            loadAll()
        }
    }

    fun markAsIgnored(forecastId: Long) {
        viewModelScope.launch {
            repo.markAsIgnored(forecastId)
            loadAll()
        }
    }

    // ─── EXPORT CSV ──────────────────────────────────────────────────────────

    fun exportCsv(memoryType: String? = null) {
        viewModelScope.launch {
            try {
                val csv = repo.exportToCsv(memoryType)
                _uiState.value = _uiState.value.copy(exportCsvContent = csv)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Erreur export CSV : ${e.message}"
                )
            }
        }
    }

    fun clearCsvExport() {
        _uiState.value = _uiState.value.copy(exportCsvContent = null)
    }

    // ─── APPRENTISSAGE ADAPTATIF ─────────────────────────────────────────────

    /** Force l'alimentation de l'AdaptiveWeightsManager depuis les pronostics non joués. */
    fun triggerAdaptiveLearning() {
        viewModelScope.launch {
            try {
                repo.feedAdaptiveWeightsFromForecasts(adaptiveWeights, MemoryType.CT)
                repo.feedAdaptiveWeightsFromForecasts(adaptiveWeights, MemoryType.LT)
            } catch (e: Exception) {
                // Silent — pas critique pour l'UI
            }
        }
    }

    // ─── RAFRAÎCHISSEMENT MANUEL ─────────────────────────────────────────────

    fun refreshOutcomes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repo.refreshOutcomes()
                loadAll()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Erreur de rafraîchissement : ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

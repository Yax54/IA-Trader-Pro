package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.repository.OpportunityStrategyType
import com.privateinvest.aitraderpro.repository.StrategyFollowUpItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StrategyMonitoringUiState(
    val activeFollowUps: List<StrategyFollowUpItem> = emptyList(),
    val closedFollowUps: List<StrategyFollowUpItem> = emptyList(),
    val alertCount: Int = 0,
    val loading: Boolean = false,
    val statusMessage: String? = null,
    val showClosedHistory: Boolean = false
)

class StrategyMonitoringViewModel : ViewModel() {

    private val repository get() = ServiceLocator.strategyMonitoringRepository

    private val _uiState = MutableStateFlow(StrategyMonitoringUiState())
    val uiState: StateFlow<StrategyMonitoringUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, statusMessage = null)
            runCatching {
                val items = repository.getFollowUps()
                val active = items.filter { it.status != "CLOSED" }
                val closed = items.filter { it.status == "CLOSED" }
                val alerts = active.count { it.status == "SELL_ALERT" }
                _uiState.value = _uiState.value.copy(
                    activeFollowUps = active,
                    closedFollowUps = closed,
                    alertCount = alerts,
                    loading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    statusMessage = "Erreur de chargement : ${it.message}"
                )
            }
        }
    }

    /**
     * Crée un suivi à partir d'un actif et d'un mode de trading.
     * Ne fait rien si la stratégie retournée est DEFENSIVE sans horizon.
     */
    fun createFollowUp(
        symbol: String,
        name: String,
        mode: String,
        preferredType: OpportunityStrategyType? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, statusMessage = null)
            runCatching {
                val created = repository.createFollowUp(symbol, name, mode, preferredType)
                _uiState.value = _uiState.value.copy(
                    statusMessage = if (created)
                        "Suivi activé pour $symbol en mode $mode. L'application t'alertera sans jamais vendre automatiquement."
                    else
                        "Aucune stratégie applicable pour $symbol pour l'instant. Surveille et réessaie plus tard."
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Impossible d'activer le suivi : ${it.message}"
                )
            }
            load()
        }
    }

    /** Clôture manuellement un suivi avec enregistrement de l'historique. */
    fun closeFollowUp(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.closeFollowUp(id)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Suivi clôturé. Résultat enregistré dans l'historique."
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Erreur lors de la clôture : ${it.message}"
                )
            }
            load()
        }
    }

    /** Prolonge la durée maximale d'un suivi actif. */
    fun extendFollowUp(id: Long, extraDays: Int = 3) {
        viewModelScope.launch {
            runCatching {
                repository.extendFollowUp(id, extraDays)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Durée de suivi prolongée de $extraDays jour(s)."
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Erreur lors de la prolongation : ${it.message}"
                )
            }
            load()
        }
    }

    /** Force un refresh immédiat des prix et des statuts. */
    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.refreshFollowUps() }
            load()
        }
    }

    fun toggleClosedHistory() {
        _uiState.value = _uiState.value.copy(
            showClosedHistory = !_uiState.value.showClosedHistory
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}

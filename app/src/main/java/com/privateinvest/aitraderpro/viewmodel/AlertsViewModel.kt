package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.data.model.NotificationHistoryItem
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertsViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _alert = MutableStateFlow<AssetSignal?>(null)
    val alert: StateFlow<AssetSignal?> = _alert.asStateFlow()

    private val _history = MutableStateFlow<List<NotificationHistoryItem>>(emptyList())
    val history: StateFlow<List<NotificationHistoryItem>> = _history.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _alert.value = repository.getLatestAlert()
            _history.value = repository.getNotificationHistory()
        }
    }

    fun confirmAlert() {
        val current = _alert.value ?: return
        viewModelScope.launch {
            val success = repository.confirmSimulationOrder(current.symbol)
            _statusMessage.value = if (success) {
                "Signal enregistré en mémoire, achat simulé exécuté et suivi J+1 / J+7 / J+30 démarré."
            } else {
                "Validation impossible sans données réelles disponibles ou sans capital virtuel suffisant."
            }
            refresh()
        }
    }
}

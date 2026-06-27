package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.AssetDetail
import com.privateinvest.aitraderpro.navigation.SelectedAssetStore
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssetDetailViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _detail = MutableStateFlow<AssetDetail?>(null)
    val detail: StateFlow<AssetDetail?> = _detail.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _detail.value = repository.getMarketData(SelectedAssetStore.currentSymbol)
        }
    }

    fun confirmSimulationOrder() {
        viewModelScope.launch {
            val success = repository.confirmSimulationOrder(SelectedAssetStore.currentSymbol)
            _statusMessage.value = if (success) {
                "Signal mémorisé. Le suivi intelligent J+1 / J+7 / J+30 est activé."
            } else {
                "Impossible d'enregistrer le signal sans données de marché réelles."
            }
        }
    }
}

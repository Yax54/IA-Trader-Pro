package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.navigation.SelectedAssetStore
import com.privateinvest.aitraderpro.repository.MarketDataCenterState
import com.privateinvest.aitraderpro.repository.MarketDataConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarketDataCenterUiState(
    val loading: Boolean = true,
    val symbol: String = SelectedAssetStore.currentSymbol.ifBlank { "NVDA" },
    val state: MarketDataCenterState? = null,
    val error: String? = null
)

class MarketDataCenterViewModel(
    private val repository: MarketDataConnectionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MarketDataCenterUiState())
    val uiState: StateFlow<MarketDataCenterUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun setSymbol(symbol: String) {
        _uiState.value = _uiState.value.copy(symbol = symbol.uppercase())
    }

    fun refresh() {
        val symbol = _uiState.value.symbol.ifBlank { SelectedAssetStore.currentSymbol.ifBlank { "NVDA" } }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching { repository.inspect(symbol) }
                .onSuccess { _uiState.value = _uiState.value.copy(loading = false, state = it, symbol = it.symbol) }
                .onFailure { _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "Erreur flux marché") }
        }
    }
}

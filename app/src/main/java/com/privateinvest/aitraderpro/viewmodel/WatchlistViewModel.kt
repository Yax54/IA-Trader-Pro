package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchlistViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<AssetSignal>>(emptyList())
    val items: StateFlow<List<AssetSignal>> = _items.asStateFlow()

    init {
        viewModelScope.launch { _items.value = repository.getWatchlist() }
    }
}

package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// R-13 : ajout de loading + refresh()
class WatchlistViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<AssetSignal>>(emptyList())
    val items: StateFlow<List<AssetSignal>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { repository.getWatchlist() }
                .onSuccess { _items.value = it }
                .onFailure { _items.value = emptyList() }
            _loading.value = false
        }
    }
}

package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.RiskRule
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RiskViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _rules = MutableStateFlow<List<RiskRule>>(emptyList())
    val rules: StateFlow<List<RiskRule>> = _rules.asStateFlow()

    init {
        viewModelScope.launch { _rules.value = repository.getRiskRules() }
    }
}

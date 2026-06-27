package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.data.model.AuditSummary
import com.privateinvest.aitraderpro.data.model.LearningStats
import com.privateinvest.aitraderpro.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuditViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _summary = MutableStateFlow(
        AuditSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyMap())
    )
    val summary: StateFlow<AuditSummary> = _summary.asStateFlow()

    private val _learningStats = MutableStateFlow(LearningStats())
    val learningStats: StateFlow<LearningStats> = _learningStats.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val audit = repository.getAuditSummary()
            _summary.value = audit
            _learningStats.value = audit.learningStats
        }
    }
}

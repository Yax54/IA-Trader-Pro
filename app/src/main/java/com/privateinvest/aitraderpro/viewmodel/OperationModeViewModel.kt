package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.repository.OperationMode
import com.privateinvest.aitraderpro.repository.OperationModeRepository
import com.privateinvest.aitraderpro.repository.OperationModeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OperationModeViewModel(
    private val repository: OperationModeRepository
) : ViewModel() {
    val settings: StateFlow<OperationModeSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        OperationModeSettings()
    )

    fun setMode(mode: OperationMode) = viewModelScope.launch { repository.setMode(mode) }
    fun setLiveFrequency(label: String) = viewModelScope.launch { repository.setLiveFrequency(label) }
    fun setSurveillanceFrequency(label: String) = viewModelScope.launch { repository.setSurveillanceFrequency(label) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    fun setBatteryMode(label: String) = viewModelScope.launch { repository.setBatteryMode(label) }
}

package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val beginnerModeEnabled: StateFlow<Boolean> = userPreferencesRepository.beginnerModeEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val expertModeEnabled: StateFlow<Boolean> = userPreferencesRepository.expertModeEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setBeginnerModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBeginnerModeEnabled(enabled)
        }
    }

    fun setExpertModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBeginnerModeEnabled(!enabled)
        }
    }
}

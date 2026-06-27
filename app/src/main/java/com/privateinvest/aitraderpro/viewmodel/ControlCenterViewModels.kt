package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.repository.ControlCenterRepository
import com.privateinvest.aitraderpro.repository.ControlCenterState
import com.privateinvest.aitraderpro.repository.LocalBackupSnapshot
import com.privateinvest.aitraderpro.repository.RealValidationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ControlCenterUiState(
    val loading: Boolean = true,
    val state: ControlCenterState? = null,
    val error: String? = null
)

data class RealValidationUiState(
    val loading: Boolean = true,
    val state: RealValidationState? = null,
    val lastBackup: LocalBackupSnapshot? = null,
    val message: String? = null,
    val error: String? = null
)

class ControlCenterViewModel : ViewModel() {
    private val repo = ControlCenterRepository()
    private val _uiState = MutableStateFlow(ControlCenterUiState())
    val uiState: StateFlow<ControlCenterUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        runCatching { repo.cockpit() }
            .onSuccess { _uiState.value = ControlCenterUiState(false, it) }
            .onFailure { _uiState.value = ControlCenterUiState(false, error = it.message) }
    }
}

class RealValidationViewModel : ViewModel() {
    private val repo = ControlCenterRepository()
    private val _uiState = MutableStateFlow(RealValidationUiState())
    val uiState: StateFlow<RealValidationUiState> = _uiState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        runCatching { repo.realValidation() }
            .onSuccess { _uiState.value = _uiState.value.copy(loading = false, state = it) }
            .onFailure { _uiState.value = _uiState.value.copy(loading = false, error = it.message) }
    }
    fun createSafetyBackup() = viewModelScope.launch {
        runCatching { repo.createSafetyBackup("validation_reelle") }
            .onSuccess {
                _uiState.value = _uiState.value.copy(
                    lastBackup = it,
                    message = "Sauvegarde locale créée : ${it.fileName}"
                )
                refresh()
            }
            .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
    }
}

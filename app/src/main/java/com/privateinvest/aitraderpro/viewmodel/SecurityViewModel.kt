package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privateinvest.aitraderpro.database.SecurityLogEntity
import com.privateinvest.aitraderpro.repository.SecurityRepository
import com.privateinvest.aitraderpro.repository.SecurityState
import com.privateinvest.aitraderpro.repository.TradingExecutionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SecurityUiState(
    val state: SecurityState = SecurityState(),
    val logs: List<SecurityLogEntity> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

class SecurityViewModel(private val repository: SecurityRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.securityState.collect { state ->
                _uiState.value = _uiState.value.copy(state = state)
                refreshLogs()
            }
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(logs = repository.latestLogs())
        }
    }

    fun createOrChangePin(pin: String, confirm: String) {
        viewModelScope.launch {
            when {
                pin != confirm -> _uiState.value = _uiState.value.copy(error = "Les deux PIN ne correspondent pas.", message = null)
                !pin.matches(Regex("^[0-9]{4,6}$")) -> _uiState.value = _uiState.value.copy(error = "Le PIN doit contenir 4 à 6 chiffres.", message = null)
                repository.createOrChangePin(pin) -> _uiState.value = _uiState.value.copy(message = "PIN sécurisé enregistré.", error = null)
                else -> _uiState.value = _uiState.value.copy(error = "PIN invalide.", message = null)
            }
            refreshLogs()
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.verifyPin(pin)
            _uiState.value = _uiState.value.copy(
                message = if (ok) "Authentification validée." else null,
                error = if (!ok) "PIN incorrect. Après 3 erreurs, le mode réel est verrouillé." else null
            )
            refreshLogs()
            onResult(ok)
        }
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFingerprintEnabled(enabled)
            _uiState.value = _uiState.value.copy(message = if (enabled) "Empreinte activée." else "Empreinte désactivée.", error = null)
            refreshLogs()
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            repository.unlockWithBiometric()
            _uiState.value = _uiState.value.copy(message = "Empreinte validée.", error = null)
            refreshLogs()
        }
    }

    fun setTradingMode(mode: TradingExecutionMode) {
        viewModelScope.launch {
            repository.setTradingMode(mode)
            _uiState.value = _uiState.value.copy(message = "Mode ${mode.label} activé.", error = null)
            refreshLogs()
        }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setAutoLockMinutes(minutes)
            _uiState.value = _uiState.value.copy(message = "Verrouillage automatique : $minutes minutes.", error = null)
            refreshLogs()
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}

package com.privateinvest.aitraderpro.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.privateinvest.aitraderpro.database.SecurityLogDao
import com.privateinvest.aitraderpro.database.SecurityLogEntity
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.securityDataStore by preferencesDataStore(name = "ai_trader_security")

enum class TradingExecutionMode(val label: String) {
    SIMULATION("Simulation"),
    PAPER_TRADING("Paper trading"),
    REAL("Réel")
}

data class SecurityState(
    val pinConfigured: Boolean = false,
    val fingerprintEnabled: Boolean = false,
    val tradingMode: TradingExecutionMode = TradingExecutionMode.SIMULATION,
    val requirePinBeforeRealOrder: Boolean = true,
    val requirePinBeforeModeChange: Boolean = true,
    val requirePinBeforeBrokerSettings: Boolean = true,
    val requirePinBeforeBackupRestore: Boolean = true,
    val autoLockMinutes: Int = 10,
    val failedPinAttempts: Int = 0,
    val realModeLocked: Boolean = false,
    val lastUnlockedAt: Long = 0L
)

class SecurityRepository(
    private val context: Context,
    private val securityLogDao: SecurityLogDao
) {
    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val FINGERPRINT = booleanPreferencesKey("fingerprint_enabled")
        val TRADING_MODE = stringPreferencesKey("trading_mode")
        val REQUIRE_ORDER = booleanPreferencesKey("require_pin_before_real_order")
        val REQUIRE_MODE = booleanPreferencesKey("require_pin_before_mode_change")
        val REQUIRE_BROKER = booleanPreferencesKey("require_pin_before_broker_settings")
        val REQUIRE_BACKUP = booleanPreferencesKey("require_pin_before_backup_restore")
        val AUTO_LOCK = intPreferencesKey("auto_lock_minutes")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val REAL_LOCKED = booleanPreferencesKey("real_mode_locked")
        val LAST_UNLOCKED = stringPreferencesKey("last_unlocked_at")
    }

    val securityState: Flow<SecurityState> = context.securityDataStore.data.map { prefs: Preferences ->
        val mode = runCatching {
            TradingExecutionMode.valueOf(prefs[Keys.TRADING_MODE] ?: TradingExecutionMode.SIMULATION.name)
        }.getOrDefault(TradingExecutionMode.SIMULATION)
        SecurityState(
            pinConfigured = !prefs[Keys.PIN_HASH].isNullOrBlank(),
            fingerprintEnabled = prefs[Keys.FINGERPRINT] ?: false,
            tradingMode = mode,
            requirePinBeforeRealOrder = prefs[Keys.REQUIRE_ORDER] ?: true,
            requirePinBeforeModeChange = prefs[Keys.REQUIRE_MODE] ?: true,
            requirePinBeforeBrokerSettings = prefs[Keys.REQUIRE_BROKER] ?: true,
            requirePinBeforeBackupRestore = prefs[Keys.REQUIRE_BACKUP] ?: true,
            autoLockMinutes = prefs[Keys.AUTO_LOCK] ?: 10,
            failedPinAttempts = prefs[Keys.FAILED_ATTEMPTS] ?: 0,
            realModeLocked = prefs[Keys.REAL_LOCKED] ?: false,
            lastUnlockedAt = prefs[Keys.LAST_UNLOCKED]?.toLongOrNull() ?: 0L
        )
    }

    suspend fun createOrChangePin(pin: String): Boolean {
        if (!pin.matches(Regex("^[0-9]{4,6}$"))) return false
        val salt = randomSalt()
        val hash = hashPin(pin, salt)
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.REAL_LOCKED] = false
            prefs[Keys.LAST_UNLOCKED] = System.currentTimeMillis().toString()
        }
        log("PIN_CONFIGURED", "OK", "Code PIN créé ou modifié")
        return true
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.securityDataStore.data.first()
        val hash = prefs[Keys.PIN_HASH]
        val salt = prefs[Keys.PIN_SALT]
        if (hash.isNullOrBlank() || salt.isNullOrBlank()) return false
        val ok = hashPin(pin, salt) == hash
        context.securityDataStore.edit { edit ->
            if (ok) {
                edit[Keys.FAILED_ATTEMPTS] = 0
                edit[Keys.REAL_LOCKED] = false
                edit[Keys.LAST_UNLOCKED] = System.currentTimeMillis().toString()
            } else {
                val attempts = (edit[Keys.FAILED_ATTEMPTS] ?: 0) + 1
                edit[Keys.FAILED_ATTEMPTS] = attempts
                if (attempts >= 3) {
                    edit[Keys.REAL_LOCKED] = true
                    edit[Keys.TRADING_MODE] = TradingExecutionMode.SIMULATION.name
                }
            }
        }
        log(if (ok) "PIN_SUCCESS" else "PIN_FAILED", if (ok) "OK" else "WARNING", if (ok) "PIN validé" else "PIN incorrect")
        return ok
    }

    suspend fun unlockWithBiometric() {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.REAL_LOCKED] = false
            prefs[Keys.LAST_UNLOCKED] = System.currentTimeMillis().toString()
        }
        log("BIOMETRIC_SUCCESS", "OK", "Authentification par empreinte validée")
    }

    suspend fun setFingerprintEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs -> prefs[Keys.FINGERPRINT] = enabled }
        log("FINGERPRINT_${if (enabled) "ENABLED" else "DISABLED"}", "OK", "Empreinte digitale ${if (enabled) "activée" else "désactivée"}")
    }

    suspend fun setTradingMode(mode: TradingExecutionMode) {
        context.securityDataStore.edit { prefs -> prefs[Keys.TRADING_MODE] = mode.name }
        log("TRADING_MODE_CHANGED", if (mode == TradingExecutionMode.REAL) "WARNING" else "OK", "Mode actif : ${mode.label}")
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.securityDataStore.edit { prefs -> prefs[Keys.AUTO_LOCK] = minutes.coerceIn(5, 120) }
        log("AUTO_LOCK_CHANGED", "OK", "Déconnexion automatique : $minutes minutes")
    }

    suspend fun setRequireOrder(enabled: Boolean) {
        context.securityDataStore.edit { prefs -> prefs[Keys.REQUIRE_ORDER] = enabled }
    }

    suspend fun setRequireModeChange(enabled: Boolean) {
        context.securityDataStore.edit { prefs -> prefs[Keys.REQUIRE_MODE] = enabled }
    }

    suspend fun log(event: String, level: String, details: String) {
        securityLogDao.insert(SecurityLogEntity(event = event, level = level, details = details, createdAt = System.currentTimeMillis()))
    }

    suspend fun latestLogs(limit: Int = 80): List<SecurityLogEntity> = securityLogDao.getLatest(limit)

    fun securityBackupNotice(): String = "Les paramètres de sécurité exportables peuvent inclure le mode actif et les préférences, mais jamais le PIN ni son hash."

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

package com.privateinvest.aitraderpro.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class OperationMode(val label: String, val shortLabel: String, val description: String) {
    LIVE("LIVE", "🟢 LIVE", "Application ouverte : actualisation rapide et visible."),
    SURVEILLANCE("SURVEILLANCE", "🟡 SURVEILLANCE", "Arrière-plan : vérifications périodiques et notifications importantes."),
    VEILLE("VEILLE", "⚪ VEILLE", "Aucune surveillance automatique. Mise à jour à l’ouverture.")
}

data class OperationModeSettings(
    val mode: OperationMode = OperationMode.LIVE,
    val liveFrequencyLabel: String = "30 s",
    val liveFrequencySeconds: Int = 30,
    val surveillanceFrequencyLabel: String = "30 min",
    val surveillanceFrequencyMinutes: Int = 30,
    val notificationsEnabled: Boolean = true,
    val batteryMode: String = "Équilibré"
)

private val Context.operationModeDataStore by preferencesDataStore(name = "ai_trader_operation_mode")

class OperationModeRepository(private val context: Context) {
    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val LIVE_FREQ = stringPreferencesKey("live_frequency")
        val SURVEILLANCE_FREQ = stringPreferencesKey("surveillance_frequency")
        val NOTIFICATIONS = stringPreferencesKey("notifications")
        val BATTERY = stringPreferencesKey("battery")
    }

    val settings: Flow<OperationModeSettings> = context.operationModeDataStore.data.map { prefs ->
        val mode = runCatching { OperationMode.valueOf(prefs[Keys.MODE] ?: OperationMode.LIVE.name) }.getOrDefault(OperationMode.LIVE)
        val live = prefs[Keys.LIVE_FREQ] ?: "30 s"
        val surveillance = prefs[Keys.SURVEILLANCE_FREQ] ?: "30 min"
        OperationModeSettings(
            mode = mode,
            liveFrequencyLabel = live,
            liveFrequencySeconds = liveToSeconds(live),
            surveillanceFrequencyLabel = surveillance,
            surveillanceFrequencyMinutes = surveillanceToMinutes(surveillance),
            notificationsEnabled = (prefs[Keys.NOTIFICATIONS] ?: "true").toBoolean(),
            batteryMode = prefs[Keys.BATTERY] ?: "Équilibré"
        )
    }

    suspend fun setMode(mode: OperationMode) {
        context.operationModeDataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setLiveFrequency(label: String) {
        context.operationModeDataStore.edit { it[Keys.LIVE_FREQ] = label }
    }

    suspend fun setSurveillanceFrequency(label: String) {
        context.operationModeDataStore.edit { it[Keys.SURVEILLANCE_FREQ] = label }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.operationModeDataStore.edit { it[Keys.NOTIFICATIONS] = enabled.toString() }
    }

    suspend fun setBatteryMode(label: String) {
        context.operationModeDataStore.edit { it[Keys.BATTERY] = label }
    }

    suspend fun exportSnapshot(): String {
        val s = settings.first()
        return """
            {
              "mode": "${s.mode.name}",
              "liveFrequency": "${s.liveFrequencyLabel}",
              "surveillanceFrequency": "${s.surveillanceFrequencyLabel}",
              "notificationsEnabled": ${s.notificationsEnabled},
              "batteryMode": "${s.batteryMode}"
            }
        """.trimIndent()
    }

    companion object {
        val liveOptions = listOf("15 s", "30 s", "1 min", "5 min")
        val surveillanceOptions = listOf("15 min", "30 min", "1 h", "3 h")
        val batteryOptions = listOf("Réactif", "Équilibré", "Économie")
        fun liveToSeconds(label: String): Int = when (label) {
            "15 s" -> 15
            "1 min" -> 60
            "5 min" -> 300
            else -> 30
        }
        fun surveillanceToMinutes(label: String): Int = when (label) {
            "15 min" -> 15
            "1 h" -> 60
            "3 h" -> 180
            else -> 30
        }
    }
}

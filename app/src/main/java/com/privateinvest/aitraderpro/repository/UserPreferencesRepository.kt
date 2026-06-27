package com.privateinvest.aitraderpro.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "ai_trader_user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val BEGINNER_MODE_ENABLED = booleanPreferencesKey("beginner_mode_enabled")

        // ── Quotas Pronostics IA (CT — quotidien) ─────────────────────────────
        val QUOTA_QUICK     = intPreferencesKey("quota_quick")      // défaut : 5/jour
        val QUOTA_SWING     = intPreferencesKey("quota_swing")      // défaut : 5/jour
        val QUOTA_HIGH_VOL  = intPreferencesKey("quota_high_vol")   // défaut : 2/jour

        // ── Quotas Pronostics IA (LT — hebdomadaire) ─────────────────────────
        val QUOTA_LONG_TERM = intPreferencesKey("quota_long_term")  // défaut : 2/semaine
        val QUOTA_GROWTH    = intPreferencesKey("quota_growth")     // défaut : 2/semaine
        val QUOTA_DEFENSIVE = intPreferencesKey("quota_defensive")  // défaut : 2/semaine
    }

    val beginnerModeEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences: Preferences ->
        preferences[Keys.BEGINNER_MODE_ENABLED] ?: true
    }

    val expertModeEnabled: Flow<Boolean> = beginnerModeEnabled.map { !it }

    suspend fun setBeginnerModeEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.BEGINNER_MODE_ENABLED] = enabled
        }
    }

    // ─── QUOTAS PRONOSTICS IA ─────────────────────────────────────────────────

    /** Quota journalier — Opportunité rapide (QUICK). Défaut : 5. */
    val quotaQuick: Flow<Int> = context.userPreferencesDataStore.data.map { it[Keys.QUOTA_QUICK] ?: 5 }

    /** Quota journalier — Swing. Défaut : 5. */
    val quotaSwing: Flow<Int> = context.userPreferencesDataStore.data.map { it[Keys.QUOTA_SWING] ?: 5 }

    /** Quota journalier — Forte volatilité. Défaut : 2. */
    val quotaHighVol: Flow<Int> = context.userPreferencesDataStore.data.map { it[Keys.QUOTA_HIGH_VOL] ?: 2 }

    /** Quota hebdomadaire — Long terme. Défaut : 2. */
    val quotaLongTerm: Flow<Int> = context.userPreferencesDataStore.data.map { it[Keys.QUOTA_LONG_TERM] ?: 2 }

    /** Quota hebdomadaire — Croissance. Défaut : 2. */
    val quotaGrowth: Flow<Int> = context.userPreferencesDataStore.data.map { it[Keys.QUOTA_GROWTH] ?: 2 }

    /** Quota hebdomadaire — Défensif. Défaut : 2. */
    val quotaDefensive: Flow<Int> = context.userPreferencesDataStore.data.map { it[Keys.QUOTA_DEFENSIVE] ?: 2 }

    /** Met à jour le quota d'une stratégie. Valeur bornée entre 1 et 20. */
    suspend fun setQuota(strategyType: String, value: Int) {
        val bounded = value.coerceIn(1, 20)
        context.userPreferencesDataStore.edit { preferences ->
            when (strategyType) {
                "QUICK"           -> preferences[Keys.QUOTA_QUICK]     = bounded
                "SWING"           -> preferences[Keys.QUOTA_SWING]     = bounded
                "HIGH_VOLATILITY" -> preferences[Keys.QUOTA_HIGH_VOL]  = bounded
                "LONG_TERM"       -> preferences[Keys.QUOTA_LONG_TERM] = bounded
                "GROWTH"          -> preferences[Keys.QUOTA_GROWTH]    = bounded
                "DEFENSIVE"       -> preferences[Keys.QUOTA_DEFENSIVE] = bounded
            }
        }
    }

    /** Retourne la map des quotas actuels (depuis DataStore — valeurs par défaut si non configurés). */
    suspend fun getCurrentQuotas(): Map<String, Int> {
        val prefs = context.userPreferencesDataStore.data
        var result = mapOf<String, Int>()
        prefs.collect { preferences ->
            result = mapOf(
                "QUICK"           to (preferences[Keys.QUOTA_QUICK]     ?: 5),
                "SWING"           to (preferences[Keys.QUOTA_SWING]     ?: 5),
                "HIGH_VOLATILITY" to (preferences[Keys.QUOTA_HIGH_VOL]  ?: 2),
                "LONG_TERM"       to (preferences[Keys.QUOTA_LONG_TERM] ?: 2),
                "GROWTH"          to (preferences[Keys.QUOTA_GROWTH]    ?: 2),
                "DEFENSIVE"       to (preferences[Keys.QUOTA_DEFENSIVE] ?: 2)
            )
            return@collect
        }
        return result
    }
}

package com.privateinvest.aitraderpro.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "ai_trader_user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val BEGINNER_MODE_ENABLED = booleanPreferencesKey("beginner_mode_enabled")
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
}

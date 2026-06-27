package com.privateinvest.aitraderpro.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class MarketSortMode(val label: String) {
    SCORE("Score IA"),
    POTENTIAL("Potentiel"),
    RISK("Risque faible"),
    VOLUME("Volume"),
    VARIATION("Variation"),
    NAME("Nom")
}

data class MarketFiltersState(
    val query: String = "",
    val opportunityQuick: Boolean = true,
    val swing: Boolean = true,
    val longTerm: Boolean = false,
    val volatility: Boolean = false,
    val growth: Boolean = true,
    val defensive: Boolean = false,
    val dividend: Boolean = false,
    val minScore: Int = 70,
    val maxRisk: Int = 70,
    val minConfidence: Int = 65,
    val minPotential: Int = 0,
    val favoritesOnly: Boolean = false,
    val inPortfolioOnly: Boolean = false,
    val activeForecastOnly: Boolean = false,
    val sellAlertOnly: Boolean = false,
    val sortMode: MarketSortMode = MarketSortMode.SCORE,
    val lastScrollIndex: Int = 0
)

private val Context.marketFiltersDataStore by preferencesDataStore(name = "ai_trader_market_filters")

class MarketFiltersRepository(private val context: Context) {
    private object Keys {
        val QUERY = stringPreferencesKey("query")
        val QUICK = stringPreferencesKey("quick")
        val SWING = stringPreferencesKey("swing")
        val LONG = stringPreferencesKey("long")
        val VOLATILITY = stringPreferencesKey("volatility")
        val GROWTH = stringPreferencesKey("growth")
        val DEFENSIVE = stringPreferencesKey("defensive")
        val DIVIDEND = stringPreferencesKey("dividend")
        val MIN_SCORE = stringPreferencesKey("min_score")
        val MAX_RISK = stringPreferencesKey("max_risk")
        val MIN_CONFIDENCE = stringPreferencesKey("min_confidence")
        val MIN_POTENTIAL = stringPreferencesKey("min_potential")
        val FAVORITES = stringPreferencesKey("favorites")
        val PORTFOLIO = stringPreferencesKey("portfolio")
        val FORECAST = stringPreferencesKey("forecast")
        val SELL_ALERT = stringPreferencesKey("sell_alert")
        val SORT = stringPreferencesKey("sort")
        val SCROLL = stringPreferencesKey("scroll")
    }

    val filters: Flow<MarketFiltersState> = context.marketFiltersDataStore.data.map { p ->
        MarketFiltersState(
            query = p[Keys.QUERY] ?: "",
            opportunityQuick = (p[Keys.QUICK] ?: "true").toBoolean(),
            swing = (p[Keys.SWING] ?: "true").toBoolean(),
            longTerm = (p[Keys.LONG] ?: "false").toBoolean(),
            volatility = (p[Keys.VOLATILITY] ?: "false").toBoolean(),
            growth = (p[Keys.GROWTH] ?: "true").toBoolean(),
            defensive = (p[Keys.DEFENSIVE] ?: "false").toBoolean(),
            dividend = (p[Keys.DIVIDEND] ?: "false").toBoolean(),
            minScore = (p[Keys.MIN_SCORE] ?: "70").toIntOrNull() ?: 70,
            maxRisk = (p[Keys.MAX_RISK] ?: "70").toIntOrNull() ?: 70,
            minConfidence = (p[Keys.MIN_CONFIDENCE] ?: "65").toIntOrNull() ?: 65,
            minPotential = (p[Keys.MIN_POTENTIAL] ?: "0").toIntOrNull() ?: 0,
            favoritesOnly = (p[Keys.FAVORITES] ?: "false").toBoolean(),
            inPortfolioOnly = (p[Keys.PORTFOLIO] ?: "false").toBoolean(),
            activeForecastOnly = (p[Keys.FORECAST] ?: "false").toBoolean(),
            sellAlertOnly = (p[Keys.SELL_ALERT] ?: "false").toBoolean(),
            sortMode = runCatching { MarketSortMode.valueOf(p[Keys.SORT] ?: MarketSortMode.SCORE.name) }.getOrDefault(MarketSortMode.SCORE),
            lastScrollIndex = (p[Keys.SCROLL] ?: "0").toIntOrNull() ?: 0
        )
    }

    suspend fun save(filters: MarketFiltersState) {
        context.marketFiltersDataStore.edit { p ->
            p[Keys.QUERY] = filters.query
            p[Keys.QUICK] = filters.opportunityQuick.toString()
            p[Keys.SWING] = filters.swing.toString()
            p[Keys.LONG] = filters.longTerm.toString()
            p[Keys.VOLATILITY] = filters.volatility.toString()
            p[Keys.GROWTH] = filters.growth.toString()
            p[Keys.DEFENSIVE] = filters.defensive.toString()
            p[Keys.DIVIDEND] = filters.dividend.toString()
            p[Keys.MIN_SCORE] = filters.minScore.toString()
            p[Keys.MAX_RISK] = filters.maxRisk.toString()
            p[Keys.MIN_CONFIDENCE] = filters.minConfidence.toString()
            p[Keys.MIN_POTENTIAL] = filters.minPotential.toString()
            p[Keys.FAVORITES] = filters.favoritesOnly.toString()
            p[Keys.PORTFOLIO] = filters.inPortfolioOnly.toString()
            p[Keys.FORECAST] = filters.activeForecastOnly.toString()
            p[Keys.SELL_ALERT] = filters.sellAlertOnly.toString()
            p[Keys.SORT] = filters.sortMode.name
            p[Keys.SCROLL] = filters.lastScrollIndex.toString()
        }
    }

    suspend fun reset() = save(MarketFiltersState())

    suspend fun exportSnapshot(): String {
        val f = filters.first()
        return """
            {
              "query": "${escape(f.query)}",
              "quick": ${f.opportunityQuick},
              "swing": ${f.swing},
              "longTerm": ${f.longTerm},
              "volatility": ${f.volatility},
              "growth": ${f.growth},
              "defensive": ${f.defensive},
              "dividend": ${f.dividend},
              "minScore": ${f.minScore},
              "maxRisk": ${f.maxRisk},
              "minConfidence": ${f.minConfidence},
              "minPotential": ${f.minPotential},
              "favoritesOnly": ${f.favoritesOnly},
              "inPortfolioOnly": ${f.inPortfolioOnly},
              "activeForecastOnly": ${f.activeForecastOnly},
              "sellAlertOnly": ${f.sellAlertOnly},
              "sortMode": "${f.sortMode.name}",
              "lastScrollIndex": ${f.lastScrollIndex}
            }
        """.trimIndent()
    }

    // FIX V1.5 : échappement correct des backslash et guillemets dans les valeurs JSON
    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}

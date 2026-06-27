package com.privateinvest.aitraderpro.navigation

/**
 * Store léger utilisé comme SelectedAssetStore pour ouvrir une fiche de pronostic
 * sans refondre toute la navigation en arguments typés.
 */
object SelectedForecastStore {
    var currentForecastId: Long = 0L
}

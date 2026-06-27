package com.privateinvest.aitraderpro.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Login : AppDestination("login", "Connexion")
    data object Dashboard : AppDestination("dashboard", "Accueil")
    data object Watchlist : AppDestination("watchlist", "Watchlist")
    data object Alerts : AppDestination("alerts", "Alertes")
    data object Portfolio : AppDestination("portfolio", "Portefeuille")
    data object Audit : AppDestination("audit", "Audit IA")
    data object Admin : AppDestination("admin", "Paramètres")
    data object Risk : AppDestination("risk", "Risque")
    data object AssetDetail : AppDestination("asset_detail", "Actif")
    data object SignalAssistant : AppDestination("signal_assistant", "Pourquoi ce signal ?")
}

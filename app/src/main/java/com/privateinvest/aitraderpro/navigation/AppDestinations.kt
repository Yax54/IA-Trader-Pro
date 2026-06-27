package com.privateinvest.aitraderpro.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Login : AppDestination("login", "Connexion")
    data object Dashboard : AppDestination("dashboard", "Accueil")
    data object TopOpportunities : AppDestination("top_opportunities", "Opportunités")
    data object Watchlist : AppDestination("watchlist", "Watchlist")
    data object Alerts : AppDestination("alerts", "Alertes")
    data object Portfolio : AppDestination("portfolio", "Portefeuille")
    data object Audit : AppDestination("audit", "Audit IA")
    data object Admin : AppDestination("admin", "Paramètres")
    data object Risk : AppDestination("risk", "Risque")
    data object AssetDetail : AppDestination("asset_detail", "Actif")
    data object SignalAssistant : AppDestination("signal_assistant", "Pourquoi ce signal ?")
    data object More : AppDestination("more", "Plus")
    data object InstallTest : AppDestination("install_test", "Test complet")
    data object JournalIA : AppDestination("journal_ia", "Journal IA")
    data object CalendarIA : AppDestination("calendar_ia", "Calendrier IA")
    data object Favorites : AppDestination("favorites", "Favoris")
    data object Health : AppDestination("health", "Santé IA")
    data object Goals : AppDestination("goals", "Objectifs")
    data object Backup : AppDestination("backup", "Sauvegarde")
    data object Exports : AppDestination("exports", "Exports")
}

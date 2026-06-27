package com.privateinvest.aitraderpro.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.privateinvest.aitraderpro.ui.screens.AdminScreen
import com.privateinvest.aitraderpro.ui.screens.AlertsScreen
import com.privateinvest.aitraderpro.ui.screens.AssetDetailScreen
import com.privateinvest.aitraderpro.ui.screens.AuditScreen
import com.privateinvest.aitraderpro.ui.screens.BackupScreen
import com.privateinvest.aitraderpro.ui.screens.CalendarIAScreen
import com.privateinvest.aitraderpro.ui.screens.DashboardScreen
import com.privateinvest.aitraderpro.ui.screens.ExportCenterScreen
import com.privateinvest.aitraderpro.ui.screens.FavoritesScreen
import com.privateinvest.aitraderpro.ui.screens.GoalsScreen
import com.privateinvest.aitraderpro.ui.screens.HealthScreen
import com.privateinvest.aitraderpro.ui.screens.InstallTestScreen
import com.privateinvest.aitraderpro.ui.screens.JournalIAScreen
import com.privateinvest.aitraderpro.ui.screens.LoginScreen
import com.privateinvest.aitraderpro.ui.screens.MoreScreen
import com.privateinvest.aitraderpro.ui.screens.PortfolioScreen
import com.privateinvest.aitraderpro.ui.screens.RiskScreen
import com.privateinvest.aitraderpro.ui.screens.SignalAssistantScreen
import com.privateinvest.aitraderpro.ui.screens.TopOpportunitiesScreen
import com.privateinvest.aitraderpro.ui.screens.WatchlistScreen

@Composable
fun AITraderApp() {
    val navController = rememberNavController()
    val bottomItems = listOf(
        AppDestination.Dashboard,
        AppDestination.TopOpportunities,
        AppDestination.Portfolio,
        AppDestination.Alerts,
        AppDestination.More
    )

    fun openAsset(symbol: String, name: String) {
        SelectedAssetStore.currentSymbol = symbol
        SelectedAssetStore.currentName = name
        navController.navigate(AppDestination.AssetDetail.route)
    }

    Scaffold(
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            if (currentRoute != AppDestination.Login.route) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(iconFor(item), fontSize = 20.sp) },
                            label = { Text(item.label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Login.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppDestination.Login.route) {
                LoginScreen(onLogin = { navController.navigate(AppDestination.Dashboard.route) })
            }
            composable(AppDestination.Dashboard.route) {
                DashboardScreen(
                    onOpenRisk = { navController.navigate(AppDestination.Risk.route) },
                    onOpenAdmin = { navController.navigate(AppDestination.Admin.route) },
                    onOpenAsset = ::openAsset
                )
            }
            composable(AppDestination.TopOpportunities.route) { TopOpportunitiesScreen(onOpenAsset = ::openAsset) }
            composable(AppDestination.Watchlist.route) { WatchlistScreen(onOpenAsset = ::openAsset) }
            composable(AppDestination.Alerts.route) { AlertsScreen() }
            composable(AppDestination.Portfolio.route) { PortfolioScreen() }
            composable(AppDestination.Audit.route) { AuditScreen() }
            composable(AppDestination.Admin.route) { AdminScreen() }
            composable(AppDestination.Risk.route) { RiskScreen() }
            composable(AppDestination.AssetDetail.route) {
                AssetDetailScreen(onOpenAssistant = { navController.navigate(AppDestination.SignalAssistant.route) })
            }
            composable(AppDestination.SignalAssistant.route) { SignalAssistantScreen() }
            composable(AppDestination.More.route) { MoreScreen { route -> navController.navigate(route) } }
            composable(AppDestination.InstallTest.route) { InstallTestScreen() }
            composable(AppDestination.JournalIA.route) { JournalIAScreen() }
            composable(AppDestination.CalendarIA.route) { CalendarIAScreen() }
            composable(AppDestination.Favorites.route) { FavoritesScreen(onOpenAsset = ::openAsset) }
            composable(AppDestination.Health.route) { HealthScreen() }
            composable(AppDestination.Goals.route) { GoalsScreen() }
            composable(AppDestination.Backup.route) { BackupScreen() }
            composable(AppDestination.Exports.route) { ExportCenterScreen() }
        }
    }
}

private fun iconFor(destination: AppDestination): String = when (destination) {
    AppDestination.Dashboard -> "⌂"
    AppDestination.TopOpportunities -> "★"
    AppDestination.Portfolio -> "€"
    AppDestination.Alerts -> "!"
    AppDestination.More -> "+"
    else -> "•"
}

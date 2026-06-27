package com.privateinvest.aitraderpro.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.privateinvest.aitraderpro.ui.screens.BrokerAssistantScreen
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
import com.privateinvest.aitraderpro.ui.screens.SecurityCenterScreen
import com.privateinvest.aitraderpro.ui.screens.SignalAssistantScreen
import com.privateinvest.aitraderpro.ui.screens.TradingModeBanner
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.SecurityViewModel
import com.privateinvest.aitraderpro.ui.screens.TopOpportunitiesScreen
import com.privateinvest.aitraderpro.ui.screens.WatchlistScreen

@Composable
fun AITraderApp() {
    val navController = rememberNavController()
    val securityViewModel: SecurityViewModel = viewModel(factory = AITraderViewModelFactory)
    val securityUi = securityViewModel.uiState.collectAsStateWithLifecycle()
    val bottomItems = listOf(
        AppDestination.Dashboard,
        AppDestination.TopOpportunities,
        AppDestination.Portfolio,
        AppDestination.Alerts,
        AppDestination.More
    )

    // R-15 : passage du symbol+name via SelectedAssetStore (conservé — migration nav args hors scope build)
    fun openAsset(symbol: String, name: String) {
        SelectedAssetStore.currentSymbol = symbol
        SelectedAssetStore.currentName = name
        navController.navigate(AppDestination.AssetDetail.route)
    }

    fun openBroker(symbol: String, name: String) {
        SelectedAssetStore.currentSymbol = symbol
        SelectedAssetStore.currentName = name
        navController.navigate(AppDestination.BrokerAssistant.route)
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
                            // R-08 : icônes Material Icons au lieu de caractères Unicode
                            icon = { Icon(iconFor(item), contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (currentRoute != AppDestination.Login.route) {
                TradingModeBanner(mode = securityUi.value.state.tradingMode.name)
            }
            NavHost(
                navController = navController,
                startDestination = AppDestination.Login.route,
                modifier = Modifier.weight(1f)
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
            composable(AppDestination.TopOpportunities.route) {
                TopOpportunitiesScreen(
                    onOpenAsset = ::openAsset,
                    onOpenBroker = ::openBroker
                )
            }
            composable(AppDestination.Watchlist.route) { WatchlistScreen(onOpenAsset = ::openAsset) }
            composable(AppDestination.Alerts.route) { AlertsScreen() }
            composable(AppDestination.Portfolio.route) { PortfolioScreen() }
            composable(AppDestination.Audit.route) { AuditScreen() }
            composable(AppDestination.Admin.route) { AdminScreen() }
            composable(AppDestination.Risk.route) { RiskScreen() }
            composable(AppDestination.AssetDetail.route) {
                AssetDetailScreen(
                    onBack = { navController.popBackStack() },       // R-10
                    onOpenAssistant = { navController.navigate(AppDestination.SignalAssistant.route) }
                )
            }
            composable(AppDestination.SignalAssistant.route) {
                // R-11 : bouton retour branché
                SignalAssistantScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.More.route) { MoreScreen { route -> navController.navigate(route) } }
            composable(AppDestination.InstallTest.route) { InstallTestScreen() }
            composable(AppDestination.JournalIA.route) { JournalIAScreen() }
            composable(AppDestination.CalendarIA.route) { CalendarIAScreen() }
            composable(AppDestination.Favorites.route) { FavoritesScreen(onOpenAsset = ::openAsset) }
            composable(AppDestination.Health.route) { HealthScreen() }
            composable(AppDestination.Goals.route) { GoalsScreen() }
            composable(AppDestination.Backup.route) { BackupScreen() }
            composable(AppDestination.Exports.route) { ExportCenterScreen() }
            composable(AppDestination.SecurityCenter.route) { SecurityCenterScreen() }
            // MODULE BROKER
            composable(AppDestination.BrokerAssistant.route) {
                BrokerAssistantScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSecurity = { navController.navigate(AppDestination.SecurityCenter.route) }
                )
            }
        }
    }
}
}

// R-08 : icônes Material Icons
@Composable
private fun iconFor(destination: AppDestination) = when (destination) {
    AppDestination.Dashboard -> Icons.Filled.Home
    AppDestination.TopOpportunities -> Icons.Filled.Star
    AppDestination.Portfolio -> Icons.Filled.AccountBalance
    AppDestination.Alerts -> Icons.Filled.Notifications
    AppDestination.More -> Icons.Filled.Add
    else -> Icons.Filled.Home
}

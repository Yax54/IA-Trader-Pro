package com.privateinvest.aitraderpro.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.privateinvest.aitraderpro.ui.screens.AdminScreen
import com.privateinvest.aitraderpro.ui.screens.AlertsScreen
import com.privateinvest.aitraderpro.ui.screens.AssetDetailScreen
import com.privateinvest.aitraderpro.ui.screens.AuditScreen
import com.privateinvest.aitraderpro.ui.screens.DashboardScreen
import com.privateinvest.aitraderpro.ui.screens.LoginScreen
import com.privateinvest.aitraderpro.ui.screens.PortfolioScreen
import com.privateinvest.aitraderpro.ui.screens.RiskScreen
import com.privateinvest.aitraderpro.ui.screens.SignalAssistantScreen
import com.privateinvest.aitraderpro.ui.screens.WatchlistScreen

@Composable
fun AITraderApp() {
    val navController = rememberNavController()
    val bottomItems = listOf(
        AppDestination.Dashboard,
        AppDestination.Watchlist,
        AppDestination.Alerts,
        AppDestination.Portfolio,
        AppDestination.Audit
    )

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
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(item.label.take(1)) },
                            label = { Text(item.label) }
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
                    onOpenAsset = { symbol, name ->
                        SelectedAssetStore.currentSymbol = symbol
                        SelectedAssetStore.currentName = name
                        navController.navigate(AppDestination.AssetDetail.route)
                    }
                )
            }
            composable(AppDestination.Watchlist.route) {
                WatchlistScreen(onOpenAsset = { symbol, name ->
                    SelectedAssetStore.currentSymbol = symbol
                    SelectedAssetStore.currentName = name
                    navController.navigate(AppDestination.AssetDetail.route)
                })
            }
            composable(AppDestination.Alerts.route) { AlertsScreen() }
            composable(AppDestination.Portfolio.route) { PortfolioScreen() }
            composable(AppDestination.Audit.route) { AuditScreen() }
            composable(AppDestination.Admin.route) { AdminScreen() }
            composable(AppDestination.Risk.route) { RiskScreen() }
            composable(AppDestination.AssetDetail.route) {
                AssetDetailScreen(
                    onOpenAssistant = { navController.navigate(AppDestination.SignalAssistant.route) }
                )
            }
            composable(AppDestination.SignalAssistant.route) { SignalAssistantScreen() }
        }
    }
}

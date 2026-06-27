package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.repository.MarketFeedHealth
import com.privateinvest.aitraderpro.repository.MarketFeedSource
import com.privateinvest.aitraderpro.repository.MarketFeedStatus
import com.privateinvest.aitraderpro.repository.MarketPriceComparison
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.MarketDataCenterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDataCenterScreen(onBack: () -> Unit) {
    val viewModel: MarketDataCenterViewModel = viewModel(factory = AITraderViewModelFactory)
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Données marché", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { PremiumScreenTitle("Centre Données Marché", "Vérifie que simulation, pronostics et réel utilisent des cours réels.") }
            item {
                PremiumCardBox("Symbole à contrôler", "Par défaut, l'actif actuellement sélectionné est utilisé.") {
                    OutlinedTextField(
                        value = ui.symbol,
                        onValueChange = viewModel::setSymbol,
                        label = { Text("Symbole") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PremiumActionButton("Rafraîchir les flux", onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
            ui.error?.let { error -> item { PremiumCardBox("Erreur", error, accent = DangerRed) { Text(error, color = Color.White, fontSize = 16.sp) } } }
            if (ui.loading) {
                item { PremiumCardBox("Contrôle en cours", "Connexion aux sources de données...") { Text("Analyse des flux Alpha Vantage et Broker.", color = Color.LightGray, fontSize = 16.sp) } }
            }
            ui.state?.let { state ->
                item { SourceSummaryCard(state.primarySource.name, state.usableForSimulation, state.usableForForecasts, state.usableForRealOrders) }
                item { FeedStatusCard("Alpha Vantage", state.alpha) }
                item { FeedStatusCard("Broker", state.broker) }
                item { PriceComparisonCard(state.comparison) }
                state.globalWarning?.let { warning -> item { PremiumCardBox("Alerte données", warning, accent = Warning) { Text(warning, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp) } } }
                item {
                    SafetyBanner(
                        "Simulation, Pronostics IA et Suivi intelligent doivent toujours s'appuyer sur des données réelles. Le broker devient prioritaire quand il fournit un prix exploitable."
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceSummaryCard(primary: String, simulation: Boolean, forecasts: Boolean, realOrders: Boolean) {
    PremiumCardBox("Source utilisée", "Priorité : Broker si disponible, sinon Alpha Vantage, sinon aucune décision fiable.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumMetric("Principale", primary, modifier = Modifier.weight(1f), accent = PremiumBlue)
            PremiumMetric("Simulation", if (simulation) "OK" else "NON", modifier = Modifier.weight(1f), accent = if (simulation) com.privateinvest.aitraderpro.ui.theme.Success else DangerRed)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumMetric("Pronostics", if (forecasts) "OK" else "NON", modifier = Modifier.weight(1f), accent = if (forecasts) com.privateinvest.aitraderpro.ui.theme.Success else DangerRed)
            PremiumMetric("Ordres réels", if (realOrders) "OK" else "À connecter", modifier = Modifier.weight(1f), accent = if (realOrders) com.privateinvest.aitraderpro.ui.theme.Success else Warning)
        }
    }
}

@Composable
private fun FeedStatusCard(title: String, status: MarketFeedStatus) {
    val accent = when (status.health) {
        MarketFeedHealth.OK -> com.privateinvest.aitraderpro.ui.theme.Success
        MarketFeedHealth.ATTENTION -> Warning
        MarketFeedHealth.ERREUR -> DangerRed
        MarketFeedHealth.NON_CONFIGURE -> PremiumMuted
    }
    PremiumCardBox(title, status.label, accent = accent) {
        SignalBadge(status.health.name)
        Text(status.detail, color = Color.LightGray, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp))
        RowLine("Dernière synchro", status.lastSyncLabel)
        RowLine("Prix", status.price?.let { String.format("%.2f", it) } ?: "Non disponible")
        RowLine("Source", status.source.name)
    }
}

@Composable
private fun PriceComparisonCard(comparison: MarketPriceComparison) {
    val accent = when {
        comparison.warning != null -> Warning
        comparison.differencePercent != null -> com.privateinvest.aitraderpro.ui.theme.Success
        else -> PremiumMuted
    }
    PremiumCardBox("Comparaison prix", comparison.statusLabel, accent = accent) {
        RowLine("Symbole", comparison.symbol)
        RowLine("Alpha Vantage", comparison.alphaPrice?.let { String.format("%.2f", it) } ?: "Non disponible")
        RowLine("Broker", comparison.brokerPrice?.let { String.format("%.2f", it) } ?: "Non disponible")
        RowLine("Écart", comparison.differencePercent?.let { String.format("%.2f %%", it) } ?: "Non calculable")
        comparison.warning?.let { Text(it, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp)) }
    }
}

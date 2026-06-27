package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.PortfolioViewModel

@Composable
fun PortfolioScreen() {
    val viewModel: PortfolioViewModel = viewModel(factory = AITraderViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var symbolToSell by remember { mutableStateOf<String?>(null) }

    symbolToSell?.let { sym ->
        AlertDialog(
            onDismissRequest = { symbolToSell = null },
            title = { Text("Confirmer la vente") },
            text = { Text("Clôturer la position simulée sur $sym ?\nCette action est irréversible.") },
            confirmButton = { Button(onClick = { viewModel.sellPosition(sym); symbolToSell = null }) { Text("Vendre") } },
            dismissButton = { TextButton(onClick = { symbolToSell = null }) { Text("Annuler") } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Portefeuille", "Mode test IA avec capital virtuel") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumMetric("Capital", state.summary.initialCapital, Modifier.weight(1f), PremiumBlue)
                PremiumMetric("Cash", state.summary.availableCash, Modifier.weight(1f), Success)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumMetric("Investi", state.summary.investedCapital, Modifier.weight(1f), Warning)
                PremiumMetric("Valeur", state.summary.totalValue, Modifier.weight(1f), PremiumBlue)
            }
        }
        item {
            val positive = !state.summary.totalPerformance.trimStart().startsWith("-")
            PremiumCardBox("Performance simulation", "Suivi J+1 / J+7 / J+30", accent = if (positive) Success else DangerRed) {
                SignalBadge(state.summary.totalPerformance)
                RowLine("Positions ouvertes", state.summary.openPositions.toString())
                RowLine("Réussite J+1", state.summary.successRateJ1)
                RowLine("Réussite J+7", state.summary.successRateJ7)
                RowLine("Réussite J+30", state.summary.successRateJ30)
            }
        }
        item { PremiumScreenTitle("Positions simulées") }
        if (state.positions.isEmpty()) {
            item { PremiumCardBox("Aucune position", "Teste un achat depuis une fiche actif ou une alerte") { Text("Aucune position simulée ouverte pour le moment.", color = Color.LightGray, fontSize = 16.sp) } }
        }
        items(state.positions) { position ->
            val positive = !position.performance.trimStart().startsWith("-")
            PremiumCardBox("${position.symbol} · ${position.performance}", "Position ouverte", accent = if (positive) Success else DangerRed) {
                RowLine("Quantité", String.format("%.0f", position.quantity))
                RowLine("Prix moyen", position.averagePrice)
                RowLine("Prix marché", position.marketPrice)
                RowLine("Valeur", position.marketValue)
                PremiumActionButton("Vendre en simulation", { symbolToSell = position.symbol }, Modifier.fillMaxWidth(), danger = true)
            }
        }
        item { PremiumScreenTitle("Historique simulation") }
        if (state.history.isEmpty()) {
            item { PremiumCardBox("Aucun historique") { Text("Aucun achat / vente simulé enregistré.", color = Color.LightGray) } }
        }
        items(state.history) { trade ->
            PremiumCardBox("${trade.symbol} · ${trade.side}", trade.followUp, accent = if (trade.side.contains("SELL", true)) DangerRed else Success) {
                RowLine("Date", trade.createdAtLabel)
                RowLine("Quantité", String.format("%.0f", trade.quantity))
                RowLine("Prix", trade.price)
                Text("Suivi : ${trade.followUp}", color = Warning, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        state.statusMessage?.let { message -> item { PremiumCardBox("Statut", message) { } } }
        item { SafetyBanner("Simulation uniquement : aucun ordre réel n'est envoyé depuis cet écran.") }
    }
}

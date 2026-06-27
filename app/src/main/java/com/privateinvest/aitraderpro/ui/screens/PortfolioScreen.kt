package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.PortfolioViewModel

@Composable
fun PortfolioScreen() {
    val viewModel: PortfolioViewModel = viewModel(factory = AITraderViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Mode Test IA · Portefeuille virtuel") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCard("Capital initial", state.summary.initialCapital, Modifier.weight(1f))
                InfoCard("Cash dispo", state.summary.availableCash, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCard("Investi", state.summary.investedCapital, Modifier.weight(1f))
                InfoCard("Valeur totale", state.summary.totalValue, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Performance simulation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        state.summary.totalPerformance,
                        modifier = Modifier
                            .background(Success, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White
                    )
                    RowLine("Positions ouvertes", state.summary.openPositions.toString())
                    RowLine("Réussite J+1", state.summary.successRateJ1)
                    RowLine("Réussite J+7", state.summary.successRateJ7)
                    RowLine("Réussite J+30", state.summary.successRateJ30)
                }
            }
        }

        item { SectionTitle("Positions simulées") }
        if (state.positions.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Slate)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Aucune position simulée ouverte pour le moment.")
                    }
                }
            }
        }
        items(state.positions) { position ->
            Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(position.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    RowLine("Quantité", String.format("%.0f", position.quantity))
                    RowLine("Prix moyen", position.averagePrice)
                    RowLine("Prix marché", position.marketPrice)
                    RowLine("Valeur", position.marketValue)
                    RowLine("Performance", position.performance)
                    Button(onClick = { viewModel.sellPosition(position.symbol) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Vendre en simulation")
                    }
                }
            }
        }

        item { SectionTitle("Historique simulation") }
        if (state.history.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Slate)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Aucun achat / vente simulé enregistré.")
                    }
                }
            }
        }
        items(state.history) { trade ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${trade.symbol} · ${trade.side}", color = SoftWhite, fontWeight = FontWeight.Bold)
                    RowLine("Date", trade.createdAtLabel)
                    RowLine("Quantité", String.format("%.0f", trade.quantity))
                    RowLine("Prix", trade.price)
                    Text("Suivi : ${trade.followUp}", color = Warning)
                }
            }
        }

        state.statusMessage?.let { message ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Slate)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(message)
                    }
                }
            }
        }
    }
}

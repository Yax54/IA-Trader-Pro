package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.DashboardViewModel
import com.privateinvest.aitraderpro.viewmodel.SettingsViewModel

@Composable
fun DashboardScreen(
    onOpenRisk: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenAsset: (String, String) -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(factory = AITraderViewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AITraderViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val beginnerModeEnabled by settingsViewModel.beginnerModeEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Tableau de bord") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCard("Capital", state.summary.capital, Modifier.weight(1f))
                InfoCard("Aujourd'hui", state.summary.dayPnL, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCard("Mois", state.summary.monthPnL, Modifier.weight(1f))
                InfoCard("Risque", state.summary.riskLevel, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Actions rapides", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (beginnerModeEnabled) "Mode actuel : Débutant" else "Mode actuel : Expert",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(if (beginnerModeEnabled) Success else Warning, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Button(onClick = onOpenRisk, modifier = Modifier.weight(1f)) { Text("Risque") }
                        Button(onClick = onOpenAdmin, modifier = Modifier.weight(1f)) { Text("Paramètres") }
                    }
                    state.error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }
        item { SectionTitle("Opportunités du moment") }
        if (state.signals.isEmpty()) {
            item { Text("Aucune donnée disponible. Ajoute une clé API Alpha Vantage pour alimenter l'écran.") }
        }
        items(state.signals) { signal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAsset(signal.symbol, signal.name) },
                colors = CardDefaults.cardColors(containerColor = Slate),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("${signal.symbol} · ${signal.name}", style = MaterialTheme.typography.titleMedium)
                    RowLine("Score", "${signal.score}/100")
                    RowLine("Confiance", "${signal.confidence}%")
                    RowLine("Objectif", signal.target)
                    RowLine("Risque", signal.risk)
                    RowLine("Décision", signal.action.name)
                }
            }
        }
    }
}

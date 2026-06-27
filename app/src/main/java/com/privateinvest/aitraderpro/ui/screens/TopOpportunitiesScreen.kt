package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.viewmodel.TopOpportunitiesViewModel

@Composable
fun TopOpportunitiesScreen(onOpenAsset: (String, String) -> Unit) {
    val viewModel: TopOpportunitiesViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Top Opportunités", "Les meilleurs signaux classés par score et confiance") }
        if (state.items.isEmpty()) {
            item { PremiumCardBox("Aucune opportunité", "Ajoute une clé API ou rafraîchis les signaux") { Text(state.error ?: "Pas encore de données disponibles.", fontSize = 16.sp) } }
        }
        items(state.items) { signal ->
            PremiumCardBox(
                title = "${signal.symbol} · ${signal.name}",
                subtitle = signal.explanation.firstOrNull() ?: "Analyse IA disponible",
                accent = scoreColor(signal.score),
                modifier = Modifier.clickable { onOpenAsset(signal.symbol, signal.name) }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SignalBadge(signal.action.name)
                    Text("Score ${signal.score}/100", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                ConfidenceBar("Confiance IA", signal.confidence.clampPercent(), Modifier.padding(top = 12.dp))
                RowLine("Risque", signal.risk)
                RowLine("Objectif / prix", signal.target)
                PremiumActionButton("Voir la fiche", onClick = { onOpenAsset(signal.symbol, signal.name) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
    }
}

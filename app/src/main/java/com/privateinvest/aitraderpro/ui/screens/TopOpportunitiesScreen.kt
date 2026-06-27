package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.viewmodel.TopOpportunitiesViewModel

@Composable
fun TopOpportunitiesScreen(
    onOpenAsset: (String, String) -> Unit,
    onOpenBroker: (String, String) -> Unit = { _, _ -> }  // Point d'entrée broker depuis les opportunités
) {
    val viewModel: TopOpportunitiesViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { PremiumScreenTitle("Top Opportunités", "Les meilleurs signaux classés par score et confiance") }

        if (state.items.isEmpty()) {
            item {
                PremiumCardBox("Aucune opportunité", "Ajoute une clé API ou rafraîchis les signaux") {
                    Text(state.error ?: "Pas encore de données disponibles.", fontSize = 16.sp, color = Color.LightGray)
                }
            }
        }

        items(state.items) { signal ->
            // R-16 : la carte entière est cliquable → suppression du bouton "Voir la fiche" redondant
            // Le bouton Broker est ajouté à la place pour action directe
            PremiumCardBox(
                title = "${signal.symbol} · ${signal.name}",
                subtitle = signal.explanation.firstOrNull() ?: "Analyse IA disponible",
                accent = scoreColor(signal.score),
                modifier = Modifier.clickable { onOpenAsset(signal.symbol, signal.name) }
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignalBadge(signal.action.name)
                    Text("Score ${signal.score}/100", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                ConfidenceBar("Confiance IA", signal.confidence.clampPercent(), Modifier.padding(top = 12.dp))
                RowLine("Risque", signal.risk)
                RowLine("Objectif / prix", signal.target)
                Spacer(Modifier.height(10.dp))
                // Bouton broker (action unique — plus de "Voir la fiche" redondant avec le tap carte)
                PremiumActionButton(
                    text = "Assistant d'investissement",
                    onClick = { onOpenBroker(signal.symbol, signal.name) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

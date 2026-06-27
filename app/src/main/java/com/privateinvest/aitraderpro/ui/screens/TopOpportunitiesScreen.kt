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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.repository.DataFreshnessGuard
import com.privateinvest.aitraderpro.viewmodel.TopOpportunitiesViewModel

private enum class OpportunitySort { SCORE, CONFIANCE, RISQUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopOpportunitiesScreen(
    onOpenAsset: (String, String) -> Unit,
    onOpenBroker: (String, String) -> Unit = { _, _ -> }
) {
    val viewModel: TopOpportunitiesViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sort by remember { mutableStateOf(OpportunitySort.SCORE) }
    val sorted = remember(state.items, sort) {
        when (sort) {
            OpportunitySort.SCORE -> state.items.sortedByDescending { it.score }
            OpportunitySort.CONFIANCE -> state.items.sortedByDescending { it.confidence }
            OpportunitySort.RISQUE -> state.items.sortedBy { riskRank(it.risk) }
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // V1.3 : bannière données marché si aucune source réelle disponible
        item { DataQualityBanner(message = DataFreshnessGuard.globalBannerMessage()) }
        item { PremiumScreenTitle("Top Opportunités", "Tri par score, confiance ou risque") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip("Score", sort == OpportunitySort.SCORE) { sort = OpportunitySort.SCORE }
                SortChip("Confiance", sort == OpportunitySort.CONFIANCE) { sort = OpportunitySort.CONFIANCE }
                SortChip("Risque faible", sort == OpportunitySort.RISQUE) { sort = OpportunitySort.RISQUE }
            }
        }
        if (sorted.isEmpty()) {
            item { PremiumCardBox("Aucune opportunité", "Ajoute une clé API ou rafraîchis les signaux") { Text(state.error ?: "Pas encore de données disponibles.", fontSize = 16.sp, color = Color.LightGray) } }
        }
        items(sorted) { signal ->
            PremiumCardBox(
                title = "${signal.symbol} · ${signal.name}",
                subtitle = signal.explanation.firstOrNull() ?: "Analyse IA disponible",
                accent = scoreColor(signal.score),
                modifier = Modifier.clickable { onOpenAsset(signal.symbol, signal.name) }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SignalBadge(signal.action.name)
                    Text("Score ${signal.score}/100", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                ConfidenceBar("Confiance IA", signal.confidence.clampPercent(), Modifier.padding(top = 12.dp))
                RowLine("Risque", signal.risk)
                RowLine("Objectif / prix", signal.target)
                Spacer(Modifier.height(10.dp))
                PremiumActionButton("Assistant d'investissement", { onOpenBroker(signal.symbol, signal.name) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 15.sp) })
}

private fun riskRank(risk: String): Int = when {
    risk.contains("faible", true) -> 0
    risk.contains("mod", true) -> 1
    risk.contains("élev", true) || risk.contains("eleve", true) -> 3
    else -> 2
}

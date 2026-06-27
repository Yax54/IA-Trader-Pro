package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.privateinvest.aitraderpro.repository.MarketSortMode
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.MarketPlaceViewModel

@Composable
fun MarketPlaceScreen(
    onBack: () -> Unit,
    onOpenAsset: (String, String) -> Unit,
    onOpenBroker: (String, String) -> Unit
) {
    val viewModel: MarketPlaceViewModel = viewModel(factory = AITraderViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val f = state.filters

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumScreenTitle("Place du marché", "Recherche, filtres et tri sauvegardés automatiquement")
            PremiumSecondaryButton("Retour", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }

        item {
            PremiumCardBox("Recherche", "Nom, symbole ou ISIN si disponible") {
                OutlinedTextField(
                    value = f.query,
                    onValueChange = { q -> viewModel.updateFilters { it.copy(query = q) } },
                    label = { Text("Rechercher une action") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${state.filteredItems.size} résultat(s) sur ${state.allItems.size} signaux disponibles", color = Color.LightGray, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }

        item {
            PremiumCardBox("Filtres stratégies", "Affiche seulement les catégories utiles") {
                FilterSwitch("⚡ Opportunité rapide", f.opportunityQuick) { viewModel.updateFilters { it.copy(opportunityQuick = it.opportunityQuick.not()) } }
                FilterSwitch("📈 Swing", f.swing) { viewModel.updateFilters { it.copy(swing = it.swing.not()) } }
                FilterSwitch("🏆 Long terme", f.longTerm) { viewModel.updateFilters { it.copy(longTerm = it.longTerm.not()) } }
                FilterSwitch("🔥 Forte volatilité", f.volatility) { viewModel.updateFilters { it.copy(volatility = it.volatility.not()) } }
                FilterSwitch("🌱 Croissance", f.growth) { viewModel.updateFilters { it.copy(growth = it.growth.not()) } }
                FilterSwitch("🛡️ Défensif", f.defensive) { viewModel.updateFilters { it.copy(defensive = it.defensive.not()) } }
                FilterSwitch("💰 Dividendes", f.dividend) { viewModel.updateFilters { it.copy(dividend = it.dividend.not()) } }
            }
        }

        item {
            PremiumCardBox("Filtres IA", "Garde seulement les signaux assez solides") {
                SliderLine("Score mini", f.minScore, 0..100) { v -> viewModel.updateFilters { it.copy(minScore = v) } }
                SliderLine("Risque max", f.maxRisk, 0..100) { v -> viewModel.updateFilters { it.copy(maxRisk = v) } }
                SliderLine("Confiance mini", f.minConfidence, 0..100) { v -> viewModel.updateFilters { it.copy(minConfidence = v) } }
            }
        }

        item {
            PremiumCardBox("Tri", "L’état est conservé quand tu quittes l’écran") {
                MarketSortMode.values().forEach { mode ->
                    PremiumSecondaryButton(
                        text = if (mode == f.sortMode) "✓ ${mode.label}" else mode.label,
                        onClick = { viewModel.updateFilters { it.copy(sortMode = mode) } },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                PremiumActionButton("Réinitialiser les filtres", onClick = { viewModel.resetFilters() }, modifier = Modifier.fillMaxWidth())
            }
        }

        state.error?.let { item { SafetyBanner(it) } }

        items(state.filteredItems) { signal ->
            PremiumCardBox(title = "${signal.symbol} · ${signal.name}", subtitle = "${signal.action.name} · ${signal.target}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SignalBadge(signal.action.name)
                    Text("Score ${signal.score}/100", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                ConfidenceBar("Confiance IA", signal.confidence, modifier = Modifier.padding(top = 10.dp))
                RowLine("Risque", signal.risk)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSecondaryButton("Fiche", onClick = { onOpenAsset(signal.symbol, signal.name) }, modifier = Modifier.weight(1f))
                    PremiumActionButton("Broker", onClick = { onOpenBroker(signal.symbol, signal.name) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FilterSwitch(label: String, checked: Boolean, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Switch(checked = checked, onCheckedChange = { onChange() })
    }
}

@Composable
private fun SliderLine(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.LightGray, fontSize = 16.sp)
            Text("$value", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = range.first.toFloat()..range.last.toFloat())
    }
}

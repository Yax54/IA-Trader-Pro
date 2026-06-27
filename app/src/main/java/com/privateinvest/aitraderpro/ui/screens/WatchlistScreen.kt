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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(onOpenAsset: (String, String) -> Unit) {
    val viewModel: WatchlistViewModel = viewModel(factory = AITraderViewModelFactory)
    val signals by viewModel.items.collectAsStateWithLifecycle()
    // C1 : barre de recherche actifs
    var searchQuery by remember { mutableStateOf("") }

    // A2 : Refresh disponible
        // C1 : filtrage local selon la recherche
        val filteredSignals = remember(signals, searchQuery) {
            if (searchQuery.isBlank()) signals
            else signals.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PremiumScreenTitle(
                    title = "Watchlist",
                    subtitle = "${filteredSignals.size} actif${if (filteredSignals.size > 1) "s" else ""} surveillé${if (filteredSignals.size > 1) "s" else ""}"
                )
                // C1 : champ de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Rechercher un actif") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Recherche") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (filteredSignals.isEmpty()) {
                item {
                    PremiumCardBox(title = if (searchQuery.isBlank()) "Watchlist vide" else "Aucun résultat") {
                        Text(
                            if (searchQuery.isBlank()) "Aucun actif à surveiller. Configure une clé API Alpha Vantage pour alimenter la watchlist."
                            else "Aucun actif ne correspond à \"$searchQuery\".",
                            color = Color.LightGray
                        )
                    }
                }
            }

            // R-05 + R-06 : cartes enrichies avec PremiumCardBox + SignalBadge + ConfidenceBar
            items(filteredSignals) { signal ->
                PremiumCardBox(
                    title = signal.symbol,
                    subtitle = signal.name,
                    modifier = Modifier.clickable { onOpenAsset(signal.symbol, signal.name) }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SignalBadge(signal.action.name)
                        Text(
                            "Score ${signal.score}/100",
                            color = SoftWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ConfidenceBar("Confiance IA", signal.confidence)
                    Spacer(Modifier.height(6.dp))
                    RowLine("Objectif", signal.target)
                    RowLine("Risque", signal.risk)
                }
            }
        }
}

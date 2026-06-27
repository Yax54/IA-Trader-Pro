package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(onOpenAsset: (String, String) -> Unit) {
    val viewModel: WatchlistViewModel = viewModel(factory = AITraderViewModelFactory)
    val signals by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    // R-12 : état de chargement
    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PremiumBlue)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)  // R-06 : espacement
    ) {
        item {
            PremiumScreenTitle(
                title = "Watchlist",
                subtitle = "${signals.size} actif${if (signals.size > 1) "s" else ""} surveillé${if (signals.size > 1) "s" else ""}"
            )
        }

        if (signals.isEmpty()) {
            item {
                PremiumCardBox(title = "Watchlist vide") {
                    Text(
                        "Aucun actif à surveiller. Configure une clé API Alpha Vantage pour alimenter la watchlist.",
                        color = Color.LightGray
                    )
                }
            }
        }

        // R-05 + R-06 : cartes enrichies avec PremiumCardBox + SignalBadge + ConfidenceBar
        items(signals) { signal ->
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

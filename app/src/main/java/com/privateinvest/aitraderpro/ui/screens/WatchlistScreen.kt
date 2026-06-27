package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(onOpenAsset: (String, String) -> Unit) {
    val viewModel: WatchlistViewModel = viewModel(factory = AITraderViewModelFactory)
    val signals by viewModel.items.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { SectionTitle("Watchlist") }
        if (signals.isEmpty()) item { Text("Watchlist vide ou clé API non configurée.") }
        items(signals) { signal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onOpenAsset(signal.symbol, signal.name) },
                colors = CardDefaults.cardColors(containerColor = Slate)
            ) {
                androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                    Text("${signal.symbol} · ${signal.name}")
                    Text("Score ${signal.score}/100 · Confiance ${signal.confidence}%")
                }
            }
        }
    }
}

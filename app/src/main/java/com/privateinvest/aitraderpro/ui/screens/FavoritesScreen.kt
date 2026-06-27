package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(onOpenAsset: (String, String) -> Unit) {
    val viewModel: FavoritesViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PremiumScreenTitle("Favoris", "Tes actions prioritaires et alertes importantes") }
        item {
            PremiumCardBox("Ajout rapide", "Exemples utiles pour démarrer") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumSecondaryButton("NVDA", { viewModel.addExampleFavorite("NVDA", "NVIDIA") }, Modifier.weight(1f))
                    PremiumSecondaryButton("MSFT", { viewModel.addExampleFavorite("MSFT", "Microsoft") }, Modifier.weight(1f))
                    PremiumSecondaryButton("AAPL", { viewModel.addExampleFavorite("AAPL", "Apple") }, Modifier.weight(1f))
                }
                state.message?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        if (state.items.isEmpty()) item { PremiumCardBox("Aucun favori", "Ajoute tes actifs importants") { Text("Les favoris permettent de filtrer plus facilement les signaux.") } }
        items(state.items) { fav ->
            PremiumCardBox(fav.symbol, fav.name) {
                PremiumActionButton("Ouvrir la fiche", { onOpenAsset(fav.symbol, fav.name) }, Modifier.fillMaxWidth())
            }
        }
    }
}

package com.privateinvest.aitraderpro.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.viewmodel.JournalViewModel

@Composable
fun JournalIAScreen() {
    val viewModel: JournalViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PremiumScreenTitle("Journal IA", "Historique complet des decisions, alertes et simulations") }
        // C2 : stub explicite avec etat chargement et message pedagogique
        if (state.loading) {
            item { PremiumCardBox("Chargement...", "Recuperation du journal IA") {
                Text("Le journal se charge...", fontSize = 16.sp, color = Color.LightGray)
            } }
        } else if (state.items.isEmpty()) {
            item { PremiumCardBox("Journal vide", "Les actions de l'IA apparaitront ici") {
                Text(state.error ?: "Aucune decision enregistree. Validez des signaux en simulation pour alimenter le journal IA.", fontSize = 16.sp, color = Color.LightGray)
                Spacer(Modifier.height(10.dp))
                Text("Le journal enregistre chaque achat simule, alerte et validation.", fontSize = 14.sp, color = Color.Gray)
            } }
        }
        items(state.items) { item ->
            PremiumCardBox(title = item.symbol, subtitle = item.dateLabel, accent = scoreColor(item.score)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SignalBadge(item.decision)
                    Text(if (item.score > 0) "Score ${item.score}" else "Info", fontSize = 16.sp)
                }
                Text(item.detail, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

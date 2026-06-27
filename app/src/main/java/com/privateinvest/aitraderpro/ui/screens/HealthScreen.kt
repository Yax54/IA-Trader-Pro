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
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.HealthViewModel

@Composable
fun HealthScreen() {
    val viewModel: HealthViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Santé IA", "Diagnostic lisible de l'application") }
        item { SafetyBanner("Contrôle rapide : API, Room, mémoire, simulation et notifications.") }
        items(state.checks) { check ->
            PremiumCardBox(check.title, check.detail, accent = if (check.status == "OK") scoreColor(80) else Warning) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("État")
                    SignalBadge(check.status)
                }
            }
        }
        state.chartBundle?.let { charts ->
            item { SimpleLineChart("Capital virtuel", charts.capitalCurve, accent = PremiumBlue) }
            item { SimpleLineChart("Win rate IA", charts.winRateCurve, accent = scoreColor(75)) }
            item { SimpleLineChart("Poids IA", charts.weightCurve, accent = Warning) }
        }
        item {
            PremiumSecondaryButton("Relancer le diagnostic", onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

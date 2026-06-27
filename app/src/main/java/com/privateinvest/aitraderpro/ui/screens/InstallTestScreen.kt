package com.privateinvest.aitraderpro.ui.screens

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
import com.privateinvest.aitraderpro.viewmodel.RobustnessViewModel

@Composable
fun InstallTestScreen() {
    val viewModel: RobustnessViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = state.summary

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Test complet", "Checklist avant utilisation quotidienne") }
        item { SafetyBanner() }

        if (summary == null) {
            item { PremiumCardBox("Chargement", "Diagnostic en cours") { Text(state.error ?: "Analyse de l'application...") } }
        } else {
            item {
                PremiumCardBox("Statut global", "Diagnostic robuste de l'installation", accent = if (summary.readyForDailyTest) scoreColor(85) else scoreColor(55)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("État", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        SignalBadge(summary.globalStatus)
                    }
                    Text(
                        "Objectif : tester l'application en simulation plusieurs jours avant toute décision réelle.",
                        modifier = Modifier.padding(top = 10.dp),
                        fontSize = 16.sp
                    )
                }
            }

            items(summary.checks) { check ->
                PremiumCardBox(check.title, check.detail, accent = when (check.status) { "OK", "Verrouillé" -> scoreColor(85); "Erreur" -> scoreColor(20); else -> scoreColor(55) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Priorité : ${check.priority}", fontSize = 15.sp)
                        }
                        SignalBadge(check.status)
                    }
                }
            }

            item {
                PremiumCardBox("Premiers tests à faire", "À suivre dans cet ordre", accent = PremiumBlue) {
                    summary.nextActions.forEachIndexed { index, action ->
                        Text("${index + 1}. $action", fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    PremiumSecondaryButton("Relancer le diagnostic", onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }

        state.chartBundle?.let { charts ->
            item { SimpleLineChart("Courbe capital virtuel", charts.capitalCurve, accent = PremiumBlue) }
            item { SimpleLineChart("Évolution win rate", charts.winRateCurve, accent = scoreColor(75)) }
        }
    }
}

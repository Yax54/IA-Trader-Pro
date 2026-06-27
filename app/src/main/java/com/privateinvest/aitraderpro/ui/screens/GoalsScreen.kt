package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.viewmodel.GoalViewModel

@Composable
fun GoalsScreen() {
    val viewModel: GoalViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = state.summary
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PremiumScreenTitle("Objectifs", "Suivi simple de ton objectif portefeuille") }
        if (summary != null) {
            item {
                PremiumCardBox("Objectif annuel", summary.label, accent = scoreColor(summary.progressPercent)) {
                    PremiumMetric("Objectif", "${summary.annualTargetPercent.formatPlainPercent()}", Modifier.fillMaxWidth())
                    ConfidenceBar("Progression", summary.progressPercent, Modifier.padding(top = 12.dp))
                    RowLine("Performance actuelle", summary.currentPerformancePercent.formatPercent())
                    PremiumActionButton("Objectif prudent 10 %", { viewModel.setGoal(10.0) }, Modifier.fillMaxWidth().padding(top = 10.dp))
                    PremiumSecondaryButton("Objectif dynamique 20 %", { viewModel.setGoal(20.0) }, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        } else item { Text("Chargement de l’objectif...") }
    }
}

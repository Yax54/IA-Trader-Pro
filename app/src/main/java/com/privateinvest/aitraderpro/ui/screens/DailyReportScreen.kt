package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.viewmodel.DailyReportViewModel

@Composable
fun DailyReportScreen() {
    val viewModel: DailyReportViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Rapport quotidien", "Résumé simple pour tester l'application chaque jour") }
        state.report?.let { report ->
            item {
                PremiumCardBox(report.title, "Lecture rapide") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumMetric("Signaux", report.signalCount.toString(), Modifier.weight(1f))
                        PremiumMetric("Trades sim.", report.simulatedTrades.toString(), Modifier.weight(1f), Success)
                    }
                    RowLine("Performance", report.globalPerformance)
                    RowLine("Taux réussite", report.winRate)
                    RowLine("Meilleur signal", report.bestSignal)
                    RowLine("Signal faible", report.worstSignal)
                }
            }
            item { PremiumCardBox("Conseil du jour", report.advice, accent = Warning) { Text("Continue à tester en simulation avant toute décision réelle.", color = Color.LightGray, fontSize = 16.sp) } }
        }
        state.error?.let { item { PremiumCardBox("Erreur", it, accent = DangerRed) { } } }
    }
}

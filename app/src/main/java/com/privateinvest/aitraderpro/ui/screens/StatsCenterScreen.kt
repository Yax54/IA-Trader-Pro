package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.repository.StatsLine
import com.privateinvest.aitraderpro.viewmodel.StatsCenterViewModel

@Composable
fun StatsCenterScreen() {
    val viewModel: StatsCenterViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Centre statistiques", "Vue complète des performances IA, simulation et stratégies") }
        if (state.loading) item { CircularProgressIndicator() }
        state.summary?.let { summary ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumMetric("Signaux", summary.signalCount.toString(), Modifier.weight(1f), PremiumBlue)
                    PremiumMetric("Win rate", String.format("%.1f %%", summary.winRate), Modifier.weight(1f), scoreColor(summary.winRate.toInt()))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumMetric("Profit factor", String.format("%.2f", summary.profitFactor), Modifier.weight(1f), scoreColor((summary.profitFactor * 40).toInt()))
                    PremiumMetric("Drawdown", String.format("%.2f", summary.maxDrawdown), Modifier.weight(1f), DangerRed)
                }
            }
            item {
                PremiumCardBox("Synthèse", "Meilleur et pire actif détectés") {
                    RowLine("Meilleur actif", summary.bestSymbol)
                    RowLine("Actif à surveiller", summary.worstSymbol)
                    RowLine("Gain moyen", String.format("%.2f %%", summary.averageGain))
                    RowLine("Perte moyenne", String.format("%.2f %%", summary.averageLoss))
                }
            }
            item { PremiumSection("Par mois", summary.byMonth) }
            item { PremiumSection("Par actif", summary.bySymbol) }
            item { PremiumSection("Par stratégie", summary.byStrategy) }
            item { SafetyBanner("Statistiques en simulation : elles aident à décider, mais ne garantissent jamais un gain.") }
        }
        state.error?.let { item { PremiumCardBox("Erreur", it, accent = DangerRed) { } } }
    }
}

@Composable
private fun PremiumSection(title: String, lines: List<StatsLine>) {
    PremiumCardBox(title, "Lisible et exploitable en un coup d'œil") {
        if (lines.isEmpty()) {
            Text("Pas encore assez de données.", color = Color.LightGray, fontSize = 16.sp)
        } else {
            lines.forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(line.label, color = Color.LightGray, fontSize = 16.sp)
                    Text(line.value, color = scoreColor(line.score), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                ConfidenceBar("Score", line.score, Modifier.padding(top = 6.dp, bottom = 10.dp))
            }
        }
    }
}

package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AuditViewModel

@Composable
fun AuditScreen() {
    val viewModel: AuditViewModel = viewModel(factory = AITraderViewModelFactory)
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val learningStats by viewModel.learningStats.collectAsStateWithLifecycle()
    val reliabilityColor = when {
        summary.winRate >= 60.0 -> Success
        summary.winRate >= 50.0 -> Warning
        else -> Color(0xFFE11D48)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionTitle("Audit IA")

        Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Lecture simple", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(
                        text = beginnerReliabilityLabel(summary.winRate),
                        modifier = Modifier
                            .background(reliabilityColor, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White
                    )
                }
                Text(
                    text = "Performance ${beginnerPerformanceLabel(summary.averageGain, summary.averageLoss)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Mémoire IA : ${learningStats.memorizedConfigurations} configurations enregistrées.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Cette lecture aide un débutant à comprendre si l'IA est cohérente et si ses résultats historiques restent utilisables.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Warning,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                RowLine("Nombre de signaux", summary.signalCount.toString())
                RowLine("Taux de réussite global", String.format("%.2f %%", summary.winRate))
                RowLine("Gain moyen", String.format("%.2f %%", summary.averageGain))
                RowLine("Perte moyenne", String.format("%.2f %%", summary.averageLoss))
                RowLine("Profit factor", String.format("%.2f", summary.profitFactor))
                RowLine("Drawdown max", String.format("%.2f", summary.maxDrawdown))
                summary.currentWeights.forEach { (factor, value) ->
                    RowLine("Poids $factor", String.format("%.2f", value))
                }
            }
        }

        SectionTitle("Mémoire et apprentissage")
        Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                RowLine("Configurations mémorisées", learningStats.memorizedConfigurations.toString())
                RowLine("Réussite J+1", String.format("%.2f %%", learningStats.successRateJ1))
                RowLine("Réussite J+7", String.format("%.2f %%", learningStats.successRateJ7))
                RowLine("Réussite J+30", String.format("%.2f %%", learningStats.successRateJ30))

                Text("Top configurations gagnantes", modifier = Modifier.padding(top = 12.dp))
                if (learningStats.topWinningConfigurations.isEmpty()) {
                    Text("Aucune configuration gagnante calculable pour le moment.")
                } else {
                    learningStats.topWinningConfigurations.forEach {
                        Text("• ${it.label} | ${it.occurrences} cas | Win ${String.format("%.1f", it.winRate)} % | Perf ${String.format("%.2f", it.averagePerformance)} %")
                    }
                }

                Text("Top configurations perdantes", modifier = Modifier.padding(top = 12.dp))
                if (learningStats.topLosingConfigurations.isEmpty()) {
                    Text("Aucune configuration perdante calculable pour le moment.")
                } else {
                    learningStats.topLosingConfigurations.forEach {
                        Text("• ${it.label} | ${it.occurrences} cas | Win ${String.format("%.1f", it.winRate)} % | Perf ${String.format("%.2f", it.averagePerformance)} %")
                    }
                }

                Text("Évolution des poids", modifier = Modifier.padding(top = 12.dp))
                if (learningStats.weightEvolution.isEmpty()) {
                    Text("Historique des poids indisponible.")
                } else {
                    learningStats.weightEvolution.forEach { Text("• $it") }
                }
            }
        }
    }
}

private fun beginnerReliabilityLabel(winRate: Double): String {
    return when {
        winRate >= 60.0 -> "Fiabilité forte"
        winRate >= 50.0 -> "Fiabilité correcte"
        winRate >= 40.0 -> "Fiabilité fragile"
        else -> "Fiabilité faible"
    }
}

private fun beginnerPerformanceLabel(averageGain: Double, averageLoss: Double): String {
    return when {
        averageGain > kotlin.math.abs(averageLoss) && averageGain > 0 -> "plutôt positive"
        averageGain > 0 -> "équilibrée"
        else -> "encore insuffisante"
    }
}

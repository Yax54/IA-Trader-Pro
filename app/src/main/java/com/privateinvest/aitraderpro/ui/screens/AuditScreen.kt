package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AuditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(onBack: () -> Unit = {}) {
    val viewModel: AuditViewModel = viewModel(factory = AITraderViewModelFactory)
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val learningStats by viewModel.learningStats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit IA", color = SoftWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        }
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumScreenTitle(
                title = "Audit IA",
                subtitle = "Analyse des performances de l'intelligence artificielle"
            )
        }

        // Lecture simple / résumé débutant
        item {
            PremiumCardBox(title = "Lecture simple", subtitle = "Résumé pour débutants") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignalBadge(beginnerReliabilityLabel(summary.winRate))
                    Text(
                        "Win rate : ${String.format("%.1f", summary.winRate)} %",
                        color = Color.LightGray,
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                ConfidenceBar("Fiabilité globale", summary.winRate.toInt().coerceIn(0, 100))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Performance ${beginnerPerformanceLabel(summary.averageGain, summary.averageLoss)}",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Mémoire IA : ${learningStats.memorizedConfigurations} configurations enregistrées.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Cette lecture aide à comprendre si l'IA est cohérente et si ses résultats historiques restent utilisables.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Warning,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Métriques détaillées
        item {
            PremiumCardBox(title = "Métriques détaillées", subtitle = "Statistiques de l'IA") {
                RowLine("Nombre de signaux", summary.signalCount.toString())
                RowLine("Taux de réussite global", String.format("%.2f %%", summary.winRate))
                RowLine("Gain moyen", String.format("%.2f %%", summary.averageGain))
                RowLine("Perte moyenne", String.format("%.2f %%", summary.averageLoss))
                RowLine("Profit factor", String.format("%.2f", summary.profitFactor))
                RowLine("Drawdown max", String.format("%.2f", summary.maxDrawdown))
                if (summary.currentWeights.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Poids des facteurs", fontWeight = FontWeight.Bold, color = PremiumBlue, fontSize = 15.sp)
                    summary.currentWeights.forEach { (factor, value) ->
                        RowLine("Poids $factor", String.format("%.2f", value))
                    }
                }
            }
        }

        // Mémoire et apprentissage
        item {
            PremiumCardBox(title = "Mémoire et apprentissage", subtitle = "Historique de l'apprentissage IA") {
                RowLine("Configurations mémorisées", learningStats.memorizedConfigurations.toString())
                RowLine("Réussite J+1", String.format("%.2f %%", learningStats.successRateJ1))
                RowLine("Réussite J+7", String.format("%.2f %%", learningStats.successRateJ7))
                RowLine("Réussite J+30", String.format("%.2f %%", learningStats.successRateJ30))
            }
        }

        // R-17 : Top configurations GAGNANTES — lisibles avec cards individuelles
        item {
            PremiumCardBox(title = "Top configurations gagnantes", accent = Success) {
                if (learningStats.topWinningConfigurations.isEmpty()) {
                    Text("Aucune configuration gagnante calculable pour le moment.", color = Color.LightGray)
                } else {
                    learningStats.topWinningConfigurations.forEachIndexed { index, config ->
                        if (index > 0) Spacer(Modifier.height(10.dp))
                        TopConfigCard(
                            label = config.label,
                            occurrences = config.occurrences,
                            winRate = config.winRate,
                            averagePerformance = config.averagePerformance,
                            isWinning = true
                        )
                    }
                }
            }
        }

        // R-17 : Top configurations PERDANTES
        item {
            PremiumCardBox(title = "Top configurations perdantes", accent = DangerRed) {
                if (learningStats.topLosingConfigurations.isEmpty()) {
                    Text("Aucune configuration perdante calculable pour le moment.", color = Color.LightGray)
                } else {
                    learningStats.topLosingConfigurations.forEachIndexed { index, config ->
                        if (index > 0) Spacer(Modifier.height(10.dp))
                        TopConfigCard(
                            label = config.label,
                            occurrences = config.occurrences,
                            winRate = config.winRate,
                            averagePerformance = config.averagePerformance,
                            isWinning = false
                        )
                    }
                }
            }
        }

        // Évolution des poids
        item {
            PremiumCardBox(title = "Évolution des poids") {
                if (learningStats.weightEvolution.isEmpty()) {
                    Text("Historique des poids indisponible.", color = Color.LightGray)
                } else {
                    learningStats.weightEvolution.forEach { line ->
                        Text("• $line", color = Color.LightGray, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        item { SafetyBanner() }
    }
    } // fin Scaffold
}

/** R-17 : carte pour une configuration, lisible et structurée */
@Composable
private fun TopConfigCard(
    label: String,
    occurrences: Int,
    winRate: Double,
    averagePerformance: Double,
    isWinning: Boolean
) {
    val accent = if (isWinning) Success else DangerRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumCardAlt),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$occurrences cas", color = Color.LightGray, fontSize = 13.sp)
                Text("Win ${String.format("%.1f", winRate)} %", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Perf ${String.format("%.2f", averagePerformance)} %",
                    color = if (averagePerformance >= 0) Success else DangerRed, fontSize = 13.sp)
            }
            ConfidenceBar("Taux de réussite", winRate.toInt().coerceIn(0, 100))
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

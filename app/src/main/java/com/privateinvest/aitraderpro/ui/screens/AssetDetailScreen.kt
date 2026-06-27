package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.navigation.SelectedAssetStore
import com.privateinvest.aitraderpro.repository.BeginnerExplanation
import com.privateinvest.aitraderpro.repository.BeginnerSignalSummary
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AssetDetailViewModel
import com.privateinvest.aitraderpro.viewmodel.SettingsViewModel

@Composable
fun AssetDetailScreen(onOpenAssistant: () -> Unit = {}) {
    val viewModel: AssetDetailViewModel = viewModel(factory = AITraderViewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AITraderViewModelFactory)
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val beginnerModeEnabled by settingsViewModel.beginnerModeEnabled.collectAsStateWithLifecycle()
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(beginnerModeEnabled) {
        showAdvanced = !beginnerModeEnabled
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Fiche actif · ${SelectedAssetStore.currentName}")
        Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (detail == null) {
                    Text("Aucune donnée réelle disponible tant que la clé API Alpha Vantage n'est pas configurée.")
                } else {
                    val currentDetail = detail!!
                    val beginner = BeginnerExplanation.fromAssetDetail(currentDetail)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumSignalBadge(beginner.actionLabel)
                        Text(
                            text = if (beginnerModeEnabled) "Mode débutant" else "Mode expert",
                            modifier = Modifier
                                .background(Color(0xFF243447), RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = SoftWhite,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    ConfidenceCard(beginner = beginner, scoreTechnique = currentDetail.scoring.scoreTechnique)

                    if (beginnerModeEnabled) {
                        BeginnerSummaryCard(beginner = beginner)
                    } else {
                        ExpertScoreCard(currentDetail.scoring.scoreTechnique, currentDetail.scoring.scoreRisque)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onOpenAssistant, modifier = Modifier.weight(1f)) {
                            Text("Pourquoi ce signal ?")
                        }
                        OutlinedButton(
                            onClick = { showAdvanced = !showAdvanced },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (showAdvanced) "Masquer détails" else "Voir détails")
                        }
                    }

                    if (showAdvanced) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                RowLine("Cours", String.format("%.2f", currentDetail.quotePrice))
                                RowLine("Score technique", "${currentDetail.scoring.scoreTechnique}/100")
                                RowLine("Score risque", "${currentDetail.scoring.scoreRisque}/100")
                                RowLine("Signal", currentDetail.scoring.signal.name)
                                RowLine("RSI", String.format("%.2f", currentDetail.technicalSnapshot.rsi))
                                RowLine("MACD", String.format("%.4f", currentDetail.technicalSnapshot.macd))
                                RowLine("MACD Signal", String.format("%.4f", currentDetail.technicalSnapshot.macdSignal))
                                RowLine("EMA20", String.format("%.2f", currentDetail.technicalSnapshot.ema20))
                                RowLine("EMA50", String.format("%.2f", currentDetail.technicalSnapshot.ema50))
                                RowLine("EMA200", String.format("%.2f", currentDetail.technicalSnapshot.ema200))
                                RowLine("ATR14", String.format("%.2f", currentDetail.technicalSnapshot.atr14))
                                RowLine("Volatilité", String.format("%.2f %%", currentDetail.technicalSnapshot.volatility))
                                RowLine(
                                    "Volume actuel / moyen",
                                    String.format(
                                        "%.0f / %.0f",
                                        currentDetail.technicalSnapshot.currentVolume,
                                        currentDetail.technicalSnapshot.averageVolume
                                    )
                                )
                                SignalExplanation(
                                    explanation = currentDetail.scoring.explanation,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = viewModel::confirmSimulationOrder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Valider l'ordre en simulation")
                    }

                    statusMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfidenceCard(beginner: BeginnerSignalSummary, scoreTechnique: Int) {
    val gaugeColor = when {
        beginner.confidencePercent >= 75 -> Success
        beginner.confidencePercent >= 55 -> Warning
        else -> Color(0xFFE11D48)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Confiance IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${beginner.confidencePercent} %",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftWhite
                )
                Text(
                    text = beginner.confidenceVisualLabel,
                    modifier = Modifier
                        .background(gaugeColor, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White
                )
            }
            LinearProgressIndicator(
                progress = { beginner.confidencePercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = gaugeColor,
                trackColor = Color(0xFF0F172A)
            )
            RowLine("Lecture du signal", beginner.confidenceLabel)
            RowLine("Score technique brut", "$scoreTechnique/100")
        }
    }
}

@Composable
private fun PremiumSignalBadge(actionLabel: String) {
    val signalColor = when (actionLabel) {
        "ACHETER" -> Success
        "VENDRE" -> Color(0xFFE11D48)
        else -> Warning
    }
    Box(
        modifier = Modifier
            .background(signalColor, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExpertScoreCard(scoreTechnique: Int, scoreRisque: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vue expert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            RowLine("Score technique", "$scoreTechnique/100")
            RowLine("Score risque", "$scoreRisque/100")
            Text("Les indicateurs détaillés sont visibles juste en dessous.", color = SoftWhite)
        }
    }
}

@Composable
private fun BeginnerSummaryCard(beginner: BeginnerSignalSummary) {
    val signalColor = when (beginner.actionLabel) {
        "ACHETER" -> Success
        "VENDRE" -> Color(0xFFE11D48)
        else -> Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Résumé débutant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(signalColor, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = beginner.actionLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = beginner.headline,
                style = MaterialTheme.typography.bodyLarge,
                color = SoftWhite
            )

            RowLine("Lecture du risque", beginner.riskLabel)
            RowLine("Lisibilité du signal", beginner.confidenceLabel)
            RowLine("Exposition max conseillée", "${beginner.maxInvestmentPercent} % du capital")
            RowLine("Stop de protection indicatif", String.format("%.1f %%", beginner.stopLossPercent))

            Text(
                text = "Pourquoi l'IA dit cela",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            beginner.whyText.forEach {
                Text(
                    text = "• $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite
                )
            }

            Text(
                text = beginner.similarSignalsText,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftWhite,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = beginner.historicalEdgeText,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftWhite
            )
            Text(
                text = beginner.warningText,
                style = MaterialTheme.typography.bodySmall,
                color = Warning,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

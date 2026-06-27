package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.navigation.SelectedAssetStore
import com.privateinvest.aitraderpro.repository.BeginnerExplanation
import com.privateinvest.aitraderpro.repository.BeginnerSignalSummary
import com.privateinvest.aitraderpro.repository.OpportunityStrategy
import com.privateinvest.aitraderpro.repository.OpportunityStrategyEngine
import com.privateinvest.aitraderpro.repository.OpportunityStrategyType
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AssetDetailViewModel
import com.privateinvest.aitraderpro.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    onBack: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenBroker: () -> Unit = {},
    onOpenStrategyMonitoring: () -> Unit = {}
) {
    val viewModel: AssetDetailViewModel = viewModel(factory = AITraderViewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AITraderViewModelFactory)
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val beginnerModeEnabled by settingsViewModel.beginnerModeEnabled.collectAsStateWithLifecycle()
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    // Stratégies : calculées en mémoire dès que detail est disponible
    var expandedStrategyType by remember { mutableStateOf<OpportunityStrategyType?>(null) }
    var showFollowUpConfirm by remember { mutableStateOf<OpportunityStrategy?>(null) }

    // R-04 : état du dialog de confirmation d'achat simulé
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(beginnerModeEnabled) {
        showAdvanced = !beginnerModeEnabled
    }

    // Dialog de confirmation de suivi intelligent
    showFollowUpConfirm?.let { strategy ->
        AlertDialog(
            onDismissRequest = { showFollowUpConfirm = null },
            title = { Text("${strategy.type.emoji} Activer le suivi ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stratégie : ${strategy.type.label}")
                    Text("Horizon : ${strategy.horizon}")
                    Text("Objectif : +${strategy.targetGainPercent} %   Stop : -${strategy.stopLossPercent} %")
                    Text(
                        "L'application t'alertera quand une vente devient intéressante. Elle ne vend jamais automatiquement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            },
            confirmButton = {
                PremiumActionButton(
                    text = "Activer le suivi",
                    onClick = {
                        viewModel.createFollowUp(
                            symbol = SelectedAssetStore.currentSymbol,
                            name = SelectedAssetStore.currentName,
                            mode = "SIMULATION",
                            preferredType = strategy.type
                        )
                        showFollowUpConfirm = null
                        onOpenStrategyMonitoring()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showFollowUpConfirm = null }) { Text("Annuler") }
            }
        )
    }

    // R-04 : dialog de confirmation avant achat simulé
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmer l'ordre simulé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Valider cet ordre simulé sur ${SelectedAssetStore.currentName} (${SelectedAssetStore.currentSymbol}) ?")
                    Text(
                        "Le signal sera mémorisé et le suivi J+1 / J+7 / J+30 démarrera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmSimulationOrder()
                        showConfirmDialog = false
                    }
                ) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        topBar = {
            // R-10 : TopAppBar avec bouton retour explicite
            TopAppBar(
                title = { Text(SelectedAssetStore.currentName, color = SoftWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)), shape = RoundedCornerShape(24.dp)) {
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
                        PremiumSecondaryButton(
                            text = "Assistant investissement",
                            onClick = onOpenBroker,
                            modifier = Modifier.fillMaxWidth()
                        )

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

                        // Carte Profils compatibles (calcul moteur stratégies)
                        val strategies = remember(detail) {
                            detail?.let { OpportunityStrategyEngine.classify(it) } ?: emptyList()
                        }
                        if (strategies.isNotEmpty()) {
                            StrategyProfilesCard(
                                strategies = strategies,
                                expandedType = expandedStrategyType,
                                onToggle = { type ->
                                    expandedStrategyType = if (expandedStrategyType == type) null else type
                                },
                                onStartFollowUp = { strategy -> showFollowUpConfirm = strategy }
                            )
                        }

                        // R-04 : bouton ouvre le dialog au lieu de valider directement
                        PremiumActionButton(
                            text = "Valider l'ordre en simulation",
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )

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
}

@Composable
private fun StrategyProfilesCard(
    strategies: List<OpportunityStrategy>,
    expandedType: OpportunityStrategyType?,
    onToggle: (OpportunityStrategyType) -> Unit,
    onStartFollowUp: (OpportunityStrategy) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2E45)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Profils compatibles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Clique sur un profil pour comprendre la stratégie. Appuie sur Surveiller pour activer le suivi.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite
            )
            strategies.forEach { strategy ->
                val isExpanded = expandedType == strategy.type
                val compatColor = when {
                    strategy.compatibility >= 80 -> Success
                    strategy.compatibility >= 65 -> Warning
                    else -> PremiumBlue
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(strategy.type) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) Color(0xFF243447) else Color(0xFF132238)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strategy.type.emoji, fontSize = 20.sp)
                                Column {
                                    Text(strategy.type.label, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(strategy.horizon, color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(compatColor.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${strategy.compatibility} %", color = compatColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(strategy.beginnerExplanation, color = SoftWhite, style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Objectif : +${strategy.targetGainPercent} %", color = Success, style = MaterialTheme.typography.labelSmall)
                                    Text("Stop : -${strategy.stopLossPercent} %", color = DangerRed, style = MaterialTheme.typography.labelSmall)
                                }
                                strategy.reasons.forEach { reason ->
                                    Text("• $reason", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(strategy.whatAppDoes, color = PremiumBlue, style = MaterialTheme.typography.bodySmall)
                                if (strategy.type != OpportunityStrategyType.DEFENSIVE) {
                                    PremiumActionButton(
                                        text = "🔔 Surveiller",
                                        onClick = { onStartFollowUp(strategy) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
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

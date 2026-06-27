package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.repository.StrategyFollowUpItem
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.StrategyMonitoringViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyMonitoringScreen(
    onBack: () -> Unit = {},
    onOpenAsset: (symbol: String, name: String) -> Unit = { _, _ -> }
) {
    val viewModel: StrategyMonitoringViewModel = viewModel(factory = AITraderViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Dialog de confirmation de clôture
    var pendingCloseId by remember { mutableStateOf<Long?>(null) }
    var pendingCloseSymbol by remember { mutableStateOf("") }

    // Message flash
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearStatusMessage()
        }
    }

    pendingCloseId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingCloseId = null },
            title = { Text("Clôturer le suivi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Clôturer le suivi sur $pendingCloseSymbol ?")
                    Text(
                        "Le résultat sera enregistré dans l'historique. Aucun ordre ne sera passé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            },
            confirmButton = {
                PremiumActionButton(
                    text = "Clôturer",
                    onClick = {
                        viewModel.closeFollowUp(id)
                        pendingCloseId = null
                    },
                    danger = true
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingCloseId = null }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suivi intelligent", color = SoftWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = SoftWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser", tint = SoftWhite)
                    }
                    IconButton(onClick = { viewModel.toggleClosedHistory() }) {
                        Icon(Icons.Filled.History, contentDescription = "Historique", tint = SoftWhite)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Bannière de sécurité
            item {
                SafetyBanner("L'application surveille et alerte. Elle ne vend jamais automatiquement. Tu valides toujours manuellement.")
            }

            // Message flash
            state.statusMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            msg,
                            modifier = Modifier.padding(14.dp),
                            color = PremiumBlue,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Compteur d'alertes actives
            if (state.alertCount > 0) {
                item {
                    AlertSummaryBanner(count = state.alertCount)
                }
            }

            // Loading
            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PremiumBlue)
                    }
                }
            }

            // Suivis actifs
            item {
                PremiumScreenTitle(
                    "Suivis actifs (${state.activeFollowUps.size})",
                    "Alertes automatiques — validation manuelle obligatoire"
                )
            }

            if (state.activeFollowUps.isEmpty() && !state.loading) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Aucun suivi actif", color = PremiumMuted, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Ouvre la fiche d'un actif et appuie sur \"Surveiller\" pour démarrer un suivi.",
                                color = PremiumMuted,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(state.activeFollowUps, key = { it.id }) { item ->
                FollowUpCard(
                    item = item,
                    onOpenAsset = { onOpenAsset(item.symbol, item.name) },
                    onClose = {
                        pendingCloseId = item.id
                        pendingCloseSymbol = item.symbol
                    },
                    onExtend = { viewModel.extendFollowUp(item.id) }
                )
            }

            // Historique des suivis clôturés
            if (state.showClosedHistory) {
                item {
                    PremiumScreenTitle(
                        "Historique (${state.closedFollowUps.size})",
                        "Suivis clôturés"
                    )
                }
                if (state.closedFollowUps.isEmpty()) {
                    item {
                        Text(
                            "Aucun suivi clôturé pour l'instant.",
                            color = PremiumMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(state.closedFollowUps, key = { "closed_${it.id}" }) { item ->
                    ClosedFollowUpCard(item = item, onOpenAsset = { onOpenAsset(item.symbol, item.name) })
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AlertSummaryBanner(count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1A1A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = DangerRed, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    "$count vente(s) à envisager",
                    color = DangerRed,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Consulte les cartes ci-dessous. Aucune vente automatique — tu décides.",
                    color = SoftWhite,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FollowUpCard(
    item: StrategyFollowUpItem,
    onOpenAsset: () -> Unit,
    onClose: () -> Unit,
    onExtend: () -> Unit
) {
    val isSellAlert = item.status == "SELL_ALERT"
    val perfColor = when {
        item.performancePercent >= 0 -> Success
        else -> DangerRed
    }
    val cardBg = if (isSellAlert) Color(0xFF2A1A1A) else Color(0xFF132238)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.strategyEmoji, fontSize = 22.sp)
                    Column {
                        Text(item.symbol, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(item.name, color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    ModeChip(item.mode)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.strategyLabel,
                        color = PremiumBlue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Perf + objectifs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricMini("Performance", formatSignedPct(item.performancePercent), perfColor)
                MetricMini("Objectif", "+${item.targetPercent} %", Success)
                MetricMini("Stop", "-${item.stopPercent} %", DangerRed)
                MetricMini("Durée", "${item.daysOpen}j / ${item.maxHoldingDays}j", Warning)
            }

            // Alerte de vente
            if (isSellAlert) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1A1A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Vente à envisager", color = DangerRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            item.alertReason?.let {
                                Text(it, color = SoftWhite, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Aucune vente automatique — tu valides manuellement dans l'Assistant.",
                                color = PremiumMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Conseil
            Text(item.advice, color = PremiumMuted, style = MaterialTheme.typography.bodySmall)

            // Actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenAsset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Voir la fiche", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onExtend,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+3 jours", style = MaterialTheme.typography.labelMedium)
                }
                PremiumActionButton(
                    text = "Clôturer",
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    danger = isSellAlert
                )
            }
        }
    }
}

@Composable
private fun ClosedFollowUpCard(
    item: StrategyFollowUpItem,
    onOpenAsset: () -> Unit
) {
    val perfColor = when {
        (item.performancePercent) >= 0 -> Success
        else -> DangerRed
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenAsset() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1F30)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PremiumMuted, modifier = Modifier.size(20.dp))
                Column {
                    Text(item.symbol, color = SoftWhite, fontWeight = FontWeight.SemiBold)
                    Text("${item.strategyEmoji} ${item.strategyLabel}", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                    item.alertReason?.let {
                        Text(it, color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatSignedPct(item.performancePercent),
                    color = perfColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("${item.daysOpen}j", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                ModeChip(item.mode)
            }
        }
    }
}

@Composable
private fun ModeChip(mode: String) {
    val (bg, label) = when (mode.uppercase()) {
        "REEL", "REAL" -> Pair(DangerRed, "Réel")
        "PAPER", "PAPER_TRADING" -> Pair(PremiumBlue, "Paper")
        else -> Pair(Color(0xFF1E3A5F), "Simu")
    }
    Box(
        modifier = Modifier
            .background(bg.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = bg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricMini(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = PremiumMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatSignedPct(value: Double): String {
    val rounded = (value * 10.0).toLong() / 10.0
    val sign = if (rounded > 0) "+" else ""
    return "$sign$rounded %"
}

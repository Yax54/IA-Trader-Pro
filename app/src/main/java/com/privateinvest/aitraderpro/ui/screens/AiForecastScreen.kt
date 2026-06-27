package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AiForecastViewModel
import com.privateinvest.aitraderpro.viewmodel.ForecastTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiForecastScreen(
    onBack: () -> Unit,
    onOpenForecastDetail: (Long) -> Unit,
    onOpenAsset: (String, String) -> Unit,
    onOpenBroker: (String, String) -> Unit,
    viewModel: AiForecastViewModel = viewModel(factory = AITraderViewModelFactory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    state.csvContent?.let { csv ->
        AlertDialog(
            onDismissRequest = { viewModel.clearCsv() },
            title = { Text("Export CSV prêt") },
            text = { Text("Le contenu CSV des pronostics est généré. L'autre IA peut le relier au partage Android si besoin.\n\n${csv.take(400)}...") },
            confirmButton = { TextButton(onClick = { viewModel.clearCsv() }) { Text("Fermer") } }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = SoftWhite)
            }
            Column(Modifier.weight(1f)) {
                PremiumScreenTitle(
                    title = "🤖 Pronostics IA",
                    subtitle = "Ce que l'IA a prévu, même si tu n'as pas acheté."
                )
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Rafraîchir", tint = SoftWhite)
            }
            IconButton(onClick = { viewModel.exportCsv() }) {
                Icon(Icons.Filled.Download, contentDescription = "Export CSV", tint = SoftWhite)
            }
        }

        ForecastSummaryStrip(
            active = state.active.size,
            success = state.success.size,
            failed = state.failed.size,
            memory = state.memory.size
        )

        ScrollableTabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = Color(0xFF132238),
            edgePadding = 0.dp
        ) {
            ForecastTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = { Text(tabLabel(tab), fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.selectedTab == ForecastTab.MEMORY) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Rechercher par nom ou symbole") },
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            StrategyChips(selected = state.selectedStrategy, onSelect = viewModel::updateStrategy)
            Spacer(Modifier.height(10.dp))
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumBlue)
            }
            return@Column
        }

        state.error?.let {
            PremiumCardBox("Erreur", it, accent = DangerRed) { Text(it, color = Color.White, fontSize = 16.sp) }
        }

        val list = when (state.selectedTab) {
            ForecastTab.ACTIVE -> state.active
            ForecastTab.SUCCESS -> state.success
            ForecastTab.FAILED -> state.failed
            ForecastTab.MEMORY -> viewModel.filtered(state.memory)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            if (list.isEmpty()) {
                item {
                    PremiumCardBox(
                        title = "Aucun pronostic",
                        subtitle = when (state.selectedTab) {
                            ForecastTab.ACTIVE -> "Aucun pronostic actif pour le moment. L'IA en créera uniquement quand un vrai signal intéressant apparaît."
                            ForecastTab.SUCCESS -> "Les pronostics réussis apparaîtront ici."
                            ForecastTab.FAILED -> "Les pronostics échoués apparaîtront ici pour comprendre ce qui n'a pas fonctionné."
                            ForecastTab.MEMORY -> "Aucune archive ne correspond aux filtres."
                        },
                        accent = Warning
                    ) { Text("Ce n'est pas une erreur : l'IA évite de remplir la mémoire avec du bruit.", color = Color.LightGray, fontSize = 16.sp) }
                }
            }
            items(list, key = { it.id }) { forecast ->
                ForecastCard(
                    forecast = forecast,
                    outcome = state.outcomes[forecast.id],
                    onOpen = { onOpenForecastDetail(forecast.id) },
                    onSimulate = { onOpenAsset(forecast.symbol, forecast.name) },
                    onBuy = { onOpenBroker(forecast.symbol, forecast.name) },
                    onPlayed = { viewModel.markAsPlayed(forecast.id) },
                    onIgnored = { viewModel.markAsIgnored(forecast.id) }
                )
            }
        }
    }
}

@Composable
private fun ForecastSummaryStrip(active: Int, success: Int, failed: Int, memory: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PremiumMetric("En cours", active.toString(), Modifier.weight(1f), PremiumBlue)
        PremiumMetric("Réussis", success.toString(), Modifier.weight(1f), Success)
        PremiumMetric("Échoués", failed.toString(), Modifier.weight(1f), DangerRed)
        PremiumMetric("Mémoire", memory.toString(), Modifier.weight(1f), Warning)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ForecastCard(
    forecast: AiForecastEntity,
    outcome: AiForecastOutcomeEntity?,
    onOpen: () -> Unit,
    onSimulate: () -> Unit,
    onBuy: () -> Unit,
    onPlayed: () -> Unit,
    onIgnored: () -> Unit
) {
    // V1.7 : accent = couleur fixe par catégorie de stratégie (identité visuelle constante)
    val stratAccent = strategyColor(forecast.strategyType)
    PremiumCardBox(
        title = "${forecast.symbol} — ${forecast.name}",
        subtitle = "${strategyEmoji(forecast.strategyType)} ${forecast.strategyLabel} · ${forecast.status}",
        accent = stratAccent,
        modifier = Modifier.clickable { onOpen() }
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // Badge stratégie avec couleur fixe de catégorie
            Box(
                modifier = Modifier
                    .background(stratAccent.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${strategyEmoji(forecast.strategyType)} ${forecast.strategyLabel.take(16)}",
                    color = stratAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("${forecast.score} / 100", color = SoftWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        ConfidenceBar("Confiance IA", forecast.confidence)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallInfo("Prix signal", money(forecast.entryPrice))
            SmallInfo("Objectif", "+${one(forecast.targetPercent)} %")
            SmallInfo("Stop", "-${one(forecast.stopPercent)} %")
            SmallInfo("Horizon", "${forecast.horizonDays} j")
        }
        outcome?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                "Suivi : J+1 ${perfLabel(it.performanceJ1)} · J+3 ${perfLabel(it.performanceJ3)} · J+7 ${perfLabel(it.performanceJ7)} · J+30 ${perfLabel(it.performanceJ30)} · J+90 ${perfLabel(it.performanceJ90)}",
                color = Color.LightGray,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumActionButton("Voir", onOpen, Modifier.weight(1f))
            PremiumSecondaryButton("Simuler", onSimulate, Modifier.weight(1f))
            PremiumSecondaryButton("Acheter", onBuy, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPlayed, modifier = Modifier.weight(1f)) { Text("Marquer joué") }
            Button(onClick = onIgnored, modifier = Modifier.weight(1f)) { Text("Ignoré") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrategyChips(selected: String, onSelect: (String) -> Unit) {
    val strategies = listOf(
        "TOUTES" to "Toutes",
        "QUICK" to "⚡ Rapide",
        "SWING" to "📈 Swing",
        "HIGH_VOLATILITY" to "🔥 Volatilité",
        "LONG_TERM" to "🏆 Long terme",
        "GROWTH" to "🌱 Croissance",
        "DEFENSIVE" to "🛡 Défensif"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        strategies.take(4).forEach { (key, label) ->
            FilterChip(selected = selected == key, onClick = { onSelect(key) }, label = { Text(label, fontSize = 13.sp) })
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        strategies.drop(4).forEach { (key, label) ->
            FilterChip(selected = selected == key, onClick = { onSelect(key) }, label = { Text(label, fontSize = 13.sp) })
        }
    }
}

@Composable
private fun SmallInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = PremiumMuted, fontSize = 12.sp)
        Text(value, color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private fun tabLabel(tab: ForecastTab): String = when (tab) {
    ForecastTab.ACTIVE -> "🟢 En cours"
    ForecastTab.SUCCESS -> "🔵 Réussis"
    ForecastTab.FAILED -> "🔴 Échoués"
    ForecastTab.MEMORY -> "🧠 Mémoire"
}

private fun strategyEmoji(type: String): String = when (type) {
    "QUICK" -> "⚡"
    "SWING" -> "📈"
    "HIGH_VOLATILITY" -> "🔥"
    "LONG_TERM" -> "🏆"
    "GROWTH" -> "🌱"
    "DEFENSIVE" -> "🛡"
    else -> "🤖"
}

private fun money(v: Double): String = String.format(Locale.FRANCE, "%.2f", v)
private fun one(v: Double): String = String.format(Locale.FRANCE, "%.1f", v)
private fun perfLabel(v: Double?): String = if (v == null) "—" else String.format(Locale.FRANCE, "%+.1f%%", v)
private fun date(ts: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(ts))

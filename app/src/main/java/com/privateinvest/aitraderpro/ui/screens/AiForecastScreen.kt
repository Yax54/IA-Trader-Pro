package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import com.privateinvest.aitraderpro.repository.AiForecastRepository
import com.privateinvest.aitraderpro.repository.BucketConfidence
import com.privateinvest.aitraderpro.repository.ForecastVsPlayedResult
import com.privateinvest.aitraderpro.repository.MemoryType
import com.privateinvest.aitraderpro.repository.RemarkableForecast
import com.privateinvest.aitraderpro.repository.SectorStats
import com.privateinvest.aitraderpro.repository.StrategyStats
import com.privateinvest.aitraderpro.viewmodel.AiForecastViewModel
import com.privateinvest.aitraderpro.viewmodel.AiForecastUiState
import com.privateinvest.aitraderpro.viewmodel.ForecastTab

// ─── COULEURS ────────────────────────────────────────────────────────────────
private val GreenPositive = Color(0xFF4CAF50)
private val RedNegative   = Color(0xFFF44336)
private val OrangeWarning = Color(0xFFFF9800)
private val BlueCT        = Color(0xFF1E88E5)
private val GreenLT       = Color(0xFF43A047)
private val DarkSurface   = Color(0xFF1C1C2E)
private val CardSurface   = Color(0xFF252538)

// ─── ÉCRAN PRINCIPAL ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiForecastScreen(
    onBack: () -> Unit,
    vm: AiForecastViewModel = viewModel(factory = com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory)
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Dialog CSV export
    if (state.exportCsvContent != null) {
        CsvExportDialog(
            csvContent = state.exportCsvContent!!,
            onDismiss = { vm.clearCsvExport() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // ── En-tête ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("🧠 Pronostics IA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Mémoire glissante — CT & LT indépendants", fontSize = 12.sp, color = Color(0xFF90A4AE))
            }
            IconButton(onClick = { vm.refreshOutcomes() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Rafraîchir", tint = Color(0xFF90A4AE))
            }
            IconButton(onClick = { vm.exportCsv() }) {
                Icon(Icons.Filled.Download, contentDescription = "Export CSV", tint = Color(0xFF90A4AE))
            }
        }

        // ── Pronostics remarquables (bandeau alert) ───────────────────────────
        if (state.remarkableUnplayed.isNotEmpty()) {
            RemarkableBanner(state.remarkableUnplayed)
        }

        // ── Onglets ───────────────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = CardSurface,
            contentColor = Color.White,
            edgePadding = 8.dp
        ) {
            ForecastTab.entries.forEachIndexed { _, tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { vm.onTabSelected(tab) },
                    text = {
                        Text(
                            text = tabLabel(tab),
                            fontSize = 13.sp,
                            color = if (state.selectedTab == tab) Color.White else Color(0xFF90A4AE)
                        )
                    }
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueCT)
            }
            return@Column
        }

        // ── Contenu par onglet ────────────────────────────────────────────────
        when (state.selectedTab) {
            ForecastTab.CT -> MemoryPanel(
                state = state,
                memoryType = MemoryType.CT,
                activeForecasts = state.activeForecastsCT,
                allForecasts = state.allForecastsCT,
                outcomes = state.outcomes,
                stratStats = state.stratStatsCT,
                bucketConfidence = state.bucketConfidenceCT,
                precisionData = state.precisionOverTimeCT,
                sectorStats = state.sectorStatsCT,
                accentColor = BlueCT,
                label = "Court Terme",
                onMarkPlayed = vm::markAsPlayed,
                onMarkIgnored = vm::markAsIgnored,
                onExportMemory = { vm.exportCsv(MemoryType.CT) }
            )
            ForecastTab.LT -> MemoryPanel(
                state = state,
                memoryType = MemoryType.LT,
                activeForecasts = state.activeForecastsLT,
                allForecasts = state.allForecastsLT,
                outcomes = state.outcomes,
                stratStats = state.stratStatsLT,
                bucketConfidence = state.bucketConfidenceLT,
                precisionData = state.precisionOverTimeLT,
                sectorStats = state.sectorStatsLT,
                accentColor = GreenLT,
                label = "Long Terme",
                onMarkPlayed = vm::markAsPlayed,
                onMarkIgnored = vm::markAsIgnored,
                onExportMemory = { vm.exportCsv(MemoryType.LT) }
            )
            ForecastTab.COMPARE -> ComparePanel(state.forecastVsPlayed)
            ForecastTab.STATS -> GlobalStatsPanel(
                statsCT = state.stratStatsCT,
                statsLT = state.stratStatsLT,
                bucketsCT = state.bucketConfidenceCT,
                bucketsLT = state.bucketConfidenceLT
            )
        }
    }
}

// ─── PANNEAU MÉMOIRE (CT ou LT) ──────────────────────────────────────────────

@Composable
private fun MemoryPanel(
    @Suppress("UNUSED_PARAMETER") state: AiForecastUiState,
    @Suppress("UNUSED_PARAMETER") memoryType: String,
    activeForecasts: List<AiForecastEntity>,
    allForecasts: List<AiForecastEntity>,
    outcomes: Map<Long, AiForecastOutcomeEntity>,
    stratStats: List<StrategyStats>,
    bucketConfidence: List<BucketConfidence>,
    precisionData: List<Pair<Long, Double>>,
    sectorStats: List<SectorStats>,
    accentColor: Color,
    label: String,
    onMarkPlayed: (Long) -> Unit,
    onMarkIgnored: (Long) -> Unit,
    onExportMemory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // En-tête mémoire
        item {
            MemoryHeaderCard(label = label, accentColor = accentColor, count = allForecasts.size)
        }

        // Pronostics actifs (non joués)
        if (activeForecasts.isNotEmpty()) {
            item {
                Text(
                    "🎯 Pronostics actifs (non joués) — ${activeForecasts.size}",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
            }
            items(activeForecasts) { forecast ->
                ForecastCard(
                    forecast = forecast,
                    outcome = outcomes[forecast.id],
                    accentColor = accentColor,
                    onMarkPlayed = onMarkPlayed,
                    onMarkIgnored = onMarkIgnored
                )
            }
        } else {
            item {
                InfoCard("Aucun pronostic actif pour l'instant.\nLe WorkManager générera des pronostics toutes les 6h.")
            }
        }

        // Graphique de précision IA
        if (precisionData.size >= 2) {
            item {
                Text(
                    "📈 Précision de l'IA dans le temps",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                PrecisionLineChart(data = precisionData, accentColor = accentColor)
            }
        }

        // Score de confiance par bucket
        if (bucketConfidence.isNotEmpty()) {
            item {
                Text(
                    "🧠 Confiance par horizon mémoire",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
            }
            items(bucketConfidence) { bc ->
                BucketConfidenceCard(bc, accentColor)
            }
        }

        // Stats par stratégie
        val reliableStats = stratStats.filter { it.isReliable }
        if (reliableStats.isNotEmpty()) {
            item {
                Text(
                    "📊 Statistiques par stratégie",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
                Text(
                    "Min. ${AiForecastRepository.MIN_FORECASTS_FOR_STATS} pronostics pour affichage",
                    fontSize = 11.sp, color = Color(0xFF90A4AE)
                )
            }
            items(reliableStats) { stats ->
                StrategyStatsCard(stats, accentColor)
            }
        }

        // Stats par secteur
        if (sectorStats.isNotEmpty()) {
            item {
                Text(
                    "🏭 Statistiques par secteur",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
            }
            items(sectorStats) { ss ->
                SectorStatsCard(ss)
            }
        }

        // Bouton export
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onExportMemory) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Exporter mémoire $label", color = accentColor, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── CARTES ──────────────────────────────────────────────────────────────────

@Composable
private fun MemoryHeaderCard(label: String, accentColor: Color, count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (label == "Court Terme") "CT" else "LT", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Mémoire $label", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "$count pronostics enregistrés",
                    color = Color(0xFF90A4AE), fontSize = 12.sp
                )
                Text(
                    if (label == "Court Terme") "QUICK · SWING · FORTE VOLATILITÉ"
                    else "LONG TERME · CROISSANCE · DÉFENSIF",
                    color = accentColor.copy(alpha = 0.8f), fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ForecastCard(
    forecast: AiForecastEntity,
    outcome: AiForecastOutcomeEntity?,
    accentColor: Color,
    onMarkPlayed: (Long) -> Unit,
    onMarkIgnored: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column {
            // Ligne principale
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(forecast.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        BucketChip(forecast.memoryBucket, accentColor)
                    }
                    Text(forecast.name, color = Color(0xFF90A4AE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(forecast.strategyLabel, color = accentColor, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    ScoreBadge(forecast.score)
                    Text("Conf. ${forecast.confidence}%", color = Color(0xFF90A4AE), fontSize = 11.sp)
                }
            }

            // Données de base
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricMini("Entrée", String.format("%.2f", forecast.entryPrice))
                MetricMini("Cible", "+${forecast.targetPercent}%")
                MetricMini("Stop", "-${forecast.stopPercent}%")
                MetricMini("Horizon", "${forecast.horizonDays}j")
            }

            // Performances si disponibles
            if (outcome != null) {
                Spacer(Modifier.height(6.dp))
                PerformanceRow(outcome)
            }

            // Actions (expandable)
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(
                            onClick = { onMarkPlayed(forecast.id) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenPositive.copy(alpha = 0.15f))
                        ) {
                            Text("✓ Joué", color = GreenPositive, fontSize = 13.sp)
                        }
                        TextButton(
                            onClick = { onMarkIgnored(forecast.id) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.15f))
                        ) {
                            Text("✗ Ignoré", color = Color(0xFF90A4AE), fontSize = 13.sp)
                        }
                    }
                    if (forecast.notes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(forecast.notes, color = Color(0xFF607D8B), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceRow(outcome: AiForecastOutcomeEntity) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        outcome.performanceJ1?.let  { PerfChip("J+1",  it) }
        outcome.performanceJ3?.let  { PerfChip("J+3",  it) }
        outcome.performanceJ7?.let  { PerfChip("J+7",  it) }
        outcome.performanceJ30?.let { PerfChip("J+30", it) }
        outcome.performanceJ90?.let { PerfChip("J+90", it) }
    }
}

@Composable
private fun PerfChip(label: String, perf: Double) {
    val color = when {
        perf > 0  -> GreenPositive
        perf < 0  -> RedNegative
        else -> Color(0xFF90A4AE)
    }
    val sign = if (perf > 0) "+" else ""
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF90A4AE), fontSize = 9.sp)
        Text("$sign${String.format("%.1f", perf)}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BucketChip(bucket: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accentColor.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(bucket, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    val color = when {
        score >= 80 -> GreenPositive
        score >= 60 -> OrangeWarning
        else -> RedNegative
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("$score", color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF607D8B), fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── GRAPHIQUE DE PRÉCISION ──────────────────────────────────────────────────

@Composable
private fun PrecisionLineChart(
    data: List<Pair<Long, Double>>,
    accentColor: Color
) {
    if (data.size < 2) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minY = 0.0
            val maxY = 1.0
            val range = maxY - minY

            // Ligne de référence 50%
            val midY = h * (1f - (0.5 - minY) / range).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1f
            )

            // Ligne de précision
            val path = Path()
            data.forEachIndexed { index, (_, winRate) ->
                val x = w * (index.toFloat() / (data.size - 1))
                val y = h * (1f - ((winRate - minY) / range)).toFloat().coerceIn(0f, 1f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = accentColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            // Points
            data.forEachIndexed { index, (_, winRate) ->
                val x = w * (index.toFloat() / (data.size - 1))
                val y = h * (1f - ((winRate - minY) / range)).toFloat().coerceIn(0f, 1f)
                drawCircle(color = accentColor, radius = 3f, center = Offset(x, y))
            }
        }

        // Labels axes
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("100%", color = Color(0xFF607D8B), fontSize = 9.sp)
            Text("50%", color = Color(0xFF607D8B), fontSize = 9.sp)
            Text("0%", color = Color(0xFF607D8B), fontSize = 9.sp)
        }
    }
}

// ─── CONFIANCE PAR BUCKET ────────────────────────────────────────────────────

@Composable
private fun BucketConfidenceCard(bc: BucketConfidence, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardSurface)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BucketChip(bc.bucket, accentColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Win Rate : ${String.format("%.0f", bc.winRate * 100)}%", color = if (bc.winRate >= 0.5) GreenPositive else RedNegative, fontSize = 13.sp)
                    Text("Moy. : ${String.format("%+.1f", bc.avgPerformance)}%", color = Color.White, fontSize = 13.sp)
                    Text("${bc.count} pronostics", color = Color(0xFF90A4AE), fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
                // Barre de confiance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (bc.confidenceScore / 100f).toFloat())
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                }
                Text("Confiance IA : ${String.format("%.0f", bc.confidenceScore)}/100", color = Color(0xFF90A4AE), fontSize = 10.sp)
            }
        }
    }
}

// ─── STATS PAR STRATÉGIE ─────────────────────────────────────────────────────

@Composable
private fun StrategyStatsCard(stats: StrategyStats, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardSurface)
            .padding(12.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stats.strategyLabel, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${stats.count} pronostics", color = Color(0xFF90A4AE), fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Win Rate", "${String.format("%.0f", stats.winRate * 100)}%",
                    if (stats.winRate >= 0.5) GreenPositive else RedNegative)
                StatItem("Gain moy.", "+${String.format("%.1f", stats.avgGain)}%", GreenPositive)
                StatItem("Perte moy.", "-${String.format("%.1f", stats.avgLoss)}%", RedNegative)
                StatItem("Profit F.", String.format("%.2f", stats.profitFactor), accentColor)
                StatItem("DrawDown", "-${String.format("%.1f", stats.maxDrawdown)}%", OrangeWarning)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF607D8B), fontSize = 9.sp, textAlign = TextAlign.Center)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── STATS PAR SECTEUR ───────────────────────────────────────────────────────

@Composable
private fun SectorStatsCard(ss: SectorStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardSurface)
            .padding(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(ss.sector, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("${ss.count} sig.", color = Color(0xFF90A4AE), fontSize = 11.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                "Win: ${String.format("%.0f", ss.winRate * 100)}%",
                color = if (ss.winRate >= 0.5) GreenPositive else RedNegative,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${String.format("%+.1f", ss.avgPerformance)}%",
                color = if (ss.avgPerformance >= 0) GreenPositive else RedNegative,
                fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }
    }
}

// ─── BANDEAU PRONOSTICS REMARQUABLES ─────────────────────────────────────────

@Composable
private fun RemarkableBanner(items: List<RemarkableForecast>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrangeWarning.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                "⚡ ${items.size} pronostic(s) non joué(s) déjà rentable(s) :",
                color = OrangeWarning, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
            )
            items.take(3).forEach { rf ->
                Text(
                    "• ${rf.forecast.symbol} : +${String.format("%.1f", rf.bestPerformance)}% à ${rf.horizon}",
                    color = Color.White, fontSize = 12.sp
                )
            }
        }
    }
}

// ─── PANNEAU COMPARAISON ─────────────────────────────────────────────────────

@Composable
private fun ComparePanel(comparisons: List<ForecastVsPlayedResult>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Comparaison Pronostic IA vs Signal joué",
                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                "Delta = Performance pronostic IA − Performance signal joué",
                color = Color(0xFF90A4AE), fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
        }
        if (comparisons.isEmpty()) {
            item { InfoCard("Aucune comparaison disponible.\nJouez des signaux pour voir la différence avec les pronostics IA.") }
        } else {
            items(comparisons) { comp ->
                CompareCard(comp)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CompareCard(comp: ForecastVsPlayedResult) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardSurface)
            .padding(12.dp)
    ) {
        Column {
            Text(comp.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pronostic IA", color = Color(0xFF607D8B), fontSize = 10.sp)
                    val fp = comp.forecastPerformance
                    Text(
                        if (fp != null) "${String.format("%+.1f", fp)}%" else "—",
                        color = if (fp != null && fp > 0) GreenPositive else RedNegative,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Signal joué", color = Color(0xFF607D8B), fontSize = 10.sp)
                    val pp = comp.playedPerformance
                    Text(
                        if (pp != null) "${String.format("%+.1f", pp)}%" else "—",
                        color = if (pp != null && pp > 0) GreenPositive else RedNegative,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Delta IA", color = Color(0xFF607D8B), fontSize = 10.sp)
                    val d = comp.delta
                    Text(
                        if (d != null) "${String.format("%+.1f", d)}%" else "—",
                        color = when {
                            d == null -> Color(0xFF90A4AE)
                            d > 0 -> GreenPositive
                            else -> RedNegative
                        },
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ─── PANNEAU STATS GLOBALES ───────────────────────────────────────────────────

@Composable
private fun GlobalStatsPanel(
    statsCT: List<StrategyStats>,
    statsLT: List<StrategyStats>,
    @Suppress("UNUSED_PARAMETER") bucketsCT: List<BucketConfidence>,
    @Suppress("UNUSED_PARAMETER") bucketsLT: List<BucketConfidence>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        if (statsCT.isEmpty() && statsLT.isEmpty()) {
            item {
                InfoCard("Les statistiques s'affichent après ${AiForecastRepository.MIN_FORECASTS_FOR_STATS} pronostics enregistrés par stratégie.\n\nContinuez à utiliser l'application pour accumuler des données.")
            }
        } else {
            if (statsCT.any { it.isReliable }) {
                item {
                    Text("Court Terme — Statistiques", color = BlueCT, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                items(statsCT.filter { it.isReliable }) { StrategyStatsCard(it, BlueCT) }
            }
            if (statsLT.any { it.isReliable }) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Long Terme — Statistiques", color = GreenLT, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                items(statsLT.filter { it.isReliable }) { StrategyStatsCard(it, GreenLT) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── DIALOG EXPORT CSV ───────────────────────────────────────────────────────

@Composable
private fun CsvExportDialog(csvContent: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export CSV", color = Color.White) },
        text = {
            Column {
                Text("Le CSV est prêt — ${csvContent.lines().size} lignes.", color = Color(0xFF90A4AE), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    csvContent.lines().take(3).joinToString("\n"),
                    color = Color(0xFF607D8B), fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Fermer") }
        },
        containerColor = CardSurface
    )
}

// ─── UTILITAIRES ─────────────────────────────────────────────────────────────

@Composable
private fun InfoCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardSurface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color(0xFF90A4AE), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

private fun tabLabel(tab: ForecastTab): String = when (tab) {
    ForecastTab.CT      -> "⚡ Court Terme"
    ForecastTab.LT      -> "🌱 Long Terme"
    ForecastTab.COMPARE -> "🔄 Comparaison"
    ForecastTab.STATS   -> "📊 Statistiques"
}

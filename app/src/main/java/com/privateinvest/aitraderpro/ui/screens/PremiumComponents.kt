package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privateinvest.aitraderpro.repository.PerformancePoint
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import kotlin.math.roundToInt

val DangerRed = Color(0xFFE11D48)
val PremiumBlue = Color(0xFF38BDF8)
val PremiumCard = Color(0xFF132238)
val PremiumCardAlt = Color(0xFF1E314D)
val PremiumMuted = Color(0xFF94A3B8)

@Composable
fun PremiumScreenTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(text = title, color = SoftWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        subtitle?.let {
            Text(text = it, color = Color.LightGray, fontSize = 17.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
fun PremiumCardBox(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBlue,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(accent, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) { Text("AI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SoftWhite)
                    subtitle?.let { Text(it, fontSize = 15.sp, lineHeight = 20.sp, color = Color.LightGray) }
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun PremiumMetric(title: String, value: String, modifier: Modifier = Modifier, accent: Color = PremiumBlue) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Color.LightGray, fontSize = 15.sp)
            Text(value, color = SoftWhite, fontSize = 31.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
            Box(Modifier.padding(top = 10.dp).height(5.dp).fillMaxWidth().background(accent, RoundedCornerShape(999.dp)))
        }
    }
}

@Composable
fun SignalBadge(label: String, modifier: Modifier = Modifier) {
    val color = when (label.uppercase()) {
        "ACHETER", "ACHAT", "BUY_SIMULATION", "OK", "POSITIF", "PRÊT POUR TEST QUOTIDIEN", "VERROUILLÉ" -> Success
        "SURVEILLER", "ATTENTION", "MOYEN", "À TESTER", "OPTIONNEL", "NEUTRE" -> Warning
        "VENDRE", "ERREUR", "SELL_SIMULATION", "NÉGATIF" -> DangerRed
        else -> Color.Gray
    }
    Text(
        text = label,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.background(color, RoundedCornerShape(999.dp)).padding(horizontal = 13.dp, vertical = 8.dp)
    )
}

@Composable
fun ConfidenceBar(label: String, value: Int, modifier: Modifier = Modifier) {
    val progress = (value / 100f).coerceIn(0f, 1f)
    val color = when { value >= 70 -> Success; value >= 50 -> Warning; else -> DangerRed }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 16.sp, color = Color.LightGray)
            Text("$value %", fontSize = 18.sp, color = SoftWhite, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp).padding(top = 7.dp),
            color = color,
            trackColor = PremiumCardAlt
        )
    }
}

@Composable
fun PremiumActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (danger) DangerRed else PremiumBlue)
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun PremiumSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(54.dp), shape = RoundedCornerShape(17.dp)) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SimpleLineChart(
    title: String,
    points: List<PerformancePoint>,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBlue
) {
    PremiumCardBox(title = title, subtitle = "Graphique simple, lisible sur téléphone", accent = accent, modifier = modifier) {
        val safePoints = if (points.size < 2) listOf(PerformancePoint("Départ", 0.0), PerformancePoint("Actuel", 0.0)) else points
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val values = safePoints.map { it.value }
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 1.0
            val range = (max - min).takeIf { it != 0.0 } ?: 1.0
            val stepX = size.width / (safePoints.size - 1).coerceAtLeast(1)
            val path = Path()
            safePoints.forEachIndexed { index, point ->
                val x = index * stepX
                val y = size.height - (((point.value - min) / range).toFloat() * size.height).coerceIn(0f, size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawLine(PremiumCardAlt, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 3f)
            drawPath(path, accent, style = Stroke(width = 7f, cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(safePoints.first().label, color = PremiumMuted, fontSize = 13.sp)
            Text(safePoints.last().label, color = PremiumMuted, fontSize = 13.sp)
        }
    }
}


@Composable
fun TradingModeBanner(mode: String, modifier: Modifier = Modifier) {
    val normalized = mode.uppercase()
    val color = when {
        normalized.contains("REAL") || normalized.contains("REEL") || normalized.contains("RÉEL") -> DangerRed
        normalized.contains("PAPER") -> PremiumBlue
        else -> Success
    }
    val label = when {
        normalized.contains("REAL") || normalized.contains("REEL") || normalized.contains("RÉEL") -> "MODE RÉEL — opérations liées au broker"
        normalized.contains("PAPER") -> "MODE PAPER TRADING — broker en démonstration"
        else -> "MODE SIMULATION — aucun argent réel"
    }
    Text(
        text = label,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
fun SafetyBanner(text: String = "Mode réel verrouillé : simulation uniquement, aucune opération automatique.") {
    PremiumCardBox("Sécurité", text, accent = Warning) {
        Text(
            "Cette application fournit une aide à la décision. Elle ne garantit aucun gain. Toute action doit rester manuelle.",
            color = Color.LightGray,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
    }
}

fun scoreColor(score: Int): Color = when { score >= 75 -> Success; score >= 55 -> Warning; else -> DangerRed }
fun Double.formatPercent(): String = if (this >= 0) "+${String.format(java.util.Locale.FRANCE, "%.2f", this)} %" else "${String.format(java.util.Locale.FRANCE, "%.2f", this)} %"
fun Double.formatPlainPercent(): String = String.format(java.util.Locale.FRANCE, "%.1f %%", this)
fun Int.clampPercent(): Int = coerceIn(0, 100)

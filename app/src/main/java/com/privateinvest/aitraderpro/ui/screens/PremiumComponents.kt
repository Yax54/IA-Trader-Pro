package com.privateinvest.aitraderpro.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import kotlin.math.roundToInt

val DangerRed = Color(0xFFE11D48)
val PremiumBlue = Color(0xFF38BDF8)
val PremiumCard = Color(0xFF132238)
val PremiumCardAlt = Color(0xFF1E314D)

@Composable
fun PremiumScreenTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = title,
            color = SoftWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        subtitle?.let {
            Text(
                text = it,
                color = Color.LightGray,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
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
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(accent, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("AI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = SoftWhite)
                    subtitle?.let { Text(it, fontSize = 14.sp, color = Color.LightGray) }
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun PremiumMetric(title: String, value: String, modifier: Modifier = Modifier, accent: Color = PremiumBlue) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Slate),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.LightGray, fontSize = 14.sp)
            Text(value, color = SoftWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Box(Modifier.padding(top = 8.dp).height(4.dp).fillMaxWidth().background(accent, RoundedCornerShape(999.dp)))
        }
    }
}

@Composable
fun SignalBadge(label: String, modifier: Modifier = Modifier) {
    val color = when (label.uppercase()) {
        "ACHETER", "ACHAT", "BUY_SIMULATION", "OK" -> Success
        "SURVEILLER", "ATTENTION", "MOYEN" -> Warning
        "VENDRE", "ERREUR", "SELL_SIMULATION" -> DangerRed
        else -> Color.Gray
    }
    Text(
        text = label,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
fun ConfidenceBar(label: String, value: Int, modifier: Modifier = Modifier) {
    val progress = (value / 100f).coerceIn(0f, 1f)
    val color = when {
        value >= 70 -> Success
        value >= 50 -> Warning
        else -> DangerRed
    }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 15.sp, color = Color.LightGray)
            Text("$value %", fontSize = 16.sp, color = SoftWhite, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(top = 6.dp),
            color = color,
            trackColor = PremiumCardAlt
        )
    }
}

@Composable
fun PremiumActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (danger) DangerRed else PremiumBlue)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun PremiumSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

fun scoreColor(score: Int): Color = when {
    score >= 75 -> Success
    score >= 55 -> Warning
    else -> DangerRed
}



fun Double.formatPercent(): String = if (this >= 0) "+${String.format(java.util.Locale.FRANCE, "%.2f", this)} %" else "${String.format(java.util.Locale.FRANCE, "%.2f", this)} %"
fun Double.formatPlainPercent(): String = String.format(java.util.Locale.FRANCE, "%.1f %%", this)
fun Int.clampPercent(): Int = coerceIn(0, 100)

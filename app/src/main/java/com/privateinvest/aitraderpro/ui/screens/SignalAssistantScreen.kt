package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.privateinvest.aitraderpro.navigation.SelectedAssetStore
import com.privateinvest.aitraderpro.repository.BeginnerExplanation
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AssetDetailViewModel

@Composable
fun SignalAssistantScreen() {
    val viewModel: AssetDetailViewModel = viewModel(factory = AITraderViewModelFactory)
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Pourquoi ${SelectedAssetStore.currentName} est ${detail?.scoring?.signal?.name ?: "analysé"} ?")

        if (detail == null) {
            Card(colors = CardDefaults.cardColors(containerColor = Slate)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Impossible d'expliquer le signal sans données de marché réelles.")
                }
            }
        } else {
            val currentDetail = detail!!
            val beginner = BeginnerExplanation.fromAssetDetail(currentDetail)
            val snapshot = currentDetail.technicalSnapshot
            val explanation = currentDetail.scoring.explanation

            PremiumAssistantCard(
                title = "Décision IA",
                accent = Success,
                content = "${beginner.actionLabel} · ${beginner.headline}"
            )

            PremiumAssistantCard(
                title = "RSI",
                accent = indicatorColor(snapshot.rsi, 50.0, 70.0),
                content = "Valeur ${String.format("%.2f", snapshot.rsi)} · ${BeginnerExplanation.beginnerLabelForIndicator("RSI", snapshot.rsi)}"
            )

            PremiumAssistantCard(
                title = "MACD",
                accent = if (snapshot.macd > snapshot.macdSignal) Success else Warning,
                content = if (snapshot.macd > snapshot.macdSignal) {
                    "MACD haussier : ${String.format("%.4f", snapshot.macd)} au-dessus du signal ${String.format("%.4f", snapshot.macdSignal)}."
                } else {
                    "MACD prudent : ${String.format("%.4f", snapshot.macd)} n'est pas encore au-dessus du signal ${String.format("%.4f", snapshot.macdSignal)}."
                }
            )

            PremiumAssistantCard(
                title = "EMA",
                accent = if (snapshot.ema20 > snapshot.ema50 && snapshot.ema50 > snapshot.ema200) Success else Warning,
                content = buildString {
                    append("EMA20 ${formatPrice(snapshot.ema20)}")
                    append(" · EMA50 ${formatPrice(snapshot.ema50)}")
                    append(" · EMA200 ${formatPrice(snapshot.ema200)}. ")
                    append(
                        if (snapshot.ema20 > snapshot.ema50) {
                            "La tendance courte est au-dessus de la tendance intermédiaire."
                        } else {
                            "La tendance courte n'est pas encore au-dessus de la tendance intermédiaire."
                        }
                    )
                }
            )

            PremiumAssistantCard(
                title = "Historique similaire",
                accent = if (explanation.historicalWinRate >= 60.0) Success else Warning,
                content = if (explanation.similarConfigurations > 0) {
                    "${explanation.similarConfigurations} configurations similaires · ${String.format("%.1f", explanation.historicalWinRate)} % de réussite · gain moyen ${String.format("%.2f", explanation.averageGain)} % · impact max ${explanation.maxImpactPoints} points."
                } else {
                    "Pas encore assez de cas comparables. Les futures validations alimenteront cet historique explicable."
                }
            )

            PremiumAssistantCard(
                title = "Synthèse pédagogique",
                accent = Color(0xFF60A5FA),
                content = beginner.whyText.joinToString(separator = " ")
            )
        }
    }
}

@Composable
private fun PremiumAssistantCard(title: String, accent: Color, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = " ",
                    modifier = Modifier
                        .background(accent, RoundedCornerShape(99.dp))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = SoftWhite,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

private fun indicatorColor(value: Double, min: Double, max: Double): Color {
    return if (value in min..max) Success else Warning
}

private fun formatPrice(value: Double): String = String.format("%.2f", value)

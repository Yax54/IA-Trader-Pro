package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.privateinvest.aitraderpro.data.model.SignalExplanation
import com.privateinvest.aitraderpro.ui.theme.Slate

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun InfoCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Slate),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun RowLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value)
    }
}

@Composable
fun SignalExplanation(explanation: SignalExplanation, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 10.dp)) {
        Text("Justification du signal", style = MaterialTheme.typography.titleSmall)
        explanation.reasons.ifEmpty { listOf("Aucune justification disponible") }.forEach { reason ->
            Text(
                text = "• $reason",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (explanation.similarConfigurations > 0) {
            Text(
                text = "Configurations similaires : ${explanation.similarConfigurations}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Réussite historique : ${String.format("%.2f", explanation.historicalWinRate)} %",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Gain moyen : ${String.format("%.2f", explanation.averageGain)} %",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Perte moyenne : ${String.format("%.2f", explanation.averageLoss)} %",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Impact maximum : ${if (explanation.maxImpactPoints >= 0) "+" else ""}${explanation.maxImpactPoints} points",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SignalExplanation(reasons: List<String>, modifier: Modifier = Modifier) {
    SignalExplanation(explanation = SignalExplanation(reasons = reasons), modifier = modifier)
}

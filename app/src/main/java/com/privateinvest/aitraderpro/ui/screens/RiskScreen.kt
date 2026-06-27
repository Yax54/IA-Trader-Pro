package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.RiskViewModel

@Composable
fun RiskScreen() {
    val viewModel: RiskViewModel = viewModel(factory = AITraderViewModelFactory)
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionTitle("Gestion du risque")
        Card(colors = CardDefaults.cardColors(containerColor = Slate)) {
            Column(Modifier.padding(16.dp)) {
                rules.forEach { rule ->
                    RowLine(rule.title, rule.value)
                    Text(
                        text = beginnerRiskExplanation(rule.title),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}

private fun beginnerRiskExplanation(title: String): String {
    return when {
        title.contains("position", ignoreCase = true) -> "Cette règle évite de mettre trop d'argent sur une seule idée."
        title.contains("daily", ignoreCase = true) || title.contains("journ", ignoreCase = true) -> "Cette limite protège votre capital si la journée se passe mal."
        title.contains("open", ignoreCase = true) || title.contains("ouverte", ignoreCase = true) -> "Trop de positions à la fois rendent le portefeuille difficile à piloter."
        title.contains("mode", ignoreCase = true) -> "Le mode réel demande plus de prudence que le mode simulation."
        title.contains("validation", ignoreCase = true) -> "Aucune décision ne doit partir seule : vous gardez toujours le dernier mot."
        else -> "Cette règle sert à garder une gestion du risque simple, lisible et disciplinée."
    }
}

package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.RiskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskScreen(onBack: () -> Unit = {}) {
    val viewModel: RiskViewModel = viewModel(factory = AITraderViewModelFactory)
    val rules by viewModel.rules.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion du risque", color = SoftWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = SoftWhite)
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumScreenTitle(
                title = "Gestion du risque",
                subtitle = "Règles de protection de votre capital"
            )
        }

        // Règles affichées individuellement avec niveau de risque visuel
        items(rules) { rule ->
            RiskRuleCard(title = rule.title, value = rule.value)
        }

        item { SafetyBanner() }
    }
    } // fin Scaffold
}

/** Carte pour une règle de risque avec explication et badge niveau */
@Composable
private fun RiskRuleCard(title: String, value: String) {
    val (riskLevel, accent) = riskLevelFor(title)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // En-tête : titre + badge niveau
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = riskLevel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = androidx.compose.ui.Modifier
                        .background(accent, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            // Valeur de la règle
            Text(
                value,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            // Explication pédagogique
            Text(
                beginnerRiskExplanation(title),
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
}

/** Détermine le niveau de risque et la couleur d'accent selon le titre de la règle */
private fun riskLevelFor(title: String): Pair<String, androidx.compose.ui.graphics.Color> {
    return when {
        title.contains("validation", ignoreCase = true) || title.contains("mode", ignoreCase = true) ->
            "Protection" to Success
        title.contains("position", ignoreCase = true) || title.contains("max", ignoreCase = true) ->
            "Limite" to Warning
        title.contains("daily", ignoreCase = true) || title.contains("journ", ignoreCase = true) ->
            "Quota" to Warning
        title.contains("open", ignoreCase = true) || title.contains("ouverte", ignoreCase = true) ->
            "Contrôle" to PremiumBlue
        else -> "Règle" to PremiumBlue
    }
}

private fun beginnerRiskExplanation(title: String): String {
    return when {
        title.contains("position", ignoreCase = true) -> "Cette règle évite de mettre trop d'argent sur une seule idée."
        title.contains("daily", ignoreCase = true) || title.contains("journ", ignoreCase = true) -> "Cette limite protège votre capital si la journée se passe mal."
        title.contains("open", ignoreCase = true) || title.contains("ouverte", ignoreCase = true) -> "Trop de positions à la fois rendent le portefeuille difficile à piloter."
        title.contains("mode", ignoreCase = true) -> "Le mode réel demande plus de prudence que le mode simulation."
        title.contains("validation", ignoreCase = true) -> "Aucune décision ne part seule : vous gardez toujours le dernier mot."
        else -> "Cette règle sert à garder une gestion du risque simple, lisible et disciplinée."
    }
}

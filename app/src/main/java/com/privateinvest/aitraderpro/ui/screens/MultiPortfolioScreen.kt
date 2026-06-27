package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.viewmodel.MultiPortfolioViewModel

@Composable
fun MultiPortfolioScreen() {
    val viewModel: MultiPortfolioViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Portefeuilles", "Simulation et réel restent séparés") }
        items(state.items) { portfolio ->
            PremiumCardBox(portfolio.name, portfolio.type, accent = if (portfolio.active) Success else PremiumBlue) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(portfolio.value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    SignalBadge(if (portfolio.active) "Actif" else "Disponible")
                }
                RowLine("Risque", portfolio.risk)
                Text("Les portefeuilles sont séparés : simulation, paper trading et réel ne se mélangent pas.", color = Color.LightGray, fontSize = 15.sp)
            }
        }
        item { SafetyBanner("Le portefeuille réel est uniquement synchronisé avec le broker. L'application ne détient jamais ton argent.") }
    }
}

package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.repository.ControlModuleStatus
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.ControlCenterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCenterScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val viewModel: ControlCenterViewModel = viewModel(factory = AITraderViewModelFactory)
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centre de Contrôle Intelligent", fontSize = 21.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ui.error?.let { item { PremiumCardBox("Erreur Cockpit IA", it, accent = DangerRed) { Text(it, color = Color.White, fontSize = 16.sp) } } }
            if (ui.loading) {
                item { PremiumCardBox("Contrôle en cours", "Analyse des modules critiques...") { Text("Chargement du Cockpit IA.", color = Color.LightGray, fontSize = 16.sp) } }
            }
            ui.state?.let { state ->
                item {
                    PremiumScreenTitle("Centre de Contrôle Intelligent", "Cockpit IA — supervision, pas trading")
                    PremiumCardBox("⭐ Indice Global IA", state.globalMessage, accent = scoreColor(state.globalScore)) {
                        PremiumMetric("Indice global", "${state.globalScore} %", accent = scoreColor(state.globalScore))
                        Text(state.globalLabel, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                        RowLine("Dernier contrôle", state.checkedAtLabel)
                    }
                }
                items(state.modules) { module ->
                    ControlModuleCard(module = module, onNavigate = onNavigate)
                }
                item {
                    PremiumCardBox("📋 Journal des événements", "Dernières actions techniques") {
                        if (state.latestEvents.isEmpty()) {
                            Text("Aucun événement récent.", color = Color.LightGray, fontSize = 16.sp)
                        } else {
                            state.latestEvents.forEach { Text("• $it", color = Color.LightGray, fontSize = 15.sp, lineHeight = 21.sp) }
                        }
                    }
                }
                item { SafetyBanner("Le Cockpit IA indique si la plateforme est fiable. Il ne remplace pas ta décision d'investissement.") }
            }
        }
    }
}

@Composable
private fun ControlModuleCard(module: ControlModuleStatus, onNavigate: (String) -> Unit) {
    PremiumCardBox(
        title = "${module.icon} ${module.title}",
        subtitle = module.summary,
        accent = if (module.blocking) DangerRed else scoreColor(module.score),
        modifier = if (module.route != null) Modifier.clickable { onNavigate(module.route) } else Modifier
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumMetric("Score", "${module.score} %", modifier = Modifier.weight(1f), accent = scoreColor(module.score))
            PremiumMetric("État", module.status, modifier = Modifier.weight(1f), accent = scoreColor(module.score))
        }
        module.details.forEach { (label, value) -> RowLine(label, value) }
        module.route?.let { Text("Touchez la carte pour ouvrir le détail.", color = PremiumMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

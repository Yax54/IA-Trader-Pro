package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import com.privateinvest.aitraderpro.repository.ValidationCheckItem
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.RealValidationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealValidationScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val viewModel: RealValidationViewModel = viewModel(factory = AITraderViewModelFactory)
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Validation Réelle", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { PremiumScreenTitle("Mode Validation Réelle", "Check avant d'utiliser un petit capital réel") }
            ui.message?.let { item { PremiumCardBox("Information", it) { Text(it, color = Color.LightGray, fontSize = 16.sp) } } }
            ui.error?.let { item { PremiumCardBox("Erreur", it, accent = DangerRed) { Text(it, color = Color.White, fontSize = 16.sp) } } }
            if (ui.loading) {
                item { PremiumCardBox("Contrôle en cours", "Vérification broker, données et sécurité...") { Text("Analyse des points critiques.", color = Color.LightGray, fontSize = 16.sp) } }
            }
            ui.state?.let { state ->
                item {
                    PremiumCardBox("⚡ Résultat", state.statusLabel, accent = if (state.canUseRealMode) scoreColor(state.score) else DangerRed) {
                        PremiumMetric("Score préparation", "${state.score} %", accent = scoreColor(state.score))
                        RowLine("Décision", state.statusLabel)
                        RowLine("Dernier contrôle", state.checkedAtLabel)
                        Text(
                            if (state.canUseRealMode) "Aucun blocage dur. Commence uniquement avec une petite somme et validation manuelle."
                            else "Le réel reste bloqué tant qu'un point critique n'est pas corrigé.",
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
                if (state.hardBlocks.isNotEmpty()) {
                    item { PremiumScreenTitle("Blocages réels", "Ces points empêchent un ordre réel sécurisé") }
                    items(state.hardBlocks) { CheckCard(it) }
                } else {
                    item { PremiumCardBox("Blocages réels", "Aucun blocage dur détecté", accent = com.privateinvest.aitraderpro.ui.theme.Success) { Text("Le réel peut être envisagé avec prudence.", color = Color.LightGray, fontSize = 16.sp) } }
                }
                item { PremiumScreenTitle("Diagnostic Broker", "Contrôles liés au compte et aux flux") }
                items(state.brokerDiagnostic) { CheckCard(it) }
                if (state.warnings.isNotEmpty()) {
                    item { PremiumScreenTitle("Avertissements", "Ces points ne bloquent pas le trading") }
                    items(state.warnings) { CheckCard(it) }
                }
                item {
                    PremiumCardBox("📦 Sauvegarde", "Recommandée, jamais bloquante") {
                        RowLine("Dernière sauvegarde", state.latestBackupLabel)
                        Text("La sauvegarde n'est pas un critère de blocage. Elle reste ta responsabilité, comme dans Pronostic Hippique.", color = Color.LightGray, fontSize = 16.sp, lineHeight = 22.sp)
                        PremiumActionButton("Créer sauvegarde locale de sécurité", onClick = viewModel::createSafetyBackup, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                        PremiumSecondaryButton("Ouvrir Sauvegarde", onClick = { onNavigate("backup") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
                item {
                    PremiumCardBox("Accès rapide", "Corriger un point bloquant") {
                        PremiumSecondaryButton("Centre Données Marché", onClick = { onNavigate("market_data_center") }, modifier = Modifier.fillMaxWidth())
                        PremiumSecondaryButton("Centre Broker", onClick = { onNavigate("broker_assistant") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        PremiumSecondaryButton("Centre Sécurité", onClick = { onNavigate("security_center") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
                item { SafetyBanner("Même si la validation est verte, aucun ordre réel ne part sans confirmation manuelle et PIN/empreinte.") }
            }
        }
    }
}

@Composable
private fun CheckCard(item: ValidationCheckItem) {
    val accent = when {
        item.blocking -> DangerRed
        item.status == "OK" -> com.privateinvest.aitraderpro.ui.theme.Success
        else -> com.privateinvest.aitraderpro.ui.theme.Warning
    }
    PremiumCardBox(item.title, item.status, accent = accent) {
        Text(item.detail, color = Color.LightGray, fontSize = 16.sp, lineHeight = 22.sp)
    }
}

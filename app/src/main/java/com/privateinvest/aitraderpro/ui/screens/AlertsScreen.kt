package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AlertsViewModel

@Composable
fun AlertsScreen() {
    val viewModel: AlertsViewModel = viewModel(factory = AITraderViewModelFactory)
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    var alertToConfirm by remember { mutableStateOf<AssetSignal?>(null) }

    alertToConfirm?.let { alert ->
        AlertDialog(
            onDismissRequest = { alertToConfirm = null },
            title = { Text("Confirmer l'achat simulé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Valider le signal sur ${alert.name} (${alert.symbol}) ?")
                    Text("Cette action enregistre le signal et démarre le suivi J+1 / J+7 / J+30.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }
            },
            confirmButton = { Button(onClick = { viewModel.confirmAlert(alert.symbol); alertToConfirm = null }) { Text("Valider") } },
            dismissButton = { TextButton(onClick = { alertToConfirm = null }) { Text("Annuler") } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Alertes intelligentes", "Toutes les opportunités actives, pas seulement la première") }
        if (activeAlerts.isEmpty()) {
            item {
                PremiumCardBox("Aucune alerte active", "L'IA surveille les marchés") {
                    Text("Quand un signal fort est détecté, il apparaîtra ici.", color = Color.LightGray)
                }
            }
        } else {
            items(activeAlerts) { alert ->
                PremiumCardBox("${alert.symbol} · ${alert.name}", alert.explanation.firstOrNull() ?: "Signal IA actif", accent = scoreColor(alert.score)) {
                    SignalBadge(alert.action.name)
                    Spacer(Modifier.height(8.dp))
                    ConfidenceBar("Confiance IA", alert.confidence.clampPercent())
                    RowLine("Score", "${alert.score}/100")
                    RowLine("Risque", alert.risk)
                    Spacer(Modifier.height(10.dp))
                    PremiumActionButton("Valider l'achat en simulation", onClick = { alertToConfirm = alert }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        statusMessage?.let { msg -> item { PremiumCardBox("Statut", msg, accent = Warning) { } } }
        item { PremiumScreenTitle("Historique notifications") }
        if (history.isEmpty()) {
            item { PremiumCardBox("Aucun historique") { Text("Aucune notification enregistrée pour le moment.", color = Color.LightGray) } }
        }
        items(history) { item ->
            PremiumCardBox(item.title, item.message, accent = if (item.level == "HIGH") DangerRed else PremiumBlue) {
                Text("${item.createdAtLabel} · ${item.status}", color = Color.LightGray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

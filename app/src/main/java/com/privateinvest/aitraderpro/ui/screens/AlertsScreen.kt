package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.privateinvest.aitraderpro.ui.theme.Slate
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.AlertsViewModel

@Composable
fun AlertsScreen() {
    val viewModel: AlertsViewModel = viewModel(factory = AITraderViewModelFactory)
    val alert by viewModel.alert.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // R-04 : état du dialog de confirmation d'achat
    var showConfirmDialog by remember { mutableStateOf(false) }

    // R-04 : dialog de confirmation
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmer l'achat simulé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    alert?.let {
                        Text("Valider le signal sur ${it.name} (${it.symbol}) ?")
                        Text(
                            "Cette action enregistre le signal et démarre le suivi J+1 / J+7 / J+30.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmAlert()
                        showConfirmDialog = false
                    }
                ) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Annuler") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PremiumScreenTitle("Alertes intelligentes", subtitle = "Notifications & signaux IA") }
        item {
            if (alert == null) {
                PremiumCardBox(title = "Aucune alerte active") {
                    Text(
                        "L'IA surveille les marchés. Quand un signal fort est détecté, il apparaîtra ici.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val currentAlert = alert!!
                PremiumCardBox(title = "Opportunité détectée", subtitle = "${currentAlert.name} · ${currentAlert.symbol}") {
                    // Confiance avec barre colorée
                    ConfidenceBar("Confiance IA", currentAlert.confidence)
                    Spacer(Modifier.height(8.dp))
                    SignalBadge(currentAlert.action.name)
                    Spacer(Modifier.height(8.dp))
                    SignalExplanation(currentAlert.explanation)
                    Spacer(Modifier.height(12.dp))
                    // R-04 : bouton ouvre le dialog au lieu d'agir directement
                    PremiumActionButton(
                        text = "Valider l'achat en simulation",
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        statusMessage?.let { msg ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(16.dp)) {
                    Text(
                        msg,
                        modifier = Modifier.padding(16.dp),
                        color = Warning,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { PremiumScreenTitle("Historique notifications") }
        if (history.isEmpty()) {
            item {
                PremiumCardBox(title = "Aucun historique") {
                    Text("Aucune notification enregistrée pour le moment.", color = Color.LightGray)
                }
            }
        }
        items(history) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    item.symbol?.let { Text("Actif : $it") }
                    Text(item.message)
                    RowLine("Niveau", item.level)
                    RowLine("Statut", item.status)
                    RowLine("Date", item.createdAtLabel)
                }
            }
        }
    }
}

package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Alertes intelligentes & notifications") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alert == null) {
                        Text("Aucune alerte active")
                    } else {
                        Text("🚀 Opportunité détectée", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${alert!!.name} (${alert!!.symbol})")
                        Text(
                            text = "Confiance ${alert!!.confidence}%",
                            modifier = Modifier
                                .background(Success, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.White
                        )
                        Text("Signal : ${alert!!.action.name}")
                        SignalExplanation(alert!!.explanation)
                        Button(onClick = viewModel::confirmAlert, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Valider l'achat en simulation")
                        }
                    }
                    statusMessage?.let {
                        Text(it, modifier = Modifier.padding(top = 8.dp), color = Warning)
                    }
                }
            }
        }

        item { SectionTitle("Historique notifications") }
        if (history.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Slate)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Aucune notification enregistrée pour le moment.")
                    }
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

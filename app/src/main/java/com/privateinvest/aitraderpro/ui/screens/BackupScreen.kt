package com.privateinvest.aitraderpro.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.viewmodel.BackupViewModel

@Composable
fun BackupScreen() {
    val viewModel: BackupViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PremiumScreenTitle("Sauvegarde", "Exporter la mémoire IA et le rapport de suivi") }
        state.summary?.let { summary ->
            item {
                PremiumCardBox("Résumé sauvegarde", "Format JSON à finaliser côté fichier") {
                    RowLine("Signaux", summary.signalCount.toString())
                    RowLine("Mémoire IA", summary.memoryCount.toString())
                    RowLine("Trades simulation", summary.tradeCount.toString())
                    RowLine("Favoris", summary.favoriteCount.toString())
                    Text(summary.jsonPreview, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        item {
            PremiumCardBox("Export rapport", "Partage texte immédiat") {
                PremiumActionButton("Partager le rapport", onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Rapport AI Trader Pro")
                        putExtra(Intent.EXTRA_TEXT, state.report)
                    }
                    context.startActivity(Intent.createChooser(intent, "Partager le rapport"))
                }, modifier = Modifier.fillMaxWidth())
                Text("L’export fichier JSON complet sera branché ensuite sur le stockage Android.", modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

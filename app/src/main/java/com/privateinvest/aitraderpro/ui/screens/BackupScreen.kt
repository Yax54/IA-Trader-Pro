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

    fun shareText(title: String, text: String, mime: String = "text/plain") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Sauvegarde", "Exporter la mémoire IA, les poids et la simulation") }
        item { SafetyBanner("Avant chaque grosse modification, fais une sauvegarde de la mémoire IA.") }

        state.summary?.let { summary ->
            item {
                PremiumCardBox("Résumé sauvegarde", "Format JSON versionné") {
                    RowLine("Signaux", summary.signalCount.toString())
                    RowLine("Mémoire IA", summary.memoryCount.toString())
                    RowLine("Trades simulation", summary.tradeCount.toString())
                    RowLine("Favoris", summary.favoriteCount.toString())
                    Text("Aperçu JSON :", modifier = Modifier.padding(top = 12.dp))
                    Text(summary.jsonPreview, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        item {
            PremiumCardBox("Export sauvegarde", "Partage immédiat") {
                PremiumActionButton("Partager backup JSON", onClick = {
                    shareText("backup_ai_trader.json", state.backupJson, "application/json")
                }, modifier = Modifier.fillMaxWidth())
                PremiumSecondaryButton("Partager rapport texte", onClick = {
                    shareText("Rapport AI Trader Pro", state.report)
                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("Restauration complète : à valider dans un lot séparé avant d'écraser la base locale.", modifier = Modifier.padding(top = 10.dp))
            }
        }

        item {
            PremiumCardBox("Sécurité sauvegarde", "Contrôle d'intégrité") {
                Text("Le JSON contient un compteur de signaux, mémoire, trades, favoris et poids pour vérifier que l'export n'est pas vide.")
                Text("Sécurité : le PIN, son hash et son sel ne doivent jamais être exportés. Après restauration sur un autre téléphone, un nouveau PIN devra être créé.", modifier = Modifier.padding(top = 8.dp))
                Text("Conseil : conserve une sauvegarde avant chaque nouvelle version APK.", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

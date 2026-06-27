package com.privateinvest.aitraderpro.ui.screens

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
import com.privateinvest.aitraderpro.export.ExportFormat
import com.privateinvest.aitraderpro.export.PremiumExportManager
import com.privateinvest.aitraderpro.viewmodel.ExportCenterViewModel

@Composable
fun ExportCenterScreen() {
    val viewModel: ExportCenterViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Exports", "PNG/PDF pleine page, même si l'écran est scrollable") }
        item { SafetyBanner("Les exports sont générés depuis toutes les données du rapport, pas seulement depuis la partie visible de l'écran.") }

        item {
            PremiumCardBox("Rapport complet", "Audit, simulation, diagnostic, journal et favoris") {
                Text("Ce moteur crée une page complète longue et lisible, proche de l'export de Pronostic Hippique.")
                PremiumActionButton(
                    text = "Exporter PNG pleine page",
                    onClick = { state.report?.let { PremiumExportManager.exportAndShare(context, it, ExportFormat.PNG) } },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                PremiumSecondaryButton(
                    text = "Exporter PDF complet",
                    onClick = { state.report?.let { PremiumExportManager.exportAndShare(context, it, ExportFormat.PDF) } },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        item {
            PremiumCardBox("Type de rapport", "Préparer un export spécialisé") {
                PremiumSecondaryButton("Audit IA", { viewModel.loadReport("audit") }, Modifier.fillMaxWidth())
                PremiumSecondaryButton("Portefeuille", { viewModel.loadReport("portefeuille") }, Modifier.fillMaxWidth().padding(top = 8.dp))
                PremiumSecondaryButton("Journal IA", { viewModel.loadReport("journal") }, Modifier.fillMaxWidth().padding(top = 8.dp))
                PremiumSecondaryButton("Diagnostic", { viewModel.loadReport("diagnostic") }, Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }

        item {
            PremiumCardBox("Prévisualisation", state.report?.title ?: "Rapport non chargé") {
                Text(state.report?.subtitle ?: state.message ?: "Chargement...")
                Text("Sections : ${state.report?.sections?.size ?: 0}", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

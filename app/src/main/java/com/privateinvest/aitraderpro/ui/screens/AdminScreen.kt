package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.SettingsViewModel

@Composable
fun AdminScreen() {
    val settingsViewModel: SettingsViewModel = viewModel(factory = AITraderViewModelFactory)
    val beginnerModeEnabled by settingsViewModel.beginnerModeEnabled.collectAsStateWithLifecycle()
    val expertModeEnabled by settingsViewModel.expertModeEnabled.collectAsStateWithLifecycle()

    // R-14 : état du dialog de confirmation de changement de mode
    var pendingMode by remember { mutableStateOf<String?>(null) } // "debutant" ou "expert"

    // R-14 : dialog de confirmation avant changement de mode
    pendingMode?.let { mode ->
        val modeLabel = if (mode == "debutant") "Débutant" else "Expert"
        AlertDialog(
            onDismissRequest = { pendingMode = null },
            title = { Text("Changer de mode ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Passer en mode $modeLabel ?")
                    Text(
                        "Cela changera l'affichage de toute l'application. Le mode Expert déverrouille les indicateurs avancés.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (mode) {
                            "debutant" -> settingsViewModel.setBeginnerModeEnabled(true)
                            "expert" -> settingsViewModel.setExpertModeEnabled(true)
                        }
                        pendingMode = null
                    }
                ) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMode = null }) { Text("Annuler") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumScreenTitle(
                title = "Paramètres",
                subtitle = "Configuration du mode d'utilisation"
            )
        }

        item {
            PremiumCardBox(title = "Mode d'utilisation") {
                Text(
                    "Le mode Débutant simplifie l'affichage et active les résumés pédagogiques. Le mode Expert déverrouille tous les indicateurs.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    // R-14 : boutons sans emojis, ouvrent le dialog
                    PremiumActionButton(
                        text = if (beginnerModeEnabled) "Débutant (actif)" else "Activer Débutant",
                        onClick = { if (!beginnerModeEnabled) pendingMode = "debutant" },
                        modifier = Modifier.weight(1f)
                    )
                    PremiumSecondaryButton(
                        text = if (expertModeEnabled) "Expert (actif)" else "Activer Expert",
                        onClick = { if (!expertModeEnabled) pendingMode = "expert" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            PremiumCardBox(title = "État actuel", accent = if (beginnerModeEnabled) Success else PremiumBlue) {
                ModeStatusRow("Mode débutant", if (beginnerModeEnabled) "Actif" else "Inactif",
                    if (beginnerModeEnabled) Success else Warning)
                Spacer(Modifier.height(6.dp))
                ModeStatusRow("Mode expert", if (expertModeEnabled) "Actif" else "Inactif",
                    if (expertModeEnabled) Success else Warning)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rappel : même en mode expert, l'IA ne prend jamais de décision automatique à votre place.",
                    color = Warning,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item { SafetyBanner() }
    }
}

@Composable
private fun ModeStatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SoftWhite, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(color, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

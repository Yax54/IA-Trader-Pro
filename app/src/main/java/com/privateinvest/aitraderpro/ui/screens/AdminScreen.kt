package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Paramètres & modes")

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mode d'utilisation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Le mode débutant simplifie l'affichage, active les résumés pédagogiques et replie les indicateurs avancés par défaut.",
                    color = SoftWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { settingsViewModel.setBeginnerModeEnabled(true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (beginnerModeEnabled) "🟢 Débutant actif" else "Activer débutant")
                    }
                    OutlinedButton(
                        onClick = { settingsViewModel.setExpertModeEnabled(true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (expertModeEnabled) "⚪ Expert actif" else "Activer expert")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("État actuel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ModeStatusRow("Mode débutant", if (beginnerModeEnabled) "ON" else "OFF", if (beginnerModeEnabled) Success else Warning)
                ModeStatusRow("Mode expert", if (expertModeEnabled) "ON" else "OFF", if (expertModeEnabled) Success else Warning)
                Text(
                    "Rappel : même en mode débutant, l'IA ne prend jamais de décision automatique à votre place.",
                    color = Warning,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ModeStatusRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SoftWhite)
        Text(
            text = value,
            color = Color.White,
            modifier = Modifier
                .background(color, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

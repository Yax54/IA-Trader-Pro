package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.repository.OperationMode
import com.privateinvest.aitraderpro.repository.OperationModeRepository
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.OperationModeViewModel
import com.privateinvest.aitraderpro.ui.theme.Success

@Composable
fun OperationModeScreen(onBack: () -> Unit) {
    val viewModel: OperationModeViewModel = viewModel(factory = AITraderViewModelFactory)
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumScreenTitle("Synchronisation & Surveillance", "Choisis comment l’application lit les flux en direct ou en arrière-plan.")
            PremiumSecondaryButton("Retour", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }

        item {
            PremiumCardBox("Mode de fonctionnement", "Le raccourci du Dashboard ouvre cet écran") {
                OperationMode.values().forEach { mode ->
                    ModeChoiceRow(
                        label = mode.shortLabel,
                        description = mode.description,
                        selected = settings.mode == mode,
                        onClick = { viewModel.setMode(mode) }
                    )
                }
            }
        }

        item {
            PremiumCardBox("Fréquence LIVE", "Quand l’application est ouverte") {
                OptionRows(OperationModeRepository.liveOptions, settings.liveFrequencyLabel) { viewModel.setLiveFrequency(it) }
                Text("Conseil : 30 s est le meilleur compromis entre réactivité, batterie et quotas API.", color = Color.LightGray, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        item {
            PremiumCardBox("Fréquence SURVEILLANCE", "Quand l’application est fermée ou en arrière-plan") {
                OptionRows(OperationModeRepository.surveillanceOptions, settings.surveillanceFrequencyLabel) { viewModel.setSurveillanceFrequency(it) }
                Text("Android peut différer les tâches de fond pour économiser la batterie. Les notifications restent réservées aux alertes importantes.", color = Color.LightGray, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        item {
            PremiumCardBox("Notifications et batterie") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notifications de surveillance", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Alertes quand une condition importante est détectée.", color = Color.LightGray, fontSize = 14.sp)
                    }
                    Switch(checked = settings.notificationsEnabled, onCheckedChange = { viewModel.setNotifications(it) })
                }
                OptionRows(OperationModeRepository.batteryOptions, settings.batteryMode) { viewModel.setBatteryMode(it) }
            }
        }

        item {
            SafetyBanner("Le mode LIVE ne tourne pas en continu quand l’application est fermée. En arrière-plan, l’application utilise une surveillance périodique plus économe.")
        }
    }
}

@Composable
private fun ModeChoiceRow(label: String, description: String, selected: Boolean, onClick: () -> Unit) {
    PremiumCardBox(
        title = label,
        subtitle = description,
        accent = if (selected) Success else PremiumBlue,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        PremiumActionButton(if (selected) "Actif" else "Activer", onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun OptionRows(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumSecondaryButton(
                    text = if (option == selected) "✓ $option" else option,
                    onClick = { onSelected(option) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

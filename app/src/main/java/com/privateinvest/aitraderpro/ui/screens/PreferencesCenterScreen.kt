package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import com.privateinvest.aitraderpro.viewmodel.PreferencesCenterViewModel

@Composable
fun PreferencesCenterScreen() {
    val viewModel: PreferencesCenterViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Préférences", "Tous les réglages importants au même endroit") }
        if (prefs != null) {
            item {
                PremiumCardBox("Profil utilisateur", "Le PIN ne bloque jamais l'ouverture de l'application") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mode débutant", color = Color.LightGray, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = prefs.beginnerMode, onCheckedChange = { viewModel.setBeginnerMode(it) })
                    }
                    Text(if (prefs.beginnerMode) "Explications simples et détails techniques repliés." else "Mode expert : indicateurs et détails visibles.", color = Color.LightGray, fontSize = 15.sp)
                }
            }
            item {
                PremiumCardBox("Affichage", "Lisibilité prioritaire") {
                    RowLine("Thème", prefs.theme)
                    RowLine("Taille texte", prefs.fontSize)
                    RowLine("Langue", prefs.language)
                    RowLine("Devise", prefs.currency)
                }
            }
            item {
                PremiumCardBox("Notifications et synchronisation") {
                    RowLine("Notifications", prefs.notifications)
                    RowLine("Rafraîchissement", prefs.syncFrequency)
                    RowLine("Broker", "Simulation par défaut, réel protégé par PIN")
                    RowLine("Sécurité", "PIN/empreinte uniquement pour actions sensibles")
                }
            }
            state.message?.let { item { SignalBadge(it) } }
        }
    }
}

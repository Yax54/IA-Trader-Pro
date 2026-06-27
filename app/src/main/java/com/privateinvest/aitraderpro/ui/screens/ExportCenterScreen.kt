package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportCenterScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PremiumScreenTitle("Exports", "Préparation capture intégrale PNG/PDF") }
        item {
            PremiumCardBox("Export image", "À brancher sur capture complète de page") {
                Text("Écrans ciblés : Accueil, Fiche actif, Portefeuille, Audit IA, Journal IA, Calendrier IA, Test IA.")
                Text("Règle UX : bouton Exporter visible, texte lisible, image complète même si la page scrolle.", modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            PremiumCardBox("Export PDF", "Rapports lisibles") {
                Text("PDF ciblés : Audit IA, Portefeuille, Journal IA, Historique simulation.")
                Text("Ce centre sert de point d’entrée. L’implémentation fichier natif peut être ajoutée sans toucher au moteur IA.", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

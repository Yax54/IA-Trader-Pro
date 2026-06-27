package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val items = listOf(
        "watchlist" to "Watchlist",
        "journal_ia" to "Journal IA",
        "calendar_ia" to "Calendrier IA",
        "favorites" to "Favoris",
        "audit" to "Audit IA",
        "risk" to "Gestion du risque",
        "health" to "Santé IA",
        "goals" to "Objectifs",
        "backup" to "Sauvegarde",
        "exports" to "Exports PNG/PDF",
        "admin" to "Paramètres"
    )
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PremiumScreenTitle("Plus", "Tous les modules d’AI Trader Pro") }
        items(items) { item ->
            PremiumCardBox(item.second, "Ouvrir le module", modifier = Modifier.clickable { onNavigate(item.first) }) { }
        }
    }
}

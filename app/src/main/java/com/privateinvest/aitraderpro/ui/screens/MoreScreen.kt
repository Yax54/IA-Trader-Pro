package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MoreItem(val route: String, val label: String, val description: String)
private data class MoreGroup(val title: String, val items: List<MoreItem>)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val groups = listOf(
        MoreGroup(
            "Analyse",
            listOf(
                MoreItem("control_center", "🧠 Cockpit IA", "Supervision globale — score IA, modules, validation"),
                MoreItem("watchlist", "Watchlist", "Actifs suivis et signaux"),
                MoreItem("top_opportunities", "Top Opportunités", "Meilleurs signaux du moment"),
                MoreItem("ai_forecasts", "🧠 Pronostics IA", "Mémoire glissante CT/LT — L'IA apprend même sans signal joué"),
                MoreItem("stats_center", "Centre statistiques", "Performance par mois, actif et stratégie"),
                MoreItem("audit", "Audit IA", "Fiabilité, mémoire et poids IA"),
                MoreItem("journal_ia", "Journal IA", "Historique des décisions")
            )
        ),
        MoreGroup(
            "Gestion",
            listOf(
                MoreItem("strategy_monitoring", "🔔 Suivi intelligent", "Alertes vente — tu décides, l'app surveille"),
                MoreItem("portfolio", "Portefeuilles", "Simulation et suivi"),
                MoreItem("multi_portfolio", "Multi-portefeuilles", "Simulation, réel, long terme, dividendes"),
                MoreItem("daily_report", "Rapport quotidien", "Résumé d'utilisation"),
                MoreItem("calendar_ia", "Calendrier IA", "Jours positifs, neutres ou négatifs"),
                MoreItem("favorites", "Favoris", "Actifs prioritaires")
            )
        ),
        MoreGroup(
            "Réglages",
            listOf(
                MoreItem("real_validation", "⚡ Validation Réelle", "Diagnostic avant passage au capital réel — blocages uniquement sur dangers réels"),
                MoreItem("market_data_center", "📡 Données marché", "Sources Alpha Vantage / Broker — vérifie la fraîcheur des cours"),
                MoreItem("onboarding", "Première utilisation", "Checklist de départ"),
                MoreItem("preferences_center", "Préférences", "Affichage, devise, notifications"),
                MoreItem("risk", "Gestion du risque", "Limites et protections"),
                MoreItem("health", "Santé IA", "Diagnostic complet"),
                MoreItem("security_center", "Sécurité", "PIN uniquement pour actions sensibles"),
                MoreItem("backup", "Sauvegarde", "Export/import sans PIN"),
                MoreItem("exports", "Exports PNG/PDF", "Rapports pleine page"),
                MoreItem("admin", "Admin", "Options avancées")
            )
        )
    )
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Plus", "Modules regroupés pour une navigation claire") }
        item { SafetyBanner("Le PIN ne bloque pas l'application : il protège seulement les actions sensibles.") }
        groups.forEach { group ->
            item { Text(group.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            items(group.items) { item ->
                PremiumCardBox(
                    title = item.label,
                    subtitle = item.description,
                    modifier = Modifier.clickable { onNavigate(item.route) }
                ) { }
            }
        }
    }
}

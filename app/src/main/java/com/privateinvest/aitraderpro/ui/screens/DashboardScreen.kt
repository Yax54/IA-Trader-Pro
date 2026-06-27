package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.privateinvest.aitraderpro.repository.DataFreshnessGuard
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.DashboardViewModel
import com.privateinvest.aitraderpro.viewmodel.SettingsViewModel

@Composable
fun DashboardScreen(
    onOpenRisk: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenAsset: (String, String) -> Unit,
    onOpenCockpit: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(factory = AITraderViewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AITraderViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val beginnerModeEnabled by settingsViewModel.beginnerModeEnabled.collectAsStateWithLifecycle()

    // A2 : Bouton refresh premium
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // V1.3 : bannière données marché si aucune source réelle disponible
        item {
            DataQualityBanner(message = DataFreshnessGuard.globalBannerMessage())
        }

        // R-05 : titre premium + accès discret Cockpit IA (V1.4)
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    PremiumScreenTitle(
                        title = "Tableau de bord",
                        subtitle = if (beginnerModeEnabled) "Mode débutant actif" else "Mode expert actif"
                    )
                }
                Text(
                    text = "🧠 Cockpit IA",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(PremiumBlue, RoundedCornerShape(999.dp))
                        .clickable { onOpenCockpit() }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }

        // R-05 : métriques en style PremiumMetric
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PremiumMetric("Capital", state.summary.capital, modifier = Modifier.weight(1f))
                PremiumMetric("Aujourd'hui", state.summary.dayPnL, modifier = Modifier.weight(1f),
                    accent = if (state.summary.dayPnL.trimStart().startsWith("-")) DangerRed else Success)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PremiumMetric("Mois", state.summary.monthPnL, modifier = Modifier.weight(1f),
                    accent = if (state.summary.monthPnL.trimStart().startsWith("-")) DangerRed else Success)
                // R-02 : badge risque coloré selon niveau
                RiskMetricCard(state.summary.riskLevel, modifier = Modifier.weight(1f))
            }
        }

        // R-05 : actions rapides en PremiumCardBox
        item {
            PremiumCardBox(title = "Actions rapides") {
                Text(
                    text = if (beginnerModeEnabled) "Mode actuel : Débutant" else "Mode actuel : Expert",
                    modifier = Modifier
                        .background(
                            if (beginnerModeEnabled) Success else Warning,
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumSecondaryButton("Risque", onClick = onOpenRisk, modifier = Modifier.weight(1f))
                    PremiumActionButton("Paramètres", onClick = onOpenAdmin, modifier = Modifier.weight(1f))
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            PremiumScreenTitle(title = "Opportunités du moment")
        }

        if (state.signals.isEmpty()) {
            item {
                PremiumCardBox(title = "Données indisponibles") {
                    Text(
                        "Ajoute une clé API Alpha Vantage dans les paramètres pour alimenter le tableau de bord.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // R-05 + R-06 : cartes signal enrichies avec badge et barre de confiance
        items(state.signals) { signal ->
            PremiumCardBox(
                title = signal.symbol,
                subtitle = signal.name,
                modifier = Modifier.clickable { onOpenAsset(signal.symbol, signal.name) }
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignalBadge(signal.action.name)
                    Text(
                        "Score ${signal.score}/100",
                        color = SoftWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                ConfidenceBar("Confiance IA", signal.confidence)
                Spacer(Modifier.height(6.dp))
                RowLine("Objectif", signal.target)
                RowLine("Risque", signal.risk)
            }
        }
    }
}

/** R-02 : carte métrique "Risque" avec couleur selon le niveau */
@Composable
private fun RiskMetricCard(riskLevel: String, modifier: Modifier = Modifier) {
    val accentColor = when (riskLevel.lowercase()) {
        "faible", "bas" -> Success
        "modéré", "moyen" -> Warning
        "élevé", "fort", "critique" -> DangerRed
        else -> PremiumBlue
    }
    PremiumMetric("Risque", riskLevel, modifier = modifier, accent = accentColor)
}

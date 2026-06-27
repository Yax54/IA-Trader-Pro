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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.BrokerMode
import com.privateinvest.aitraderpro.viewmodel.BrokerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokerAssistantScreen(onBack: () -> Unit = {}) {
    val viewModel: BrokerViewModel = viewModel(factory = AITraderViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ─── État du récapitulatif de confirmation ───────────────────────────
    var showRecapDialog by remember { mutableStateOf(false) }
    var confirmChecked by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    val confirmWord = "CONFIRMER"

    // ─── Dialog de résultat d'ordre ──────────────────────────────────────
    state.orderResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearOrderResult() },
            title = { Text("Ordre envoyé") },
            text = { Text(result, color = Color.LightGray) },
            confirmButton = {
                Button(onClick = { viewModel.clearOrderResult(); onBack() }) { Text("OK") }
            }
        )
    }

    // ─── Dialog récapitulatif avant envoi (sécurité broker) ─────────────
    if (showRecapDialog) {
        BrokerConfirmationDialog(
            state = state,
            confirmChecked = confirmChecked,
            onCheckedChange = { confirmChecked = it },
            confirmText = confirmText,
            onConfirmTextChange = { confirmText = it },
            confirmWord = confirmWord,
            onConfirm = {
                showRecapDialog = false
                confirmChecked = false
                confirmText = ""
                viewModel.submitOrder()
            },
            onDismiss = {
                showRecapDialog = false
                confirmChecked = false
                confirmText = ""
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant d'investissement", color = SoftWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ─── En-tête actif + recommandation IA ──────────────────────
            PremiumCardBox(
                title = state.symbol.ifBlank { "Actif" },
                subtitle = state.name
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SignalBadge(state.actionLabel)
                    Text(
                        "Prix actuel",
                        color = Color.LightGray, fontSize = 13.sp
                    )
                    Text(
                        if (state.currentPrice > 0) String.format("%.2f €", state.currentPrice) else "—",
                        color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                ConfidenceBar("Confiance IA", state.confidencePercent)
                Spacer(Modifier.height(6.dp))
                RowLine("Risque actuel", state.riskLabel)
                RowLine("Score technique", "${state.scoreTechnique}/100")
            }

            // ─── Liquidités et pouvoir d'achat ───────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumMetric("Liquidités dispo", String.format("%.0f €", state.availableCash), Modifier.weight(1f))
                PremiumMetric("Valeur portefeuille", String.format("%.0f €", state.portfolioValue), Modifier.weight(1f))
            }
            PremiumMetric("Pouvoir d'achat", String.format("%.0f €", state.buyingPower), Modifier.fillMaxWidth(),
                accent = if (state.buyingPower >= state.selectedAmount) Success else DangerRed)

            // ─── Montant recommandé IA ────────────────────────────────────
            PremiumCardBox(title = "Recommandation IA", accent = PremiumBlue) {
                Text(
                    "Montant suggéré selon la confiance IA et votre capital :",
                    color = Color.LightGray, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        String.format("%.0f €", state.aiRecommendedAmount),
                        color = PremiumBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                    PremiumSecondaryButton("Utiliser", onClick = {
                        viewModel.setAmount(state.aiRecommendedAmount)
                    })
                }
            }

            // ─── Sélection du montant ─────────────────────────────────────
            PremiumCardBox(title = "Choisir un montant") {
                // Boutons rapides
                val presets = listOf(250.0, 500.0, 1_000.0, 2_500.0)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        val isSelected = state.selectedAmount == preset
                        Button(
                            onClick = { viewModel.setAmount(preset) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PremiumBlue else PremiumCardAlt
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "${preset.toInt()}€",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Slider
                Text("Montant via slider", color = Color.LightGray, fontSize = 13.sp)
                Slider(
                    value = state.selectedAmount.toFloat().coerceIn(0f, state.buyingPower.toFloat().coerceAtLeast(10_000f)),
                    onValueChange = { viewModel.setAmount(it.toDouble()) },
                    valueRange = 0f..state.buyingPower.toFloat().coerceAtLeast(10_000f),
                    steps = 199,
                    colors = SliderDefaults.colors(thumbColor = PremiumBlue, activeTrackColor = PremiumBlue),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0 €", color = PremiumMuted, fontSize = 12.sp)
                    Text(
                        String.format("%.0f €", state.buyingPower.coerceAtLeast(10_000.0)),
                        color = PremiumMuted, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                // Montant personnalisé
                var customInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { input ->
                        customInput = input
                        input.toDoubleOrNull()?.let { viewModel.setAmount(it) }
                    },
                    label = { Text("Montant personnalisé (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Montant sélectionné : ${String.format("%.2f €", state.selectedAmount)}",
                    color = PremiumBlue, fontWeight = FontWeight.Bold
                )
            }

            // ─── Calculs automatiques ─────────────────────────────────────
            PremiumCardBox(title = "Impact calculé automatiquement") {
                RowLine("Quantité estimée", "${String.format("%.0f", state.estimatedQuantity)} titres")
                RowLine("Montant total", String.format("%.2f €", state.totalAmount))
                RowLine("Impact portefeuille", String.format("%.1f %%", state.portfolioImpactPercent))
                RowLine("Exposition actif", String.format("%.1f %%", state.assetExposurePercent))
                RowLine("Liquidités restantes", String.format("%.2f €", state.remainingCash))
                Spacer(Modifier.height(6.dp))
                // Niveau de risque après achat — coloré
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Risque après achat", color = Color.LightGray)
                    val riskColor = when (state.riskAfterBuy) {
                        "Élevé" -> DangerRed
                        "Modéré" -> Warning
                        else -> Success
                    }
                    Text(
                        state.riskAfterBuy,
                        color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(riskColor, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // Alerte si liquidités insuffisantes
                if (state.remainingCash < 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Liquidités insuffisantes pour ce montant.",
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ─── Sélection du mode ────────────────────────────────────────
            PremiumCardBox(title = "Mode d'exécution", accent = Warning) {
                Text(
                    "La simulation reste le mode par défaut. Aucun ordre réel ne peut partir sans confirmation manuelle.",
                    color = Color.LightGray, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrokerModeButton("Simulation", state.brokerMode == BrokerMode.SIMULATION,
                        accent = Success, modifier = Modifier.weight(1f)) {
                        viewModel.setBrokerMode(BrokerMode.SIMULATION)
                    }
                    BrokerModeButton("Paper", state.brokerMode == BrokerMode.PAPER_TRADING,
                        accent = PremiumBlue, modifier = Modifier.weight(1f)) {
                        viewModel.setBrokerMode(BrokerMode.PAPER_TRADING)
                    }
                    BrokerModeButton("Réel", state.brokerMode == BrokerMode.REEL,
                        accent = DangerRed, modifier = Modifier.weight(1f)) {
                        viewModel.setBrokerMode(BrokerMode.REEL)
                    }
                }
                if (state.brokerMode == BrokerMode.REEL) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ATTENTION : le mode Réel enverra un ordre à votre broker. Assurez-vous que votre clé API est configurée.",
                        color = DangerRed, style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ─── Bouton envoi (ouvre le récapitulatif) ────────────────────
            val canOrder = state.estimatedQuantity >= 1.0 && state.remainingCash >= 0
            PremiumActionButton(
                text = when (state.brokerMode) {
                    BrokerMode.SIMULATION -> "Valider en simulation"
                    BrokerMode.PAPER_TRADING -> "Envoyer ordre paper trading"
                    BrokerMode.REEL -> "Envoyer ordre RÉEL"
                },
                onClick = { showRecapDialog = true },
                modifier = Modifier.fillMaxWidth(),
                danger = state.brokerMode == BrokerMode.REEL
            )

            if (!canOrder) {
                Text(
                    "Ajustez le montant (quantité < 1 ou liquidités insuffisantes).",
                    color = Warning, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SafetyBanner("Aucun ordre automatique. Toute action doit être validée manuellement par l'utilisateur.")
        }
    }
}

/** Bouton de sélection de mode avec couleur d'accent */
@Composable
private fun BrokerModeButton(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) accent else PremiumCardAlt),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

/**
 * Dialog récapitulatif de confirmation avant envoi d'un ordre broker.
 * Contient :
 * - Récapitulatif complet de l'ordre
 * - Case obligatoire "Je comprends que cet ordre sera envoyé à mon broker"
 * - Champ texte CONFIRMER obligatoire
 * - Bouton Envoyer désactivé tant que tout n'est pas rempli
 */
@Composable
private fun BrokerConfirmationDialog(
    state: com.privateinvest.aitraderpro.viewmodel.BrokerUiState,
    confirmChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
    confirmWord: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val canConfirm = confirmChecked && confirmText.trim().uppercase() == confirmWord
    val modeLabel = when (state.brokerMode) {
        BrokerMode.SIMULATION -> "Simulation interne"
        BrokerMode.PAPER_TRADING -> "Paper trading broker"
        BrokerMode.REEL -> "ORDRE RÉEL"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Récapitulatif de l'ordre",
                fontWeight = FontWeight.Bold,
                color = if (state.brokerMode == BrokerMode.REEL) DangerRed else SoftWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Récapitulatif
                RecapRow("Actif", "${state.symbol} · ${state.name}")
                RecapRow("Mode", modeLabel)
                RecapRow("Prix actuel", String.format("%.2f €", state.currentPrice))
                RecapRow("Montant investi", String.format("%.2f €", state.totalAmount))
                RecapRow("Quantité", String.format("%.0f titres", state.estimatedQuantity))
                RecapRow("Exposition", String.format("%.1f %% du portefeuille", state.assetExposurePercent))
                RecapRow("Liquidités restantes", String.format("%.2f €", state.remainingCash))
                RecapRow("Risque après achat", state.riskAfterBuy)

                Spacer(Modifier.height(8.dp))

                // Case obligatoire
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = confirmChecked, onCheckedChange = onCheckedChange)
                    Text(
                        if (state.brokerMode == BrokerMode.SIMULATION)
                            "Je comprends que cet ordre sera enregistré en simulation."
                        else
                            "Je comprends que cet ordre sera envoyé à mon broker et peut engager de l'argent réel.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Champ texte obligatoire
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = onConfirmTextChange,
                    label = { Text("Tapez $confirmWord pour confirmer") },
                    singleLine = true,
                    isError = confirmText.isNotBlank() && confirmText.trim().uppercase() != confirmWord,
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmText.isNotBlank() && confirmText.trim().uppercase() != confirmWord) {
                    Text("Saisir exactement : $confirmWord", color = DangerRed,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.brokerMode == BrokerMode.REEL) DangerRed else PremiumBlue,
                    disabledContainerColor = PremiumCardAlt
                )
            ) { Text("Envoyer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun RecapRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.LightGray, fontSize = 13.sp)
        Text(value, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

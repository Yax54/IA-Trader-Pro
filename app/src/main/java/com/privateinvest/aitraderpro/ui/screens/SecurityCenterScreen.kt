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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.repository.TradingExecutionMode
import com.privateinvest.aitraderpro.security.BiometricAuthHelper
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.SecurityViewModel

@Composable
fun SecurityCenterScreen() {
    val viewModel: SecurityViewModel = viewModel(factory = AITraderViewModelFactory)
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val security = ui.state
    val context = LocalContext.current
    val biometricAvailable = remember { BiometricAuthHelper.canUseFingerprint(context) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var authPin by remember { mutableStateOf("") }
    var pendingMode by remember { mutableStateOf<TradingExecutionMode?>(null) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    // ─── Correction 2 : dialog PIN obligatoire avant Paper/Réel ────────────
    pendingMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { pendingMode = null; authPin = "" },
            title = { Text(if (mode == TradingExecutionMode.REAL) "Activer le mode réel" else "Changer de mode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (mode == TradingExecutionMode.REAL)
                            "Le mode réel peut permettre l'envoi d'ordres vers un broker configuré. Saisissez votre PIN pour continuer."
                        else
                            "Saisissez votre PIN pour confirmer le changement de mode.",
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = authPin,
                        onValueChange = { authPin = it.filter(Char::isDigit).take(6) },
                        label = { Text("Code PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.verifyPin(authPin) { ok ->
                        if (ok) viewModel.setTradingMode(mode)
                        if (ok) pendingMode = null
                        authPin = ""
                    }
                }) { Text("Valider") }
            },
            dismissButton = { TextButton(onClick = { pendingMode = null; authPin = "" }) { Text("Annuler") } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Centre Sécurité", "PIN, empreinte digitale, mode réel et journal de sécurité") }
        item { TradingModeBanner(mode = security.tradingMode.name) }

        // ─── PIN ────────────────────────────────────────────────────────────
        item {
            PremiumCardBox(
                "Code PIN",
                if (security.pinConfigured) "PIN configuré" else "À créer avant tout mode réel",
                accent = if (security.pinConfigured) Success else Warning
            ) {
                Text(
                    "Le PIN est demandé avant chaque opération sensible. Il n'est jamais stocké en clair.",
                    color = Color.LightGray, fontSize = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = { Text(if (security.pinConfigured) "Nouveau PIN" else "Créer un PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter(Char::isDigit).take(6) },
                    label = { Text("Confirmer le PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                PremiumActionButton(
                    text = if (security.pinConfigured) "Modifier le PIN" else "Créer le PIN",
                    onClick = {
                        viewModel.createOrChangePin(pin, confirmPin)
                        pin = ""
                        confirmPin = ""
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }

        // ─── Empreinte ──────────────────────────────────────────────────────
        item {
            PremiumCardBox("Empreinte digitale", "Optionnelle, fallback PIN", accent = PremiumBlue) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Utiliser l'empreinte digitale",
                            color = SoftWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (biometricAvailable) "Disponible sur ce téléphone"
                            else "Non disponible ou non configurée sur ce téléphone",
                            color = Color.LightGray, fontSize = 14.sp
                        )
                    }
                    Switch(
                        checked = security.fingerprintEnabled,
                        enabled = biometricAvailable && security.pinConfigured,
                        onCheckedChange = { viewModel.setFingerprintEnabled(it) }
                    )
                }
                if (security.fingerprintEnabled && biometricAvailable) {
                    PremiumSecondaryButton(
                        "Tester l'empreinte",
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                biometricError = "Activité Android incompatible avec BiometricPrompt."
                            } else {
                                BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    onSuccess = { viewModel.onBiometricSuccess(); biometricError = null },
                                    onError = { biometricError = it }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
                biometricError?.let {
                    Text(it, color = DangerRed, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // ─── Mode de trading ────────────────────────────────────────────────
        item {
            PremiumCardBox(
                "Mode de trading",
                "Simulation par défaut",
                accent = modeColor(security.tradingMode)
            ) {
                Text(
                    "Les modes Paper et Réel ne remplacent pas la simulation. Ils s'ajoutent comme niveaux d'exécution.",
                    color = Color.LightGray
                )
                Spacer(Modifier.height(12.dp))

                // ── Correction 2 : afficher un message clair si PIN non configuré ──
                if (!security.pinConfigured) {
                    Text(
                        "Créez d'abord un PIN avant d'activer Paper Trading ou Réel.",
                        color = Warning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Warning.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                ModeRow(security.tradingMode) { mode ->
                    when {
                        // ── Correction 2 : PIN absent → message, pas d'appel silencieux ──
                        !security.pinConfigured && mode != TradingExecutionMode.SIMULATION -> {
                            // Le message ci-dessus est déjà visible ; on ne fait rien de plus
                        }
                        security.requirePinBeforeModeChange && mode != TradingExecutionMode.SIMULATION -> {
                            pendingMode = mode
                        }
                        else -> {
                            viewModel.setTradingMode(mode)
                        }
                    }
                }

                if (security.realModeLocked) {
                    Text(
                        "Mode réel verrouillé après erreurs PIN. Revalidez votre PIN pour déverrouiller.",
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        // ─── Règles sensibles ───────────────────────────────────────────────
        item {
            PremiumCardBox("Règles sensibles", "Protection des actions importantes", accent = Warning) {
                RowLine("Avant ordre réel", if (security.requirePinBeforeRealOrder) "PIN ou empreinte obligatoire" else "Désactivé")
                RowLine("Changement de mode", if (security.requirePinBeforeModeChange) "PIN obligatoire" else "Désactivé")
                RowLine("Paramètres broker", if (security.requirePinBeforeBrokerSettings) "PIN obligatoire" else "Désactivé")
                RowLine("Restauration backup", if (security.requirePinBeforeBackupRestore) "PIN obligatoire" else "Désactivé")
                RowLine("Verrouillage auto", "${security.autoLockMinutes} min")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 30).forEach { min ->
                        Button(
                            onClick = { viewModel.setAutoLockMinutes(min) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (security.autoLockMinutes == min) PremiumBlue else PremiumCardAlt
                            )
                        ) { Text("${min}m") }
                    }
                }
            }
        }

        // ─── Backup et sécurité ─────────────────────────────────────────────
        item {
            PremiumCardBox("Backup et sécurité", "Le PIN n'est jamais exporté", accent = PremiumBlue) {
                Text(
                    "Les sauvegardes peuvent conserver le mode actif et les préférences, mais jamais le PIN, son hash ou le sel de sécurité.",
                    color = Color.LightGray
                )
                Text(
                    "Après restauration sur un autre téléphone, il faudra recréer un PIN.",
                    color = Color.LightGray, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // ─── Messages ───────────────────────────────────────────────────────
        ui.message?.let { item { StatusCard("OK", it, Success) } }
        ui.error?.let { item { StatusCard("Attention", it, DangerRed) } }

        // ─── Journal ────────────────────────────────────────────────────────
        item { PremiumScreenTitle("Journal sécurité", "Dernières actions sensibles") }
        items(ui.logs) { log ->
            PremiumCardBox(
                log.event,
                log.details,
                accent = if (log.level == "WARNING") Warning else Success
            ) {
                RowLine("Niveau", log.level)
                RowLine(
                    "Date",
                    java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.FRANCE)
                        .format(java.util.Date(log.createdAt))
                )
            }
        }
    }
}

@Composable
private fun ModeRow(active: TradingExecutionMode, onSelect: (TradingExecutionMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeButton("Simulation", active == TradingExecutionMode.SIMULATION, Success, Modifier.weight(1f)) {
            onSelect(TradingExecutionMode.SIMULATION)
        }
        ModeButton("Paper", active == TradingExecutionMode.PAPER_TRADING, PremiumBlue, Modifier.weight(1f)) {
            onSelect(TradingExecutionMode.PAPER_TRADING)
        }
        ModeButton("Réel", active == TradingExecutionMode.REAL, DangerRed, Modifier.weight(1f)) {
            onSelect(TradingExecutionMode.REAL)
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) color else PremiumCardAlt),
        shape = RoundedCornerShape(14.dp)
    ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun StatusCard(title: String, text: String, color: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(text, color = SoftWhite, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun modeColor(mode: TradingExecutionMode): Color = when (mode) {
    TradingExecutionMode.SIMULATION -> Success
    TradingExecutionMode.PAPER_TRADING -> PremiumBlue
    TradingExecutionMode.REAL -> DangerRed
}

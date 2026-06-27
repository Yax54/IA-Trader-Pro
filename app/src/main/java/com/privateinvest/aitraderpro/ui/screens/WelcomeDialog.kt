package com.privateinvest.aitraderpro.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.privateinvest.aitraderpro.ui.theme.SoftWhite

private const val PREFS_NAME = "aitraderpro_welcome"
private const val KEY_FIRST_LAUNCH = "first_launch_seen"

/**
 * Vérifie si le dialog de bienvenue a déjà été affiché.
 * Retourne true uniquement au premier lancement.
 */
fun isFirstLaunch(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return !prefs.getBoolean(KEY_FIRST_LAUNCH, false)
}

/**
 * Marque le premier lancement comme vu. Appeler après affichage ou choix utilisateur.
 */
fun markFirstLaunchSeen(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_FIRST_LAUNCH, true)
        .apply()
}

/**
 * WelcomeDialog — Premier lancement uniquement.
 *
 * Affiche une fenêtre de bienvenue avec 3 choix simples :
 * - Découvrir l'application
 * - Commencer en simulation
 * - Connecter mon broker plus tard
 *
 * La fenêtre ne s'affiche qu'une seule fois (contrôle via SharedPreferences).
 * Après le choix, markFirstLaunchSeen() est appelé automatiquement.
 *
 * @param onDiscover    L'utilisateur veut découvrir l'app (reste sur Dashboard)
 * @param onSimulation  L'utilisateur veut démarrer en simulation
 * @param onLater       L'utilisateur connectera son broker plus tard
 */
@Composable
fun WelcomeDialog(
    onDiscover: () -> Unit,
    onSimulation: () -> Unit,
    onLater: () -> Unit
) {
    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF132238))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // En-tête
                Text(
                    text = "🚀",
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Bienvenue dans\nAI Trader Pro",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Application privée d'investissement assisté par IA.\nQue souhaitez-vous faire ?",
                    fontSize = 15.sp,
                    color = PremiumMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )

                Spacer(Modifier.height(24.dp))

                // Choix 1 — Découvrir
                WelcomeChoiceButton(
                    emoji = "🔭",
                    title = "Découvrir l'application",
                    subtitle = "Explorer le dashboard et les fonctionnalités",
                    color = PremiumBlue,
                    onClick = onDiscover
                )
                Spacer(Modifier.height(12.dp))

                // Choix 2 — Simulation
                WelcomeChoiceButton(
                    emoji = "🎮",
                    title = "Commencer en simulation",
                    subtitle = "Pratiquer sans argent réel",
                    color = Color(0xFF22C55E),
                    onClick = onSimulation
                )
                Spacer(Modifier.height(12.dp))

                // Choix 3 — Plus tard
                WelcomeChoiceButton(
                    emoji = "🔗",
                    title = "Connecter mon broker plus tard",
                    subtitle = "Je configurerai ça dans les paramètres",
                    color = Color(0xFFA855F7),
                    onClick = onLater
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Cette fenêtre ne s'affichera plus.",
                    fontSize = 13.sp,
                    color = PremiumMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeChoiceButton(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E314D)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pastille colorée avec emoji
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SoftWhite)
                Text(subtitle, fontSize = 13.sp, color = PremiumMuted, lineHeight = 18.sp)
            }
            // Flèche indicative
            Text("›", fontSize = 22.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Hook de composition pour gérer le WelcomeDialog depuis n'importe quel écran.
 * Utilise SharedPreferences pour ne s'afficher qu'une seule fois.
 *
 * Usage :
 * ```
 * WelcomeDialogHost()
 * ```
 */
@Composable
fun WelcomeDialogHost(
    onDiscover: () -> Unit = {},
    onSimulation: () -> Unit = {},
    onLater: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(isFirstLaunch(context)) }

    if (showDialog) {
        WelcomeDialog(
            onDiscover = {
                markFirstLaunchSeen(context)
                showDialog = false
                onDiscover()
            },
            onSimulation = {
                markFirstLaunchSeen(context)
                showDialog = false
                onSimulation()
            },
            onLater = {
                markFirstLaunchSeen(context)
                showDialog = false
                onLater()
            }
        )
    }
}

package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.privateinvest.aitraderpro.ui.theme.SoftWhite

/**
 * Fenêtre d'aide Premium réutilisable dans toute l'application.
 *
 * Règles UX :
 * - ouverture depuis un bouton ? ou un élément cliquable ;
 * - croix X en haut à droite ;
 * - contenu scrollable si le téléphone est petit ;
 * - bouton Compris en bas ;
 * - aucun blocage métier, uniquement pédagogie.
 */
@Composable
fun PremiumEducationDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    subtitle: String = "Explication simple",
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1220), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = SoftWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = PremiumMuted, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = SoftWhite)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    content.split("\n").forEach { line ->
                        val isTitle = line.startsWith("###")
                        val clean = line.removePrefix("###").trim()
                        if (clean.isNotBlank()) {
                            Text(
                                text = clean,
                                color = if (isTitle) SoftWhite else Color.LightGray,
                                fontSize = if (isTitle) 18.sp else 16.sp,
                                lineHeight = if (isTitle) 24.sp else 23.sp,
                                fontWeight = if (isTitle) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = if (isTitle) 7.dp else 4.dp)
                            )
                        } else {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                PremiumActionButton("Compris", onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}

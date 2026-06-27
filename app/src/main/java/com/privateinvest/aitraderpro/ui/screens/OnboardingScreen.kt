package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen() {
    val viewModel: OnboardingViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PremiumScreenTitle("Première utilisation", "Checklist pour tester l'application finie") }
        items(state.steps) { step ->
            PremiumCardBox(step.title, step.description, accent = if (step.status == "OK") Success else Warning) {
                SignalBadge(step.status)
            }
        }
        item { SafetyBanner("Le PIN n'est jamais demandé pour ouvrir l'application. Il sert uniquement aux actions sensibles.") }
    }
}

package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.CalendarIaViewModel

@Composable
fun CalendarIAScreen() {
    val viewModel: CalendarIaViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PremiumScreenTitle("Calendrier IA", "Lecture simple des journées positives, neutres ou négatives") }
        items(state.days) { day ->
            val accent = when (day.status) { "Positif" -> scoreColor(80); "Négatif" -> DangerRed; "Neutre" -> Warning; else -> Color.Gray }
            PremiumCardBox(day.dateLabel, day.status, accent = accent) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SignalBadge(day.status)
                    Text(day.performance.formatPercent())
                }
                RowLine("Signaux du jour", day.signalCount.toString())
                Text("Vert = positif, orange = neutre, rouge = négatif.", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

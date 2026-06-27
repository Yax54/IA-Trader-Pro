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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privateinvest.aitraderpro.database.AiForecastEntity
import com.privateinvest.aitraderpro.database.AiForecastOutcomeEntity
import com.privateinvest.aitraderpro.navigation.SelectedForecastStore
import com.privateinvest.aitraderpro.ui.theme.SoftWhite
import com.privateinvest.aitraderpro.ui.theme.Success
import com.privateinvest.aitraderpro.ui.theme.Warning
import com.privateinvest.aitraderpro.viewmodel.AITraderViewModelFactory
import com.privateinvest.aitraderpro.viewmodel.ForecastDetailViewModel
import java.util.Locale

@Composable
fun ForecastDetailScreen(
    onBack: () -> Unit,
    onOpenAsset: (String, String) -> Unit,
    onOpenBroker: (String, String) -> Unit,
    viewModel: ForecastDetailViewModel = viewModel(factory = AITraderViewModelFactory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(SelectedForecastStore.currentForecastId) {
        viewModel.load(SelectedForecastStore.currentForecastId)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = SoftWhite)
            }
            PremiumScreenTitle(
                title = "Fiche pronostic IA",
                subtitle = "Pourquoi l'IA a choisi cette action et comment le pronostic évolue."
            )
        }

        if (state.loading) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumBlue)
            }
            return@Column
        }

        val forecast = state.forecast
        if (forecast == null) {
            PremiumCardBox("Pronostic introuvable", state.error ?: "Aucun détail disponible", accent = DangerRed) {
                Text("Retournez aux Pronostics IA et ouvrez une fiche existante.", color = Color.LightGray, fontSize = 16.sp)
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            item { ForecastHeader(forecast) }
            item { WhyCard(forecast) }
            item { IndicatorsCard(forecast) }
            item { CategoryCard(forecast) }
            item { ConfidenceCard(forecast) }
            item { TargetStopCard(forecast) }
            item { FollowUpTimelineCard(forecast, state.outcome) }
            item { AiEvolutionCard(forecast, state.outcome) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumActionButton("Ouvrir fiche actif", { onOpenAsset(forecast.symbol, forecast.name) }, Modifier.weight(1f))
                    PremiumSecondaryButton("Assistant investissement", { onOpenBroker(forecast.symbol, forecast.name) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ForecastHeader(forecast: AiForecastEntity) {
    PremiumCardBox(
        title = "${forecast.symbol} — ${forecast.name}",
        subtitle = "${strategyLabel(forecast.strategyType)} · ${forecast.status}",
        accent = scoreColor(forecast.score)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SignalBadge(forecast.strategyLabel)
            Text("${forecast.score}/100", color = SoftWhite, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        ConfidenceBar("Confiance IA", forecast.confidence)
    }
}

@Composable
private fun WhyCard(forecast: AiForecastEntity) {
    PremiumCardBox("Pourquoi l'IA a choisi cette action", "Explication simple pour débutant", accent = PremiumBlue) {
        Text(buildWhyText(forecast), color = Color.LightGray, fontSize = 17.sp, lineHeight = 24.sp)
    }
}

@Composable
private fun IndicatorsCard(forecast: AiForecastEntity) {
    PremiumCardBox("Indicateurs utilisés", "Les données techniques qui ont déclenché le pronostic", accent = Warning) {
        ExplainableInfoRow("RSI", indicatorHint(forecast, "RSI"), explanationFor("RSI", forecast))
        ExplainableInfoRow("MACD", indicatorHint(forecast, "MACD"), explanationFor("MACD", forecast))
        ExplainableInfoRow("EMA", "Tendance moyenne exponentielle.", explanationFor("EMA", forecast))
        ExplainableInfoRow("SMA", "Tendance moyenne simple.", explanationFor("SMA", forecast))
        ExplainableInfoRow("ATR", "Stop-loss adapté au mouvement réel.", explanationFor("ATR", forecast))
        ExplainableInfoRow("Volume", indicatorHint(forecast, "VOLUME"), explanationFor("Volume", forecast))
        ExplainableInfoRow("Support", "Zone où le prix peut rebondir.", explanationFor("Support", forecast))
        ExplainableInfoRow("Résistance", "Zone où le prix peut bloquer.", explanationFor("Résistance", forecast))
        ExplainableInfoRow("Volatilité", volatilityHint(forecast), explanationFor("Volatilité", forecast))
    }
}

@Composable
private fun CategoryCard(forecast: AiForecastEntity) {
    PremiumCardBox("Catégorie", forecast.strategyLabel, accent = PremiumBlue) {
        ExplainableInfoRow(
            label = forecast.strategyLabel,
            value = strategyExplanation(forecast.strategyType),
            explanation = explanationFor("catégorie", forecast)
        )
    }
}

@Composable
private fun ConfidenceCard(forecast: AiForecastEntity) {
    PremiumCardBox("Niveau de confiance", "Pourquoi l'IA donne ${forecast.confidence}%", accent = scoreColor(forecast.confidence)) {
        ConfidenceBar("Confiance", forecast.confidence)
        Spacer(Modifier.height(10.dp))
        ExplainableInfoRow("Score IA", "${forecast.score}/100", explanationFor("score IA", forecast))
        ExplainableInfoRow("Confiance", "${forecast.confidence}%", explanationFor("confiance", forecast))
        Text(confidenceExplanation(forecast), color = Color.LightGray, fontSize = 16.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun TargetStopCard(forecast: AiForecastEntity) {
    val targetPrice = forecast.entryPrice * (1.0 + forecast.targetPercent / 100.0)
    val stopPrice = forecast.entryPrice * (1.0 - forecast.stopPercent / 100.0)
    PremiumCardBox("Objectif et protection", "Ce que l'IA surveille après le signal", accent = Success) {
        RowLinePremium("Prix au signal", money(forecast.entryPrice))
        ExplainableInfoRow("Objectif", "${money(targetPrice)} (+${one(forecast.targetPercent)}%)", explanationFor("objectif", forecast))
        ExplainableInfoRow("Stop-loss", "${money(stopPrice)} (-${one(forecast.stopPercent)}%)", explanationFor("stop-loss", forecast))
        ExplainableInfoRow("Horizon", "${forecast.horizonDays} jours", explanationFor("horizon", forecast))
        Text(
            "L'objectif sert à savoir quand une vente est à envisager. Le stop-loss sert à identifier quand le scénario ne fonctionne plus.",
            color = Color.LightGray,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FollowUpTimelineCard(forecast: AiForecastEntity, outcome: AiForecastOutcomeEntity?) {
    PremiumCardBox("Suivi intelligent", "Évolution réelle du pronostic dans le temps", accent = PremiumBlue) {
        ExplainableInfoRow("Suivi intelligent", "J+1 / J+3 / J+7 / J+30 / J+90", explanationFor("suivi intelligent", forecast))
        TimelineLine("J+1", outcome?.performanceJ1)
        TimelineLine("J+3", outcome?.performanceJ3)
        TimelineLine("J+7", outcome?.performanceJ7)
        TimelineLine("J+30", outcome?.performanceJ30)
        TimelineLine("J+90", outcome?.performanceJ90)
        if (outcome == null) {
            Text("Le suivi démarre dès que le pronostic a assez d'ancienneté. Rien n'est vendu automatiquement.", color = Color.LightGray, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AiEvolutionCard(forecast: AiForecastEntity, outcome: AiForecastOutcomeEntity?) {
    PremiumCardBox("Évolution de l'avis de l'IA", "Ce que l'IA pense après le signal initial", accent = Warning) {
        val latestPerf = listOfNotNull(
            outcome?.performanceJ90,
            outcome?.performanceJ30,
            outcome?.performanceJ7,
            outcome?.performanceJ3,
            outcome?.performanceJ1
        ).firstOrNull()
        val opinion = when {
            forecast.status.equals("HIT_TARGET", true) -> "Objectif atteint : vente à envisager."
            forecast.status.equals("HIT_STOP", true) -> "Stop-loss touché : le scénario ne fonctionne plus."
            latestPerf != null && latestPerf >= forecast.targetPercent -> "Objectif théorique atteint : surveiller une sortie."
            latestPerf != null && latestPerf > 0.0 -> "Pronostic positif pour le moment : conserver la surveillance."
            latestPerf != null && latestPerf < 0.0 -> "Pronostic sous pression : prudence, scénario à surveiller."
            else -> "Pas encore assez de recul : l'IA garde le pronostic en observation."
        }
        ExplainableInfoRow("Évolution de l'avis IA", opinion, explanationFor("évolution", forecast))
        Text(opinion, color = SoftWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Cette fiche explique l'avis de l'IA, mais aucune vente ni aucun achat ne sont automatiques. Tu gardes toujours la validation finale.",
            color = Color.LightGray,
            fontSize = 16.sp,
            lineHeight = 22.sp
        )
    }
}


@Composable
private fun ExplainableInfoRow(label: String, value: String, explanation: String) {
    var showInfo by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PremiumMuted, fontSize = 15.sp, modifier = Modifier.weight(0.34f))
        Text(value, color = SoftWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.54f))
        TextButton(onClick = { showInfo = true }, modifier = Modifier.weight(0.12f)) {
            Text("?", color = PremiumBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showInfo) {
        PremiumEducationDialog(
            title = label,
            subtitle = "Explication dynamique du pronostic",
            content = explanation,
            onDismiss = { showInfo = false }
        )
    }
}

@Composable
private fun RowLinePremium(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PremiumMuted, fontSize = 15.sp, modifier = Modifier.weight(0.38f))
        Text(value, color = SoftWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.62f))
    }
}

@Composable
private fun TimelineLine(label: String, value: Double?) {
    val color = when {
        value == null -> PremiumMuted
        value >= 0.0 -> Success
        else -> DangerRed
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.LightGray, fontSize = 16.sp)
        Text(if (value == null) "En attente" else String.format(Locale.FRANCE, "%+.2f %%", value), color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}


@Suppress("UNUSED_PARAMETER")
private fun explanationFor(key: String, f: AiForecastEntity): String {
    val targetPrice = f.entryPrice * (1.0 + f.targetPercent / 100.0)
    val stopPrice = f.entryPrice * (1.0 - f.stopPercent / 100.0)
    return when (key.lowercase(Locale.ROOT)) {
        "rsi" -> """
### RSI actuel dans ce pronostic
Le RSI aide à voir si l'action est trop vendue ou trop achetée.

### Comment le lire simplement
Sous 30, l'action peut être en zone de survente. Cela peut annoncer un rebond, surtout pour une opportunité rapide.

### Dans ce pronostic
Catégorie : ${f.strategyLabel}
Score IA : ${f.score}/100
Confiance : ${f.confidence}%

### Impact sur la décision
L'IA ne décide jamais avec le RSI seul. Elle le combine avec MACD, volume, tendance, objectif et risque.
""".trimIndent()
        "macd" -> """
### MACD dans ce pronostic
Le MACD sert à repérer un changement de tendance.

### Comment le lire simplement
Quand le MACD devient haussier, cela peut indiquer que les acheteurs reprennent la main.

### Dans ce pronostic
L'IA l'utilise pour confirmer que le mouvement peut continuer vers l'objectif.
Objectif visé : ${money(targetPrice)} (+${one(f.targetPercent)}%)

### Impact sur la décision
Un MACD favorable augmente la confiance, mais l'IA vérifie aussi le volume et le niveau de risque.
""".trimIndent()
        "ema" -> """
### EMA
L'EMA est une moyenne mobile qui donne plus d'importance aux prix récents.

### Pourquoi l'IA l'utilise
Elle aide à savoir si la tendance récente est favorable.

### Dans ce pronostic
Catégorie : ${f.strategyLabel}
Horizon : ${f.horizonDays} jours

### Interprétation simple
Si les moyennes courtes sont mieux orientées que les longues, l'IA considère que le mouvement est plus propre.
""".trimIndent()
        "sma" -> """
### SMA
La SMA est une moyenne simple des prix.

### Pourquoi elle est utile
Elle montre la tendance générale sans réagir trop fortement aux petites variations.

### Dans ce pronostic
Elle sert de repère complémentaire pour éviter de prendre un signal uniquement sur un mouvement trop court.
""".trimIndent()
        "atr" -> """
### ATR
L'ATR mesure l'amplitude habituelle des mouvements.

### Dans ce pronostic
Prix au signal : ${money(f.entryPrice)}
Stop-loss estimé : ${money(stopPrice)} (-${one(f.stopPercent)}%)

### Pourquoi c'est important
Le stop-loss doit tenir compte du mouvement normal de l'action. Trop proche, il déclenche trop vite. Trop loin, il protège mal ton capital.
""".trimIndent()
        "volume" -> """
### Volume
Le volume indique combien d'actions sont échangées.

### Comment le lire simplement
Un signal avec du volume est plus crédible qu'un signal avec peu d'échanges.

### Dans ce pronostic
L'IA vérifie que le mouvement n'est pas un simple bruit de marché.
""".trimIndent()
        "support" -> """
### Support
Un support est une zone où le prix peut rebondir.

### Dans ce pronostic
Prix au signal : ${money(f.entryPrice)}
Stop-loss : ${money(stopPrice)}

### Pourquoi l'IA l'utilise
Si le prix est proche d'un support, le risque peut être mieux contrôlé.
""".trimIndent()
        "résistance", "resistance" -> """
### Résistance
Une résistance est une zone où le prix peut bloquer ou ralentir.

### Dans ce pronostic
Objectif estimé : ${money(targetPrice)}
Gain attendu : +${one(f.targetPercent)}%

### Pourquoi l'IA l'utilise
La résistance aide à fixer un objectif réaliste et à savoir quand une vente est à envisager.
""".trimIndent()
        "catégorie" -> """
### Catégorie du pronostic
${f.strategyLabel}

### Ce que ça veut dire
${strategyExplanation(f.strategyType)}

### Pourquoi c'est utile
Toutes les opportunités ne se jouent pas pareil. Une opportunité rapide se juge en jours. Un long terme se juge en mois.
""".trimIndent()
        "score ia" -> """
### Score IA
Score actuel : ${f.score}/100

### Ce que ça veut dire
Le score résume la qualité du signal détecté par l'IA.

### Comment l'interpréter
Plus le score est haut, plus les critères techniques sont alignés. Ce score ne garantit jamais un gain, il sert à classer les opportunités.
""".trimIndent()
        "confiance" -> """
### Confiance
Confiance actuelle : ${f.confidence}%

### Ce que ça veut dire
La confiance mesure la cohérence globale du pronostic avec les données disponibles et les cas similaires.

### Dans ce pronostic
${confidenceExplanation(f)}
""".trimIndent()
        "objectif" -> """
### Objectif
Prix au signal : ${money(f.entryPrice)}
Prix cible : ${money(targetPrice)}
Gain attendu : +${one(f.targetPercent)}%

### Pourquoi c'est important
L'objectif indique le niveau où une vente peut être étudiée. L'application t'alerte, mais ne vend jamais automatiquement.
""".trimIndent()
        "stop-loss" -> """
### Stop-loss
Prix de protection : ${money(stopPrice)}
Distance : -${one(f.stopPercent)}%

### Ce que ça veut dire
Si le prix atteint ce niveau, le scénario de départ est probablement invalidé.

### Rôle
Limiter la perte et éviter de rester bloqué dans une mauvaise position.
""".trimIndent()
        "horizon" -> """
### Horizon
Durée estimée : ${f.horizonDays} jours

### Pourquoi c'est important
Une opportunité rapide ne se juge pas comme un long terme. L'horizon évite de conclure trop vite ou trop tard.

### Dans ce pronostic
L'IA suit les étapes J+1, J+3, J+7, J+30 et J+90 selon la stratégie.
""".trimIndent()
        "suivi intelligent" -> """
### Suivi intelligent
Le suivi mesure le résultat du pronostic après plusieurs dates : J+1, J+3, J+7, J+30 et J+90.

### Ce que l'application vérifie
Prix, objectif, stop-loss, évolution du signal et cohérence avec la catégorie.

### Important
Ce suivi fonctionne même si tu n'as pas acheté. C'est ce qui permet à l'IA d'apprendre sur les pronostics non joués.
""".trimIndent()
        "évolution" -> """
### Évolution de l'avis IA
L'IA ne se limite pas au signal initial. Elle observe comment le pronostic évolue dans le temps.

### Exemples
Conserver, surveiller, objectif atteint, stop touché, vente à envisager.

### Important
L'application n'achète pas et ne vend pas toute seule. Elle explique et t'alerte. Tu valides toujours.
""".trimIndent()
        "volatilité" -> """
### Volatilité
La volatilité indique à quel point le prix bouge fortement.

### Dans ce pronostic
Catégorie : ${f.strategyLabel}
Stop-loss : -${one(f.stopPercent)}%
Objectif : +${one(f.targetPercent)}%

### Interprétation
Plus la volatilité est forte, plus le potentiel peut être élevé, mais plus le risque augmente aussi.
""".trimIndent()
        else -> "Cette information aide à comprendre pourquoi l'IA a créé ce pronostic. Elle doit toujours être lue avec le score, le risque, l'objectif et le suivi intelligent."
    }
}

private fun buildWhyText(f: AiForecastEntity): String = when (f.strategyType) {
    "QUICK" -> "L'IA a détecté une configuration pouvant provoquer un mouvement rapide : score élevé, objectif court et suivi serré. Cette catégorie vise quelques jours, pas un investissement long terme."
    "SWING" -> "L'IA estime que le mouvement peut durer plus longtemps qu'un rebond rapide. Le but est de profiter d'une tendance sur plusieurs jours ou semaines."
    "HIGH_VOLATILITY" -> "L'action bouge beaucoup. L'IA peut y voir une opportunité, mais le risque est plus élevé et le stop-loss doit être respecté."
    "LONG_TERM" -> "L'IA classe cette action comme plus adaptée à une vision longue. Le résultat doit être lu sur plusieurs mois."
    "GROWTH" -> "L'IA voit un profil de croissance : potentiel intéressant, mais résultat à juger sur un horizon plus long."
    "DEFENSIVE" -> "L'IA voit un profil plus défensif : moins orienté coup rapide, davantage orienté stabilité."
    else -> "L'IA a retenu ce pronostic car plusieurs critères techniques ont dépassé les seuils de qualité."
}

private fun strategyExplanation(type: String): String = when (type) {
    "QUICK" -> "Opportunité rapide : stratégie de quelques jours visant un petit gain rapide avec une surveillance automatique."
    "SWING" -> "Swing trading : stratégie intermédiaire qui cherche à accompagner une tendance pendant plusieurs jours ou semaines."
    "HIGH_VOLATILITY" -> "Forte volatilité : l'action varie beaucoup. Potentiel élevé, mais risque élevé également."
    "LONG_TERM" -> "Long terme : stratégie à suivre sur plusieurs mois, séparée de la mémoire court terme."
    "GROWTH" -> "Croissance : action potentiellement intéressante pour une progression durable."
    "DEFENSIVE" -> "Défensif : action plus stable, utile pour limiter le risque."
    else -> "Catégorie IA utilisée pour classer le type d'opportunité détecté."
}

private fun confidenceExplanation(f: AiForecastEntity): String = when {
    f.confidence >= 85 -> "Confiance élevée : plusieurs critères vont dans le même sens. L'IA suivra quand même le résultat pour vérifier si elle avait raison."
    f.confidence >= 65 -> "Confiance correcte : le signal est intéressant, mais il faut rester prudent et surveiller le suivi."
    else -> "Confiance limitée : pronostic à observer, pas à suivre aveuglément."
}

private fun indicatorHint(f: AiForecastEntity, key: String): String = when (key) {
    "RSI" -> if (f.strategyType == "QUICK") "Probable zone de rebond ou sortie de survente." else "Mesure la force du mouvement."
    "MACD" -> "Recherche un changement de tendance ou une confirmation."
    "EMA" -> "Compare les tendances courtes et longues pour vérifier le sens du marché."
    "VOLUME" -> "Vérifie si le mouvement est accompagné par assez d'échanges."
    else -> "Indicateur utilisé dans le score IA."
}

private fun volatilityHint(f: AiForecastEntity): String = if (f.strategyType == "HIGH_VOLATILITY") "Élevée : opportunité possible mais risque supérieur." else "Contrôlée selon la catégorie du pronostic."
private fun strategyLabel(type: String): String = when (type) {
    "QUICK" -> "⚡ Opportunité rapide"
    "SWING" -> "📈 Swing trading"
    "HIGH_VOLATILITY" -> "🔥 Forte volatilité"
    "LONG_TERM" -> "🏆 Long terme"
    "GROWTH" -> "🌱 Croissance"
    "DEFENSIVE" -> "🛡 Défensif"
    else -> "🤖 Pronostic IA"
}
private fun money(v: Double): String = String.format(Locale.FRANCE, "%.2f", v)
private fun one(v: Double): String = String.format(Locale.FRANCE, "%.1f", v)

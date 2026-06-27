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
import androidx.compose.material3.AlertDialog
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
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(label, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = { Text(explanation, color = Color.LightGray, fontSize = 16.sp, lineHeight = 23.sp) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Compris", color = PremiumBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF111827),
            titleContentColor = SoftWhite,
            textContentColor = Color.LightGray
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
private fun explanationFor(key: String, f: AiForecastEntity): String = when (key.lowercase(Locale.ROOT)) {
    "rsi" -> "RSI = Relative Strength Index. En simple : il mesure si une action est plutôt trop achetée ou trop vendue. Un RSI très bas peut indiquer une opportunité de rebond, mais ce n'est jamais suffisant seul. L'IA le combine avec le volume, la tendance et le risque."
    "macd" -> "MACD = indicateur de changement de tendance. En simple : il aide à voir si le mouvement commence à repartir à la hausse ou à se retourner à la baisse. Un croisement haussier renforce un signal d'achat, surtout s'il est confirmé par le volume."
    "ema" -> "EMA = moyenne mobile exponentielle. Elle donne plus de poids aux prix récents. En simple : elle aide l'IA à savoir si la tendance récente est positive ou négative. EMA20 > EMA50 est souvent un signe de tendance courte favorable."
    "sma" -> "SMA = moyenne mobile simple. Elle lisse les prix sur une période. En simple : elle permet de voir la tendance générale sans être perturbé par chaque petite variation."
    "atr" -> "ATR = amplitude moyenne des mouvements. En simple : il mesure combien l'action bouge habituellement. L'IA l'utilise surtout pour placer un stop-loss réaliste : assez proche pour protéger, mais pas trop proche pour éviter une sortie sur une variation normale."
    "volume" -> "Le volume indique combien d'actions sont échangées. En simple : un mouvement avec beaucoup de volume est plus crédible qu'un mouvement avec peu d'échanges. C'est une confirmation importante pour éviter les faux signaux."
    "support" -> "Un support est une zone où le prix a souvent tendance à rebondir. En simple : c'est une zone de prix où des acheteurs peuvent revenir. L'IA l'utilise pour comprendre si le risque de baisse est limité."
    "résistance", "resistance" -> "Une résistance est une zone où le prix peut avoir du mal à monter plus haut. En simple : c'est une zone où beaucoup de vendeurs peuvent apparaître. L'IA l'utilise pour fixer un objectif réaliste."
    "catégorie" -> "La catégorie explique le type d'opportunité détectée : opportunité rapide, swing, long terme, forte volatilité, croissance ou défensif. Elle t'aide à comprendre l'horizon et le comportement attendu, sans mélanger toutes les stratégies."
    "score ia" -> "Le score IA résume la qualité technique du signal sur 100. Plus il est haut, plus les critères sont favorables. Mais il ne garantit jamais un gain : il sert à classer les opportunités et à éviter les signaux faibles."
    "confiance" -> "La confiance indique à quel point l'IA estime que le signal est cohérent avec les données disponibles et avec la mémoire des cas similaires. Une forte confiance signifie que plusieurs éléments vont dans le même sens."
    "objectif" -> "L'objectif est le niveau où l'IA estime qu'une vente peut être envisagée. Ce n'est pas une obligation : c'est un repère pour savoir quand un gain attendu est atteint."
    "stop-loss" -> "Le stop-loss est le niveau de protection. Si le prix descend jusque-là, le scénario de départ est probablement invalidé. L'application t'alerte, mais elle ne vend jamais automatiquement."
    "horizon" -> "L'horizon indique la durée normale du pronostic : quelques jours pour une opportunité rapide, plusieurs semaines pour un swing, plusieurs mois pour du long terme. Il évite de juger trop vite une stratégie qui n'a pas le même rythme."
    "suivi intelligent" -> "Le suivi intelligent compare le prix réel après J+1, J+3, J+7, J+30 et J+90. Il sert à mesurer si l'IA avait raison, même si tu n'as pas acheté. C'est ce qui alimente la mémoire et l'apprentissage."
    "évolution" -> "L'évolution de l'avis IA montre comment le pronostic change dans le temps : conserver, surveiller, objectif atteint, stop touché ou vente à envisager. L'idée est de suivre le raisonnement, pas seulement le signal initial."
    "volatilité" -> "La volatilité mesure à quel point le prix varie fortement. Une forte volatilité peut créer des opportunités rapides, mais augmente aussi le risque. L'IA l'utilise pour adapter l'objectif, le stop-loss et la prudence."
    else -> "Cette information aide à comprendre pourquoi l'IA a créé ce pronostic. Elle doit toujours être lue avec le score, le risque, l'objectif et le suivi intelligent."
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

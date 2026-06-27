package com.privateinvest.aitraderpro.repository

import com.privateinvest.aitraderpro.BuildConfig
import com.privateinvest.aitraderpro.data.model.AssetDetail
import com.privateinvest.aitraderpro.data.model.AssetSignal
import com.privateinvest.aitraderpro.data.model.AuditSummary
import com.privateinvest.aitraderpro.data.model.GlobalQuoteDto
import com.privateinvest.aitraderpro.data.model.LearningStats
import com.privateinvest.aitraderpro.data.model.NotificationHistoryItem
import com.privateinvest.aitraderpro.data.model.PortfolioSummary
import com.privateinvest.aitraderpro.data.model.PriceBar
import com.privateinvest.aitraderpro.data.model.RiskRule
import com.privateinvest.aitraderpro.data.model.ScoringResult
import com.privateinvest.aitraderpro.data.model.SignalAction
import com.privateinvest.aitraderpro.data.model.SignalExplanation
import com.privateinvest.aitraderpro.data.model.SimulationPositionItem
import com.privateinvest.aitraderpro.data.model.SimulationSummary
import com.privateinvest.aitraderpro.data.model.SimulationTradeItem
import com.privateinvest.aitraderpro.data.model.SymbolSearchMatchDto
import com.privateinvest.aitraderpro.data.model.TechnicalSnapshot
import com.privateinvest.aitraderpro.data.model.TopConfiguration
import com.privateinvest.aitraderpro.database.AppDatabase
import com.privateinvest.aitraderpro.database.DecisionLogEntity
import com.privateinvest.aitraderpro.database.NotificationHistoryEntity
import com.privateinvest.aitraderpro.database.PositionEntity
import com.privateinvest.aitraderpro.database.SignalEntity
import com.privateinvest.aitraderpro.database.SignalMemoryEntity
import com.privateinvest.aitraderpro.database.SignalOutcomeEntity
import com.privateinvest.aitraderpro.database.SignalResultEntity
import com.privateinvest.aitraderpro.database.SimulationAccountEntity
import com.privateinvest.aitraderpro.database.TradeEntity
import com.privateinvest.aitraderpro.network.MarketApiService
import com.privateinvest.aitraderpro.notifications.TraderNotificationManager
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MarketRepositoryImpl(
    private val api: MarketApiService,
    private val db: AppDatabase,
    private val adaptiveWeightsManager: AdaptiveWeightsManager,
    private val notificationManager: TraderNotificationManager,
    private val riskSettings: RiskSettings = RiskSettings()
) : MarketRepository {

    private val watchlistSymbols = listOf(
        "AAPL" to "Apple",
        "MSFT" to "Microsoft",
        "NVDA" to "NVIDIA",
        "AMD" to "AMD",
        "ASML" to "ASML"
    )

    override suspend fun getWatchlist(): List<AssetSignal> = getSignals()

    override suspend fun getPortfolio(): PortfolioSummary {
        val simulation = getSimulationSummary()
        return PortfolioSummary(
            capital = simulation.totalValue,
            dayPnL = simulation.totalPerformance,
            monthPnL = simulation.totalPerformance,
            openPositions = simulation.openPositions,
            riskLevel = when {
                simulation.openPositions >= riskSettings.maxOpenPositions -> "Élevé"
                simulation.openPositions >= riskSettings.maxOpenPositions / 2 -> "Modéré"
                else -> "Faible"
            }
        )
    }

    override suspend fun getSimulationSummary(): SimulationSummary {
        val account = ensureSimulationAccount()
        val snapshots = loadSimulationSnapshots()
        val investedCapital = snapshots.sumOf { it.marketValue }
        val totalValue = account.availableCash + investedCapital
        val totalPerformanceAmount = totalValue - account.initialCapital
        val totalPerformancePercent = if (account.initialCapital > 0.0) {
            (totalPerformanceAmount / account.initialCapital) * 100.0
        } else {
            0.0
        }
        val outcomes = db.signalOutcomeDao().getAll()
        return SimulationSummary(
            initialCapital = formatCurrency(account.initialCapital),
            availableCash = formatCurrency(account.availableCash),
            investedCapital = formatCurrency(investedCapital),
            totalValue = formatCurrency(totalValue),
            totalPerformance = "${formatSignedCurrency(totalPerformanceAmount)} (${formatSignedPercent(totalPerformancePercent)})",
            openPositions = snapshots.size,
            successRateJ1 = String.format(Locale.FRANCE, "%.1f %%", PerformanceAnalyzer.computeSuccessRate(outcomes) { it.successJ1 }),
            successRateJ7 = String.format(Locale.FRANCE, "%.1f %%", PerformanceAnalyzer.computeSuccessRate(outcomes) { it.successJ7 }),
            successRateJ30 = String.format(Locale.FRANCE, "%.1f %%", PerformanceAnalyzer.computeSuccessRate(outcomes) { it.successJ30 })
        )
    }

    override suspend fun getSimulationPositions(): List<SimulationPositionItem> = loadSimulationSnapshots().map {
        SimulationPositionItem(
            symbol = it.symbol,
            quantity = it.quantity,
            averagePrice = formatCurrency(it.averagePrice),
            marketPrice = formatCurrency(it.marketPrice),
            marketValue = formatCurrency(it.marketValue),
            performance = formatSignedPercent(it.performancePercent)
        )
    }

    override suspend fun getSimulationHistory(): List<SimulationTradeItem> {
        return db.tradeDao().getAll().map { trade ->
            SimulationTradeItem(
                symbol = trade.symbol,
                side = trade.side,
                quantity = trade.quantity,
                price = formatCurrency(trade.price),
                createdAtLabel = formatDateTime(trade.createdAt),
                followUp = buildFollowUpText(trade.symbol)
            )
        }
    }

    override suspend fun getNotificationHistory(): List<NotificationHistoryItem> {
        return db.notificationHistoryDao().getAll().map {
            NotificationHistoryItem(
                title = it.title,
                message = it.message,
                level = it.level,
                symbol = it.symbol,
                createdAtLabel = formatDateTime(it.createdAt),
                status = it.status
            )
        }
    }

    override suspend fun getSignals(): List<AssetSignal> {
        // V1.3 DataFreshnessGuard — bloquer si aucune donnée réelle disponible
        if (!DataFreshnessGuard.canGenerateSignal()) {
            // Retourner le cache — aucune génération avec données absentes
            return db.signalDao().getAllSignals().map { it.toDomain() }
        }

        if (BuildConfig.ALPHA_VANTAGE_API_KEY == "YOUR_API_KEY_HERE") {
            return db.signalDao().getAllSignals().map { it.toDomain() }
        }

        val openPositions = db.positionDao().getAll().size
        val weights = adaptiveWeightsManager.currentWeights()
        val signals = mutableListOf<AssetSignal>()

        for ((symbol, name) in watchlistSymbols) {
            runCatching {
                val quote = api.getQuote(symbol = symbol).globalQuote
                val historical = api.getHistorical(symbol = symbol)
                val bars = parseHistorical(historical)
                val currentPrice = quote?.price?.toDoubleOrNull() ?: bars.lastOrNull()?.close ?: 0.0
                refreshLearningOutcomes(symbol, currentPrice)
                val detail = buildAssetDetail(symbol, name, quote, bars, openPositions, weights)
                val assetSignal = detail.toAssetSignal()
                db.signalDao().insert(
                    SignalEntity(
                        symbol = assetSignal.symbol,
                        name = assetSignal.name,
                        score = assetSignal.score,
                        confidence = assetSignal.confidence,
                        target = String.format(Locale.FRANCE, "%.2f", detail.quotePrice),
                        risk = assetSignal.risk,
                        action = assetSignal.action.name,
                        explanation = assetSignal.explanation.joinToString(" | "),
                        dateCreated = System.currentTimeMillis()
                    )
                )
                db.decisionLogDao().insert(
                    DecisionLogEntity(
                        date = System.currentTimeMillis(),
                        symbol = symbol,
                        score = assetSignal.score,
                        decision = assetSignal.action.name,
                        weightsUsed = weights.entries.joinToString(",") { "${it.key}:${String.format(Locale.FRANCE, "%.2f", it.value)}" }
                    )
                )
                registerSignalNotificationIfNeeded(assetSignal)
                signals += assetSignal
            }
        }

        return if (signals.isNotEmpty()) signals else db.signalDao().getAllSignals().map { it.toDomain() }
    }

    override suspend fun getMarketData(symbol: String): AssetDetail? {
        if (BuildConfig.ALPHA_VANTAGE_API_KEY == "YOUR_API_KEY_HERE") return null
        val name = watchlistSymbols.find { it.first == symbol }?.second ?: symbol
        val quote = api.getQuote(symbol = symbol).globalQuote
        val bars = parseHistorical(api.getHistorical(symbol = symbol))
        val currentPrice = quote?.price?.toDoubleOrNull() ?: bars.lastOrNull()?.close ?: 0.0
        refreshLearningOutcomes(symbol, currentPrice)
        return buildAssetDetail(symbol, name, quote, bars, db.positionDao().getAll().size, adaptiveWeightsManager.currentWeights())
    }

    override suspend fun getAuditSummary(): AuditSummary {
        val memories = db.signalMemoryDao().getAll()
        val outcomes = db.signalOutcomeDao().getAll()
        val performances = outcomes.mapNotNull { it.performanceJ30 ?: it.performanceJ7 ?: it.performanceJ1 }
        val updatedWeights = adaptiveWeightsManager.updateWeightsFromResults(memories, outcomes)
        val learningStats = buildLearningStats(memories, outcomes)
        return AuditSummary(
            signalCount = memories.size,
            winRate = PerformanceAnalyzer.computeWinRate(performances),
            averageGain = PerformanceAnalyzer.computeAverageGain(performances),
            averageLoss = PerformanceAnalyzer.computeAverageLoss(performances),
            profitFactor = PerformanceAnalyzer.computeProfitFactor(performances),
            maxDrawdown = PerformanceAnalyzer.computeMaxDrawdown(performances),
            currentWeights = updatedWeights,
            learningStats = learningStats.copy(weightEvolution = adaptiveWeightsManager.historyLabels())
        )
    }

    override suspend fun getLearningStats(): LearningStats = buildLearningStats(
        memories = db.signalMemoryDao().getAll(),
        outcomes = db.signalOutcomeDao().getAll()
    ).copy(weightEvolution = adaptiveWeightsManager.historyLabels())

    override suspend fun getRiskRules(): List<RiskRule> = listOf(
        RiskRule("maxPositionPercent", "${riskSettings.maxPositionPercent}%"),
        RiskRule("maxDailyLoss", "${riskSettings.maxDailyLoss}%"),
        RiskRule("maxOpenPositions", riskSettings.maxOpenPositions.toString()),
        RiskRule("capital virtuel initial", formatCurrency(ensureSimulationAccount().initialCapital)),
        RiskRule("validation utilisateur", "obligatoire")
    )

    override suspend fun getLatestAlert(): AssetSignal? = getSignals().firstOrNull { it.action == SignalAction.ACHETER }

    override suspend fun searchAsset(keywords: String): List<SymbolSearchMatchDto> {
        if (BuildConfig.ALPHA_VANTAGE_API_KEY == "YOUR_API_KEY_HERE") return emptyList()
        return api.searchAsset(keywords = keywords).bestMatches
    }

    override suspend fun confirmSimulationOrder(symbol: String): Boolean {
        val detail = getMarketData(symbol) ?: return false
        val account = ensureSimulationAccount()
        val price = detail.quotePrice.takeIf { it > 0.0 } ?: return false
        if (account.availableCash < price) return false

        val now = System.currentTimeMillis()
        db.simulationAccountDao().upsert(
            account.copy(
                availableCash = account.availableCash - price,
                updatedAt = now
            )
        )

        val memoryId = db.signalMemoryDao().insert(
            SignalMemoryEntity(
                symbol = detail.symbol,
                signal = detail.scoring.signal.name,
                scoreTechnique = detail.scoring.scoreTechnique,
                scoreRisque = detail.scoring.scoreRisque,
                rsi = detail.technicalSnapshot.rsi,
                macd = detail.technicalSnapshot.macd,
                macdSignal = detail.technicalSnapshot.macdSignal,
                ema20 = detail.technicalSnapshot.ema20,
                ema50 = detail.technicalSnapshot.ema50,
                ema200 = detail.technicalSnapshot.ema200,
                atr14 = detail.technicalSnapshot.atr14,
                volatility = detail.technicalSnapshot.volatility,
                averageVolume = detail.technicalSnapshot.averageVolume,
                currentVolume = detail.technicalSnapshot.currentVolume,
                createdAt = now
            )
        )
        db.signalOutcomeDao().insert(SignalOutcomeEntity(signalId = memoryId))
        db.tradeDao().insert(
            TradeEntity(
                symbol = detail.symbol,
                side = "BUY_SIMULATION",
                quantity = 1.0,
                price = price,
                createdAt = now
            )
        )

        val existingPosition = db.positionDao().findBySymbol(detail.symbol)
        val updatedPosition = if (existingPosition == null) {
            PositionEntity(
                symbol = detail.symbol,
                quantity = 1.0,
                averagePrice = price,
                marketValue = price,
                updatedAt = now
            )
        } else {
            val newQuantity = existingPosition.quantity + 1.0
            val newAverage = ((existingPosition.averagePrice * existingPosition.quantity) + price) / newQuantity
            existingPosition.copy(
                quantity = newQuantity,
                averagePrice = newAverage,
                marketValue = newQuantity * price,
                updatedAt = now
            )
        }
        db.positionDao().upsert(updatedPosition)

        db.signalResultDao().insert(
            SignalResultEntity(
                symbol = detail.symbol,
                signal = detail.scoring.signal.name,
                score = detail.scoring.scoreTechnique,
                entryPrice = price,
                exitPrice = price,
                profitPercent = 0.0,
                dateCreated = now,
                dateClosed = null
            )
        )
        db.decisionLogDao().insert(
            DecisionLogEntity(
                date = now,
                symbol = detail.symbol,
                score = detail.scoring.scoreTechnique,
                decision = "BUY_SIMULATION",
                weightsUsed = adaptiveWeightsManager.currentWeights().entries.joinToString(",") { "${it.key}:${String.format(Locale.FRANCE, "%.2f", it.value)}" }
            )
        )
        registerEventNotification(
            title = "Achat simulé ${detail.symbol}",
            message = "Achat virtuel exécuté à ${formatCurrency(price)}. Suivi J+1 / J+7 / J+30 activé.",
            level = "SUCCESS",
            symbol = detail.symbol,
            duplicateKey = "simulation-buy:${detail.symbol}:$now"
        )
        return true
    }

    override suspend fun sellSimulationPosition(symbol: String): Boolean {
        val position = db.positionDao().findBySymbol(symbol) ?: return false
        val account = ensureSimulationAccount()
        val now = System.currentTimeMillis()
        val marketPrice = currentMarketPrice(symbol, position.averagePrice)
        val proceeds = marketPrice * position.quantity
        db.simulationAccountDao().upsert(
            account.copy(
                availableCash = account.availableCash + proceeds,
                updatedAt = now
            )
        )
        db.tradeDao().insert(
            TradeEntity(
                symbol = symbol,
                side = "SELL_SIMULATION",
                quantity = position.quantity,
                price = marketPrice,
                createdAt = now
            )
        )
        db.positionDao().deleteBySymbol(symbol)

        db.signalResultDao().findLatestOpenBySymbol(symbol)?.let { openResult ->
            val performance = if (openResult.entryPrice > 0.0) ((marketPrice - openResult.entryPrice) / openResult.entryPrice) * 100.0 else 0.0
            db.signalResultDao().update(
                openResult.copy(
                    exitPrice = marketPrice,
                    profitPercent = performance,
                    dateClosed = now
                )
            )
        }
        db.decisionLogDao().insert(
            DecisionLogEntity(
                date = now,
                symbol = symbol,
                score = 0,
                decision = "SELL_SIMULATION",
                weightsUsed = adaptiveWeightsManager.currentWeights().entries.joinToString(",") { "${it.key}:${String.format(Locale.FRANCE, "%.2f", it.value)}" }
            )
        )
        registerEventNotification(
            title = "Vente simulée $symbol",
            message = "Position virtuelle clôturée à ${formatCurrency(marketPrice)}.",
            level = "INFO",
            symbol = symbol,
            duplicateKey = "simulation-sell:$symbol:$now"
        )
        return true
    }

    private suspend fun ensureSimulationAccount(): SimulationAccountEntity {
        val existing = db.simulationAccountDao().getAccount()
        if (existing != null) return existing
        val default = SimulationAccountEntity(
            id = 1,
            initialCapital = DEFAULT_SIMULATION_CAPITAL,
            availableCash = DEFAULT_SIMULATION_CAPITAL,
            updatedAt = System.currentTimeMillis()
        )
        db.simulationAccountDao().upsert(default)
        return default
    }

    private suspend fun registerSignalNotificationIfNeeded(signal: AssetSignal) {
        if (signal.action != SignalAction.ACHETER) return
        val now = System.currentTimeMillis()
        val duplicateKey = "signal:${signal.symbol}:${signal.action.name}:${now / MILLIS_PER_DAY}"
        if (db.notificationHistoryDao().findByDuplicateKey(duplicateKey) != null) return
        val title = "Signal IA ${signal.action.name} · ${signal.symbol}"
        val message = "Score ${signal.score}/100 · Confiance ${signal.confidence}% · Risque ${signal.risk}"
        val status = notificationManager.publishNotification(title, message, duplicateKey.hashCode())
        db.notificationHistoryDao().insert(
            NotificationHistoryEntity(
                title = title,
                message = message,
                level = "INFO",
                symbol = signal.symbol,
                duplicateKey = duplicateKey,
                createdAt = now,
                status = status
            )
        )
    }

    private suspend fun registerEventNotification(
        title: String,
        message: String,
        level: String,
        symbol: String?,
        duplicateKey: String
    ) {
        if (db.notificationHistoryDao().findByDuplicateKey(duplicateKey) != null) return
        val status = notificationManager.publishNotification(title, message, duplicateKey.hashCode())
        db.notificationHistoryDao().insert(
            NotificationHistoryEntity(
                title = title,
                message = message,
                level = level,
                symbol = symbol,
                duplicateKey = duplicateKey,
                createdAt = System.currentTimeMillis(),
                status = status
            )
        )
    }

    private suspend fun loadSimulationSnapshots(): List<SimulationSnapshot> {
        val now = System.currentTimeMillis()
        return db.positionDao().getAll().map { position ->
            val marketPrice = currentMarketPrice(position.symbol, position.averagePrice)
            val marketValue = marketPrice * position.quantity
            db.positionDao().upsert(position.copy(marketValue = marketValue, updatedAt = now))
            SimulationSnapshot(
                symbol = position.symbol,
                quantity = position.quantity,
                averagePrice = position.averagePrice,
                marketPrice = marketPrice,
                marketValue = marketValue,
                performancePercent = if (position.averagePrice > 0.0) ((marketPrice - position.averagePrice) / position.averagePrice) * 100.0 else 0.0
            )
        }
    }

    private suspend fun currentMarketPrice(symbol: String, fallback: Double): Double {
        if (BuildConfig.ALPHA_VANTAGE_API_KEY == "YOUR_API_KEY_HERE") return fallback
        return runCatching {
            api.getQuote(symbol = symbol).globalQuote?.price?.toDoubleOrNull() ?: fallback
        }.getOrDefault(fallback)
    }

    private suspend fun buildFollowUpText(symbol: String): String {
        val memory = db.signalMemoryDao().findBySymbol(symbol).firstOrNull() ?: return "Suivi J+1 / J+7 / J+30 en attente"
        val outcome = db.signalOutcomeDao().findBySignalId(memory.id) ?: return "Suivi J+1 / J+7 / J+30 en attente"
        val parts = mutableListOf<String>()
        outcome.performanceJ1?.let { parts += "J+1 ${formatSignedPercent(it)}" }
        outcome.performanceJ7?.let { parts += "J+7 ${formatSignedPercent(it)}" }
        outcome.performanceJ30?.let { parts += "J+30 ${formatSignedPercent(it)}" }
        return if (parts.isEmpty()) "Suivi J+1 / J+7 / J+30 en attente" else parts.joinToString(" · ")
    }

    private fun parseHistorical(jsonObject: JsonObject): List<PriceBar> {
        val series = jsonObject["Time Series (Daily)"]?.jsonObject ?: return emptyList()
        return series.entries.mapNotNull { (date, node) ->
            val obj = node.jsonObject
            PriceBar(
                date = date,
                open = obj["1. open"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                high = obj["2. high"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                low = obj["3. low"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                close = obj["4. close"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                volume = obj["5. volume"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            )
        }.sortedBy { it.date }
    }

    private suspend fun buildAssetDetail(
        symbol: String,
        name: String,
        quote: GlobalQuoteDto?,
        bars: List<PriceBar>,
        openPositionsCount: Int,
        weights: Map<String, Double>
    ): AssetDetail {
        val closes = bars.map { it.close }
        val highs = bars.map { it.high }
        val lows = bars.map { it.low }
        val volumes = bars.map { it.volume }
        val macd = TechnicalIndicators.calculateMACD(closes, 12, 26, 9)
        val snapshot = TechnicalSnapshot(
            rsi = TechnicalIndicators.calculateRSI(closes, 14),
            macd = macd.macd,
            macdSignal = macd.signal,
            ema20 = TechnicalIndicators.calculateEMA(closes.takeLast(20), 20),
            ema50 = TechnicalIndicators.calculateEMA(closes.takeLast(50), 50),
            ema200 = TechnicalIndicators.calculateEMA(closes.takeLast(200), 200),
            atr14 = TechnicalIndicators.calculateATR(highs, lows, closes, 14),
            volatility = TechnicalIndicators.calculateVolatility(closes.takeLast(60)),
            averageVolume = volumes.takeLast(20).averageOrZero(),
            currentVolume = volumes.lastOrNull() ?: 0.0
        )
        val baseScoring = HybridScoringEngine.evaluate(snapshot, weights, riskSettings, openPositionsCount)
        val patternInsight = PatternEvaluator.evaluate(
            snapshot = snapshot,
            memories = db.signalMemoryDao().getAll(),
            outcomesBySignalId = db.signalOutcomeDao().getAll().associateBy { it.signalId }
        )
        val enrichedScoring = enrichScoring(baseScoring, patternInsight)
        return AssetDetail(
            symbol = symbol,
            name = name,
            quotePrice = quote?.price?.toDoubleOrNull() ?: closes.lastOrNull() ?: 0.0,
            scoring = enrichedScoring,
            technicalSnapshot = snapshot
        )
    }

    private fun enrichScoring(base: ScoringResult, insight: PatternInsight): ScoringResult {
        val impact = insight.maxImpactPoints
        val adjustedScore = (base.scoreTechnique + impact).coerceIn(0, 100)
        val adjustedSignal = when {
            adjustedScore > riskSettings.buyThreshold && base.scoreRisque <= riskSettings.acceptableRiskThreshold -> SignalAction.ACHETER
            adjustedScore >= 55 -> SignalAction.SURVEILLER
            adjustedScore >= 40 -> SignalAction.CONSERVER
            else -> SignalAction.VENDRE
        }
        return base.copy(
            scoreTechnique = adjustedScore,
            signal = adjustedSignal,
            explanation = SignalExplanation(
                reasons = base.explanation.reasons,
                similarConfigurations = insight.similarCount,
                historicalWinRate = insight.historicalWinRate,
                averageGain = insight.historicalAverageGain,
                averageLoss = insight.historicalAverageLoss,
                maxImpactPoints = insight.maxImpactPoints
            )
        )
    }

    private suspend fun refreshLearningOutcomes(symbol: String, currentPrice: Double) {
        if (currentPrice <= 0.0) return
        val memories = db.signalMemoryDao().findBySymbol(symbol)
        val trades = db.tradeDao().findBySymbol(symbol)
        val now = System.currentTimeMillis()
        memories.forEach { memory ->
            val outcome = db.signalOutcomeDao().findBySignalId(memory.id) ?: return@forEach
            val entryTrade = trades.minByOrNull { abs(it.createdAt - memory.createdAt) } ?: return@forEach
            val entryPrice = entryTrade.price.takeIf { it > 0.0 } ?: return@forEach
            val daysElapsed = ((now - memory.createdAt) / MILLIS_PER_DAY).toInt()
            var updated = outcome
            var changed = false

            if (daysElapsed >= 1 && updated.priceJ1 == null) {
                updated = updated.copy(
                    priceJ1 = currentPrice,
                    performanceJ1 = performanceFor(memory.signal, entryPrice, currentPrice),
                    successJ1 = successFor(memory.signal, performanceFor(memory.signal, entryPrice, currentPrice))
                )
                changed = true
            }
            if (daysElapsed >= 7 && updated.priceJ7 == null) {
                updated = updated.copy(
                    priceJ7 = currentPrice,
                    performanceJ7 = performanceFor(memory.signal, entryPrice, currentPrice),
                    successJ7 = successFor(memory.signal, performanceFor(memory.signal, entryPrice, currentPrice))
                )
                changed = true
            }
            if (daysElapsed >= 30 && updated.priceJ30 == null) {
                val finalPerformance = performanceFor(memory.signal, entryPrice, currentPrice)
                updated = updated.copy(
                    priceJ30 = currentPrice,
                    performanceJ30 = finalPerformance,
                    successJ30 = successFor(memory.signal, finalPerformance)
                )
                db.signalResultDao().insert(
                    SignalResultEntity(
                        symbol = memory.symbol,
                        signal = memory.signal,
                        score = memory.scoreTechnique,
                        entryPrice = entryPrice,
                        exitPrice = currentPrice,
                        profitPercent = finalPerformance,
                        dateCreated = memory.createdAt,
                        dateClosed = now
                    )
                )
                changed = true
            }

            if (changed) {
                db.signalOutcomeDao().update(updated)
            }
        }
    }

    private suspend fun buildLearningStats(
        memories: List<SignalMemoryEntity>,
        outcomes: List<SignalOutcomeEntity>
    ): LearningStats {
        val topWinning = buildTopConfigurations(memories, outcomes, winners = true)
        val topLosing = buildTopConfigurations(memories, outcomes, winners = false)
        return LearningStats(
            memorizedConfigurations = memories.size,
            successRateJ1 = PerformanceAnalyzer.computeSuccessRate(outcomes) { it.successJ1 },
            successRateJ7 = PerformanceAnalyzer.computeSuccessRate(outcomes) { it.successJ7 },
            successRateJ30 = PerformanceAnalyzer.computeSuccessRate(outcomes) { it.successJ30 },
            topWinningConfigurations = topWinning,
            topLosingConfigurations = topLosing,
            weightEvolution = adaptiveWeightsManager.historyLabels()
        )
    }

    private fun buildTopConfigurations(
        memories: List<SignalMemoryEntity>,
        outcomes: List<SignalOutcomeEntity>,
        winners: Boolean
    ): List<TopConfiguration> {
        val outcomesBySignalId = outcomes.associateBy { it.signalId }
        return memories.groupBy { configurationLabel(it) }
            .mapNotNull { (label, items) ->
                val performances = items.mapNotNull { outcomesBySignalId[it.id]?.preferredPerformance() }
                if (performances.isEmpty()) return@mapNotNull null
                val average = performances.average()
                val winRate = performances.count { it > 0.0 } * 100.0 / performances.size
                TopConfiguration(
                    label = label,
                    occurrences = items.size,
                    winRate = winRate,
                    averagePerformance = average
                )
            }
            .filter { if (winners) it.averagePerformance > 0.0 else it.averagePerformance < 0.0 }
            .sortedByDescending { if (winners) it.averagePerformance else -it.averagePerformance }
            .take(3)
    }

    private fun configurationLabel(memory: SignalMemoryEntity): String = buildList {
        add(if (memory.rsi in 50.0..70.0) "RSI haussier" else "RSI faible")
        add(if (memory.macd > memory.macdSignal) "MACD positif" else "MACD neutre")
        add(if (memory.ema20 > memory.ema50 && memory.ema50 > memory.ema200) "Tendance EMA" else "EMA mixte")
        add(if (memory.currentVolume >= memory.averageVolume) "Volume fort" else "Volume faible")
        add(if (memory.volatility <= 35.0) "Volatilité modérée" else "Volatilité haute")
    }.joinToString(" / ")

    private fun performanceFor(signal: String, entryPrice: Double, currentPrice: Double): Double {
        if (entryPrice <= 0.0) return 0.0
        val longPerformance = ((currentPrice - entryPrice) / entryPrice) * 100.0
        return if (signal == SignalAction.VENDRE.name) -longPerformance else longPerformance
    }

    private fun successFor(signal: String, performance: Double): Boolean = when (signal) {
        SignalAction.VENDRE.name -> performance > 0.0
        SignalAction.CONSERVER.name -> performance >= -1.0
        else -> performance > 0.0
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private fun SignalOutcomeEntity.preferredPerformance(): Double? = performanceJ30 ?: performanceJ7 ?: performanceJ1

    private fun AssetDetail.toAssetSignal(): AssetSignal = AssetSignal(
        symbol = symbol,
        name = name,
        score = scoring.scoreTechnique,
        confidence = (100 - scoring.scoreRisque).coerceIn(0, 100),
        target = String.format(Locale.FRANCE, "%.2f", quotePrice),
        risk = scoring.scoreRisque.toString(),
        action = scoring.signal,
        explanation = scoring.explanation.reasons
    )

    private fun SignalEntity.toDomain(): AssetSignal = AssetSignal(
        symbol = symbol,
        name = name,
        score = score,
        confidence = confidence,
        target = target,
        risk = risk,
        action = SignalAction.valueOf(action),
        explanation = explanation.split(" | ")
    )

    private fun formatCurrency(value: Double): String = String.format(Locale.FRANCE, "%.2f €", value)

    private fun formatSignedCurrency(value: Double): String = if (value >= 0) {
        "+${formatCurrency(value)}"
    } else {
        "-${formatCurrency(abs(value))}"
    }

    private fun formatSignedPercent(value: Double): String = if (value >= 0) {
        String.format(Locale.FRANCE, "+%.2f %%", value)
    } else {
        String.format(Locale.FRANCE, "%.2f %%", value)
    }

    private fun formatDateTime(timestamp: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(timestamp))

    private data class SimulationSnapshot(
        val symbol: String,
        val quantity: Double,
        val averagePrice: Double,
        val marketPrice: Double,
        val marketValue: Double,
        val performancePercent: Double
    )

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        private const val DEFAULT_SIMULATION_CAPITAL = 10000.0
    }
}

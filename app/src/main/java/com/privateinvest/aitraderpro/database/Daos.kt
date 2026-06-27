package com.privateinvest.aitraderpro.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id DESC")
    suspend fun getAll(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
}

@Dao
interface SignalDao {
    @Query("SELECT * FROM signals ORDER BY dateCreated DESC")
    suspend fun getAllSignals(): List<SignalEntity>

    @Query("SELECT * FROM signals WHERE symbol = :symbol ORDER BY dateCreated DESC LIMIT 1")
    suspend fun findLatestBySymbol(symbol: String): SignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(signal: SignalEntity): Long
}

@Dao
interface SignalResultDao {
    @Query("SELECT * FROM signal_results ORDER BY dateCreated DESC")
    suspend fun getAll(): List<SignalResultEntity>

    @Query("SELECT * FROM signal_results WHERE symbol = :symbol AND dateClosed IS NULL ORDER BY dateCreated DESC LIMIT 1")
    suspend fun findLatestOpenBySymbol(symbol: String): SignalResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: SignalResultEntity): Long

    @Update
    suspend fun update(result: SignalResultEntity)
}

@Dao
interface SignalMemoryDao {
    @Query("SELECT * FROM signal_memory ORDER BY createdAt DESC")
    suspend fun getAll(): List<SignalMemoryEntity>

    @Query("SELECT * FROM signal_memory WHERE symbol = :symbol ORDER BY createdAt DESC")
    suspend fun findBySymbol(symbol: String): List<SignalMemoryEntity>

    @Query("SELECT * FROM signal_memory WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SignalMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SignalMemoryEntity): Long

    @Update
    suspend fun update(item: SignalMemoryEntity)

    @Delete
    suspend fun delete(item: SignalMemoryEntity)
}

@Dao
interface SignalOutcomeDao {
    @Query("SELECT * FROM signal_outcome ORDER BY id DESC")
    suspend fun getAll(): List<SignalOutcomeEntity>

    @Query("SELECT * FROM signal_outcome WHERE signalId = :signalId LIMIT 1")
    suspend fun findBySignalId(signalId: Long): SignalOutcomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SignalOutcomeEntity): Long

    @Update
    suspend fun update(item: SignalOutcomeEntity)

    @Delete
    suspend fun delete(item: SignalOutcomeEntity)
}

@Dao
interface PositionDao {
    @Query("SELECT * FROM positions ORDER BY updatedAt DESC")
    suspend fun getAll(): List<PositionEntity>

    @Query("SELECT * FROM positions WHERE symbol = :symbol LIMIT 1")
    suspend fun findBySymbol(symbol: String): PositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(positions: List<PositionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PositionEntity): Long

    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY createdAt DESC")
    suspend fun getAll(): List<TradeEntity>

    @Query("SELECT * FROM trades WHERE symbol = :symbol ORDER BY createdAt DESC")
    suspend fun findBySymbol(symbol: String): List<TradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: TradeEntity): Long
}

@Dao
interface SimulationAccountDao {
    @Query("SELECT * FROM simulation_account WHERE id = 1 LIMIT 1")
    suspend fun getAccount(): SimulationAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: SimulationAccountEntity): Long
}

@Dao
interface NotificationHistoryDao {
    @Query("SELECT * FROM notification_history ORDER BY createdAt DESC")
    suspend fun getAll(): List<NotificationHistoryEntity>

    @Query("SELECT * FROM notification_history WHERE duplicateKey = :duplicateKey LIMIT 1")
    suspend fun findByDuplicateKey(duplicateKey: String): NotificationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NotificationHistoryEntity): Long
}


@Dao
interface FavoriteAssetDao {
    @Query("SELECT * FROM favorite_assets ORDER BY createdAt DESC")
    suspend fun getAll(): List<FavoriteAssetEntity>

    @Query("SELECT * FROM favorite_assets WHERE symbol = :symbol LIMIT 1")
    suspend fun findBySymbol(symbol: String): FavoriteAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteAssetEntity)

    @Delete
    suspend fun delete(item: FavoriteAssetEntity)
}

@Dao
interface PortfolioGoalDao {
    @Query("SELECT * FROM portfolio_goals WHERE id = 1 LIMIT 1")
    suspend fun getCurrent(): PortfolioGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PortfolioGoalEntity)
}


@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLatest(limit: Int = 100): List<SecurityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SecurityLogEntity): Long

    @Query("DELETE FROM security_logs WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface WeightHistoryDao {
    @Query("SELECT * FROM weight_history ORDER BY recordedAt DESC")
    suspend fun getAll(): List<WeightHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WeightHistoryEntity>)
}

@Dao
interface DecisionLogDao {
    @Query("SELECT * FROM decision_log ORDER BY date DESC")
    suspend fun getAll(): List<DecisionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DecisionLogEntity)
}

@Dao
interface StrategyFollowUpDao {
    @Query("SELECT * FROM strategy_followups ORDER BY openedAt DESC")
    suspend fun getAll(): List<StrategyFollowUpEntity>

    @Query("SELECT * FROM strategy_followups WHERE status = 'ACTIVE' OR status = 'SELL_ALERT' ORDER BY openedAt DESC")
    suspend fun getActive(): List<StrategyFollowUpEntity>

    @Query("SELECT * FROM strategy_followups WHERE status = 'CLOSED' ORDER BY closedAt DESC")
    suspend fun getClosed(): List<StrategyFollowUpEntity>

    @Query("SELECT * FROM strategy_followups WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): StrategyFollowUpEntity?

    @Query("SELECT * FROM strategy_followups WHERE status = 'SELL_ALERT' ORDER BY openedAt DESC")
    suspend fun getSellAlerts(): List<StrategyFollowUpEntity>

    @Query("SELECT COUNT(*) FROM strategy_followups WHERE status = 'SELL_ALERT'")
    suspend fun countAlerts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StrategyFollowUpEntity): Long

    @Update
    suspend fun update(item: StrategyFollowUpEntity)

    @Query("DELETE FROM strategy_followups WHERE id = :id")
    suspend fun deleteById(id: Long)
}

// ─── MODULE PRONOSTICS IA ─────────────────────────────────────────────────────

@Dao
interface AiForecastDao {
    // Lecture générale
    @Query("SELECT * FROM ai_forecasts ORDER BY createdAt DESC")
    suspend fun getAll(): List<AiForecastEntity>

    // Pronostics par type de mémoire (CT ou LT) — mémoires indépendantes
    @Query("SELECT * FROM ai_forecasts WHERE memoryType = :memoryType ORDER BY createdAt DESC")
    suspend fun getByMemoryType(memoryType: String): List<AiForecastEntity>

    // Pronostics actifs (non joués) par type de mémoire
    @Query("SELECT * FROM ai_forecasts WHERE memoryType = :memoryType AND userAction = 'NONE' AND status = 'ACTIVE' ORDER BY score DESC")
    suspend fun getActivePendingByMemoryType(memoryType: String): List<AiForecastEntity>

    // Pronostics par bucket mémoire (7J, 30J, 90J, 3M, 6M, 12M)
    @Query("SELECT * FROM ai_forecasts WHERE memoryBucket = :bucket ORDER BY createdAt DESC")
    suspend fun getByBucket(bucket: String): List<AiForecastEntity>

    // Pronostics par stratégie
    @Query("SELECT * FROM ai_forecasts WHERE strategyType = :strategyType ORDER BY createdAt DESC")
    suspend fun getByStrategy(strategyType: String): List<AiForecastEntity>

    // Pronostics non joués (userAction = NONE) — pour les alertes
    @Query("SELECT * FROM ai_forecasts WHERE userAction = 'NONE' AND status = 'ACTIVE' ORDER BY score DESC")
    suspend fun getAllPending(): List<AiForecastEntity>

    // Pronostics joués (userAction = PLAYED) — pour comparaison vs signal joué
    @Query("SELECT * FROM ai_forecasts WHERE userAction = 'PLAYED' ORDER BY createdAt DESC")
    suspend fun getAllPlayed(): List<AiForecastEntity>

    // Pronostics non joués remarquables (perf > seuil) — pour alertes
    @Query("""
        SELECT f.* FROM ai_forecasts f
        INNER JOIN ai_forecast_outcomes o ON f.id = o.forecastId
        WHERE f.userAction = 'NONE'
        AND (o.performanceJ7 > :threshold OR o.performanceJ30 > :threshold OR o.performanceJ1 > :threshold)
        ORDER BY COALESCE(o.performanceJ30, o.performanceJ7, o.performanceJ1) DESC
    """)
    suspend fun getRemarkableUnplayed(threshold: Double): List<AiForecastEntity>

    // Compte des pronostics créés aujourd'hui par stratégie (quota quotidien)
    @Query("SELECT COUNT(*) FROM ai_forecasts WHERE strategyType = :strategyType AND createdAt > :since")
    suspend fun countSince(strategyType: String, since: Long): Int

    // Recherche par symbole
    @Query("SELECT * FROM ai_forecasts WHERE symbol = :symbol ORDER BY createdAt DESC")
    suspend fun getBySymbol(symbol: String): List<AiForecastEntity>

    // Pronostics clôturés avec performance finale (pour stats)
    @Query("SELECT * FROM ai_forecasts WHERE memoryType = :memoryType AND finalPerformancePercent IS NOT NULL ORDER BY closedAt DESC")
    suspend fun getClosedWithPerformance(memoryType: String): List<AiForecastEntity>

    // Stats par bucket — min 10 pronostics avant affichage
    @Query("SELECT COUNT(*) FROM ai_forecasts WHERE memoryBucket = :bucket AND finalPerformancePercent IS NOT NULL")
    suspend fun countClosedByBucket(bucket: String): Int

    // Dernier pronostic par symbole et stratégie
    @Query("SELECT * FROM ai_forecasts WHERE symbol = :symbol AND strategyType = :strategyType ORDER BY createdAt DESC LIMIT 1")
    suspend fun findLatest(symbol: String, strategyType: String): AiForecastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AiForecastEntity): Long

    @Update
    suspend fun update(item: AiForecastEntity)

    @Query("DELETE FROM ai_forecasts WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Purge des pronostics expirés de plus de 12 mois (garder l'historique propre)
    @Query("DELETE FROM ai_forecasts WHERE status = 'EXPIRED' AND dueAt < :before")
    suspend fun purgeExpired(before: Long)
}

@Dao
interface AiForecastOutcomeDao {
    @Query("SELECT * FROM ai_forecast_outcomes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<AiForecastOutcomeEntity>

    @Query("SELECT * FROM ai_forecast_outcomes WHERE forecastId = :forecastId LIMIT 1")
    suspend fun findByForecastId(forecastId: Long): AiForecastOutcomeEntity?

    @Query("SELECT * FROM ai_forecast_outcomes WHERE symbol = :symbol ORDER BY updatedAt DESC")
    suspend fun findBySymbol(symbol: String): List<AiForecastOutcomeEntity>

    // Outcomes pour les pronostics non joués d'un type mémoire — pour AdaptiveWeights
    @Query("""
        SELECT o.* FROM ai_forecast_outcomes o
        INNER JOIN ai_forecasts f ON f.id = o.forecastId
        WHERE f.memoryType = :memoryType AND f.userAction = 'NONE'
        AND (o.performanceJ30 IS NOT NULL OR o.performanceJ7 IS NOT NULL OR o.performanceJ1 IS NOT NULL)
    """)
    suspend fun getOutcomesForUnplayedByMemoryType(memoryType: String): List<AiForecastOutcomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AiForecastOutcomeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AiForecastOutcomeEntity): Long

    @Update
    suspend fun update(item: AiForecastOutcomeEntity)

    @Query("DELETE FROM ai_forecast_outcomes WHERE forecastId = :forecastId")
    suspend fun deleteByForecastId(forecastId: Long)
}

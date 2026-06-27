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

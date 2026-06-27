package com.privateinvest.aitraderpro.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        SignalEntity::class,
        SignalResultEntity::class,
        SignalMemoryEntity::class,
        SignalOutcomeEntity::class,
        PositionEntity::class,
        TradeEntity::class,
        SimulationAccountEntity::class,
        NotificationHistoryEntity::class,
        FavoriteAssetEntity::class,
        PortfolioGoalEntity::class,
        WeightHistoryEntity::class,
        DecisionLogEntity::class,
        SecurityLogEntity::class,
        StrategyFollowUpEntity::class,
        AiForecastEntity::class,
        AiForecastOutcomeEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun signalDao(): SignalDao
    abstract fun signalResultDao(): SignalResultDao
    abstract fun signalMemoryDao(): SignalMemoryDao
    abstract fun signalOutcomeDao(): SignalOutcomeDao
    abstract fun positionDao(): PositionDao
    abstract fun tradeDao(): TradeDao
    abstract fun simulationAccountDao(): SimulationAccountDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
    abstract fun favoriteAssetDao(): FavoriteAssetDao
    abstract fun portfolioGoalDao(): PortfolioGoalDao
    abstract fun weightHistoryDao(): WeightHistoryDao
    abstract fun decisionLogDao(): DecisionLogDao
    abstract fun securityLogDao(): SecurityLogDao
    abstract fun strategyFollowUpDao(): StrategyFollowUpDao
    abstract fun aiForecastDao(): AiForecastDao
    abstract fun aiForecastOutcomeDao(): AiForecastOutcomeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS signal_memory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        symbol TEXT NOT NULL,
                        signal TEXT NOT NULL,
                        scoreTechnique INTEGER NOT NULL,
                        scoreRisque INTEGER NOT NULL,
                        rsi REAL NOT NULL,
                        macd REAL NOT NULL,
                        macdSignal REAL NOT NULL,
                        ema20 REAL NOT NULL,
                        ema50 REAL NOT NULL,
                        ema200 REAL NOT NULL,
                        atr14 REAL NOT NULL,
                        volatility REAL NOT NULL,
                        averageVolume REAL NOT NULL,
                        currentVolume REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS signal_outcome (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        signalId INTEGER NOT NULL,
                        priceJ1 REAL,
                        priceJ7 REAL,
                        priceJ30 REAL,
                        performanceJ1 REAL,
                        performanceJ7 REAL,
                        performanceJ30 REAL,
                        successJ1 INTEGER,
                        successJ7 INTEGER,
                        successJ30 INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS simulation_account (
                        id INTEGER PRIMARY KEY NOT NULL,
                        initialCapital REAL NOT NULL,
                        availableCash REAL NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO simulation_account (id, initialCapital, availableCash, updatedAt)
                    VALUES (1, 10000.0, 10000.0, 0)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        level TEXT NOT NULL,
                        symbol TEXT,
                        duplicateKey TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }


        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorite_assets (
                        symbol TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS portfolio_goals (
                        id INTEGER PRIMARY KEY NOT NULL,
                        targetPercent REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO portfolio_goals (id, targetPercent, createdAt)
                    VALUES (1, 10.0, 0)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS security_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        event TEXT NOT NULL,
                        level TEXT NOT NULL,
                        details TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS strategy_followups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        symbol TEXT NOT NULL,
                        name TEXT NOT NULL,
                        strategyType TEXT NOT NULL,
                        strategyLabel TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        entryPrice REAL NOT NULL,
                        currentPrice REAL NOT NULL,
                        targetPercent REAL NOT NULL,
                        stopPercent REAL NOT NULL,
                        maxHoldingDays INTEGER NOT NULL,
                        openedAt INTEGER NOT NULL,
                        lastCheckedAt INTEGER NOT NULL,
                        lastAlertAt INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL,
                        alertReason TEXT,
                        alertStatus TEXT,
                        closedAt INTEGER,
                        closedReason TEXT,
                        finalPerformancePercent REAL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_forecasts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        symbol TEXT NOT NULL,
                        name TEXT NOT NULL,
                        strategyType TEXT NOT NULL,
                        strategyLabel TEXT NOT NULL,
                        memoryBucket TEXT NOT NULL,
                        memoryType TEXT NOT NULL DEFAULT 'CT',
                        score INTEGER NOT NULL,
                        confidence INTEGER NOT NULL,
                        entryPrice REAL NOT NULL,
                        targetPercent REAL NOT NULL,
                        stopPercent REAL NOT NULL,
                        horizonDays INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        userAction TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        dueAt INTEGER NOT NULL,
                        closedAt INTEGER,
                        finalPerformancePercent REAL,
                        closeReason TEXT,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_forecast_outcomes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        forecastId INTEGER NOT NULL,
                        symbol TEXT NOT NULL,
                        priceJ1 REAL,
                        priceJ3 REAL,
                        priceJ7 REAL,
                        priceJ30 REAL,
                        priceJ90 REAL,
                        performanceJ1 REAL,
                        performanceJ3 REAL,
                        performanceJ7 REAL,
                        performanceJ30 REAL,
                        performanceJ90 REAL,
                        successJ1 INTEGER,
                        successJ3 INTEGER,
                        successJ7 INTEGER,
                        successJ30 INTEGER,
                        successJ90 INTEGER,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

    }
}

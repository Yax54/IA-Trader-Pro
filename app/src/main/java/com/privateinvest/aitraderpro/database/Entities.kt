package com.privateinvest.aitraderpro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val mode: String = "SIMULATION"
)

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val name: String,
    val score: Int,
    val confidence: Int,
    val target: String,
    val risk: String,
    val action: String,
    val explanation: String,
    val dateCreated: Long
)

@Entity(tableName = "signal_results")
data class SignalResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val signal: String,
    val score: Int,
    val entryPrice: Double,
    val exitPrice: Double,
    val profitPercent: Double,
    val dateCreated: Long,
    val dateClosed: Long?
)

@Entity(tableName = "signal_memory")
data class SignalMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val signal: String,
    val scoreTechnique: Int,
    val scoreRisque: Int,
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    val ema20: Double,
    val ema50: Double,
    val ema200: Double,
    val atr14: Double,
    val volatility: Double,
    val averageVolume: Double,
    val currentVolume: Double,
    val createdAt: Long
)

@Entity(tableName = "signal_outcome")
data class SignalOutcomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val signalId: Long,
    val priceJ1: Double? = null,
    val priceJ7: Double? = null,
    val priceJ30: Double? = null,
    val performanceJ1: Double? = null,
    val performanceJ7: Double? = null,
    val performanceJ30: Double? = null,
    val successJ1: Boolean? = null,
    val successJ7: Boolean? = null,
    val successJ30: Boolean? = null
)

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double,
    val marketValue: Double,
    val updatedAt: Long
)

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val side: String,
    val quantity: Double,
    val price: Double,
    val createdAt: Long
)

@Entity(tableName = "simulation_account")
data class SimulationAccountEntity(
    @PrimaryKey val id: Long = 1,
    val initialCapital: Double,
    val availableCash: Double,
    val updatedAt: Long
)

@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val level: String,
    val symbol: String?,
    val duplicateKey: String,
    val createdAt: Long,
    val status: String
)

@Entity(tableName = "weight_history")
data class WeightHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factor: String,
    val value: Double,
    val recordedAt: Long
)

@Entity(tableName = "decision_log")
data class DecisionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val symbol: String,
    val score: Int,
    val decision: String,
    val weightsUsed: String
)

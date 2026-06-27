package com.privateinvest.aitraderpro

import android.content.Context
import androidx.room.Room
import com.privateinvest.aitraderpro.database.AppDatabase
import com.privateinvest.aitraderpro.network.MarketApiService
import com.privateinvest.aitraderpro.notifications.TraderNotificationManager
import com.privateinvest.aitraderpro.repository.AdaptiveWeightsManager
import com.privateinvest.aitraderpro.repository.BackupSafetyRepository
import com.privateinvest.aitraderpro.repository.DataFreshnessGuard
import com.privateinvest.aitraderpro.repository.MarketDataConnectionRepository
import com.privateinvest.aitraderpro.repository.MarketRepository
import com.privateinvest.aitraderpro.repository.MarketRepositoryImpl
import com.privateinvest.aitraderpro.repository.AiForecastRepository
import com.privateinvest.aitraderpro.repository.StrategyMonitoringRepository
import com.privateinvest.aitraderpro.repository.UserPreferencesRepository
import com.privateinvest.aitraderpro.repository.SecurityRepository
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ServiceLocator {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // Propriété publique nécessaire pour BackupSafetyRepository et ControlCenterRepository
    val context: Context get() = appContext

    @OptIn(ExperimentalSerializationApi::class)
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
        }
    }

    private val okHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.ALPHA_VANTAGE_BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val apiService: MarketApiService by lazy { retrofit.create(MarketApiService::class.java) }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "ai_trader_pro.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .build()
    }

    val adaptiveWeightsManager: AdaptiveWeightsManager by lazy {
        AdaptiveWeightsManager(database.weightHistoryDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val notificationManager: TraderNotificationManager by lazy {
        TraderNotificationManager(appContext)
    }

    val securityRepository: SecurityRepository by lazy {
        SecurityRepository(appContext, database.securityLogDao())
    }

    val backupSafetyRepository: BackupSafetyRepository by lazy {
        BackupSafetyRepository(appContext)
    }

    val marketDataConnectionRepository: MarketDataConnectionRepository by lazy {
        MarketDataConnectionRepository(marketRepository = marketRepository)
    }

    val dataFreshnessGuard: DataFreshnessGuard get() = DataFreshnessGuard

    val aiForecastRepository: AiForecastRepository by lazy {
        AiForecastRepository(
            db = database,
            marketRepository = marketRepository,
            notificationManager = notificationManager
        )
    }

    val strategyMonitoringRepository: StrategyMonitoringRepository by lazy {
        StrategyMonitoringRepository(
            db = database,
            marketRepository = marketRepository,
            notificationManager = notificationManager
        )
    }

    val marketRepository: MarketRepository by lazy {
        MarketRepositoryImpl(
            api = apiService,
            db = database,
            adaptiveWeightsManager = adaptiveWeightsManager,
            notificationManager = notificationManager
        )
    }
}

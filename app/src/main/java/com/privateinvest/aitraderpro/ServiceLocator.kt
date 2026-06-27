package com.privateinvest.aitraderpro

import android.content.Context
import androidx.room.Room
import com.privateinvest.aitraderpro.database.AppDatabase
import com.privateinvest.aitraderpro.network.MarketApiService
import com.privateinvest.aitraderpro.notifications.TraderNotificationManager
import com.privateinvest.aitraderpro.repository.AdaptiveWeightsManager
import com.privateinvest.aitraderpro.repository.MarketRepository
import com.privateinvest.aitraderpro.repository.MarketRepositoryImpl
import com.privateinvest.aitraderpro.repository.UserPreferencesRepository
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
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
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

    val marketRepository: MarketRepository by lazy {
        MarketRepositoryImpl(
            api = apiService,
            db = database,
            adaptiveWeightsManager = adaptiveWeightsManager,
            notificationManager = notificationManager
        )
    }
}

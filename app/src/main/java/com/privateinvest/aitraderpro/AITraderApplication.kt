package com.privateinvest.aitraderpro

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.privateinvest.aitraderpro.worker.AiForecastWorker
import com.privateinvest.aitraderpro.worker.StrategyMonitoringWorker
import java.util.concurrent.TimeUnit

class AITraderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        ServiceLocator.notificationManager.createChannel()
        scheduleStrategyMonitoring()
        scheduleAiForecasts()
    }

    /**
     * Planifie la surveillance stratégique toutes les 3 heures.
     * KEEP = pas de redémarrage si déjà planifié (idempotent).
     * Aucune vente automatique — notifications uniquement.
     */
    private fun scheduleStrategyMonitoring() {
        val request = PeriodicWorkRequestBuilder<StrategyMonitoringWorker>(3, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            StrategyMonitoringWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Planifie les pronostics IA et la mémoire glissante toutes les 6 heures.
     * L'application apprend aussi sur les signaux non joués (CT et LT indépendants).
     */
    private fun scheduleAiForecasts() {
        val request = PeriodicWorkRequestBuilder<AiForecastWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AiForecastWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

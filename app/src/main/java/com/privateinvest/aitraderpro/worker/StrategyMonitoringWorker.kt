package com.privateinvest.aitraderpro.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.privateinvest.aitraderpro.ServiceLocator

/**
 * Worker de surveillance stratégique en arrière-plan.
 * Exécuté toutes les 3 heures par WorkManager.
 *
 * Règle absolue : aucune vente automatique.
 * Ce worker rafraîchit les prix et envoie des notifications si objectif / stop / durée
 * sont atteints — l'utilisateur valide toujours manuellement.
 */
class StrategyMonitoringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            ServiceLocator.strategyMonitoringRepository.refreshFollowUps()
            Result.success()
        }.getOrElse {
            // On ne relance pas en cas d'erreur réseau — prochain cycle dans 3h
            Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "strategy_monitoring_periodic"
    }
}

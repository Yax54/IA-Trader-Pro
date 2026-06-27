package com.privateinvest.aitraderpro.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.repository.MemoryType

/**
 * Worker de fond exécuté toutes les 6 heures.
 *
 * Responsabilités :
 * 1. Rafraîchit les outcomes des pronostics actifs (J+1/J+3/J+7/J+30/J+90).
 * 2. Détecte et envoie des alertes pour les pronostics non joués remarquables (>5%).
 * 3. Alimente l'AdaptiveWeightsManager depuis les pronostics non joués (CT + LT séparés).
 *    → L'application apprend même si l'utilisateur ne joue pas les signaux.
 */
class AiForecastWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "ai_forecast_periodic"
    }

    override suspend fun doWork(): Result {
        return try {
            val repo    = ServiceLocator.aiForecastRepository
            val weights = ServiceLocator.adaptiveWeightsManager

            // 1. Mise à jour des outcomes
            repo.refreshOutcomes()

            // 2. Alertes pronostics remarquables non joués
            repo.detectAndAlertRemarkableUnplayed()

            // 3. Apprentissage adaptatif depuis les pronostics non joués
            //    CT et LT sont traités indépendamment
            repo.feedAdaptiveWeightsFromForecasts(weights, MemoryType.CT)
            repo.feedAdaptiveWeightsFromForecasts(weights, MemoryType.LT)

            Result.success()
        } catch (e: Exception) {
            // En cas d'erreur non fatale, retry au prochain cycle
            Result.retry()
        }
    }
}

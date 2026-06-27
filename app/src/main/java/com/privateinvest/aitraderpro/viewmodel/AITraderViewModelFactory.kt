package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.repository.MarketRepository
import com.privateinvest.aitraderpro.repository.UserPreferencesRepository

object AITraderViewModelFactory : ViewModelProvider.Factory {
    private val repository: MarketRepository get() = ServiceLocator.marketRepository
    private val userPreferencesRepository: UserPreferencesRepository get() = ServiceLocator.userPreferencesRepository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(WatchlistViewModel::class.java) -> WatchlistViewModel(repository) as T
            modelClass.isAssignableFrom(AlertsViewModel::class.java) -> AlertsViewModel(repository) as T
            modelClass.isAssignableFrom(PortfolioViewModel::class.java) -> PortfolioViewModel(repository) as T
            modelClass.isAssignableFrom(AuditViewModel::class.java) -> AuditViewModel(repository) as T
            modelClass.isAssignableFrom(RiskViewModel::class.java) -> RiskViewModel(repository) as T
            modelClass.isAssignableFrom(AssetDetailViewModel::class.java) -> AssetDetailViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(userPreferencesRepository) as T
            modelClass.isAssignableFrom(BrokerViewModel::class.java) -> BrokerViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

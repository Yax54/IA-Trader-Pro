package com.privateinvest.aitraderpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.privateinvest.aitraderpro.ServiceLocator
import com.privateinvest.aitraderpro.repository.MarketRepository
import com.privateinvest.aitraderpro.repository.UserPreferencesRepository
import com.privateinvest.aitraderpro.repository.SecurityRepository
import com.privateinvest.aitraderpro.viewmodel.StatsCenterViewModel
import com.privateinvest.aitraderpro.viewmodel.PreferencesCenterViewModel
import com.privateinvest.aitraderpro.viewmodel.MultiPortfolioViewModel
import com.privateinvest.aitraderpro.viewmodel.DailyReportViewModel
import com.privateinvest.aitraderpro.viewmodel.OnboardingViewModel
import com.privateinvest.aitraderpro.viewmodel.AiForecastViewModel
import com.privateinvest.aitraderpro.viewmodel.ForecastDetailViewModel
import com.privateinvest.aitraderpro.viewmodel.ControlCenterViewModel
import com.privateinvest.aitraderpro.viewmodel.MarketDataCenterViewModel
import com.privateinvest.aitraderpro.viewmodel.RealValidationViewModel
import com.privateinvest.aitraderpro.viewmodel.StrategyMonitoringViewModel
import com.privateinvest.aitraderpro.viewmodel.TopOpportunitiesViewModel
import com.privateinvest.aitraderpro.viewmodel.MarketPlaceViewModel
import com.privateinvest.aitraderpro.viewmodel.OperationModeViewModel

object AITraderViewModelFactory : ViewModelProvider.Factory {
    private val repository: MarketRepository get() = ServiceLocator.marketRepository
    private val userPreferencesRepository: UserPreferencesRepository get() = ServiceLocator.userPreferencesRepository
    private val securityRepository: SecurityRepository get() = ServiceLocator.securityRepository

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
            modelClass.isAssignableFrom(BrokerViewModel::class.java) -> BrokerViewModel(repository, securityRepository) as T
            modelClass.isAssignableFrom(SecurityViewModel::class.java) -> SecurityViewModel(securityRepository) as T
            modelClass.isAssignableFrom(StatsCenterViewModel::class.java) -> StatsCenterViewModel() as T
            modelClass.isAssignableFrom(PreferencesCenterViewModel::class.java) -> PreferencesCenterViewModel() as T
            modelClass.isAssignableFrom(MultiPortfolioViewModel::class.java) -> MultiPortfolioViewModel() as T
            modelClass.isAssignableFrom(DailyReportViewModel::class.java) -> DailyReportViewModel() as T
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel() as T
            modelClass.isAssignableFrom(TopOpportunitiesViewModel::class.java) -> TopOpportunitiesViewModel() as T
            modelClass.isAssignableFrom(StrategyMonitoringViewModel::class.java) -> StrategyMonitoringViewModel() as T
            modelClass.isAssignableFrom(AiForecastViewModel::class.java) -> AiForecastViewModel() as T
            modelClass.isAssignableFrom(ForecastDetailViewModel::class.java) -> ForecastDetailViewModel() as T
            modelClass.isAssignableFrom(MarketDataCenterViewModel::class.java) -> MarketDataCenterViewModel(ServiceLocator.marketDataConnectionRepository) as T
            modelClass.isAssignableFrom(ControlCenterViewModel::class.java) -> ControlCenterViewModel() as T
            modelClass.isAssignableFrom(RealValidationViewModel::class.java) -> RealValidationViewModel() as T
            modelClass.isAssignableFrom(OperationModeViewModel::class.java) -> OperationModeViewModel(ServiceLocator.operationModeRepository) as T
            modelClass.isAssignableFrom(MarketPlaceViewModel::class.java) -> MarketPlaceViewModel(ServiceLocator.marketFiltersRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

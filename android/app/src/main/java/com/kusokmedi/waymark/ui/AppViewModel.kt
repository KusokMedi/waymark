package com.kusokmedi.waymark.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kusokmedi.waymark.AppContainer
import com.kusokmedi.waymark.SyncScheduler
import com.kusokmedi.waymark.data.local.AppSettings
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.ThemeMode
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.domain.CountryDetailData
import com.kusokmedi.waymark.domain.HistoryItem
import com.kusokmedi.waymark.widget.WaymarkWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.settings.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    val countries: StateFlow<List<CountryCard>> = container.repository.allCountries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val watchlist: StateFlow<List<CountryCard>> = container.repository.watchlistCountries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val history: StateFlow<List<HistoryItem>> = container.repository.historyItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val syncState: StateFlow<SyncStateEntity?> = container.repository.syncStateFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    fun observeCountry(countryId: String): Flow<CountryDetailData?> =
        container.repository.observeCountry(countryId)

    fun addToWatchlist(countryId: String) = viewModelScope.launch {
        container.repository.addToWatchlist(countryId, settings.value.notificationsEnabled)
        WaymarkWidgetUpdater.updateAll(container.context)
    }

    fun removeFromWatchlist(countryId: String) = viewModelScope.launch {
        container.repository.removeFromWatchlist(countryId)
        WaymarkWidgetUpdater.updateAll(container.context)
    }

    fun toggleWatchlist(countryId: String, currentlyInWatchlist: Boolean) = viewModelScope.launch {
        if (currentlyInWatchlist) container.repository.removeFromWatchlist(countryId)
        else container.repository.addToWatchlist(countryId, settings.value.notificationsEnabled)
        WaymarkWidgetUpdater.updateAll(container.context)
    }

    fun setFavorite(countryId: String, favorite: Boolean) = viewModelScope.launch {
        container.repository.setFavorite(countryId, favorite)
        WaymarkWidgetUpdater.updateAll(container.context)
    }

    fun setNotifications(countryId: String, enabled: Boolean) = viewModelScope.launch {
        container.repository.setNotifications(countryId, enabled)
    }

    fun reorderWatchlist(ids: List<String>) = viewModelScope.launch {
        container.repository.reorderWatchlist(ids)
        WaymarkWidgetUpdater.updateAll(container.context)
    }

    fun completeOnboarding(
        countryIds: List<String>,
        notificationsEnabled: Boolean,
        themeMode: ThemeMode,
        allowMobileData: Boolean,
    ) =
        viewModelScope.launch {
            countryIds.forEach { container.repository.addToWatchlist(it, notificationsEnabled) }
            container.settings.setNotificationsEnabled(notificationsEnabled)
            container.settings.setThemeMode(themeMode)
            container.settings.setAllowMobileData(allowMobileData)
            container.settings.setOnboardingCompleted(true)
            SyncScheduler.syncNow(container.context, allowMobileData)
        }

    fun syncNow() = viewModelScope.launch {
        container.syncEngine.synchronize()
        WaymarkWidgetUpdater.updateAll(container.context)
    }

    fun syncNowWithMobileData(allowMobileData: Boolean) = viewModelScope.launch {
        container.settings.setAllowMobileData(allowMobileData)
        // Ensure settings are persisted before sync reads them.
        kotlinx.coroutines.delay(50)
        container.syncEngine.synchronize(forceMobileData = allowMobileData)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        container.settings.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        container.settings.setDynamicColor(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        container.settings.setNotificationsEnabled(enabled)
        if (enabled) {
            container.database.watchlistDao().getAll().forEach {
                container.repository.setNotifications(it.countryId, true)
            }
        }
    }

    fun setNotificationSoundUri(value: String) = viewModelScope.launch {
        container.settings.setNotificationSoundUri(value)
    }

    fun setNotificationImportance(value: String) = viewModelScope.launch {
        container.settings.setNotificationImportance(value)
    }

    fun setAutomaticSync(enabled: Boolean) = viewModelScope.launch {
        container.settings.setAutomaticSync(enabled)
        val current = container.settings.settings.first()
        if (enabled) {
            SyncScheduler.schedulePeriodic(container.context, current.syncIntervalHours, current.allowMobileData)
        } else {
            SyncScheduler.cancelPeriodic(container.context)
        }
    }

    fun setSyncInterval(hours: Int) = viewModelScope.launch {
        container.settings.setSyncIntervalHours(hours)
        val current = container.settings.settings.first()
        if (current.automaticSync) {
            SyncScheduler.schedulePeriodic(container.context, current.syncIntervalHours, current.allowMobileData)
        }
    }

    fun setAllowMobileData(enabled: Boolean) = viewModelScope.launch {
        container.settings.setAllowMobileData(enabled)
        val current = container.settings.settings.first()
        if (current.automaticSync) {
            SyncScheduler.schedulePeriodic(container.context, current.syncIntervalHours, current.allowMobileData)
        }
    }

    fun clearHistory() = viewModelScope.launch {
        container.repository.clearHistory()
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}

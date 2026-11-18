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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    }

    fun removeFromWatchlist(countryId: String) = viewModelScope.launch {
        container.repository.removeFromWatchlist(countryId)
    }

    fun toggleWatchlist(countryId: String, currentlyInWatchlist: Boolean) = viewModelScope.launch {
        if (currentlyInWatchlist) container.repository.removeFromWatchlist(countryId)
        else container.repository.addToWatchlist(countryId, settings.value.notificationsEnabled)
    }

    fun setFavorite(countryId: String, favorite: Boolean) = viewModelScope.launch {
        container.repository.setFavorite(countryId, favorite)
    }

    fun setNotifications(countryId: String, enabled: Boolean) = viewModelScope.launch {
        container.repository.setNotifications(countryId, enabled)
    }

    fun reorderWatchlist(ids: List<String>) = viewModelScope.launch {
        container.repository.reorderWatchlist(ids)
    }

    fun completeOnboarding(countryIds: List<String>, notificationsEnabled: Boolean, themeMode: ThemeMode) =
        viewModelScope.launch {
            countryIds.forEach { container.repository.addToWatchlist(it, notificationsEnabled) }
            container.settings.setNotificationsEnabled(notificationsEnabled)
            container.settings.setThemeMode(themeMode)
            container.settings.setOnboardingCompleted(true)
            SyncScheduler.syncNow(container.context)
        }

    fun syncNow() = viewModelScope.launch {
        container.syncEngine.synchronize()
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
        val settings = settings.value
        if (enabled) SyncScheduler.schedulePeriodic(container.context, settings.syncIntervalHours)
        else SyncScheduler.cancelPeriodic(container.context)
    }

    fun setSyncInterval(hours: Int) = viewModelScope.launch {
        container.settings.setSyncIntervalHours(hours)
        if (settings.value.automaticSync) SyncScheduler.schedulePeriodic(container.context, hours)
    }

    fun clearHistory() = viewModelScope.launch {
        container.repository.clearHistory()
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}

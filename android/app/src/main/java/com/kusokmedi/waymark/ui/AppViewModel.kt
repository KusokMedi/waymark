package com.kusokmedi.waymark.ui

import android.util.Log
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

sealed class ErrorEvent {
    data class WatchlistOperationFailed(val message: String) : ErrorEvent()
    data class SyncFailed(val message: String) : ErrorEvent()
    data class SettingsUpdateFailed(val message: String) : ErrorEvent()
}

class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val _errorEvents = MutableSharedFlow<ErrorEvent>(extraBufferCapacity = 1)
    val errorEvents = _errorEvents.asSharedFlow()
    
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled error in ViewModel", throwable)
        _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
            throwable.message ?: "An unexpected error occurred"
        ))
    }
    
    private val supervisedScope = viewModelScope + errorHandler

    companion object {
        private const val TAG = "AppViewModel"
    }
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

    fun addToWatchlist(countryId: String) = supervisedScope.launch {
        try {
            container.repository.addToWatchlist(countryId, settings.value.notificationsEnabled)
            WaymarkWidgetUpdater.forceUpdateAll(container.context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add to watchlist: $countryId", e)
            _errorEvents.tryEmit(ErrorEvent.WatchlistOperationFailed(
                e.message ?: "Failed to add country to watchlist"
            ))
        }
    }

    fun removeFromWatchlist(countryId: String) = supervisedScope.launch {
        try {
            container.repository.removeFromWatchlist(countryId)
            WaymarkWidgetUpdater.forceUpdateAll(container.context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove from watchlist: $countryId", e)
            _errorEvents.tryEmit(ErrorEvent.WatchlistOperationFailed(
                e.message ?: "Failed to remove country from watchlist"
            ))
        }
    }

    fun toggleWatchlist(countryId: String, currentlyInWatchlist: Boolean) = supervisedScope.launch {
        try {
            if (currentlyInWatchlist) container.repository.removeFromWatchlist(countryId)
            else container.repository.addToWatchlist(countryId, settings.value.notificationsEnabled)
            WaymarkWidgetUpdater.forceUpdateAll(container.context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle watchlist: $countryId", e)
            _errorEvents.tryEmit(ErrorEvent.WatchlistOperationFailed(
                e.message ?: "Failed to update watchlist"
            ))
        }
    }

    fun setFavorite(countryId: String, favorite: Boolean) = supervisedScope.launch {
        try {
            container.repository.setFavorite(countryId, favorite)
            WaymarkWidgetUpdater.forceUpdateAll(container.context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set favorite: $countryId", e)
            _errorEvents.tryEmit(ErrorEvent.WatchlistOperationFailed(
                e.message ?: "Failed to update favorite status"
            ))
        }
    }

    fun setNotifications(countryId: String, enabled: Boolean) = supervisedScope.launch {
        try {
            container.repository.setNotifications(countryId, enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set notifications: $countryId", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update notification settings"
            ))
        }
    }

    fun reorderWatchlist(ids: List<String>) = supervisedScope.launch {
        try {
            container.repository.reorderWatchlist(ids)
            WaymarkWidgetUpdater.forceUpdateAll(container.context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reorder watchlist", e)
            _errorEvents.tryEmit(ErrorEvent.WatchlistOperationFailed(
                e.message ?: "Failed to reorder watchlist"
            ))
        }
    }

    fun completeOnboarding(
        countryIds: List<String>,
        notificationsEnabled: Boolean,
        themeMode: ThemeMode,
        allowMobileData: Boolean,
    ) = supervisedScope.launch {
        try {
            countryIds.forEach { container.repository.addToWatchlist(it, notificationsEnabled) }
            container.settings.setNotificationsEnabled(notificationsEnabled)
            container.settings.setThemeMode(themeMode)
            container.settings.setAllowMobileData(allowMobileData)
            container.settings.setOnboardingCompleted(true)
            SyncScheduler.syncNow(container.context, allowMobileData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete onboarding", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to complete onboarding"
            ))
        }
    }

    fun syncNow() = supervisedScope.launch {
        try {
            container.syncEngine.synchronize()
            WaymarkWidgetUpdater.forceUpdateAll(container.context)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _errorEvents.tryEmit(ErrorEvent.SyncFailed(
                e.message ?: "Synchronization failed"
            ))
        }
    }

    fun syncNowWithMobileData(allowMobileData: Boolean) = supervisedScope.launch {
        try {
            container.settings.setAllowMobileData(allowMobileData)
            // Wait for DataStore write to complete
            container.settings.settings.first { it.allowMobileData == allowMobileData }
            container.syncEngine.synchronize(forceMobileData = allowMobileData)
        } catch (e: Exception) {
            Log.e(TAG, "Sync with mobile data failed", e)
            _errorEvents.tryEmit(ErrorEvent.SyncFailed(
                e.message ?: "Failed to sync with mobile data"
            ))
        }
    }

    fun setThemeMode(mode: ThemeMode) = supervisedScope.launch {
        try {
            container.settings.setThemeMode(mode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set theme mode", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update theme"
            ))
        }
    }

    fun setDynamicColor(enabled: Boolean) = supervisedScope.launch {
        try {
            container.settings.setDynamicColor(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set dynamic color", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update dynamic color setting"
            ))
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) = supervisedScope.launch {
        try {
            container.settings.setNotificationsEnabled(enabled)
            if (enabled) {
                container.database.watchlistDao().getAll().forEach {
                    container.repository.setNotifications(it.countryId, true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set notifications enabled", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update notification settings"
            ))
        }
    }

    fun setNotificationSoundUri(value: String) = supervisedScope.launch {
        try {
            container.settings.setNotificationSoundUri(value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set notification sound", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update notification sound"
            ))
        }
    }

    fun setNotificationImportance(value: String) = supervisedScope.launch {
        try {
            container.settings.setNotificationImportance(value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set notification importance", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update notification importance"
            ))
        }
    }

    fun setAutomaticSync(enabled: Boolean) = supervisedScope.launch {
        try {
            container.settings.setAutomaticSync(enabled)
            val current = container.settings.settings.first()
            if (enabled) {
                SyncScheduler.schedulePeriodic(container.context, current.syncIntervalHours, current.allowMobileData)
            } else {
                SyncScheduler.cancelPeriodic(container.context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set automatic sync", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update automatic sync"
            ))
        }
    }

    fun setSyncInterval(hours: Int) = supervisedScope.launch {
        try {
            container.settings.setSyncIntervalHours(hours)
            val current = container.settings.settings.first()
            if (current.automaticSync) {
                SyncScheduler.schedulePeriodic(container.context, current.syncIntervalHours, current.allowMobileData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set sync interval", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update sync interval"
            ))
        }
    }

    fun setAllowMobileData(enabled: Boolean) = supervisedScope.launch {
        try {
            container.settings.setAllowMobileData(enabled)
            val current = container.settings.settings.first()
            if (current.automaticSync) {
                SyncScheduler.schedulePeriodic(container.context, current.syncIntervalHours, current.allowMobileData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set allow mobile data", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to update mobile data setting"
            ))
        }
    }

    fun clearHistory() = supervisedScope.launch {
        try {
            container.repository.clearHistory()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history", e)
            _errorEvents.tryEmit(ErrorEvent.SettingsUpdateFailed(
                e.message ?: "Failed to clear history"
            ))
        }
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}

package com.kusokmedi.waymark.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val syncIntervalHours: Int = 12,
    val automaticSync: Boolean = true,
    val notificationSoundUri: String = "",
    val notificationImportance: String = "high",
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "waymark_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val theme = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val syncInterval = intPreferencesKey("sync_interval_hours")
        val automaticSync = booleanPreferencesKey("automatic_sync")
        val notificationSoundUri = stringPreferencesKey("notification_sound_uri")
        val notificationImportance = stringPreferencesKey("notification_importance")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            onboardingCompleted = prefs[Keys.onboarding] ?: false,
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.theme] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[Keys.dynamicColor] ?: true,
            notificationsEnabled = prefs[Keys.notifications] ?: false,
            syncIntervalHours = prefs[Keys.syncInterval] ?: 12,
            automaticSync = prefs[Keys.automaticSync] ?: true,
            notificationSoundUri = prefs[Keys.notificationSoundUri] ?: "",
            notificationImportance = prefs[Keys.notificationImportance] ?: "high",
        )
    }

    suspend fun setOnboardingCompleted(value: Boolean) = context.dataStore.edit { it[Keys.onboarding] = value }
    suspend fun setThemeMode(value: ThemeMode) = context.dataStore.edit { it[Keys.theme] = value.name }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.dynamicColor] = value }
    suspend fun setNotificationsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.notifications] = value }
    suspend fun setSyncIntervalHours(value: Int) = context.dataStore.edit { it[Keys.syncInterval] = value.coerceIn(6, 24) }
    suspend fun setAutomaticSync(value: Boolean) = context.dataStore.edit { it[Keys.automaticSync] = value }
    suspend fun setNotificationSoundUri(value: String) = context.dataStore.edit { it[Keys.notificationSoundUri] = value }
    suspend fun setNotificationImportance(value: String) = context.dataStore.edit { it[Keys.notificationImportance] = value }
}

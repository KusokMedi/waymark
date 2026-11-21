package com.kusokmedi.waymark

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import com.kusokmedi.waymark.backup.BackupManager
import com.kusokmedi.waymark.data.AdvisoryRepository
import com.kusokmedi.waymark.data.local.SettingsRepository
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.remote.ArcGisClient
import com.kusokmedi.waymark.sync.ChangeNotifier
import com.kusokmedi.waymark.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application-wide dependency container.
 * 
 * All components are initialized with Application Context to prevent memory leaks.
 * The ArcGisClient holds a singleton OkHttpClient which is never explicitly closed,
 * but this is acceptable as it lives for the application lifetime.
 */
class AppContainer(context: Context) {
    val context: Context = context.applicationContext
    val database: WaymarkDatabase = WaymarkDatabase.get(context)
    val settings: SettingsRepository = SettingsRepository(context)
    val client: ArcGisClient = ArcGisClient()
    val repository: AdvisoryRepository = AdvisoryRepository(database)
    val notifier: ChangeNotifier = ChangeNotifier(this.context)
    val syncEngine: SyncEngine = SyncEngine(this.context, database, client, settings, notifier)
    val backupManager: BackupManager = BackupManager(this.context, database, settings)
}

class WaymarkApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Ensure notification channels exist before any background sync can run,
        // even if a WorkManager job fires before settings are loaded.
        container.notifier.initialize("", "high")
        appScope.launch {
            val settings = container.settings.settings.first()
            container.notifier.applySettings(settings.notificationSoundUri, settings.notificationImportance)
            if (settings.automaticSync) {
                SyncScheduler.schedulePeriodic(this@WaymarkApp, settings.syncIntervalHours, settings.allowMobileData)
            } else {
                SyncScheduler.cancelPeriodic(this@WaymarkApp)
            }
            SyncScheduler.syncNow(this@WaymarkApp, settings.allowMobileData)
        }
    }
}

fun Context.waymarkContainer(): AppContainer = (applicationContext as WaymarkApp).container

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

class AppContainer(context: Context) {
    val context: Context = context.applicationContext
    val database: WaymarkDatabase = WaymarkDatabase.get(context)
    val settings: SettingsRepository = SettingsRepository(context)
    val client: ArcGisClient = ArcGisClient()
    val repository: AdvisoryRepository = AdvisoryRepository(database)
    val notifier: ChangeNotifier = ChangeNotifier(context)
    val syncEngine: SyncEngine = SyncEngine(context, database, client, settings, notifier)
    val backupManager: BackupManager = BackupManager(context, database, settings)
}

class WaymarkApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            val settings = container.settings.settings.first()
            container.notifier.initialize(settings.notificationSoundUri, settings.notificationImportance)
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

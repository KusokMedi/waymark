package com.kusokmedi.waymark

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.workDataOf
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.sync.SyncOutcome
import com.kusokmedi.waymark.widget.WaymarkWidgetUpdater
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? WaymarkApp ?: return Result.failure()
        val forceMobileData = inputData.getBoolean(KEY_FORCE_MOBILE_DATA, false)
        val fromWidget = inputData.getBoolean(KEY_FROM_WIDGET, false)
        val forceSync = inputData.getBoolean("forceSync", false)
        Log.d(TAG, "doWork start forceMobileData=$forceMobileData fromWidget=$fromWidget forceSync=$forceSync attempt=$runAttemptCount")

        val outcome = try {
            withTimeoutOrNull(WORK_TIMEOUT_MS) {
                app.container.syncEngine.synchronize(forceMobileData = forceMobileData, forceSync = forceSync)
            }
        } catch (e: Exception) {
            Log.w(TAG, "doWork exception", e)
            markFailedIfRunning(app, e.message ?: "Synchronization was interrupted")
            WaymarkWidgetUpdater.forceUpdateAll(applicationContext)
            return if (runAttemptCount < 2 && !fromWidget) Result.retry() else Result.failure()
        }

        val result = when (outcome) {
            null -> {
                Log.w(TAG, "doWork timed out")
                markFailedIfRunning(app, "Synchronization timed out")
                WaymarkWidgetUpdater.forceUpdateAll(applicationContext)
                Result.failure()
            }
            is SyncOutcome.Success -> {
                WaymarkWidgetUpdater.forceUpdateAll(applicationContext)
                Result.success()
            }
            is SyncOutcome.Offline -> {
                WaymarkWidgetUpdater.forceUpdateAll(applicationContext)
                if (runAttemptCount < 2 && !fromWidget) Result.retry() else Result.success()
            }
            is SyncOutcome.Failure -> {
                WaymarkWidgetUpdater.forceUpdateAll(applicationContext)
                if (runAttemptCount < 3 && !fromWidget) Result.retry() else Result.failure()
            }
        }

        Log.d(TAG, "doWork finished result=$result outcome=$outcome")
        return result
    }

    private suspend fun markFailedIfRunning(app: WaymarkApp, message: String) {
        val dao = app.container.database.syncStateDao()
        val current = dao.get() ?: SyncStateEntity()
        if (current.status == "running") {
            dao.upsert(current.copy(status = "failed", message = message))
        }
    }

    companion object {
        private const val TAG = "WaymarkSyncWorker"
        private const val KEY_FORCE_MOBILE_DATA = "forceMobileData"
        private const val KEY_FROM_WIDGET = "fromWidget"
        private const val KEY_FORCE_SYNC = "forceSync"
        private val WORK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(150)
    }
}

object SyncScheduler {
    private const val TAG = "WaymarkSyncScheduler"
    private const val UNIQUE_PERIODIC = "waymark-periodic-sync"
    private const val UNIQUE_ONE_TIME = "waymark-one-time-sync"
    private const val UNIQUE_WIDGET_SYNC = "waymark-widget-sync"
    private const val KEY_FORCE_MOBILE_DATA = "forceMobileData"
    private const val KEY_FROM_WIDGET = "fromWidget"

    private fun constraints(allowMobileData: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .build()

    fun schedulePeriodic(context: Context, intervalHours: Int = 12, allowMobileData: Boolean = false) {
        val interval = intervalHours.coerceIn(6, 24).toLong()
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(interval, TimeUnit.HOURS)
            .setConstraints(constraints(allowMobileData))
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,  // Don't replace if already scheduled
            periodic,
        )
        Log.d(TAG, "Scheduled periodic sync every $interval hours")
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
    }

    fun syncNow(context: Context, allowMobileData: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints(allowMobileData))
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        Log.d(TAG, "Enqueued one-time sync")
    }

    fun syncNowFromWidget(context: Context, initialDelayMs: Long = 0L, forceSync: Boolean = false) {
        Log.d(TAG, "enqueue widget sync delayMs=$initialDelayMs forceSync=$forceSync")
        val builder = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints(allowMobileData = true))
            .setInputData(workDataOf(
                KEY_FORCE_MOBILE_DATA to true, 
                KEY_FROM_WIDGET to true,
                "forceSync" to forceSync
            ))
        if (initialDelayMs > 0) {
            builder.setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        }
        // Use KEEP instead of REPLACE to avoid cancelling already-running sync
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WIDGET_SYNC,
            ExistingWorkPolicy.KEEP,
            builder.build(),
        )
    }
}
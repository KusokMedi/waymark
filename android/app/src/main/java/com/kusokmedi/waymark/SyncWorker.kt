package com.kusokmedi.waymark

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.kusokmedi.waymark.sync.SyncOutcome
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? WaymarkApp ?: return Result.failure()
        return when (val outcome = app.container.syncEngine.synchronize()) {
            is SyncOutcome.Success -> Result.success()
            is SyncOutcome.Offline -> if (runAttemptCount < 2) Result.retry() else Result.success()
            is SyncOutcome.Failure -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

object SyncScheduler {
    private const val UNIQUE_PERIODIC = "waymark-periodic-sync"
    private const val UNIQUE_ONE_TIME = "waymark-one-time-sync"

    private fun constraints(allowMobileData: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .build()

    fun schedulePeriodic(context: Context, intervalHours: Int = 12, allowMobileData: Boolean = false) {
        val interval = intervalHours.coerceIn(6, 24).toLong()
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(interval, TimeUnit.HOURS)
            .setConstraints(constraints(allowMobileData))
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic,
        )
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
    }

    fun syncNow(context: Context, allowMobileData: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints(allowMobileData))
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

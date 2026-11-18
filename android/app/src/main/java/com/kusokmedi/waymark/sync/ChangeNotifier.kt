package com.kusokmedi.waymark.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kusokmedi.waymark.R
import com.kusokmedi.waymark.domain.AdvisoryChange
import com.kusokmedi.waymark.domain.ChangeType
import com.kusokmedi.waymark.ui.MainActivity

class ChangeNotifier(private val context: Context) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val changes = NotificationChannel(
            CHANNEL_CHANGES,
            context.getString(R.string.notification_channel_changes),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_changes_description)
        }
        val sync = NotificationChannel(
            CHANNEL_SYNC,
            context.getString(R.string.notification_channel_sync),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_sync_description)
        }
        manager.createNotificationChannels(listOf(changes, sync))
    }

    fun notifyChanges(changes: List<AdvisoryChange>) {
        if (changes.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        changes.forEach { change ->
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_COUNTRY_ID, change.countryId)
            }
            val pending = PendingIntent.getActivity(
                context,
                change.fingerprint.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val priority = when (change.type) {
                ChangeType.WORSENED -> NotificationCompat.PRIORITY_HIGH
                ChangeType.REGIONAL -> NotificationCompat.PRIORITY_DEFAULT
                ChangeType.IMPROVED -> NotificationCompat.PRIORITY_DEFAULT
                ChangeType.DETAILS -> NotificationCompat.PRIORITY_LOW
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_CHANGES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(change.title)
                .setContentText(change.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(change.message))
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(change.fingerprint.hashCode(), notification)
            } catch (_: SecurityException) {
                // Permission was revoked between the check and notify().
            }
        }
    }

    fun notifySyncProblem(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Waymark could not update advisories")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(SYNC_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Ignore revoked permission.
        }
    }

    companion object {
        const val CHANNEL_CHANGES = "advisory_changes"
        const val CHANNEL_SYNC = "sync_status"
        private const val SYNC_NOTIFICATION_ID = 7001
    }
}

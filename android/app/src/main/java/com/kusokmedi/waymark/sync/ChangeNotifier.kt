package com.kusokmedi.waymark.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kusokmedi.waymark.R
import com.kusokmedi.waymark.domain.AdvisoryChange
import com.kusokmedi.waymark.domain.ChangeType
import com.kusokmedi.waymark.ui.MainActivity
import java.security.MessageDigest

class ChangeNotifier(private val context: Context) {
    @Volatile private var soundUri: String = ""
    @Volatile private var importanceSetting: String = "high"
    @Volatile private var changesChannelId: String = channelIdFor("high", "")

    fun initialize(soundUri: String, importance: String) {
        this.soundUri = soundUri
        this.importanceSetting = normalizeImportance(importance)
        this.changesChannelId = channelIdFor(this.importanceSetting, this.soundUri)
        createChannels()
    }

    fun applySettings(soundUri: String, importance: String) {
        initialize(soundUri, importance)
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // NOTE: Deleting notification channels removes user customization.
        // We keep this approach to ensure sound/importance settings are applied,
        // but Android guidelines recommend against frequent channel deletion.
        // Consider using fixed channels in future versions.
        manager.notificationChannels
            .filter { it.id.startsWith(CHANNEL_CHANGES_PREFIX) && it.id != changesChannelId }
            .forEach { 
                android.util.Log.i("ChangeNotifier", "Removing stale notification channel: ${it.id}")
                manager.deleteNotificationChannel(it.id)
            }

        val importance = when (normalizeImportance(importanceSetting)) {
            "low" -> NotificationManager.IMPORTANCE_LOW
            "default" -> NotificationManager.IMPORTANCE_DEFAULT
            else -> NotificationManager.IMPORTANCE_HIGH
        }
        val changes = NotificationChannel(
            changesChannelId,
            context.getString(R.string.notification_channel_changes),
            importance,
        ).apply {
            description = context.getString(R.string.notification_channel_changes_description)
            setSound(resolveSoundUri(soundUri), notificationAudioAttributes())
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
        ) return
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
            val priority = builderPriorityFor(change.type)
            val builder = NotificationCompat.Builder(context, changesChannelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(change.title)
                .setContentText(change.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(change.message))
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(pending)

            // Pre-O devices respect builder sound/priority; on O+ channel controls it.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val uri = resolveSoundUri(soundUri)
                if (uri != null) builder.setSound(uri) else builder.setSound(null)
            }

            try {
                NotificationManagerCompat.from(context).notify(change.fingerprint.hashCode(), builder.build())
            } catch (e: SecurityException) {
                // Permission was revoked between the check and notify().
                android.util.Log.w("ChangeNotifier", "Failed to post notification for ${change.countryId}: permission revoked", e)
            }
        }
    }

    fun notifySyncProblem(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
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
        } catch (e: SecurityException) {
            // Ignore revoked permission but log for debugging.
            android.util.Log.w("ChangeNotifier", "Failed to post sync problem notification: permission revoked", e)
        }
    }

    private fun builderPriorityFor(type: ChangeType): Int {
        val base = when (normalizeImportance(importanceSetting)) {
            "low" -> NotificationCompat.PRIORITY_LOW
            "default" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_HIGH
        }
        return when (type) {
            ChangeType.WORSENED -> maxOf(base, NotificationCompat.PRIORITY_DEFAULT)
            ChangeType.DETAILS -> minOf(base, NotificationCompat.PRIORITY_DEFAULT)
            else -> base
        }
    }

    private fun resolveSoundUri(value: String): Uri? = when {
        value == "silent" -> null
        value.isBlank() -> Settings.System.DEFAULT_NOTIFICATION_URI
        else -> runCatching { Uri.parse(value) }.getOrNull() ?: Settings.System.DEFAULT_NOTIFICATION_URI
    }

    private fun notificationAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun normalizeImportance(value: String): String = when (value) {
        "low", "default", "high" -> value
        else -> "high"
    }

    private fun channelIdFor(importance: String, sound: String): String {
        val soundKey = when {
            sound == "silent" -> "silent"
            sound.isBlank() -> "default"
            else -> shortHash(sound)
        }
        return "${CHANNEL_CHANGES_PREFIX}_${normalizeImportance(importance)}_$soundKey"
    }

    private fun shortHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val CHANNEL_CHANGES_PREFIX = "advisory_changes"
        const val CHANNEL_SYNC = "sync_status"
        private const val SYNC_NOTIFICATION_ID = 7001
    }
}

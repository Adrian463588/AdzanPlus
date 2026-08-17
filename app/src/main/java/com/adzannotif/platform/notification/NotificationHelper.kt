package com.adzannotif.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adzannotif.MainActivity
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.presentation.alarm.AlarmFullscreenActivity
import com.adzannotif.presentation.localization.prayerLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationGateway {
    companion object {
        const val CHANNEL_ADHAN_ID = "channel_adhan_alerts"
        const val CHANNEL_BEEP_ID = "channel_prayer_beeps"
        const val CHANNEL_REMINDER_ID = "channel_prayer_reminders"
        const val CHANNEL_PERSISTENT_ID = "channel_prayer_persistent"
        const val CELESTIAL_CHANNEL_ID = "celestial_events"

        const val NOTIFICATION_ID_ADHAN = 1001
        const val NOTIFICATION_ID_REMINDER = 2001
        const val NOTIFICATION_ID_PERSISTENT = 3001

        private const val TAG = "NotificationHelper"
        private val ADHAN_VIBRATION_PATTERN = longArrayOf(0, 500, 250, 500, 250, 1000)
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High Priority Channel for Adzan with Fullscreen Intent capability
            // Note: Sound is set to null because AdhanAudioPlayer handles rich audio playback (ExoPlayer/MediaPlayer)
            val adhanChannel = NotificationChannel(
                CHANNEL_ADHAN_ID,
                context.getString(com.adzannotif.R.string.notification_channel_adhan_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.adzannotif.R.string.notification_channel_adhan_description)
                enableVibration(true)
                vibrationPattern = ADHAN_VIBRATION_PATTERN
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val beepChannel = NotificationChannel(
                CHANNEL_BEEP_ID,
                context.getString(com.adzannotif.R.string.notification_channel_beep_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(com.adzannotif.R.string.notification_channel_beep_description)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build(),
                )
                enableVibration(true)
            }

            // Normal Priority Channel for Pre-Prayer Reminders (5/10/15 mins before)
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                context.getString(com.adzannotif.R.string.notification_channel_reminder_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(com.adzannotif.R.string.notification_channel_reminder_description)
                enableVibration(true)
            }

            // Low Priority Channel for Persistent / Ongoing Status
            val persistentChannel = NotificationChannel(
                CHANNEL_PERSISTENT_ID,
                context.getString(com.adzannotif.R.string.notification_channel_persistent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(com.adzannotif.R.string.notification_channel_persistent_description)
                setShowBadge(false)
            }

            val celestialChannel = NotificationChannel(
                CELESTIAL_CHANNEL_ID,
                context.getString(com.adzannotif.R.string.notification_channel_celestial_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(com.adzannotif.R.string.notification_channel_celestial_description)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(adhanChannel, beepChannel, reminderChannel, persistentChannel, celestialChannel))
        }
    }

    override fun showAdhanNotification(
        prayer: Prayer,
        prayerTitle: String,
        locationName: String,
        vibrate: Boolean,
    ) {
        val localizedPrayer = prayerLabel(context, prayer)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fullscreen Intent for lockscreen display
        val fullScreenIntent = Intent(context, AlarmFullscreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmFullscreenActivity.EXTRA_PRAYER_NAME, prayer.name)
            putExtra(AlarmFullscreenActivity.EXTRA_PRAYER_TITLE, prayerTitle)
            putExtra(AlarmFullscreenActivity.EXTRA_LOCATION_NAME, locationName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal + 100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, com.adzannotif.platform.receiver.StopAdhanReceiver::class.java).apply {
            action = com.adzannotif.platform.receiver.StopAdhanReceiver.ACTION_STOP_ADHAN
            putExtra(com.adzannotif.platform.receiver.StopAdhanReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_ADHAN + prayer.ordinal)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.ordinal + 300,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ADHAN_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(com.adzannotif.R.string.notification_adhan_title, localizedPrayer))
            .setContentText(context.getString(com.adzannotif.R.string.notification_adhan_content, localizedPrayer, locationName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(com.adzannotif.R.string.notification_stop_audio),
                stopPendingIntent,
            )
            .setVibrate(if (vibrate) ADHAN_VIBRATION_PATTERN else longArrayOf(0))

        postNotification(NOTIFICATION_ID_ADHAN + prayer.ordinal, builder)
    }

    override fun showPreReminderNotification(
        prayer: Prayer,
        minutesBefore: Int,
        locationName: String,
        vibrate: Boolean,
    ) {
        val localizedPrayer = prayerLabel(context, prayer)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal + 200,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDER_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(
                context.resources.getQuantityString(
                    com.adzannotif.R.plurals.notification_pre_reminder_title,
                    minutesBefore,
                    minutesBefore,
                    localizedPrayer,
                ),
            )
            .setContentText(context.getString(com.adzannotif.R.string.notification_pre_reminder_content, localizedPrayer, locationName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(if (vibrate) ADHAN_VIBRATION_PATTERN else longArrayOf(0))

        postNotification(NOTIFICATION_ID_REMINDER + prayer.ordinal, builder)
    }

    override fun showBeepNotification(
        prayer: Prayer,
        prayerTitle: String,
        locationName: String,
        vibrate: Boolean,
    ) {
        val localizedPrayer = prayerLabel(context, prayer)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal + 400,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_BEEP_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(prayerTitle)
            .setContentText(context.getString(com.adzannotif.R.string.notification_prayer_time_content, localizedPrayer, locationName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(if (vibrate) ADHAN_VIBRATION_PATTERN else longArrayOf(0))
        postNotification(NOTIFICATION_ID_ADHAN + prayer.ordinal + 5000, builder)
    }

    override fun showCelestialEventNotification(eventType: String, label: String) {
        if (eventType.isBlank() || label.isBlank()) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            stableNotificationId(eventType),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CELESTIAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(com.adzannotif.R.string.notification_celestial_title, label))
            .setContentText(context.getString(com.adzannotif.R.string.notification_celestial_content, label))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        postNotification(stableNotificationId(eventType), builder)
    }

    override fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun postNotification(
        notificationId: Int,
        builder: NotificationCompat.Builder,
    ) {
        if (!areNotificationsEnabled()) {
            Log.i(TAG, "Notifications are disabled; notificationId=$notificationId was not posted")
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission was revoked before posting notificationId=$notificationId", e)
        }
    }

    private fun stableNotificationId(eventType: String): Int =
        (eventType.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
}

package com.adzannotif.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adzannotif.MainActivity
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.presentation.alarm.AlarmFullscreenActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ADHAN_ID = "channel_adhan_alerts"
        const val CHANNEL_REMINDER_ID = "channel_prayer_reminders"
        const val CHANNEL_PERSISTENT_ID = "channel_prayer_persistent"

        const val NOTIFICATION_ID_ADHAN = 1001
        const val NOTIFICATION_ID_REMINDER = 2001
        const val NOTIFICATION_ID_PERSISTENT = 3001

        private const val TAG = "NotificationHelper"
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
                "Peringatan Adzan & Sholat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi adzan waktu sholat tiba dengan suara penuh dan layar bangun"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 1000)
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Normal Priority Channel for Pre-Prayer Reminders (5/10/15 mins before)
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                "Pengingat Menjelang Sholat",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat beberapa menit sebelum adzan berkumandang"
                enableVibration(true)
            }

            // Low Priority Channel for Persistent / Ongoing Status
            val persistentChannel = NotificationChannel(
                CHANNEL_PERSISTENT_ID,
                "Status Jadwal Sholat Aktif",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Informasi jadwal sholat berikutnya yang aktif di latar belakang"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(adhanChannel, reminderChannel, persistentChannel))
        }
    }

    fun showAdhanNotification(
        prayer: Prayer,
        prayerTitle: String,
        locationName: String,
    ) {
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
            .setContentTitle("Waktu ${prayer.displayNameId} Telah Tiba")
            .setContentText("Saatnya menunaikan sholat ${prayer.displayNameId} untuk wilayah $locationName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan Suara", stopPendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 1000))

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_ADHAN + prayer.ordinal,
                builder.build()
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted for Adhan alert", e)
        }
    }

    fun showPreReminderNotification(
        prayer: Prayer,
        minutesBefore: Int,
        locationName: String,
    ) {
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
            .setContentTitle("$minutesBefore Menit Menuju ${prayer.displayNameId}")
            .setContentText("Bersiaplah untuk menunaikan sholat ${prayer.displayNameId} ($locationName)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_REMINDER + prayer.ordinal,
                builder.build()
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted for pre-reminder", e)
        }
    }
}

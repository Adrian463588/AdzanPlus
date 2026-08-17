package com.adzannotif.platform.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanVoice
import com.adzannotif.platform.notification.NotificationHelper
import com.adzannotif.platform.receiver.StopAdhanReceiver
import com.adzannotif.presentation.localization.prayerLabel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps alarm playback alive after the alarm receiver finishes its callback.
 * Playback ends only when the configured source reports completion, an error is
 * reported, or the user explicitly dismisses/stops the alarm.
 */
@AndroidEntryPoint
class AdhanPlaybackService : Service() {

    @Inject
    lateinit var audioGateway: AudioGateway

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val voice = intent?.getStringExtra(EXTRA_VOICE)
            ?.let { name -> runCatching { AdhanVoice.valueOf(name) }.getOrNull() }
            ?: return stopForInvalidRequest(startId)
        val prayer = intent.getStringExtra(EXTRA_PRAYER)
            ?.let { name -> runCatching { Prayer.valueOf(name) }.getOrNull() }
        val prayerTitle = intent.getStringExtra(EXTRA_PRAYER_TITLE)
            ?.takeIf(String::isNotBlank)
            ?: getString(R.string.alarm_default_title)
        val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME)
            ?.takeIf(String::isNotBlank)
            ?: getString(R.string.location_unavailable)
        val customUriString = intent.getStringExtra(EXTRA_CUSTOM_URI)

        startForeground(
            SERVICE_NOTIFICATION_ID,
            buildPlaybackNotification(prayer, prayerTitle, locationName),
        )
        audioGateway.playAdhan(
            voice = voice,
            customUriString = customUriString,
            onCompletion = {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelfResult(startId)
            },
        )
        return START_REDELIVER_INTENT
    }

    private fun stopForInvalidRequest(startId: Int): Int {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId)
        return START_NOT_STICKY
    }

    private fun buildPlaybackNotification(
        prayer: Prayer?,
        prayerTitle: String,
        locationName: String,
    ): Notification {
        val localizedPrayer = prayer?.let { prayerLabel(this, it) } ?: prayerTitle
        val stopIntent = Intent(this, StopAdhanReceiver::class.java).apply {
            action = StopAdhanReceiver.ACTION_STOP_ADHAN
            prayer?.let {
                putExtra(
                    StopAdhanReceiver.EXTRA_NOTIFICATION_ID,
                    NotificationHelper.NOTIFICATION_ID_ADHAN + it.ordinal,
                )
            }
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ADHAN_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.notification_adhan_title, localizedPrayer))
            .setContentText(getString(R.string.notification_adhan_content, localizedPrayer, locationName))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_stop_audio),
                stopPendingIntent,
            )
            .build()
    }

    override fun onDestroy() {
        audioGateway.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_PLAY = "com.adzannotif.action.PLAY_ADHAN"
        private const val EXTRA_VOICE = "extra_voice"
        private const val EXTRA_CUSTOM_URI = "extra_custom_uri"
        private const val EXTRA_PRAYER = "extra_prayer"
        private const val EXTRA_PRAYER_TITLE = "extra_prayer_title"
        private const val EXTRA_LOCATION_NAME = "extra_location_name"
        private const val SERVICE_NOTIFICATION_ID = 9001
        private const val STOP_REQUEST_CODE = 9002

        fun start(
            context: Context,
            voice: AdhanVoice,
            customUriString: String?,
            prayer: Prayer,
            prayerTitle: String,
            locationName: String,
        ) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_VOICE, voice.name)
                putExtra(EXTRA_CUSTOM_URI, customUriString)
                putExtra(EXTRA_PRAYER, prayer.name)
                putExtra(EXTRA_PRAYER_TITLE, prayerTitle)
                putExtra(EXTRA_LOCATION_NAME, locationName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AdhanPlaybackService::class.java))
        }
    }
}

package com.adzannotif.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanSoundType
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.platform.alarm.AlarmScheduler
import com.adzannotif.platform.audio.AdhanPlaybackService
import com.adzannotif.platform.notification.NotificationGateway
import com.adzannotif.widget.AstronomyWidgetUpdater
import com.adzannotif.presentation.widget.PrayerTimesWidgetReceiver
import com.adzannotif.presentation.localization.prayerLabel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationGateway: NotificationGateway

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    companion object {
        const val ACTION_ALARM_FIRE = "com.adzannotif.ACTION_ALARM_FIRE"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TITLE = "extra_prayer_title"
        const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
        const val EXTRA_TARGET_EPOCH_MS = "extra_target_epoch_ms"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_FIRE) return

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)

        val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrNull() ?: return
        val prayerTitle = intent.getStringExtra(EXTRA_PRAYER_TITLE)
            ?.takeIf(String::isNotBlank)
            ?: prayerLabel(context, prayer)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val location = locationRepository.currentOrSelectedLocation.first()
                val locationName = location?.name ?: context.getString(R.string.location_unavailable)
                val alarmSettings = settingsRepository.alarmSettings.first()
                val config = alarmSettings.getConfigForPrayer(prayer)

                if (isPreReminder) {
                    notificationGateway.showPreReminderNotification(
                        prayer = prayer,
                        minutesBefore = config.preReminderMinutes,
                        locationName = locationName,
                        vibrate = config.isVibrate,
                    )
                } else {
                    when (config.soundType) {
                        AdhanSoundType.FULL_ADHAN, AdhanSoundType.SHORT_TAKBEER -> {
                            // Show High Importance Adzan Notification & Fullscreen Intent
                            notificationGateway.showAdhanNotification(
                                prayer = prayer,
                                prayerTitle = prayerTitle,
                                locationName = locationName,
                                vibrate = config.isVibrate,
                            )
                            AdhanPlaybackService.start(
                                context = context,
                                voice = config.adhanVoice,
                                customUriString = config.customSoundUri,
                                prayer = prayer,
                                prayerTitle = prayerTitle,
                                locationName = locationName,
                            )
                        }
                        AdhanSoundType.BEEP_NOTIFICATION -> {
                            notificationGateway.showBeepNotification(
                                prayer = prayer,
                                prayerTitle = prayerTitle,
                                locationName = locationName,
                                vibrate = config.isVibrate,
                            )
                        }
                        AdhanSoundType.SILENT -> {
                            notificationGateway.showSilentNotification(
                                prayer = prayer,
                                prayerTitle = prayerTitle,
                                locationName = locationName,
                                vibrate = config.isVibrate,
                            )
                        }
                    }

                    // Reschedule for next prayers / tomorrow
                    alarmScheduler.schedule().onFailure { error ->
                        Log.e(TAG, "Failed to reconcile alarms after prayer alarm", error)
                    }
                }

                // The displayed next/current prayer and celestial countdowns are
                // time-sensitive snapshots. Reconcile both widget families after
                // every delivered alarm, including pre-reminders.
                PrayerTimesWidgetReceiver.updateAll(context)
                AstronomyWidgetUpdater.updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm broadcast for prayer=$prayerName", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

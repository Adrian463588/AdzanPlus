package com.adzannotif.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanSoundType
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.domain.usecase.SchedulePrayerAlarmsUseCase
import com.adzannotif.platform.audio.AdhanAudioPlayer
import com.adzannotif.platform.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var audioPlayer: AdhanAudioPlayer

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase

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
        val prayerTitle = intent.getStringExtra(EXTRA_PRAYER_TITLE) ?: prayerName
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)

        val prayer = try {
            Prayer.valueOf(prayerName)
        } catch (e: Exception) {
            Prayer.FAJR
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val location = locationRepository.currentOrSelectedLocation.first()
                val alarmSettings = settingsRepository.alarmSettings.first()
                val config = alarmSettings.getConfigForPrayer(prayer)

                if (isPreReminder) {
                    notificationHelper.showPreReminderNotification(
                        prayer = prayer,
                        minutesBefore = config.preReminderMinutes,
                        locationName = location.name
                    )
                } else {
                    // Show High Importance Adzan Notification & Fullscreen Intent
                    notificationHelper.showAdhanNotification(
                        prayer = prayer,
                        prayerTitle = prayerTitle,
                        locationName = location.name
                    )

                    // Play Audio based on sound configuration
                    when (config.soundType) {
                        AdhanSoundType.FULL_ADHAN, AdhanSoundType.SHORT_TAKBEER -> {
                            audioPlayer.playAdhan(
                                voice = config.adhanVoice,
                                customUriString = config.customSoundUri,
                                durationMinutes = alarmSettings.dndAutoSilenceMinutes
                            )
                        }
                        AdhanSoundType.BEEP_NOTIFICATION -> {
                            // Handled via notification sound
                        }
                        AdhanSoundType.SILENT -> {
                            // Silent
                        }
                    }

                    // Reschedule for next prayers / tomorrow
                    schedulePrayerAlarmsUseCase()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm broadcast for prayer=$prayerName", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

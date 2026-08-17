package com.adzannotif.domain.usecase

import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.repository.AlarmRepository
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * UseCase to compute and arm exact alarms for all upcoming prayer times and pre-reminders.
 * Guarantees that at any given moment, each configured prayer has an active upcoming alarm
 * (either today's remaining time or tomorrow's occurrence).
 */
class SchedulePrayerAlarmsUseCase @Inject constructor(
    private val getTodayPrayerTimesUseCase: GetTodayPrayerTimesUseCase,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val alarmRepository: AlarmRepository,
) {
    suspend operator fun invoke() {
        val location = locationRepository.currentOrSelectedLocation.first() ?: return
        val tz = TimeZone.of(location.timeZoneId)
        val now = Clock.System.now()
        val todayDate = now.toLocalDateTime(tz).date
        val tomorrowDate = todayDate.plus(1, DateTimeUnit.DAY)

        val todayRecord = getTodayPrayerTimesUseCase(todayDate).first()
        val tomorrowRecord = getTodayPrayerTimesUseCase(tomorrowDate).first()
        val alarmSettings = settingsRepository.alarmSettings.first()

        // Fard prayers plus the optional sunrise alert exposed in Settings.
        val scheduledPrayers = listOf(
            Prayer.FAJR,
            Prayer.SUNRISE,
            Prayer.DHUHR,
            Prayer.ASR,
            Prayer.MAGHRIB,
            Prayer.ISHA
        )

        for (prayer in scheduledPrayers) {
            val config = alarmSettings.getConfigForPrayer(prayer)
            // Reconcile both the main alarm and the previous pre-reminder before
            // writing the current real occurrence.
            alarmRepository.cancelAlarm(prayer)
            if (!config.isEnabled) {
                continue
            }

            val todayTime = getInstantForPrayer(todayRecord, prayer)
            val tomorrowTime = getInstantForPrayer(tomorrowRecord, prayer)

            // Schedule only a real computed occurrence. An unavailable day is never
        // never substitutes a computed time with an invented fallback.
            val targetInstant = listOfNotNull(todayTime, tomorrowTime)
                .firstOrNull { it > now }
                ?: continue

            if (targetInstant > now) {
                alarmRepository.scheduleExactAlarm(
                    prayer = prayer,
                    targetInstant = targetInstant,
                    title = "Waktu ${prayer.displayNameId}",
                    isPreReminder = false
                )

                // Schedule pre-reminder if configured
                if (config.preReminderMinutes > 0) {
                    val preReminderInstant = targetInstant.minus(config.preReminderMinutes.minutes)
                    if (preReminderInstant > now) {
                        alarmRepository.scheduleExactAlarm(
                            prayer = prayer,
                            targetInstant = preReminderInstant,
                            title = "${config.preReminderMinutes} Menit Menuju ${prayer.displayNameId}",
                            isPreReminder = true
                        )
                    }
                }
            }
        }
    }

    private fun getInstantForPrayer(record: PrayerTimeRecord?, prayer: Prayer): Instant? =
        record?.getInstantForPrayer(prayer)
}

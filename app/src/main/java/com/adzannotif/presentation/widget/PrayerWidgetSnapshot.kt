package com.adzannotif.presentation.widget

import com.adzannotif.core.astronomy.HijriDate
import com.adzannotif.core.astronomy.internal.HijriCalendar
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.core.prayer.PrayerTimesUnavailableException
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

internal enum class PrayerWidgetAvailability {
    AVAILABLE,
    UNAVAILABLE,
}

internal data class PrayerWidgetTime(
    val prayer: Prayer,
    val timeEpochMillis: Long,
    val isCurrent: Boolean,
)

internal data class PrayerWidgetSnapshot(
    val availability: PrayerWidgetAvailability,
    val locationName: String? = null,
    val timeZoneId: String? = null,
    val hijriDate: HijriDate? = null,
    val currentPrayer: Prayer? = null,
    val nextPrayer: Prayer? = null,
    val nextTargetEpochMillis: Long? = null,
    val prayerTimes: List<PrayerWidgetTime> = emptyList(),
) {
    companion object {
        fun unavailable(): PrayerWidgetSnapshot = PrayerWidgetSnapshot(
            availability = PrayerWidgetAvailability.UNAVAILABLE,
        )
    }
}

internal object PrayerWidgetSnapshotLoader {
    suspend fun load(
        locationRepository: LocationRepository,
        prayerTimesRepository: PrayerTimesRepository,
        settingsRepository: SettingsRepository,
        now: Instant = Clock.System.now(),
    ): PrayerWidgetSnapshot {
        return try {
            val location = locationRepository.currentOrSelectedLocation.first()
                ?: return PrayerWidgetSnapshot.unavailable()
            val settings = settingsRepository.userSettings.first()
            val timeZone = TimeZone.of(location.timeZoneId)
            val today = now.toLocalDateTime(timeZone).date
            val todayRecord = prayerTimesRepository
                .getPrayerTimesForDate(today, location, settings)
                .first()
            val tomorrowRecord = if (todayRecord.findNextPrayer(now) == null) {
                prayerTimesRepository
                    .getPrayerTimesForDate(today.plus(DatePeriod(days = 1)), location, settings)
                    .first()
            } else {
                null
            }
            val hijriDate = HijriCalendar.toHijri(now.toEpochMilliseconds(), timeZone)
            PrayerWidgetSnapshotLogic.create(
                location = location,
                todayRecord = todayRecord,
                tomorrowRecord = tomorrowRecord,
                hijriDate = hijriDate,
                now = now,
            )
        } catch (_: PrayerTimesUnavailableException) {
            PrayerWidgetSnapshot.unavailable()
        } catch (_: Exception) {
            PrayerWidgetSnapshot.unavailable()
        }
    }
}

internal object PrayerWidgetSnapshotLogic {
    private val displayedPrayers = listOf(
        Prayer.FAJR,
        Prayer.SUNRISE,
        Prayer.DHUHR,
        Prayer.ASR,
        Prayer.MAGHRIB,
        Prayer.ISHA,
    )

    fun create(
        location: LocationInfo,
        todayRecord: PrayerTimeRecord,
        tomorrowRecord: PrayerTimeRecord?,
        hijriDate: HijriDate?,
        now: Instant,
    ): PrayerWidgetSnapshot {
        val currentPrayer = todayRecord.findCurrentPrayer(now)
        val nextPair = todayRecord.findNextPrayer(now)
            ?: tomorrowRecord?.let { Prayer.FAJR to it.fajr }

        return PrayerWidgetSnapshot(
            availability = PrayerWidgetAvailability.AVAILABLE,
            locationName = location.name,
            timeZoneId = location.timeZoneId,
            hijriDate = hijriDate,
            currentPrayer = currentPrayer,
            nextPrayer = nextPair?.first,
            nextTargetEpochMillis = nextPair?.second?.toEpochMilliseconds(),
            prayerTimes = displayedPrayers.map { prayer ->
                PrayerWidgetTime(
                    prayer = prayer,
                    timeEpochMillis = todayRecord.getInstantForPrayer(prayer)
                        ?.toEpochMilliseconds()
                        ?: error("Prayer time is unavailable for $prayer"),
                    isCurrent = prayer == currentPrayer,
                )
            },
        )
    }
}

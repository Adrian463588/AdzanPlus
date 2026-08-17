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

internal data class PrayerTimetableItem(
    val name: String,
    val timeEpochMillis: Long,
    val isPassed: Boolean,
    val isNext: Boolean,
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
    val timetableItems: List<PrayerTimetableItem> = emptyList(),
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
        val nextPrayer = nextPair?.first

        // Dhuha calculated as Sunrise + 25 minutes
        val dhuhaMillis = todayRecord.sunrise.toEpochMilliseconds() + 25 * 60 * 1000L
        val nowMillis = now.toEpochMilliseconds()

        val fullSchedule = listOf(
            "Imsak" to todayRecord.imsak.toEpochMilliseconds(),
            "Shubuh" to todayRecord.fajr.toEpochMilliseconds(),
            "Terbit" to todayRecord.sunrise.toEpochMilliseconds(),
            "Dhuha" to dhuhaMillis,
            "Dzuhur" to todayRecord.dhuhr.toEpochMilliseconds(),
            "Ashar" to todayRecord.asr.toEpochMilliseconds(),
            "Maghrib" to todayRecord.maghrib.toEpochMilliseconds(),
            "Isya" to todayRecord.isha.toEpochMilliseconds(),
        )

        val timetable = fullSchedule.map { (name, timeMillis) ->
            PrayerTimetableItem(
                name = name,
                timeEpochMillis = timeMillis,
                isPassed = timeMillis <= nowMillis,
                isNext = (name.equals("Shubuh", ignoreCase = true) && nextPrayer == Prayer.FAJR) ||
                        (name.equals("Terbit", ignoreCase = true) && nextPrayer == Prayer.SUNRISE) ||
                        (name.equals("Dzuhur", ignoreCase = true) && nextPrayer == Prayer.DHUHR) ||
                        (name.equals("Ashar", ignoreCase = true) && nextPrayer == Prayer.ASR) ||
                        (name.equals("Maghrib", ignoreCase = true) && nextPrayer == Prayer.MAGHRIB) ||
                        (name.equals("Isya", ignoreCase = true) && nextPrayer == Prayer.ISHA),
            )
        }

        return PrayerWidgetSnapshot(
            availability = PrayerWidgetAvailability.AVAILABLE,
            locationName = location.name,
            timeZoneId = location.timeZoneId,
            hijriDate = hijriDate,
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
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
            timetableItems = timetable,
        )
    }
}

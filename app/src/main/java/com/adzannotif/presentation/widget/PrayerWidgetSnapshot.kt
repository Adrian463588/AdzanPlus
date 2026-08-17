package com.adzannotif.presentation.widget

import com.adzannotif.core.astronomy.HijriDate
import com.adzannotif.core.astronomy.AstronomyEngine
import com.adzannotif.core.astronomy.ObserverLocation
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

/** Prayer rows rendered by the detailed widget. Labels stay in resources. */
internal enum class PrayerWidgetTimetableEntry(val prayer: Prayer?) {
    IMSAK(Prayer.IMSAK),
    FAJR(Prayer.FAJR),
    SUNRISE(Prayer.SUNRISE),
    DHUHA(null),
    DHUHR(Prayer.DHUHR),
    ASR(Prayer.ASR),
    MAGHRIB(Prayer.MAGHRIB),
    ISHA(Prayer.ISHA),
}

internal data class PrayerTimetableItem(
    val entry: PrayerWidgetTimetableEntry,
    val timeEpochMillis: Long?,
    val isPassed: Boolean,
    val isCurrent: Boolean,
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
        astronomyEngine: AstronomyEngine,
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
                dhuhaTimeEpochMillis = PrayerWidgetDhuhaCalculator.calculate(
                    location = location,
                    todayRecord = todayRecord,
                    astronomyEngine = astronomyEngine,
                ),
                now = now,
            )
        } catch (_: PrayerTimesUnavailableException) {
            PrayerWidgetSnapshot.unavailable()
        } catch (_: Exception) {
            PrayerWidgetSnapshot.unavailable()
        }
    }
}

/**
 * Dhuha starts when the Sun reaches +4.5° altitude. The crossing is solved from
 * the existing astronomy engine for the selected location and civil date.
 * If the crossing cannot be computed, the widget keeps the value unavailable.
 */
internal object PrayerWidgetDhuhaCalculator {
    private const val DHUHA_SOLAR_ALTITUDE_DEGREES = 4.5
    private const val SEARCH_ITERATIONS = 32

    fun calculate(
        location: LocationInfo,
        todayRecord: PrayerTimeRecord,
        astronomyEngine: AstronomyEngine,
    ): Long? {
        return try {
            val sunriseMillis = todayRecord.sunrise.toEpochMilliseconds()
            val noonMillis = todayRecord.dhuhr.toEpochMilliseconds()
            if (sunriseMillis >= noonMillis) return null

            val observer = ObserverLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                elevationMeters = location.elevation,
                timeZoneId = location.timeZoneId,
            )
            val sunriseAltitude = astronomyEngine
                .getSunState(observer, sunriseMillis)
                .position
                .altitude
            val noonAltitude = astronomyEngine
                .getSunState(observer, noonMillis)
                .position
                .altitude
            if (!sunriseAltitude.isFinite() || !noonAltitude.isFinite()) return null
            if (sunriseAltitude >= DHUHA_SOLAR_ALTITUDE_DEGREES) return sunriseMillis
            if (noonAltitude < DHUHA_SOLAR_ALTITUDE_DEGREES) return null

            var low = sunriseMillis
            var high = noonMillis
            repeat(SEARCH_ITERATIONS) {
                val middle = low + (high - low) / 2L
                val altitude = astronomyEngine
                    .getSunState(observer, middle)
                    .position
                    .altitude
                if (!altitude.isFinite()) return null
                if (altitude >= DHUHA_SOLAR_ALTITUDE_DEGREES) {
                    high = middle
                } else {
                    low = middle
                }
            }
            high
        } catch (_: Exception) {
            null
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
        dhuhaTimeEpochMillis: Long? = null,
        now: Instant,
    ): PrayerWidgetSnapshot {
        val currentPrayer = todayRecord.findCurrentPrayer(now)
        val nextPair = todayRecord.findNextPrayer(now)
            ?: tomorrowRecord?.findNextPrayer(now)
        val nextPrayer = nextPair?.first
        val nowMillis = now.toEpochMilliseconds()

        val fullSchedule = listOf(
            PrayerWidgetTimetableEntry.IMSAK to todayRecord.imsak.toEpochMilliseconds(),
            PrayerWidgetTimetableEntry.FAJR to todayRecord.fajr.toEpochMilliseconds(),
            PrayerWidgetTimetableEntry.SUNRISE to todayRecord.sunrise.toEpochMilliseconds(),
            PrayerWidgetTimetableEntry.DHUHA to dhuhaTimeEpochMillis,
            PrayerWidgetTimetableEntry.DHUHR to todayRecord.dhuhr.toEpochMilliseconds(),
            PrayerWidgetTimetableEntry.ASR to todayRecord.asr.toEpochMilliseconds(),
            PrayerWidgetTimetableEntry.MAGHRIB to todayRecord.maghrib.toEpochMilliseconds(),
            PrayerWidgetTimetableEntry.ISHA to todayRecord.isha.toEpochMilliseconds(),
        )

        val nextTargetMillis = nextPair?.second?.toEpochMilliseconds()
        val timetable = fullSchedule.map { (entry, timeMillis) ->
            val isCurrent = entry.prayer != null && entry.prayer == currentPrayer
            val isNext = entry.prayer != null &&
                entry.prayer == nextPrayer &&
                timeMillis != null &&
                timeMillis == nextTargetMillis
            PrayerTimetableItem(
                entry = entry,
                timeEpochMillis = timeMillis,
                isPassed = timeMillis?.let { it <= nowMillis } == true,
                isCurrent = isCurrent,
                isNext = isNext,
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

package com.adzannotif.domain.usecase

import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class NextPrayerInfo(
    val currentPrayer: Prayer?,
    val nextPrayer: Prayer,
    val targetTime: Instant,
    val todayRecord: PrayerTimeRecord,
)

class GetNextPrayerUseCase @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(): Flow<NextPrayerInfo?> {
        return combine(
            settingsRepository.userSettings,
            locationRepository.currentOrSelectedLocation,
        ) { settings, location ->
            Pair(settings, location)
        }.flatMapLatest { (settings, location) ->
            flow {
                val tz = TimeZone.of(location.timeZoneId)
                val todayDate = Clock.System.now().toLocalDateTime(tz).date
                val todayRecord = prayerTimesRepository.getPrayerTimesForDate(todayDate, location, settings).first()
                val now = Clock.System.now()

                val nextPair = todayRecord.findNextPrayer(now)
                val current = todayRecord.findCurrentPrayer(now)

                if (nextPair != null) {
                    emit(
                        NextPrayerInfo(
                            currentPrayer = current,
                            nextPrayer = nextPair.first,
                            targetTime = nextPair.second,
                            todayRecord = todayRecord,
                        )
                    )
                } else {
                    // Past Isha -> Next prayer is tomorrow's Fajr
                    val tomorrowDate = todayDate.plus(DatePeriod(days = 1))
                    val tomorrowRecord = prayerTimesRepository.getPrayerTimesForDate(tomorrowDate, location, settings).first()
                    emit(
                        NextPrayerInfo(
                            currentPrayer = Prayer.ISHA,
                            nextPrayer = Prayer.FAJR,
                            targetTime = tomorrowRecord.fajr,
                            todayRecord = todayRecord,
                        )
                    )
                }
            }
        }
    }
}

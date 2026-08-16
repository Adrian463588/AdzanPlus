package com.adzannotif.domain.usecase

import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class GetTodayPrayerTimesUseCase @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(targetDate: LocalDate? = null): Flow<PrayerTimeRecord> {
        return combine(
            settingsRepository.userSettings,
            locationRepository.currentOrSelectedLocation,
        ) { settings, location ->
            Pair(settings, location)
        }.flatMapLatest { (settings, location) ->
            val date = targetDate ?: run {
                val tz = TimeZone.of(location.timeZoneId)
                Clock.System.now().toLocalDateTime(tz).date
            }
            prayerTimesRepository.getPrayerTimesForDate(date, location, settings)
        }
    }
}

package com.adzannotif.domain.usecase

import com.adzannotif.core.prayer.PrayerTimesUnavailableException
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetMonthlyPrayerTimesUseCase @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(year: Int, month: Int): Flow<List<PrayerTimeRecord>> {
        return combine(
            settingsRepository.userSettings,
            locationRepository.currentOrSelectedLocation,
        ) { settings, location ->
            Pair(settings, location)
        }.flatMapLatest { (settings, location) ->
            if (location == null) {
                flowOf(emptyList())
            } else {
                flow {
                    try {
                        emitAll(prayerTimesRepository.getMonthlyPrayerTimes(year, month, location, settings))
                    } catch (_: PrayerTimesUnavailableException) {
                        emit(emptyList())
                    }
                }
            }
        }
    }
}

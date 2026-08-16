package com.adzannotif.domain.repository

import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface PrayerTimesRepository {
    fun getPrayerTimesForDate(date: LocalDate, location: LocationInfo, settings: UserSettings): Flow<PrayerTimeRecord>
    fun getMonthlyPrayerTimes(year: Int, month: Int, location: LocationInfo, settings: UserSettings): Flow<List<PrayerTimeRecord>>
    suspend fun computeAndCachePrayerTimes(startDate: LocalDate, daysCount: Int, location: LocationInfo, settings: UserSettings): List<PrayerTimeRecord>
}

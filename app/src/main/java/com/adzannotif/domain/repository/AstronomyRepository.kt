package com.adzannotif.domain.repository

import com.adzannotif.core.astronomy.HijriDate
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SkyEvent
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.model.astronomy.SunInfo
import kotlinx.coroutines.flow.Flow

interface AstronomyRepository {
    fun getSunInfo(location: LocationInfo, epochMillis: Long): Flow<SunInfo>
    fun getMoonInfo(location: LocationInfo, epochMillis: Long): Flow<MoonInfo>
    fun getStarMapData(location: LocationInfo, epochMillis: Long): Flow<StarMapData>
    suspend fun getHijriDate(gregorianEpochMillis: Long, timeZoneId: String): HijriDate
    suspend fun getMonthCalendar(location: LocationInfo, year: Int, month: Int): List<CalendarDay>
    suspend fun getUpcomingEvents(
        location: LocationInfo,
        fromMillis: Long,
        days: Int,
    ): List<SkyEvent>
}

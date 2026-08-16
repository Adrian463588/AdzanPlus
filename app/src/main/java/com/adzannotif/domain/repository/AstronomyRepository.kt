package com.adzannotif.domain.repository

import com.adzannotif.core.astronomy.HijriDate
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SkyEvent
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.model.astronomy.SunInfo
import kotlinx.coroutines.flow.Flow

interface AstronomyRepository {
    fun getSunInfo(latDeg: Double, lonDeg: Double, epochMillis: Long): Flow<SunInfo>
    fun getMoonInfo(latDeg: Double, lonDeg: Double, epochMillis: Long): Flow<MoonInfo>
    fun getStarMapData(latDeg: Double, lonDeg: Double, epochMillis: Long): Flow<StarMapData>
    suspend fun getHijriDate(gregorianEpochMillis: Long): HijriDate
    suspend fun getMonthCalendar(latDeg: Double, lonDeg: Double, year: Int, month: Int): List<CalendarDay>
    suspend fun getUpcomingEvents(latDeg: Double, lonDeg: Double, fromMillis: Long, days: Int): List<SkyEvent>
}

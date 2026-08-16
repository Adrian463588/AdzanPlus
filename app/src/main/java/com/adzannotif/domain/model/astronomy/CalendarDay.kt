package com.adzannotif.domain.model.astronomy

import com.adzannotif.domain.model.PrayerTimeRecord

data class CalendarDay(
    val gregorianEpochMillis: Long,
    val gregorianDay: Int,
    val gregorianMonth: Int,
    val gregorianYear: Int,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val hijriMonthName: String,
    val prayerTimes: PrayerTimeRecord? = null,
    val moonPhaseName: String,
    val moonPhaseOrdinal: Int,        // 0-7 for icon
    val moonIlluminationPercent: Double,
    val isNewMoon: Boolean,
    val isFullMoon: Boolean,
    val goldenHourMorningStartMillis: Long?,
    val sunriseMillis: Long?,
    val sunsetMillis: Long?,
    val isToday: Boolean = false
)

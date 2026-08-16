package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.HijriDate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

public object HijriCalendar {
    private const val ISLAMIC_EPOCH_JD = 1948439.5

    fun toHijri(
        gregorianEpochMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): HijriDate {
        val localDate = Instant.fromEpochMilliseconds(gregorianEpochMillis)
            .toLocalDateTime(timeZone)
            .date
        val civilJulianDay = gregorianToJulianDay(localDate)

        val year = floor(
            (30.0 * (civilJulianDay - ISLAMIC_EPOCH_JD) + 10646.0) / 10631.0
        ).toInt()
        val month = minOf(
            12,
            ceil(
                (civilJulianDay - (29.0 + islamicToJulianDay(year, 1, 1))) / 29.5
            ).toInt() + 1,
        )
        val day = (civilJulianDay - islamicToJulianDay(year, month, 1) + 1.0).toInt()

        return HijriDate(
            year = year,
            month = month,
            day = day,
            monthName = HijriDate.MONTH_NAMES[month - 1],
        )
    }

    fun fromHijri(year: Int, month: Int, day: Int): Long {
        require(year > 0) { "Hijri year must be positive" }
        require(month in 1..12) { "Hijri month must be between 1 and 12" }
        require(day in 1..30) { "Hijri day must be between 1 and 30" }

        val jd = islamicToJulianDay(year, month, day)
        return ((jd - 2440587.5) * 86_400_000.0).roundToLong()
    }

    private fun islamicToJulianDay(year: Int, month: Int, day: Int): Double {
        return day.toDouble() +
            ceil(29.5 * (month - 1)) +
            (year - 1) * 354.0 +
            floor((3.0 + 11.0 * year) / 30.0) +
            ISLAMIC_EPOCH_JD - 1.0
    }

    private fun gregorianToJulianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthNumber
        if (month <= 2) {
            year -= 1
            month += 12
        }

        val century = floor(year / 100.0)
        return floor(365.25 * (year + 4716)) +
            floor(30.6001 * (month + 1)) +
            date.dayOfMonth +
            2.0 - century +
            floor(century / 4.0) -
            1524.5
    }
}

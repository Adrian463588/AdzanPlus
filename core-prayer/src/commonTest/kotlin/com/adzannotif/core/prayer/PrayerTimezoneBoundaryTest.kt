package com.adzannotif.core.prayer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class PrayerTimezoneBoundaryTest {
    @Test
    fun computedCivilEventsStayOnRequestedLocationDate() {
        val requestedDate = DateComponents(year = 2026, month = 8, day = 16)
        val knownLocations = listOf(
            Coordinates(latitude = -6.2088, longitude = 106.8456) to "Asia/Jakarta",
            Coordinates(latitude = 35.6762, longitude = 139.6503) to "Asia/Tokyo",
            Coordinates(latitude = 51.5074, longitude = -0.1278) to "Europe/London",
            Coordinates(latitude = 40.7128, longitude = -74.0060) to "America/New_York",
        )

        knownLocations.forEach { (coordinates, timeZoneId) ->
            val times = PrayerTimes(
                coordinates = coordinates,
                dateComponents = requestedDate,
                calculationParameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.createParameters(),
            )
            val timeZone = TimeZone.of(timeZoneId)
            val expectedDate = LocalDate(requestedDate.year, requestedDate.month, requestedDate.day)

            assertEquals(expectedDate, times.fajr.toLocalDateTime(timeZone).date)
            assertEquals(expectedDate, times.sunrise.toLocalDateTime(timeZone).date)
            assertEquals(expectedDate, times.dhuhr.toLocalDateTime(timeZone).date)
            assertEquals(expectedDate, times.asr.toLocalDateTime(timeZone).date)
            assertEquals(expectedDate, times.maghrib.toLocalDateTime(timeZone).date)
            assertEquals(expectedDate, times.isha.toLocalDateTime(timeZone).date)
        }
    }
}

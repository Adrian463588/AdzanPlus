package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.SunMath
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant

class SunMathTest {
    @Test
    fun testJakartaSunrise() {
        val lat = -6.2
        val lon = 106.8
        // 17 Aug 2026 ~00:00:00 UTC = 1786924800000 (roughly)
        val epochMillis = 1786924800000L
        
        val sunrise = SunMath.computeSunRise(lat, lon, epochMillis)
        assertNotNull(sunrise)
        val sunriseHourUTC = (sunrise % 86400000L) / 3600000.0
        // Expect ~ 22.6 hours UTC (22:36 UTC -> 05:36 WIB)
        assertTrue(sunriseHourUTC > 22.0 && sunriseHourUTC < 23.5, "Sunrise should be ~22:36 UTC")
    }

    @Test
    fun testJakartaSunset() {
        val lat = -6.2
        val lon = 106.8
        val epochMillis = 1786924800000L
        
        val sunset = SunMath.computeSunSet(lat, lon, epochMillis)
        assertNotNull(sunset)
        val sunsetHourUTC = (sunset % 86400000L) / 3600000.0
        // Expect ~ 10.9 hours UTC (10:53 UTC -> 17:53 WIB)
        assertTrue(sunsetHourUTC > 10.0 && sunsetHourUTC < 11.5, "Sunset should be ~10:53 UTC")
    }

    @Test
    fun testSolarNoon() {
        val lat = -6.2
        val lon = 106.8
        val epochMillis = 1786924800000L
        val noon = SunMath.computeSolarNoon(lat, lon, epochMillis)
        val noonHourUTC = (noon % 86400000L) / 3600000.0
        // roughly 4.7-5.0 UTC -> 11:45-12:00 WIB
        assertTrue(noonHourUTC > 4.5 && noonHourUTC < 5.5, "Noon should be around 5.0 UTC")
    }

    @Test
    fun testJakartaEventsUseSelectedCivilTimezone() {
        val timeZone = TimeZone.of("Asia/Jakarta")
        val dateMillis = 1786924800000L

        val sunrise = assertNotNull(SunMath.computeSunRise(-7.7956, 110.3695, dateMillis, timeZone))
        val noon = SunMath.computeSolarNoon(-7.7956, 110.3695, dateMillis, timeZone)
        val sunset = assertNotNull(SunMath.computeSunSet(-7.7956, 110.3695, dateMillis, timeZone))

        val sunriseHour = Instant.fromEpochMilliseconds(sunrise).toLocalDateTime(timeZone).time.hour
        val noonHour = Instant.fromEpochMilliseconds(noon).toLocalDateTime(timeZone).time.hour
        val sunsetHour = Instant.fromEpochMilliseconds(sunset).toLocalDateTime(timeZone).time.hour

        assertTrue(sunriseHour in 5..7, "Sunrise must be morning in Asia/Jakarta")
        assertTrue(noonHour in 11..13, "Solar noon must be around midday in Asia/Jakarta")
        assertTrue(sunsetHour in 16..18, "Sunset must be afternoon in Asia/Jakarta")
    }

    @Test
    fun testSolarNoonUsesOffsetAtNoonAcrossDstTransition() {
        val timeZone = TimeZone.of("America/New_York")
        val dateMillis = LocalDate(2026, 3, 8)
            .atTime(12, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()
        val noon = SunMath.computeSolarNoon(40.7128, -74.0060, dateMillis, timeZone)
        val localHour = Instant.fromEpochMilliseconds(noon).toLocalDateTime(timeZone).time.hour

        assertTrue(localHour in 11..13, "Solar noon must remain near local midday on DST transition")
    }
}

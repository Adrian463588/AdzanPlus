package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.MoonMath
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant

class MoonMathTest {
    @Test
    fun testMoonDistance() {
        val epochMillis = 1786924800000L // 17 Aug 2026
        val distance = MoonMath.computeMoonDistanceKm(epochMillis)
        assertTrue(distance in 356000.0..406000.0, "Moon distance should be within physical limits")
    }

    @Test
    fun testWaxingCrescent() {
        val epochMillis = 1786924800000L // arbitrary
        val illumination = MoonMath.computeMoonIllumination(epochMillis)
        assertTrue(illumination in 0.0..1.0)
    }

    @Test
    fun nextMoonriseIsStrictlyAfterNowAndMayCrossLocalDayBoundary() {
        val timeZone = TimeZone.of("Asia/Jakarta")
        val fromMillis = LocalDate(2026, 8, 17)
            .atTime(23, 59)
            .toInstant(timeZone)
            .toEpochMilliseconds()

        val rise = assertNotNull(
            MoonMath.computeNextMoonRise(
                lat = -6.2088,
                lon = 106.8456,
                fromMillis = fromMillis,
                timeZone = timeZone,
            ),
        )

        assertTrue(rise > fromMillis)
        assertTrue(
            Instant.fromEpochMilliseconds(rise).toLocalDateTime(timeZone).date >=
                LocalDate(2026, 8, 18),
        )
    }

    @Test
    fun moonTransitStaysInsideSelectedCivilDayAcrossDstBoundary() {
        val timeZone = TimeZone.of("America/New_York")
        val date = LocalDate(2026, 3, 8)
        val dateMillis = date.atTime(12, 0).toInstant(timeZone).toEpochMilliseconds()

        val transit = assertNotNull(
            MoonMath.computeMoonTransit(40.7128, -74.0060, dateMillis, timeZone),
        )

        assertTrue(Instant.fromEpochMilliseconds(transit).toLocalDateTime(timeZone).date == date)
    }
}

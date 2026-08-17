package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.PhotoPhasePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.math.abs
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PhotoPhasePolicyTest {
    @Test
    fun testClassifySolarPhase() {
        assertEquals(SolarPhase.DAY, PhotoPhasePolicy.classifySolarPhase(20.0))
        assertEquals(SolarPhase.GOLDEN_HOUR, PhotoPhasePolicy.classifySolarPhase(3.0))
        assertEquals(SolarPhase.BLUE_HOUR, PhotoPhasePolicy.classifySolarPhase(-5.0))
        assertEquals(SolarPhase.NAUTICAL_TWILIGHT, PhotoPhasePolicy.classifySolarPhase(-10.0))
        assertEquals(SolarPhase.ASTRONOMICAL_TWILIGHT, PhotoPhasePolicy.classifySolarPhase(-15.0))
        assertEquals(SolarPhase.NIGHT, PhotoPhasePolicy.classifySolarPhase(-20.0))
    }

    @Test
    fun testPhotographyPhaseBoundariesAreConsistentWithWindows() {
        assertEquals(SolarPhase.BLUE_HOUR, PhotoPhasePolicy.classifySolarPhase(-6.0))
        assertEquals(SolarPhase.GOLDEN_HOUR, PhotoPhasePolicy.classifySolarPhase(-4.0))
        assertEquals(SolarPhase.GOLDEN_HOUR, PhotoPhasePolicy.classifySolarPhase(6.0))
        assertEquals(SolarPhase.DAY, PhotoPhasePolicy.classifySolarPhase(6.01))
    }

    @Test
    fun testJakartaGoldenBlueHourNotNull() {
        // Jakarta coordinates: -6.2088, 106.8456
        val epochMillis = 1786924800000L // 17 Aug 2026
        val result = PhotoPhasePolicy.computeGoldenBlueHour(
            lat = -6.2088,
            lon = 106.8456,
            dateMillis = epochMillis,
            timeZone = TimeZone.of("Asia/Jakarta"),
        )

        assertNotNull(result.morningBlueHour, "Morning Blue Hour must not be null")
        assertNotNull(result.morningGoldenHour, "Morning Golden Hour must not be null")
        assertNotNull(result.eveningGoldenHour, "Evening Golden Hour must not be null")
        assertNotNull(result.eveningBlueHour, "Evening Blue Hour must not be null")

        assertTrue(result.morningBlueHour!!.startMillis < result.morningBlueHour!!.endMillis)
        assertTrue(result.morningGoldenHour!!.startMillis < result.morningGoldenHour!!.endMillis)
        assertTrue(result.eveningGoldenHour!!.startMillis < result.eveningGoldenHour!!.endMillis)
        assertTrue(result.eveningBlueHour!!.startMillis < result.eveningBlueHour!!.endMillis)

        // Morning golden hour starts right when morning blue hour ends (within 1 min)
        assertTrue(result.morningBlueHour!!.endMillis <= result.morningGoldenHour!!.startMillis + 60000L)
    }

    @Test
    fun testWindowsUseSelectedCivilDateAndAltitudeThresholds() {
        val timeZone = TimeZone.of("Asia/Jakarta")
        val dateMillis = 1786924800000L // 2026-08-17 UTC input for the selected civil date
        val result = PhotoPhasePolicy.computeGoldenBlueHour(
            lat = -7.7956,
            lon = 110.3695,
            dateMillis = dateMillis,
            timeZone = timeZone,
        )
        val windows = listOfNotNull(
            result.morningBlueHour,
            result.morningGoldenHour,
            result.eveningGoldenHour,
            result.eveningBlueHour,
        )

        assertTrue(windows.isNotEmpty())
        windows.flatMap { listOf(it.startMillis, it.endMillis) }.forEach { eventMillis ->
            assertEquals(
                LocalDate(2026, 8, 17),
                Instant.fromEpochMilliseconds(eventMillis).toLocalDateTime(timeZone).date,
            )
        }

        val morningBlue = assertNotNull(result.morningBlueHour)
        val morningGolden = assertNotNull(result.morningGoldenHour)
        val eveningGolden = assertNotNull(result.eveningGoldenHour)
        val eveningBlue = assertNotNull(result.eveningBlueHour)
        assertTrue(morningBlue.endMillis <= morningGolden.startMillis)
        assertTrue(morningGolden.endMillis < eveningGolden.startMillis)
        assertTrue(eveningGolden.endMillis <= eveningBlue.startMillis)

        assertAltitudeNear(-7.7956, 110.3695, morningBlue.startMillis, PhotoPhasePolicy.BLUE_HOUR_LOW_DEG)
        assertAltitudeNear(-7.7956, 110.3695, morningBlue.endMillis, PhotoPhasePolicy.BLUE_HOUR_HIGH_DEG)
        assertAltitudeNear(-7.7956, 110.3695, morningGolden.startMillis, PhotoPhasePolicy.GOLDEN_HOUR_LOW_DEG)
        assertAltitudeNear(-7.7956, 110.3695, morningGolden.endMillis, PhotoPhasePolicy.GOLDEN_HOUR_HIGH_DEG)
    }

    private fun assertAltitudeNear(lat: Double, lon: Double, epochMillis: Long, expected: Double) {
        val actual = com.adzannotif.core.astronomy.internal.SunMath
            .computeSunPosition(lat, lon, 0.0, epochMillis)
            .altitude
        assertTrue(abs(actual - expected) < 0.02, "Expected $expected°, got $actual°")
    }
}

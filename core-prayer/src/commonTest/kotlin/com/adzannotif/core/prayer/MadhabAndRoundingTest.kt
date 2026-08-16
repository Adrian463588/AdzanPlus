package com.adzannotif.core.prayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MadhabAndRoundingTest {

    @Test
    fun testShafiVsHanafiMadhab() {
        val coords = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)

        val shafiParams = CalculationParameters(madhab = Madhab.SHAFI)
        val hanafiParams = CalculationParameters(madhab = Madhab.HANAFI)

        val shafiTimes = PrayerTimes(coords, date, shafiParams)
        val hanafiTimes = PrayerTimes(coords, date, hanafiParams)

        // Asr in Hanafi should be roughly 1 hour after Shafi Asr
        assertTrue(hanafiTimes.asr > shafiTimes.asr, "Hanafi Asr must be later than Shafi Asr")
        val diffMinutes = (hanafiTimes.asr.toEpochMilliseconds() - shafiTimes.asr.toEpochMilliseconds()) / (60 * 1000L)
        assertTrue(diffMinutes in 40..70, "Asr difference should be between 40 and 70 minutes, was $diffMinutes min")
    }

    @Test
    fun testRoundingModes() {
        val coords = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)

        val paramsNearest = CalculationParameters(rounding = RoundingType.NEAREST)
        val paramsUp = CalculationParameters(rounding = RoundingType.UP)
        val paramsDown = CalculationParameters(rounding = RoundingType.DOWN)
        val paramsNone = CalculationParameters(rounding = RoundingType.NONE)

        val timesNearest = PrayerTimes(coords, date, paramsNearest)
        val timesUp = PrayerTimes(coords, date, paramsUp)
        val timesDown = PrayerTimes(coords, date, paramsDown)
        val timesNone = PrayerTimes(coords, date, paramsNone)

        // For NEAREST, UP, DOWN, epochSeconds must be divisible by 60
        assertEquals(0, timesNearest.fajr.epochSeconds % 60)
        assertEquals(0, timesUp.fajr.epochSeconds % 60)
        assertEquals(0, timesDown.fajr.epochSeconds % 60)

        // UP should be >= DOWN
        assertTrue(timesUp.fajr >= timesDown.fajr)
    }

    @Test
    fun testPrayerAdjustments() {
        val coords = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)

        val baseParams = CalculationParameters(prayerAdjustments = PrayerAdjustments.ZERO)
        val customAdjustments = PrayerAdjustments(
            fajr = 5,
            dhuhr = -3,
            asr = 4,
            maghrib = 2,
            isha = -1
        )
        val adjustedParams = CalculationParameters(prayerAdjustments = customAdjustments)

        val baseTimes = PrayerTimes(coords, date, baseParams)
        val adjustedTimes = PrayerTimes(coords, date, adjustedParams)

        assertEquals(5 * 60 * 1000L, adjustedTimes.fajr.toEpochMilliseconds() - baseTimes.fajr.toEpochMilliseconds())
        assertEquals(-3 * 60 * 1000L, adjustedTimes.dhuhr.toEpochMilliseconds() - baseTimes.dhuhr.toEpochMilliseconds())
        assertEquals(4 * 60 * 1000L, adjustedTimes.asr.toEpochMilliseconds() - baseTimes.asr.toEpochMilliseconds())
        assertEquals(2 * 60 * 1000L, adjustedTimes.maghrib.toEpochMilliseconds() - baseTimes.maghrib.toEpochMilliseconds())
        assertEquals(-1 * 60 * 1000L, adjustedTimes.isha.toEpochMilliseconds() - baseTimes.isha.toEpochMilliseconds())
    }

    @Test
    fun testCoordinatesValidation() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates(latitude = 91.0, longitude = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            Coordinates(latitude = -91.0, longitude = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            Coordinates(latitude = 0.0, longitude = 181.0)
        }
        assertFailsWith<IllegalArgumentException> {
            Coordinates(latitude = 0.0, longitude = -181.0)
        }
    }

    @Test
    fun testDateComponentsValidation() {
        assertFailsWith<IllegalArgumentException> {
            DateComponents(year = 2026, month = 0, day = 15)
        }
        assertFailsWith<IllegalArgumentException> {
            DateComponents(year = 2026, month = 13, day = 15)
        }
        assertFailsWith<IllegalArgumentException> {
            DateComponents(year = 2026, month = 5, day = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            DateComponents(year = 2026, month = 5, day = 32)
        }
    }
}


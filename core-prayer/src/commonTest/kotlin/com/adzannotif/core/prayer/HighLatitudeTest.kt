package com.adzannotif.core.prayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HighLatitudeTest {

    @Test
    fun highLatitudeRuleUsesRealSunriseAndSunset() {
        val oslo = Coordinates(latitude = 59.9139, longitude = 10.7522)
        val summerSolstice = DateComponents(year = 2026, month = 6, day = 21)
        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            fajrAngle = 18.0,
            ishaAngle = 17.0,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
        )

        val result = PrayerTimes.calculate(oslo, summerSolstice, params)

        assertTrue(result is PrayerTimesResult.Available)
        val times = (result as PrayerTimesResult.Available).value
        assertTrue(times.fajr < times.sunrise)
        assertTrue(times.sunrise < times.dhuhr)
        assertTrue(times.dhuhr < times.asr)
        assertTrue(times.asr < times.maghrib)
        assertTrue(times.maghrib < times.isha)
    }

    @Test
    fun tromsoMidnightSunReturnsExplicitUnavailable() {
        val tromso = Coordinates(latitude = 69.6492, longitude = 18.9553)
        val summerSolstice = DateComponents(year = 2026, month = 6, day = 21)
        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
        )

        val result = PrayerTimes.calculate(tromso, summerSolstice, params)

        assertEquals(
            PrayerTimesUnavailableReason.SUNRISE_NOT_VISIBLE,
            (result as PrayerTimesResult.Unavailable).reason,
        )
        assertFailsWith<PrayerTimesUnavailableException> {
            PrayerTimes(tromso, summerSolstice, params)
        }
    }

    @Test
    fun winterLocationUsesRealSolarEventsWhenAvailable() {
        val reykjavik = Coordinates(latitude = 64.1466, longitude = -21.9426)
        val winterSolstice = DateComponents(year = 2026, month = 12, day = 21)
        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
        )

        val result = PrayerTimes.calculate(reykjavik, winterSolstice, params)

        assertTrue(result is PrayerTimesResult.Available)
        val times = (result as PrayerTimesResult.Available).value
        assertTrue(times.fajr < times.sunrise)
        assertTrue(times.sunrise < times.dhuhr)
        assertTrue(times.dhuhr < times.asr)
        assertTrue(times.asr < times.maghrib)
        assertTrue(times.maghrib < times.isha)
    }
}

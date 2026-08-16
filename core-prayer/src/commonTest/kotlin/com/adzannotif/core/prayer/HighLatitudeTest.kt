package com.adzannotif.core.prayer

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HighLatitudeTest {

    @Test
    fun testOsloSummerSolsticeWithMiddleOfTheNight() {
        // Oslo, Norway: 59.9139° N, 10.7522° E (High latitude where astronomical twilight does not end in June)
        val oslo = Coordinates(latitude = 59.9139, longitude = 10.7522)
        val summerSolstice = DateComponents(year = 2026, month = 6, day = 21)

        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            fajrAngle = 18.0,
            ishaAngle = 17.0,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        )

        val prayerTimes = PrayerTimes(oslo, summerSolstice, params)

        assertNotNull(prayerTimes.fajr)
        assertNotNull(prayerTimes.sunrise)
        assertNotNull(prayerTimes.dhuhr)
        assertNotNull(prayerTimes.asr)
        assertNotNull(prayerTimes.maghrib)
        assertNotNull(prayerTimes.isha)

        // Strict chronological ordering
        assertTrue(prayerTimes.fajr < prayerTimes.sunrise, "Fajr < Sunrise in Oslo summer")
        assertTrue(prayerTimes.sunrise < prayerTimes.dhuhr, "Sunrise < Dhuhr in Oslo summer")
        assertTrue(prayerTimes.dhuhr < prayerTimes.asr, "Dhuhr < Asr in Oslo summer")
        assertTrue(prayerTimes.asr < prayerTimes.maghrib, "Asr < Maghrib in Oslo summer")
        assertTrue(prayerTimes.maghrib < prayerTimes.isha, "Maghrib < Isha in Oslo summer")
    }

    @Test
    fun testOsloSummerSolsticeWithSeventhOfTheNight() {
        val oslo = Coordinates(latitude = 59.9139, longitude = 10.7522)
        val summerSolstice = DateComponents(year = 2026, month = 6, day = 21)

        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            fajrAngle = 18.0,
            ishaAngle = 17.0,
            highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        )

        val prayerTimes = PrayerTimes(oslo, summerSolstice, params)

        assertTrue(prayerTimes.fajr < prayerTimes.sunrise)
        assertTrue(prayerTimes.maghrib < prayerTimes.isha)
    }

    @Test
    fun testOsloSummerSolsticeWithTwilightAngle() {
        val oslo = Coordinates(latitude = 59.9139, longitude = 10.7522)
        val summerSolstice = DateComponents(year = 2026, month = 6, day = 21)

        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            fajrAngle = 18.0,
            ishaAngle = 17.0,
            highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE
        )

        val prayerTimes = PrayerTimes(oslo, summerSolstice, params)

        assertTrue(prayerTimes.fajr < prayerTimes.sunrise)
        assertTrue(prayerTimes.maghrib < prayerTimes.isha)
    }

    @Test
    fun testTromsoMidnightSunPolarRegion() {
        // Tromsø, Norway: 69.6492° N, 18.9553° E (Inside Arctic Circle - Midnight Sun in June)
        val tromso = Coordinates(latitude = 69.6492, longitude = 18.9553)
        val summerSolstice = DateComponents(year = 2026, month = 6, day = 21)

        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        )

        // Must compute safely without crashing or returning NaN/Null
        val prayerTimes = PrayerTimes(tromso, summerSolstice, params)
        assertNotNull(prayerTimes.fajr)
        assertNotNull(prayerTimes.sunrise)
        assertNotNull(prayerTimes.dhuhr)
        assertNotNull(prayerTimes.asr)
        assertNotNull(prayerTimes.maghrib)
        assertNotNull(prayerTimes.isha)

        assertTrue(prayerTimes.fajr < prayerTimes.sunrise)
        assertTrue(prayerTimes.sunrise < prayerTimes.dhuhr)
        assertTrue(prayerTimes.dhuhr < prayerTimes.asr)
        assertTrue(prayerTimes.asr < prayerTimes.maghrib)
        assertTrue(prayerTimes.maghrib < prayerTimes.isha)
    }

    @Test
    fun testReykjavikWinterSolstice() {
        // Reykjavik, Iceland: 64.1466° N, -21.9426° W (Short winter day)
        val reykjavik = Coordinates(latitude = 64.1466, longitude = -21.9426)
        val winterSolstice = DateComponents(year = 2026, month = 12, day = 21)

        val params = CalculationParameters(
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        )

        val prayerTimes = PrayerTimes(reykjavik, winterSolstice, params)
        assertNotNull(prayerTimes.fajr)
        assertNotNull(prayerTimes.isha)
        assertTrue(prayerTimes.fajr < prayerTimes.sunrise)
        assertTrue(prayerTimes.maghrib < prayerTimes.isha)
    }
}

package com.adzannotif.core.prayer

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SunnahAndAdditionalTimesTest {

    @Test
    fun testImsakTiming() {
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.KEMENAG_RI.createParameters(
            prayerAdjustments = PrayerAdjustments.KEMENAG_DEFAULT_IHTIYATH
        )

        val prayerTimes = PrayerTimes(jakarta, date, params)

        // Imsak is exactly 10 minutes before Subuh (Fajr)
        val diffMs = prayerTimes.fajr.toEpochMilliseconds() - prayerTimes.imsak.toEpochMilliseconds()
        assertEquals(10 * 60 * 1000L, diffMs)

        assertEquals(prayerTimes.imsak, prayerTimes.timeForPrayer(Prayer.IMSAK))
        assertEquals(prayerTimes.fajr, prayerTimes.timeForPrayer(Prayer.FAJR))
    }

    @Test
    fun testIslamicMidnightAndTahajjud() {
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.KEMENAG_RI.createParameters()

        val prayerTimes = PrayerTimes(jakarta, date, params)

        val midnight = prayerTimes.timeForPrayer(Prayer.MIDNIGHT)
        val tahajjud = prayerTimes.timeForPrayer(Prayer.TAHAJJUD)

        assertNotNull(midnight)
        assertNotNull(tahajjud)

        // Midnight and Tahajjud must occur during the NIGHT between Maghrib and tomorrow's Fajr
        // Maghrib is around 17:55 WIB, next Fajr is around 04:40 WIB
        assertTrue(midnight > prayerTimes.maghrib, "Midnight must be after Maghrib")
        assertTrue(tahajjud > midnight, "Tahajjud must be after Midnight")

        val wib = TimeZone.of("Asia/Jakarta")
        val midnightLocal = midnight.toLocalDateTime(wib)
        val tahajjudLocal = tahajjud.toLocalDateTime(wib)

        // Midnight in Jakarta is typically around 23:10 - 23:25
        assertTrue(midnightLocal.hour == 23, "Islamic midnight in Jakarta should be around 23:00")

        // Tahajjud (last third) in Jakarta is typically around 01:00 - 01:25 AM next morning
        assertTrue(tahajjudLocal.hour in 1..2, "Tahajjud in Jakarta should be around 01:00 AM")
    }

    @Test
    fun testSunnahTimesDataClass() {
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.KEMENAG_RI.createParameters()

        val todayTimes = PrayerTimes(jakarta, date, params)
        val tomorrowDate = DateComponents(year = 2026, month = 8, day = 17)
        val tomorrowTimes = PrayerTimes(jakarta, tomorrowDate, params)

        val sunnah = SunnahTimes.from(todayTimes.maghrib, tomorrowTimes.fajr)

        val nightDurationMs = tomorrowTimes.fajr.toEpochMilliseconds() - todayTimes.maghrib.toEpochMilliseconds()
        assertTrue(nightDurationMs in (10 * 3600 * 1000L)..(12 * 3600 * 1000L), "Night duration should be around 10 to 11 hours")

        val expectedMidpoint = todayTimes.maghrib.toEpochMilliseconds() + (nightDurationMs / 2)
        assertEquals(expectedMidpoint, sunnah.middleOfTheNight.toEpochMilliseconds())

        val expectedLastThird = todayTimes.maghrib.toEpochMilliseconds() + (2 * nightDurationMs / 3)
        assertEquals(expectedLastThird, sunnah.lastThirdOfTheNight.toEpochMilliseconds())

        assertNotNull(sunnah.firstThirdOfTheNight)
        val expectedFirstThird = todayTimes.maghrib.toEpochMilliseconds() + (nightDurationMs / 3)
        assertEquals(expectedFirstThird, sunnah.firstThirdOfTheNight!!.toEpochMilliseconds())
    }

    @Test
    fun testSunriseAndSunset() {
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.KEMENAG_RI.createParameters()

        val prayerTimes = PrayerTimes(jakarta, date, params)

        val wib = TimeZone.of("Asia/Jakarta")
        val sunriseLocal = prayerTimes.sunrise.toLocalDateTime(wib)
        val maghribLocal = prayerTimes.maghrib.toLocalDateTime(wib)

        // Sunrise is around 05:50 - 06:05
        assertTrue(sunriseLocal.hour in 5..6, "Sunrise in Jakarta should be around 05:50 - 06:05")
        // Maghrib (Sunset) is around 17:50 - 18:05
        assertTrue(maghribLocal.hour in 17..18, "Maghrib in Jakarta should be around 17:50 - 18:05")
    }
}

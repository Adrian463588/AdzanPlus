package com.adzannotif.core.prayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class PrayerTimesCalculatorTest {

    @Test
    fun testMakkahPrayerTimesCalculation() {
        val makkahCoordinates = Coordinates(latitude = 21.4225, longitude = 39.8262)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.UMM_AL_QURA.createParameters()

        val prayerTimes = PrayerTimes(
            coordinates = makkahCoordinates,
            dateComponents = date,
            calculationParameters = params
        )

        assertNotNull(prayerTimes.fajr)
        assertNotNull(prayerTimes.sunrise)
        assertNotNull(prayerTimes.dhuhr)
        assertNotNull(prayerTimes.asr)
        assertNotNull(prayerTimes.maghrib)
        assertNotNull(prayerTimes.isha)

        // Verify chronological order: Fajr < Sunrise < Dhuhr < Asr < Maghrib < Isha
        assertTrue(prayerTimes.fajr < prayerTimes.sunrise, "Fajr should be before Sunrise")
        assertTrue(prayerTimes.sunrise < prayerTimes.dhuhr, "Sunrise should be before Dhuhr")
        assertTrue(prayerTimes.dhuhr < prayerTimes.asr, "Dhuhr should be before Asr")
        assertTrue(prayerTimes.asr < prayerTimes.maghrib, "Asr should be before Maghrib")
        assertTrue(prayerTimes.maghrib < prayerTimes.isha, "Maghrib should be before Isha")
    }

    @Test
    fun testNextAndCurrentPrayerDetection() {
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.KEMENAG_RI.createParameters()

        val prayerTimes = PrayerTimes(
            coordinates = jakarta,
            dateComponents = date,
            calculationParameters = params
        )

        // 10 minutes before Dhuhr -> next prayer is Dhuhr
        val beforeDhuhr = prayerTimes.dhuhr.minus(10.minutes)
        val nextBeforeDhuhr = prayerTimes.nextPrayer(beforeDhuhr)
        assertEquals(Prayer.DHUHR, nextBeforeDhuhr)

        // 10 minutes after Dhuhr -> current prayer is Dhuhr, next is Asr
        val afterDhuhr = prayerTimes.dhuhr.plus(10.minutes)
        val currentAfterDhuhr = prayerTimes.currentPrayer(afterDhuhr)
        assertEquals(Prayer.DHUHR, currentAfterDhuhr)
        val nextAfterDhuhr = prayerTimes.nextPrayer(afterDhuhr)
        assertEquals(Prayer.ASR, nextAfterDhuhr)
    }
}


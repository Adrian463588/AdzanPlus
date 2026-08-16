package com.adzannotif.core.prayer

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndonesiaKemenagTest {

    @Test
    fun testJakartaKemenagSchedule() {
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val kemenagParams = CalculationMethod.KEMENAG_RI.createParameters(
            prayerAdjustments = PrayerAdjustments.KEMENAG_DEFAULT_IHTIYATH
        )

        val prayerTimes = PrayerTimes(jakarta, date, kemenagParams)
        val wib = TimeZone.of("Asia/Jakarta")

        val subuhLocal = prayerTimes.fajr.toLocalDateTime(wib)
        val dzuhurLocal = prayerTimes.dhuhr.toLocalDateTime(wib)
        val asharLocal = prayerTimes.asr.toLocalDateTime(wib)
        val maghribLocal = prayerTimes.maghrib.toLocalDateTime(wib)
        val isyaLocal = prayerTimes.isha.toLocalDateTime(wib)

        // Subuh in Jakarta is typically around 04:30 - 04:50
        assertTrue(subuhLocal.hour in 4..5, "Subuh hour should be around 4 AM")
        // Dzuhur is around 11:50 - 12:10
        assertTrue(dzuhurLocal.hour in 11..12, "Dzuhur hour should be around 11:50 - 12:15")
        // Ashar is around 15:10 - 15:30
        assertTrue(asharLocal.hour == 15, "Ashar hour should be around 15:00")
        // Maghrib is around 17:50 - 18:10
        assertTrue(maghribLocal.hour in 17..18, "Maghrib hour should be around 17:50 - 18:10")
        // Isya is around 19:00 - 19:25
        assertTrue(isyaLocal.hour in 19..20, "Isya hour should be around 19:00")
    }
}

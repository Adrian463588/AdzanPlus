package com.adzannotif.core.prayer

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CalculationMethodsTest {

    @Test
    fun testMuslimWorldLeagueMethod() {
        // Muslim World League: Fajr 18.0°, Isha 17.0°
        val london = Coordinates(latitude = 51.5074, longitude = -0.1278)
        val date = DateComponents(year = 2026, month = 3, day = 15) // Spring equinox period
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.createParameters()

        assertEquals(18.0, params.fajrAngle, 0.001)
        assertEquals(17.0, params.ishaAngle, 0.001)
        assertEquals(0, params.ishaInterval)

        val prayerTimes = PrayerTimes(london, date, params)
        assertNotNull(prayerTimes.fajr)
        assertNotNull(prayerTimes.dhuhr)
        assertNotNull(prayerTimes.asr)
        assertNotNull(prayerTimes.maghrib)
        assertNotNull(prayerTimes.isha)

        // Strict chronological order
        assertTrue(prayerTimes.fajr < prayerTimes.sunrise)
        assertTrue(prayerTimes.sunrise < prayerTimes.dhuhr)
        assertTrue(prayerTimes.dhuhr < prayerTimes.asr)
        assertTrue(prayerTimes.asr < prayerTimes.maghrib)
        assertTrue(prayerTimes.maghrib < prayerTimes.isha)
    }

    @Test
    fun testUmmAlQuraMethod() {
        // Umm Al-Qura: Fajr 18.5°, Isha 90 minutes after Maghrib
        val makkah = Coordinates(latitude = 21.4225, longitude = 39.8262)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.UMM_AL_QURA.createParameters()

        assertEquals(18.5, params.fajrAngle, 0.001)
        assertEquals(0.0, params.ishaAngle, 0.001)
        assertEquals(90, params.ishaInterval)

        val prayerTimes = PrayerTimes(makkah, date, params)

        // Isha should be exactly 90 minutes after Maghrib
        val ishaDiff = prayerTimes.isha.toEpochMilliseconds() - prayerTimes.maghrib.toEpochMilliseconds()
        assertEquals(90 * 60 * 1000L, ishaDiff)

        // Timezone check in Makkah (Asia/Riyadh, UTC+3)
        val ast = TimeZone.of("Asia/Riyadh")
        val fajrLocal = prayerTimes.fajr.toLocalDateTime(ast)
        val maghribLocal = prayerTimes.maghrib.toLocalDateTime(ast)
        val ishaLocal = prayerTimes.isha.toLocalDateTime(ast)

        // In August, Makkah Fajr is ~04:30 - 04:50, Maghrib is ~18:50 - 19:10
        assertTrue(fajrLocal.hour == 4, "Makkah Fajr hour should be 4")
        assertTrue(maghribLocal.hour in 18..19, "Makkah Maghrib hour should be 18 or 19")
        assertTrue(ishaLocal.hour == 20, "Makkah Isha hour should be 20")
    }

    @Test
    fun testEgyptianGeneralAuthorityMethod() {
        // Egyptian: Fajr 19.5°, Isha 17.5°
        val cairo = Coordinates(latitude = 30.0444, longitude = 31.2357)
        val date = DateComponents(year = 2026, month = 5, day = 1)
        val params = CalculationMethod.EGYPTIAN.createParameters()

        assertEquals(19.5, params.fajrAngle, 0.001)
        assertEquals(17.5, params.ishaAngle, 0.001)

        val prayerTimes = PrayerTimes(cairo, date, params)
        assertTrue(prayerTimes.fajr < prayerTimes.sunrise)
        assertTrue(prayerTimes.maghrib < prayerTimes.isha)
    }

    @Test
    fun testKarachiMethod() {
        // Karachi: Fajr 18.0°, Isha 18.0°
        val karachi = Coordinates(latitude = 24.8607, longitude = 67.0011)
        val date = DateComponents(year = 2026, month = 1, day = 15)
        val paramsHanafi = CalculationMethod.KARACHI.createParameters(madhab = Madhab.HANAFI)
        val paramsShafi = CalculationMethod.KARACHI.createParameters(madhab = Madhab.SHAFI)

        assertEquals(18.0, paramsHanafi.fajrAngle, 0.001)
        assertEquals(18.0, paramsHanafi.ishaAngle, 0.001)

        val timesHanafi = PrayerTimes(karachi, date, paramsHanafi)
        val timesShafi = PrayerTimes(karachi, date, paramsShafi)

        // Fajr, Sunrise, Dhuhr, Maghrib, Isha are identical between madhabs
        assertEquals(timesShafi.fajr, timesHanafi.fajr)
        assertEquals(timesShafi.dhuhr, timesHanafi.dhuhr)
        assertEquals(timesShafi.maghrib, timesHanafi.maghrib)

        // Hanafi Asr occurs strictly after Shafi Asr (shadowFactor 2.0 vs 1.0)
        assertTrue(timesHanafi.asr > timesShafi.asr, "Hanafi Asr must be later than Shafi Asr")
    }

    @Test
    fun testSingaporeMuisMethod() {
        // MUIS Singapore: Fajr 20.0°, Isha 18.0°
        val singapore = Coordinates(latitude = 1.3521, longitude = 103.8198)
        val date = DateComponents(year = 2026, month = 8, day = 16)
        val params = CalculationMethod.SINGAPORE_MUIS.createParameters()

        assertEquals(20.0, params.fajrAngle, 0.001)
        assertEquals(18.0, params.ishaAngle, 0.001)

        val prayerTimes = PrayerTimes(singapore, date, params)
        val sgt = TimeZone.of("Asia/Singapore")
        val fajrLocal = prayerTimes.fajr.toLocalDateTime(sgt)
        val maghribLocal = prayerTimes.maghrib.toLocalDateTime(sgt)

        // Singapore Fajr is typically ~05:40 - 06:00 (due to UTC+8 time offset in Singapore)
        assertTrue(fajrLocal.hour == 5, "Singapore Fajr should be around 5 AM")
        // Singapore Maghrib is typically ~19:10 - 19:30
        assertTrue(maghribLocal.hour == 19, "Singapore Maghrib should be around 19:00")
    }

    @Test
    fun testNorthAmericaIsnaMethod() {
        // ISNA: Fajr 15.0°, Isha 15.0°
        val newYork = Coordinates(latitude = 40.7128, longitude = -74.0060)
        val date = DateComponents(year = 2026, month = 10, day = 10)
        val params = CalculationMethod.NORTH_AMERICA.createParameters()

        assertEquals(15.0, params.fajrAngle, 0.001)
        assertEquals(15.0, params.ishaAngle, 0.001)

        val prayerTimes = PrayerTimes(newYork, date, params)
        assertTrue(prayerTimes.fajr < prayerTimes.sunrise)
        assertTrue(prayerTimes.maghrib < prayerTimes.isha)
    }

    @Test
    fun testGulfAndQatarMethods() {
        val dubai = Coordinates(latitude = 25.2048, longitude = 55.2708)
        val date = DateComponents(year = 2026, month = 6, day = 1)
        val gulfParams = CalculationMethod.GULF.createParameters()
        val qatarParams = CalculationMethod.QATAR.createParameters()

        val gulfTimes = PrayerTimes(dubai, date, gulfParams)
        val qatarTimes = PrayerTimes(dubai, date, qatarParams)

        // Gulf uses Fajr 19.5°, Qatar uses Fajr 18.0° -> Gulf Fajr earlier than Qatar Fajr
        assertTrue(gulfTimes.fajr < qatarTimes.fajr, "Gulf Fajr (19.5°) should be earlier than Qatar Fajr (18.0°)")

        // Both use 90 minute interval for Isha
        val gulfIshaDiff = gulfTimes.isha.toEpochMilliseconds() - gulfTimes.maghrib.toEpochMilliseconds()
        val qatarIshaDiff = qatarTimes.isha.toEpochMilliseconds() - qatarTimes.maghrib.toEpochMilliseconds()
        assertEquals(90 * 60 * 1000L, gulfIshaDiff)
        assertEquals(90 * 60 * 1000L, qatarIshaDiff)
    }

    @Test
    fun testTehranShiaMethod() {
        // Tehran: Fajr 17.7°, Isha 14.0°, Maghrib 4.5°
        val tehran = Coordinates(latitude = 35.6892, longitude = 51.3890)
        val date = DateComponents(year = 2026, month = 4, day = 20)
        val tehranParams = CalculationMethod.TEHRAN.createParameters()

        val times = PrayerTimes(tehran, date, tehranParams)
        assertTrue(times.fajr < times.sunrise)
        assertTrue(times.sunrise < times.dhuhr)
        assertTrue(times.dhuhr < times.asr)
        assertTrue(times.asr < times.maghrib)
        assertTrue(times.maghrib < times.isha)
    }

    @Test
    fun testCustomMethod() {
        val customParams = CalculationParameters(
            method = CalculationMethod.CUSTOM,
            fajrAngle = 16.5,
            ishaAngle = 16.0,
            ishaInterval = 0,
            maghribAngle = 0.0,
            madhab = Madhab.SHAFI,
            prayerAdjustments = PrayerAdjustments(fajr = 5, maghrib = -3)
        )
        val coords = Coordinates(latitude = -7.2575, longitude = 112.7521) // Surabaya
        val date = DateComponents(year = 2026, month = 8, day = 16)

        val times = PrayerTimes(coords, date, customParams)
        assertNotNull(times)
        assertTrue(times.fajr < times.sunrise)
    }
}

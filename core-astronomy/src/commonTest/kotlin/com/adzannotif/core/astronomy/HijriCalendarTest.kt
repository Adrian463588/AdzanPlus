package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.HijriCalendar
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class HijriCalendarTest {
    @Test
    fun testToHijri() {
        // 17 Aug 2026 UTC -> 4 Rabi' al-Awwal 1448 in the Umm al-Qura table.
        val epochMillis = 1786924800000L
        val date = HijriCalendar.toHijri(epochMillis)
        assertEquals(1448, date.year)
        assertEquals(3, date.month)
        assertEquals(4, date.day)
    }

    @Test
    fun timezoneControlsUmmAlQuraDateBoundary() {
        val localMidnight = LocalDateTime(2026, 8, 17, 0, 30)
            .toInstant(TimeZone.of("Asia/Jakarta"))
            .toEpochMilliseconds()

        val utcDate = HijriCalendar.toHijri(localMidnight, TimeZone.UTC)
        val jakartaDate = HijriCalendar.toHijri(localMidnight, TimeZone.of("Asia/Jakarta"))

        assertEquals(3, utcDate.day)
        assertEquals(4, jakartaDate.day)
    }
}

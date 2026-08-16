package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.HijriCalendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HijriCalendarTest {
    @Test
    fun testToHijri() {
        // 17 Aug 2026 -> roughly 23 Safar 1448 or close (allow +-1 day due to approx algorithm)
        val epochMillis = 1786924800000L
        val date = HijriCalendar.toHijri(epochMillis)
        assertEquals(1448, date.year)
        assertEquals(3, date.month)
    }
}

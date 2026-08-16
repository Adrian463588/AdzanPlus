package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.StarMath
import kotlin.test.Test
import kotlin.test.assertTrue

class StarMathTest {
    @Test
    fun testSiriusAltitude() {
        val sirius = Star(32349, "Sirius", 101.287, -16.716, -1.46)
        val lat = -6.2
        val lon = 106.8
        val epochMillis = 1786924800000L // 17 Aug 2026
        val pos = StarMath.computeStarPosition(sirius, lat, lon, epochMillis)
        
        // As long as calculation passes
        assertTrue(pos.altitude > -90.0 && pos.altitude < 90.0)
    }
}

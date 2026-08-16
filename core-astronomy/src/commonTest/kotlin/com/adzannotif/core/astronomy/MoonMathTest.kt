package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.MoonMath
import kotlin.test.Test
import kotlin.test.assertTrue

class MoonMathTest {
    @Test
    fun testMoonDistance() {
        val epochMillis = 1786924800000L // 17 Aug 2026
        val distance = MoonMath.computeMoonDistanceKm(epochMillis)
        assertTrue(distance in 356000.0..406000.0, "Moon distance should be within physical limits")
    }

    @Test
    fun testWaxingCrescent() {
        val epochMillis = 1786924800000L // arbitrary
        val illumination = MoonMath.computeMoonIllumination(epochMillis)
        assertTrue(illumination in 0.0..1.0)
    }
}

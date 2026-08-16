package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.PhotoPhasePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PhotoPhasePolicyTest {
    @Test
    fun testClassifySolarPhase() {
        assertEquals(SolarPhase.DAY, PhotoPhasePolicy.classifySolarPhase(20.0))
        assertEquals(SolarPhase.GOLDEN_HOUR, PhotoPhasePolicy.classifySolarPhase(3.0))
        assertEquals(SolarPhase.BLUE_HOUR, PhotoPhasePolicy.classifySolarPhase(-5.0))
        assertEquals(SolarPhase.NAUTICAL_TWILIGHT, PhotoPhasePolicy.classifySolarPhase(-10.0))
        assertEquals(SolarPhase.ASTRONOMICAL_TWILIGHT, PhotoPhasePolicy.classifySolarPhase(-15.0))
        assertEquals(SolarPhase.NIGHT, PhotoPhasePolicy.classifySolarPhase(-20.0))
    }

    @Test
    fun testJakartaGoldenBlueHourNotNull() {
        // Jakarta coordinates: -6.2088, 106.8456
        val epochMillis = 1786924800000L // 17 Aug 2026
        val result = PhotoPhasePolicy.computeGoldenBlueHour(-6.2088, 106.8456, epochMillis)

        assertNotNull(result.morningBlueHour, "Morning Blue Hour must not be null")
        assertNotNull(result.morningGoldenHour, "Morning Golden Hour must not be null")
        assertNotNull(result.eveningGoldenHour, "Evening Golden Hour must not be null")
        assertNotNull(result.eveningBlueHour, "Evening Blue Hour must not be null")

        assertTrue(result.morningBlueHour!!.startMillis < result.morningBlueHour!!.endMillis)
        assertTrue(result.morningGoldenHour!!.startMillis < result.morningGoldenHour!!.endMillis)
        assertTrue(result.eveningGoldenHour!!.startMillis < result.eveningGoldenHour!!.endMillis)
        assertTrue(result.eveningBlueHour!!.startMillis < result.eveningBlueHour!!.endMillis)

        // Morning golden hour starts right when morning blue hour ends (within 1 min)
        assertTrue(result.morningBlueHour!!.endMillis <= result.morningGoldenHour!!.startMillis + 60000L)
    }
}

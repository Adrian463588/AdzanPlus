package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.PhotoPhasePolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoPhasePolicyTest {
    @Test
    fun testClassifySolarPhase() {
        assertEquals(SolarPhase.GOLDEN_HOUR, PhotoPhasePolicy.classifySolarPhase(3.0))
        assertEquals(SolarPhase.BLUE_HOUR, PhotoPhasePolicy.classifySolarPhase(-5.0))
        assertEquals(SolarPhase.DAY, PhotoPhasePolicy.classifySolarPhase(20.0))
        assertEquals(SolarPhase.NAUTICAL_TWILIGHT, PhotoPhasePolicy.classifySolarPhase(-15.0))
        assertEquals(SolarPhase.CIVIL_TWILIGHT, PhotoPhasePolicy.classifySolarPhase(-10.0))
    }
}

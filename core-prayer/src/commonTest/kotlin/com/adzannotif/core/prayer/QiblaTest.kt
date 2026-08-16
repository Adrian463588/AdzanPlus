package com.adzannotif.core.prayer

import kotlin.test.Test
import kotlin.test.assertTrue

class QiblaTest {

    @Test
    fun testJakartaQiblaDirection() {
        // Jakarta coordinates: -6.2088, 106.8456
        // Expected Qibla bearing from Jakarta is ~295 degrees (West-Northwest)
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val qibla = Qibla.fromCoordinates(jakarta)

        assertTrue(
            qibla.direction in 290.0..298.0,
            "Jakarta Qibla should be between 290 and 298 degrees, was ${qibla.direction}"
        )
    }

    @Test
    fun testLondonQiblaDirection() {
        // London coordinates: 51.5074, -0.1278
        // Expected Qibla bearing from London is ~118 degrees (East-Southeast)
        val london = Coordinates(latitude = 51.5074, longitude = -0.1278)
        val qibla = Qibla.fromCoordinates(london)

        assertTrue(
            qibla.direction in 115.0..122.0,
            "London Qibla should be between 115 and 122 degrees, was ${qibla.direction}"
        )
    }
}


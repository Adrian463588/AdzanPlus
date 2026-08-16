package com.adzannotif.core.prayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QiblaGreatCircleTest {

    @Test
    fun testKaabaSelfCoordinates() {
        // Distance from Kaaba to Kaaba must be 0 km
        val kaaba = Qibla.KAABA_COORDINATES
        val distance = Qibla.calculateDistanceKm(kaaba)
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun testJakartaQiblaBearingAndDistance() {
        // Jakarta: -6.2088° S, 106.8456° E
        // Bearing to Kaaba: ~295.14° (West-Northwest)
        // Distance: ~7912 km
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val qibla = Qibla.fromCoordinates(jakarta)

        assertEquals(295.14, qibla.direction, 0.5)
        assertEquals(7912.0, qibla.distanceKm, 20.0)
    }

    @Test
    fun testLondonQiblaBearingAndDistance() {
        // London: 51.5074° N, -0.1278° W
        // Bearing to Kaaba: ~118.99° (East-Southeast)
        // Distance: ~4788 km
        val london = Coordinates(latitude = 51.5074, longitude = -0.1278)
        val qibla = Qibla.fromCoordinates(london)

        assertEquals(118.99, qibla.direction, 0.5)
        assertEquals(4788.0, qibla.distanceKm, 20.0)
    }

    @Test
    fun testNewYorkQiblaBearingAndDistance() {
        // New York: 40.7128° N, -74.0060° W
        // Bearing to Kaaba: ~58.48° (East-Northeast)
        // Distance: ~10287 km
        val newYork = Coordinates(latitude = 40.7128, longitude = -74.0060)
        val qibla = Qibla.fromCoordinates(newYork)

        assertEquals(58.48, qibla.direction, 0.5)
        assertEquals(10287.0, qibla.distanceKm, 30.0)
    }

    @Test
    fun testTokyoQiblaBearingAndDistance() {
        // Tokyo: 35.6762° N, 139.6503° E
        // Bearing to Kaaba: ~293.02°
        // Distance: ~9488 km
        val tokyo = Coordinates(latitude = 35.6762, longitude = 139.6503)
        val qibla = Qibla.fromCoordinates(tokyo)

        assertEquals(293.02, qibla.direction, 0.5)
        assertEquals(9488.0, qibla.distanceKm, 30.0)
    }

    @Test
    fun testCairoQiblaBearingAndDistance() {
        // Cairo: 30.0444° N, 31.2357° E
        // Bearing to Kaaba: ~136.12° (Southeast)
        // Distance: ~1288 km
        val cairo = Coordinates(latitude = 30.0444, longitude = 31.2357)
        val qibla = Qibla.fromCoordinates(cairo)

        assertEquals(136.12, qibla.direction, 0.5)
        assertEquals(1288.0, qibla.distanceKm, 20.0)
    }

    @Test
    fun testSydneyQiblaBearingAndDistance() {
        // Sydney: -33.8688° S, 151.2093° E
        // Bearing to Kaaba: ~277.50°
        // Distance: ~13236 km
        val sydney = Coordinates(latitude = -33.8688, longitude = 151.2093)
        val qibla = Qibla.fromCoordinates(sydney)

        assertEquals(277.50, qibla.direction, 0.5)
        assertEquals(13236.0, qibla.distanceKm, 30.0)
    }

    @Test
    fun testCalculateBearingAndDistanceStaticMethods() {
        val jakarta = Coordinates(latitude = -6.2088, longitude = 106.8456)
        val bearing = Qibla.calculateBearing(jakarta)
        val distance = Qibla.calculateDistanceKm(jakarta)

        assertEquals(295.14, bearing, 0.5)
        assertEquals(7912.0, distance, 20.0)
    }
}


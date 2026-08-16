package com.adzannotif.core.prayer

import com.adzannotif.core.prayer.internal.DoubleExtensions.toDegrees
import com.adzannotif.core.prayer.internal.DoubleExtensions.toRadians
import com.adzannotif.core.prayer.internal.DoubleExtensions.unwindAngle
import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculates the Qibla direction (initial great-circle bearing) and distance towards the Holy Kaaba in Makkah.
 *
 * @property direction Great-circle bearing in degrees clockwise from True North (0.0° to 360.0°)
 * @property distanceKm Great-circle distance to the Holy Kaaba in kilometers
 */
@Serializable
data class Qibla(
    val direction: Double,
    val distanceKm: Double = 0.0
) {
    companion object {
        /**
         * Geographic coordinates of the Holy Kaaba in Makkah.
         */
        val KAABA_COORDINATES = Coordinates(
            latitude = 21.4225241,
            longitude = 39.8261818
        )

        /**
         * Alias for [KAABA_COORDINATES].
         */
        val MAKKAH_COORDINATES = KAABA_COORDINATES

        /**
         * Mean Earth radius in kilometers according to the IUGG (WGS-84 spherical approximation).
         */
        const val EARTH_RADIUS_KM = 6371.0088

        /**
         * Computes the great circle initial bearing (Qibla direction) in degrees clockwise from True North (0° to 360°).
         */
        fun calculateBearing(coordinates: Coordinates): Double {
            val lat1 = coordinates.latitude.toRadians()
            val lon1 = coordinates.longitude.toRadians()
            val lat2 = KAABA_COORDINATES.latitude.toRadians()
            val lon2 = KAABA_COORDINATES.longitude.toRadians()

            val dLon = lon2 - lon1

            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

            return atan2(y, x).toDegrees().unwindAngle()
        }

        /**
         * Computes the great circle distance to the Holy Kaaba in kilometers using the Haversine formula.
         */
        fun calculateDistanceKm(coordinates: Coordinates): Double {
            val lat1 = coordinates.latitude.toRadians()
            val lon1 = coordinates.longitude.toRadians()
            val lat2 = KAABA_COORDINATES.latitude.toRadians()
            val lon2 = KAABA_COORDINATES.longitude.toRadians()

            val dLat = lat2 - lat1
            val dLon = lon2 - lon1

            val sinHalfLat = sin(dLat / 2.0)
            val sinHalfLon = sin(dLon / 2.0)

            val a = sinHalfLat * sinHalfLat +
                    cos(lat1) * cos(lat2) *
                    sinHalfLon * sinHalfLon
            val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

            return EARTH_RADIUS_KM * c
        }

        /**
         * Computes the [Qibla] instance containing bearing direction and distance in km from given [coordinates].
         */
        fun fromCoordinates(coordinates: Coordinates): Qibla {
            val bearing = calculateBearing(coordinates)
            val distance = calculateDistanceKm(coordinates)
            return Qibla(direction = bearing, distanceKm = distance)
        }
    }
}


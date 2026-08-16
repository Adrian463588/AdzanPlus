package com.adzannotif.domain.model

import com.adzannotif.core.prayer.Coordinates

/**
 * Domain model representing calculated Qibla direction from observer coordinates.
 */
data class QiblaDirection(
    val observerCoordinates: Coordinates,
    val directionDegrees: Double,
    val distanceKm: Double,
)

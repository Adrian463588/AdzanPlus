package com.adzannotif.core.astronomy

data class MoonState(
    val position: CelestialPosition,
    val riseMillis: Long?,
    val setMillis: Long?,
    val transitMillis: Long?,
    val azimuthAtRise: Double?,
    val phase: MoonPhase,
    val illuminationFraction: Double,
    val ageInDays: Double,
    val distanceKm: Double,
    val isApogee: Boolean,
    val isPerigee: Boolean
)

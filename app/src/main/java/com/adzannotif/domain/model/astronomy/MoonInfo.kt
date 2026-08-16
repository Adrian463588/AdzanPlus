package com.adzannotif.domain.model.astronomy

data class MoonInfo(
    val riseMillis: Long?,
    val setMillis: Long?,
    val transitMillis: Long?,
    val azimuth: Double,
    val altitude: Double,
    val azimuthAtRise: Double,
    val phaseName: String,       // MoonPhase.displayName
    val phaseOrdinal: Int,       // 0-7 for icon selection
    val illuminationPercent: Double, // 0-100
    val ageInDays: Double,
    val distanceKm: Double,
    val isApogee: Boolean,
    val isPerigee: Boolean
)

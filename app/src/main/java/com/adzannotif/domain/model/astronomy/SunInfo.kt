package com.adzannotif.domain.model.astronomy

// Domain model wrapping core-astronomy SunState for app use
data class SunInfo(
    val riseMillis: Long?,
    val setMillis: Long?,
    val noonMillis: Long?,
    val azimuth: Double,
    val altitude: Double,
    val azimuthAtRise: Double,
    val azimuthAtSet: Double,
    val currentPhase: String,  // SolarPhase.displayName
    val civilDawnMillis: Long?,
    val civilDuskMillis: Long?,
    val nauticalDawnMillis: Long?,
    val nauticalDuskMillis: Long?,
    val astronomicalDawnMillis: Long?,
    val astronomicalDuskMillis: Long?,
    val morningGoldenHourStartMillis: Long?,
    val morningGoldenHourEndMillis: Long?,
    val eveningGoldenHourStartMillis: Long?,
    val eveningGoldenHourEndMillis: Long?,
    val morningBlueHourStartMillis: Long?,
    val morningBlueHourEndMillis: Long?,
    val eveningBlueHourStartMillis: Long?,
    val eveningBlueHourEndMillis: Long?
)

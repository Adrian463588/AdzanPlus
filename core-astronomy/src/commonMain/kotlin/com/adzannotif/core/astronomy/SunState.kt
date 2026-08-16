package com.adzannotif.core.astronomy

data class SunState(
    val position: CelestialPosition,
    val riseMillis: Long?,
    val setMillis: Long?,
    val noonMillis: Long?,
    val azimuthAtRise: Double?,
    val azimuthAtSet: Double?,
    val twilight: TwilightTimes,
    val goldenBlueHour: GoldenBlueHour,
    val currentPhase: SolarPhase
)

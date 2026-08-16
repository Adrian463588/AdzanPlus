package com.adzannotif.core.astronomy

enum class MoonPhase(val displayName: String, val illuminationRange: ClosedFloatingPointRange<Double>) {
    NEW_MOON("New Moon", 0.0..0.02),
    WAXING_CRESCENT("Waxing Crescent", 0.02..0.48),
    FIRST_QUARTER("First Quarter", 0.48..0.52),
    WAXING_GIBBOUS("Waxing Gibbous", 0.52..0.98),
    FULL_MOON("Full Moon", 0.98..1.0),
    WANING_GIBBOUS("Waning Gibbous", 0.52..0.98),
    LAST_QUARTER("Last Quarter", 0.48..0.52),
    WANING_CRESCENT("Waning Crescent", 0.02..0.48)
}

package com.adzannotif.core.astronomy

sealed class AstronomyEvent {
    abstract val epochMillis: Long
    data class Sunrise(override val epochMillis: Long, val azimuth: Double) : AstronomyEvent()
    data class Sunset(override val epochMillis: Long, val azimuth: Double) : AstronomyEvent()
    data class Moonrise(override val epochMillis: Long, val azimuth: Double) : AstronomyEvent()
    data class Moonset(override val epochMillis: Long) : AstronomyEvent()
    data class GoldenHourStart(override val epochMillis: Long, val isMorning: Boolean) : AstronomyEvent()
    data class GoldenHourEnd(override val epochMillis: Long, val isMorning: Boolean) : AstronomyEvent()
    data class BlueHourStart(override val epochMillis: Long, val isMorning: Boolean) : AstronomyEvent()
    data class BlueHourEnd(override val epochMillis: Long, val isMorning: Boolean) : AstronomyEvent()
    data class FullMoon(override val epochMillis: Long) : AstronomyEvent()
    data class NewMoon(override val epochMillis: Long) : AstronomyEvent()
}

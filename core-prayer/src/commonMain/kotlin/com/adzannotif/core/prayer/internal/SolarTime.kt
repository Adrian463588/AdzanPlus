package com.adzannotif.core.prayer.internal

import com.adzannotif.core.prayer.Coordinates
import com.adzannotif.core.prayer.DateComponents
import com.adzannotif.core.prayer.internal.DoubleExtensions.unwindAngle
import com.adzannotif.core.prayer.internal.DoubleExtensions.unwindHours

internal class SolarTime(
    val coordinates: Coordinates,
    val dateComponents: DateComponents
) {
    val transit: Double
    val sunrise: Double?
    val sunset: Double?

    private val solar: SolarCoordinates
    private val prevSolar: SolarCoordinates
    private val nextSolar: SolarCoordinates

    init {
        val julianDay = AstronomicalMath.julianDay(
            dateComponents.year,
            dateComponents.month,
            dateComponents.day,
            0.0
        )

        prevSolar = SolarCoordinates.fromJulianDay(julianDay - 1.0)
        solar = SolarCoordinates.fromJulianDay(julianDay)
        nextSolar = SolarCoordinates.fromJulianDay(julianDay + 1.0)

        val m0 = (solar.rightAscension - coordinates.longitude - solar.apparentSiderealTime).unwindAngle() / 360.0
        val transitHours = (m0 * 24.0).unwindHours()
        transit = transitHours

        // Standard sunrise/sunset solar altitude angle (-50 arcminutes = -0.8333 degrees)
        val standardAltitude = -50.0 / 60.0
        val ha = AstronomicalMath.hourAngle(standardAltitude, coordinates.latitude, solar.declination)

        if (ha != null) {
            val deltaHours = ha / 15.0
            sunrise = (transit - deltaHours).unwindHours()
            sunset = (transit + deltaHours).unwindHours()
        } else {
            sunrise = null
            sunset = null
        }
    }

    /**
     * Calculates time for a given twilight angle (depression below horizon).
     * e.g. angle = 18.0 for 18 degrees below horizon (solar altitude = -18.0).
     */
    fun hourAngleForTwilight(angleDegrees: Double, isAfterTransit: Boolean): Double? {
        val targetAltitude = -angleDegrees
        val ha = AstronomicalMath.hourAngle(targetAltitude, coordinates.latitude, solar.declination) ?: return null
        val deltaHours = ha / 15.0

        return if (isAfterTransit) {
            (transit + deltaHours).unwindHours()
        } else {
            (transit - deltaHours).unwindHours()
        }
    }

    /**
     * Calculates Asr time using shadow factor.
     */
    fun timeForAsr(shadowFactor: Double): Double? {
        val altitude = AstronomicalMath.asrAltitude(shadowFactor, coordinates.latitude, solar.declination)
        val ha = AstronomicalMath.hourAngle(altitude, coordinates.latitude, solar.declination) ?: return null
        val deltaHours = ha / 15.0
        return (transit + deltaHours).unwindHours()
    }
}

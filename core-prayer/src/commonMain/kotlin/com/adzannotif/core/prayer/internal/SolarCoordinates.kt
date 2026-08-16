package com.adzannotif.core.prayer.internal

import com.adzannotif.core.prayer.internal.DoubleExtensions.toRadians
import kotlin.math.sin

/**
 * Solar coordinates (Declination, Right Ascension, Apparent Sidereal Time).
 */
internal data class SolarCoordinates(
    val declination: Double,
    val rightAscension: Double,
    val apparentSiderealTime: Double
) {
    companion object {
        fun fromJulianDay(julianDay: Double): SolarCoordinates {
            val t = AstronomicalMath.julianCentury(julianDay)
            val l0 = AstronomicalMath.meanSolarLongitude(t)
            val m = AstronomicalMath.meanSolarAnomaly(t)
            val c = AstronomicalMath.sunEquationOfCenter(t, m)
            val trueLongitude = AstronomicalMath.sunTrueLongitude(l0, c)
            val meanObliquity = AstronomicalMath.meanObliquityOfTheEcliptic(t)
            val apparentObliquity = AstronomicalMath.apparentObliquityOfTheEcliptic(t, meanObliquity)

            // Apparent longitude with nutation correction
            val omega = (125.04 - 1934.136 * t).toRadians()
            val lambda = trueLongitude - 0.00569 - 0.00478 * sin(omega)

            val declination = AstronomicalMath.declination(lambda, apparentObliquity)
            val rightAscension = AstronomicalMath.rightAscension(lambda, apparentObliquity)
            val gmst = AstronomicalMath.greenwichMeanSiderealTime(julianDay)

            return SolarCoordinates(
                declination = declination,
                rightAscension = rightAscension,
                apparentSiderealTime = gmst
            )
        }
    }
}

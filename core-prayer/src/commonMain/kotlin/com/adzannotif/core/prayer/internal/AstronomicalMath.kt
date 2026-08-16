package com.adzannotif.core.prayer.internal

import com.adzannotif.core.prayer.internal.DoubleExtensions.toDegrees
import com.adzannotif.core.prayer.internal.DoubleExtensions.toRadians
import com.adzannotif.core.prayer.internal.DoubleExtensions.unwindAngle
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

internal object AstronomicalMath {

    /**
     * Calculates the Julian Day from a Gregorian calendar date.
     */
    fun julianDay(year: Int, month: Int, day: Int, hours: Double = 0.0): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }

        val a = y / 100
        val b = 2 - a + (a / 4)

        val i1 = (365.25 * (y + 4716)).toInt()
        val i2 = (30.6001 * (m + 1)).toInt()

        return i1 + i2 + day + b - 1524.5 + (hours / 24.0)
    }

    /**
     * Calculates Julian Century from Julian Day.
     */
    fun julianCentury(julianDay: Double): Double {
        return (julianDay - 2451545.0) / 36525.0
    }

    /**
     * Mean solar longitude in degrees.
     */
    fun meanSolarLongitude(t: Double): Double {
        val l0 = 280.4664567 + 36000.76982779 * t + 0.0003032028 * t * t
        return l0.unwindAngle()
    }

    /**
     * Mean solar anomaly in degrees.
     */
    fun meanSolarAnomaly(t: Double): Double {
        val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + 0.000000038 * t * t * t
        return m.unwindAngle()
    }

    /**
     * Sun's equation of the center in degrees.
     */
    fun sunEquationOfCenter(t: Double, meanAnomaly: Double): Double {
        val mRad = meanAnomaly.toRadians()
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
                (0.019993 - 0.000101 * t) * sin(2.0 * mRad) +
                0.000289 * sin(3.0 * mRad)
        return c
    }

    /**
     * Sun's true longitude in degrees.
     */
    fun sunTrueLongitude(meanLongitude: Double, equationOfCenter: Double): Double {
        return (meanLongitude + equationOfCenter).unwindAngle()
    }

    /**
     * Mean obliquity of the ecliptic in degrees.
     */
    fun meanObliquityOfTheEcliptic(t: Double): Double {
        return 23.439291 - 0.0130042 * t - 0.00000016 * t * t + 0.000000504 * t * t * t
    }

    /**
     * Apparent obliquity of the ecliptic in degrees.
     */
    fun apparentObliquityOfTheEcliptic(t: Double, meanObliquity: Double): Double {
        val omega = (125.04 - 1934.136 * t).toRadians()
        return meanObliquity + 0.00256 * cos(omega)
    }

    /**
     * Sun's right ascension in degrees.
     */
    fun rightAscension(apparentLongitude: Double, apparentObliquity: Double): Double {
        val lRad = apparentLongitude.toRadians()
        val oRad = apparentObliquity.toRadians()
        val alpha = atan2(cos(oRad) * sin(lRad), cos(lRad)).toDegrees()
        return alpha.unwindAngle()
    }

    /**
     * Sun's declination in degrees.
     */
    fun declination(apparentLongitude: Double, apparentObliquity: Double): Double {
        val lRad = apparentLongitude.toRadians()
        val oRad = apparentObliquity.toRadians()
        val delta = asin(sin(oRad) * sin(lRad)).toDegrees()
        return delta
    }

    /**
     * Greenwich Mean Sidereal Time in degrees.
     */
    fun greenwichMeanSiderealTime(julianDay: Double): Double {
        val t = julianCentury(julianDay)
        val gmst0 = 280.46061837 + 360.98564736629 * (julianDay - 2451545.0) +
                0.000387933 * t * t - (t * t * t) / 38710000.0
        return gmst0.unwindAngle()
    }

    /**
     * Altitude of the sun for Asr calculation based on shadow factor.
     */
    fun asrAltitude(shadowFactor: Double, latitude: Double, declination: Double): Double {
        val latRad = latitude.toRadians()
        val decRad = declination.toRadians()
        val noonShadowLength = kotlin.math.abs(tan(latRad - decRad))
        val targetAngleRad = atan2(1.0, shadowFactor + noonShadowLength)
        return targetAngleRad.toDegrees()
    }

    /**
     * Calculates the hour angle (in degrees) for a specific solar altitude angle.
     * Returns null if the sun does not reach the specified altitude angle.
     */
    fun hourAngle(altitudeAngle: Double, latitude: Double, declination: Double): Double? {
        val altRad = altitudeAngle.toRadians()
        val latRad = latitude.toRadians()
        val decRad = declination.toRadians()

        val cosHourAngle = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))

        if (cosHourAngle < -1.0 || cosHourAngle > 1.0) {
            return null
        }

        return acos(cosHourAngle).toDegrees()
    }
}

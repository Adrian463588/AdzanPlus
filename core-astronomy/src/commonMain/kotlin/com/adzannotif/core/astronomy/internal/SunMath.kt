package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.CelestialPosition
import com.adzannotif.core.astronomy.TwilightTimes
import com.adzannotif.core.astronomy.internal.MathUtils.toDegrees
import com.adzannotif.core.astronomy.internal.MathUtils.toRadians
import com.adzannotif.core.astronomy.internal.MathUtils.unwindAngle
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal object SunMath {
    fun julianDay(epochMillis: Long): Double {
        return epochMillis / 86400000.0 + 2440587.5
    }

    private fun julianCentury(julianDay: Double): Double {
        return (julianDay - 2451545.0) / 36525.0
    }

    fun computeSunPosition(observerLat: Double, observerLon: Double, elevationM: Double, epochMillis: Long): CelestialPosition {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)

        val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).unwindAngle()
        val m = (357.52911 + t * (35999.05029 - 0.0001537 * t)).unwindAngle()

        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

        val mRad = m.toRadians()
        val c = (1.914602 - t * (0.004817 + 0.000014 * t)) * sin(mRad) +
                (0.019993 - 0.000101 * t) * sin(2.0 * mRad) +
                0.000289 * sin(3.0 * mRad)

        val trueLong = (l0 + c).unwindAngle()
        val omega = 125.04 - 1934.136 * t
        val lambda = trueLong - 0.00569 - 0.00474 * sin(omega.toRadians())

        val meanObliq = 23.439291 - t * (0.0130042 + t * (0.00000016 - t * 0.000000504))
        val trueObliq = meanObliq + 0.00256 * cos(omega.toRadians())

        val lambdaRad = lambda.toRadians()
        val obliqRad = trueObliq.toRadians()

        val raRad = atan2(cos(obliqRad) * sin(lambdaRad), cos(lambdaRad))
        val decRad = asin(sin(obliqRad) * sin(lambdaRad))

        val gmst0 = 280.46061837 + 360.98564736629 * (jd - 2451545.0) +
                t * t * 0.000387933 - (t * t * t) / 38710000.0
        val gmst = gmst0.unwindAngle()

        val lst = (gmst + observerLon).unwindAngle().toRadians()
        val ha = lst - raRad

        val latRad = observerLat.toRadians()
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)

        val sinDec = sin(decRad)
        val cosDec = cos(decRad)

        val altitudeRad = asin(sinLat * sinDec + cosLat * cosDec * cos(ha))
        val azimuthRad = atan2(sin(ha), cos(ha) * sinLat - tanDec(cosLat, sinDec, cosDec)) + kotlin.math.PI

        val distanceKm = 149597870.7 * (1.00014 - 0.01671 * cos(mRad) - 0.00014 * cos(2 * mRad))

        return CelestialPosition(azimuthRad.toDegrees().unwindAngle(), altitudeRad.toDegrees(), distanceKm)
    }

    private fun tanDec(cosLat: Double, sinDec: Double, cosDec: Double): Double {
        return (sinDec / cosDec) * cosLat
    }

    fun computeSunRise(lat: Double, lon: Double, dateMillis: Long): Long? {
        return findSolarEvent(lat, lon, dateMillis, -0.833, true)
    }

    fun computeSunSet(lat: Double, lon: Double, dateMillis: Long): Long? {
        return findSolarEvent(lat, lon, dateMillis, -0.833, false)
    }

    fun computeSolarNoon(lat: Double, lon: Double, dateMillis: Long): Long {
        var approx = startOfDay(dateMillis) + 43200000L - (lon * 240000L).toLong()
        // Simple convergence
        for (i in 0..2) {
            val jd = julianDay(approx)
            val t = julianCentury(jd)
            val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).unwindAngle()
            val m = (357.52911 + t * (35999.05029 - 0.0001537 * t)).unwindAngle()
            val c = (1.914602 - t * (0.004817 + 0.000014 * t)) * sin(m.toRadians()) +
                    (0.019993 - 0.000101 * t) * sin(2.0 * m.toRadians()) +
                    0.000289 * sin(3.0 * m.toRadians())
            val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
            val y = kotlin.math.tan((23.439291/2.0).toRadians()) * kotlin.math.tan((23.439291/2.0).toRadians())
            
            val eqTime = y * sin(2.0 * l0.toRadians()) - 2.0 * e * sin(m.toRadians()) +
                         4.0 * e * y * sin(m.toRadians()) * cos(2.0 * l0.toRadians()) -
                         0.5 * y * y * sin(4.0 * l0.toRadians()) -
                         1.25 * e * e * sin(2.0 * m.toRadians())
                         
            val eqTimeMins = eqTime.toDegrees() * 4.0
            approx = startOfDay(dateMillis) + 43200000L - (lon * 240000L).toLong() - (eqTimeMins * 60000L).toLong()
        }
        return approx
    }

    fun computeTwilightTimes(lat: Double, lon: Double, dateMillis: Long): TwilightTimes {
        val civilDawn = findSolarEvent(lat, lon, dateMillis, -6.0, true)
        val nauticalDawn = findSolarEvent(lat, lon, dateMillis, -12.0, true)
        val astroDawn = findSolarEvent(lat, lon, dateMillis, -18.0, true)
        val civilDusk = findSolarEvent(lat, lon, dateMillis, -6.0, false)
        val nauticalDusk = findSolarEvent(lat, lon, dateMillis, -12.0, false)
        val astroDusk = findSolarEvent(lat, lon, dateMillis, -18.0, false)
        return TwilightTimes(civilDawn, nauticalDawn, astroDawn, astroDusk, nauticalDusk, civilDusk)
    }

    private fun findSolarEvent(lat: Double, lon: Double, dateMillis: Long, targetAlt: Double, isRising: Boolean): Long? {
        val startOfDay = startOfDay(dateMillis)
        val noon = computeSolarNoon(lat, lon, dateMillis)
        
        val jdNoon = julianDay(noon)
        val t = julianCentury(jdNoon)
        val meanObliq = 23.439291 - t * (0.0130042 + t * (0.00000016 - t * 0.000000504))
        val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).unwindAngle()
        val m = (357.52911 + t * (35999.05029 - 0.0001537 * t)).unwindAngle()
        val c = (1.914602 - t * (0.004817 + 0.000014 * t)) * sin(m.toRadians()) +
                (0.019993 - 0.000101 * t) * sin(2.0 * m.toRadians()) +
                0.000289 * sin(3.0 * m.toRadians())
        val lambda = (l0 + c).unwindAngle()
        val decRad = asin(sin(meanObliq.toRadians()) * sin(lambda.toRadians()))
        
        val latRad = lat.toRadians()
        val altRad = targetAlt.toRadians()
        
        val cosHa = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        if (cosHa < -1.0 || cosHa > 1.0) return null // Polar day/night
        
        val ha = acos(cosHa).toDegrees()
        val offset = (ha * 240000L).toLong()
        return if (isRising) noon - offset else noon + offset
    }

    private fun startOfDay(millis: Long): Long {
        val tzOffset = 0L // Assuming UTC input or we align to roughly local day
        // To be simpler, just round to nearest day in UTC
        return (millis / 86400000L) * 86400000L
    }
}

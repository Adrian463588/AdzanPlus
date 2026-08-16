package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.CelestialPosition
import com.adzannotif.core.astronomy.MoonPhase
import com.adzannotif.core.astronomy.internal.MathUtils.toDegrees
import com.adzannotif.core.astronomy.internal.MathUtils.toRadians
import com.adzannotif.core.astronomy.internal.MathUtils.unwindAngle
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

public object MoonMath {
    private fun julianDay(epochMillis: Long): Double {
        return epochMillis / 86400000.0 + 2440587.5
    }

    private fun julianCentury(julianDay: Double): Double {
        return (julianDay - 2451545.0) / 36525.0
    }

    fun computeMoonPosition(lat: Double, lon: Double, elevationM: Double, epochMillis: Long): CelestialPosition {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)

        val lPrime = (218.3164477 + 481267.88123421 * t - 0.0015786 * t * t).unwindAngle()
        val d = (297.8501921 + 445267.1114034 * t - 0.0018819 * t * t).unwindAngle()
        val m = (357.5291092 + 35999.0502909 * t - 0.0001536 * t * t).unwindAngle()
        val mPrime = (134.9633964 + 477198.8675055 * t + 0.0087414 * t * t).unwindAngle()
        val f = (93.2720950 + 483202.0175233 * t - 0.0036539 * t * t).unwindAngle()

        val lPrimeRad = lPrime.toRadians()
        val dRad = d.toRadians()
        val mRad = m.toRadians()
        val mPrimeRad = mPrime.toRadians()
        val fRad = f.toRadians()

        val sumL = 22640.0 * sin(mPrimeRad) - 4586.0 * sin(mPrimeRad - 2 * dRad) +
                2370.0 * sin(2 * dRad) + 192.0 * sin(mPrimeRad + 2 * dRad) -
                110.0 * sin(mPrimeRad + mRad) - 148.0 * sin(mPrimeRad - mRad) -
                206.0 * sin(mPrimeRad + mRad - 2 * dRad) -
                125.0 * sin(dRad) - 212.0 * sin(2 * mPrimeRad - 2 * dRad) -
                412.0 * sin(2 * mPrimeRad) - 55.0 * sin(2 * fRad - 2 * dRad)

        val lambda = lPrime + sumL / 1000000.0

        val sumB = 5128.0 * sin(fRad) + 280.0 * sin(mPrimeRad + fRad) + 277.0 * sin(mPrimeRad - fRad) +
                173.0 * sin(fRad - 2 * dRad) + 55.0 * sin(mPrimeRad + fRad - 2 * dRad) +
                46.0 * sin(mPrimeRad - fRad + 2 * dRad) + 32.0 * sin(fRad + 2 * dRad) +
                15.0 * sin(mPrimeRad + fRad + 2 * dRad)
        val beta = sumB / 1000000.0

        val distanceKm = computeMoonDistanceKm(epochMillis)

        val lambdaRad = lambda.toRadians()
        val betaRad = beta.toRadians()
        val obliq = 23.439291 - 0.0130042 * t
        val obliqRad = obliq.toRadians()

        val raRad = atan2(sin(lambdaRad) * cos(obliqRad) - tan(betaRad) * sin(obliqRad), cos(lambdaRad))
        val decRad = asin(sin(betaRad) * cos(obliqRad) + cos(betaRad) * sin(obliqRad) * sin(lambdaRad))

        val gmst0 = 280.46061837 + 360.98564736629 * (jd - 2451545.0) +
                t * t * 0.000387933 - (t * t * t) / 38710000.0
        val lst = (gmst0.unwindAngle() + lon).unwindAngle().toRadians()

        val ha = lst - raRad
        val latRad = lat.toRadians()
        val altitudeRad = asin(sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(ha))
        val azimuthRad = atan2(sin(ha), cos(ha) * sin(latRad) - tan(decRad) * cos(latRad)) + kotlin.math.PI

        return CelestialPosition(azimuthRad.toDegrees().unwindAngle(), altitudeRad.toDegrees(), distanceKm)
    }

    private fun tan(x: Double): Double = sin(x) / cos(x)

    fun computeMoonPhase(epochMillis: Long): MoonPhase {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)
        val d = (297.8501921 + 445267.1114034 * t - 0.0018819 * t * t).unwindAngle()

        val fraction = d / 360.0
        return when {
            fraction < 1.0 / 16.0 || fraction >= 15.0 / 16.0 -> MoonPhase.NEW_MOON
            fraction < 3.0 / 16.0 -> MoonPhase.WAXING_CRESCENT
            fraction < 5.0 / 16.0 -> MoonPhase.FIRST_QUARTER
            fraction < 7.0 / 16.0 -> MoonPhase.WAXING_GIBBOUS
            fraction < 9.0 / 16.0 -> MoonPhase.FULL_MOON
            fraction < 11.0 / 16.0 -> MoonPhase.WANING_GIBBOUS
            fraction < 13.0 / 16.0 -> MoonPhase.LAST_QUARTER
            else -> MoonPhase.WANING_CRESCENT
        }
    }

    fun computeMoonIllumination(epochMillis: Long): Double {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)
        val d = (297.8501921 + 445267.1114034 * t).unwindAngle()
        return (1.0 - cos(d.toRadians())) / 2.0
    }

    fun computeMoonAgeInDays(epochMillis: Long): Double {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)
        val d = (297.8501921 + 445267.1114034 * t).unwindAngle()
        return (d / 360.0) * 29.530588853
    }

    fun computeMoonDistanceKm(epochMillis: Long): Double {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)
        val mPrime = (134.9633964 + 477198.8675055 * t).unwindAngle()
        val d = (297.8501921 + 445267.1114034 * t).unwindAngle()
        return 385000.56 - 20905.0 * cos(mPrime.toRadians()) - 3699.0 * cos((2 * d - mPrime).toRadians())
    }

    fun computeMoonRise(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        return findMoonEvent(lat, lon, dateMillis, isRising = true, timeZone = timeZone)
    }

    fun computeMoonSet(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        return findMoonEvent(lat, lon, dateMillis, isRising = false, timeZone = timeZone)
    }

    fun computeMoonTransit(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        // Approximate transit time by finding max altitude in 24h
        val startOfDay = startOfDay(dateMillis, timeZone)
        var maxAlt = -90.0
        var transitTime = startOfDay
        for (i in 0..24) {
            val ms = startOfDay + i * 3600000L
            val pos = computeMoonPosition(lat, lon, 0.0, ms)
            if (pos.altitude > maxAlt) {
                maxAlt = pos.altitude
                transitTime = ms
            }
        }
        return transitTime
    }
    
    private fun findMoonEvent(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        isRising: Boolean,
        timeZone: TimeZone,
    ): Long? {
        val startOfDay = startOfDay(dateMillis, timeZone)
        var prevAlt = computeMoonPosition(lat, lon, 0.0, startOfDay).altitude
        for (i in 1..144) { // 10-minute intervals
            val ms = startOfDay + i * 600000L
            val currAlt = computeMoonPosition(lat, lon, 0.0, ms).altitude
            
            if (isRising && prevAlt <= 0.0 && currAlt > 0.0) {
                return ms
            } else if (!isRising && prevAlt >= 0.0 && currAlt < 0.0) {
                return ms
            }
            prevAlt = currAlt
        }
        return null
    }

    private fun startOfDay(millis: Long, timeZone: TimeZone): Long {
        val localDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
        return localDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
    }
}

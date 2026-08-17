package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.CelestialPosition
import com.adzannotif.core.astronomy.TwilightTimes
import com.adzannotif.core.astronomy.internal.MathUtils.toDegrees
import com.adzannotif.core.astronomy.internal.MathUtils.toRadians
import com.adzannotif.core.astronomy.internal.MathUtils.unwindAngle
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

public object SunMath {
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

    fun computeSunRise(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        return findSolarEvent(lat, lon, dateMillis, -0.833, true, timeZone)
    }

    fun computeSunSet(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        return findSolarEvent(lat, lon, dateMillis, -0.833, false, timeZone)
    }

    fun computeSolarNoon(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long {
        val localDate = Instant
            .fromEpochMilliseconds(dateMillis)
            .toLocalDateTime(timeZone)
            .date
        val localNoon = localDate.atTime(12, 0).toInstant(timeZone).toEpochMilliseconds()
        // Start from the selected civil date's local noon. This remains valid
        // when a DST transition changes the offset between midnight and noon.
        var approx = localNoon +
            ((zoneOffsetMinutesAt(localNoon, timeZone) - lon * 4.0) * 60000.0).toLong()
        for (i in 0..2) {
            val zoneOffsetMinutes = zoneOffsetMinutesAt(approx, timeZone)
            val eqTimeMins = equationOfTimeMinutes(approx)
            approx = localNoon +
                ((zoneOffsetMinutes - lon * 4.0) * 60000.0).toLong() -
                (eqTimeMins * 60000L).toLong()
        }
        return approx
    }

    fun computeTwilightTimes(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): TwilightTimes {
        val civilDawn = findSolarEvent(lat, lon, dateMillis, -6.0, true, timeZone)
        val nauticalDawn = findSolarEvent(lat, lon, dateMillis, -12.0, true, timeZone)
        val astroDawn = findSolarEvent(lat, lon, dateMillis, -18.0, true, timeZone)
        val civilDusk = findSolarEvent(lat, lon, dateMillis, -6.0, false, timeZone)
        val nauticalDusk = findSolarEvent(lat, lon, dateMillis, -12.0, false, timeZone)
        val astroDusk = findSolarEvent(lat, lon, dateMillis, -18.0, false, timeZone)
        return TwilightTimes(civilDawn, nauticalDawn, astroDawn, astroDusk, nauticalDusk, civilDusk)
    }

    private fun findSolarEvent(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        targetAlt: Double,
        isRising: Boolean,
        timeZone: TimeZone,
    ): Long? {
        val localDate = Instant.fromEpochMilliseconds(dateMillis)
            .toLocalDateTime(timeZone)
            .date
        val dayStart = localDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        val nextDayStart = localDate.plus(DatePeriod(days = 1))
            .atTime(0, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()
        return findAltitudeCrossing(
            lat = lat,
            lon = lon,
            startMillis = dayStart,
            endMillis = nextDayStart,
            targetAltitudeDeg = targetAlt,
            isRising = isRising,
        )
    }

    private fun equationOfTimeMinutes(epochMillis: Long): Double {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)
        val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).unwindAngle()
        val m = (357.52911 + t * (35999.05029 - 0.0001537 * t)).unwindAngle()
        val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val obliquity = 23.439291 - t * (0.0130042 + t * (0.00000016 - t * 0.000000504))
        val y = tan((obliquity / 2.0).toRadians()).let { it * it }
        val l0Rad = l0.toRadians()
        val mRad = m.toRadians()
        val equation = y * sin(2.0 * l0Rad) -
            2.0 * eccentricity * sin(mRad) +
            4.0 * eccentricity * y * sin(mRad) * cos(2.0 * l0Rad) -
            0.5 * y * y * sin(4.0 * l0Rad) -
            1.25 * eccentricity * eccentricity * sin(2.0 * mRad)
        return equation.toDegrees() * 4.0
    }

    private fun findAltitudeCrossing(
        lat: Double,
        lon: Double,
        startMillis: Long,
        endMillis: Long,
        targetAltitudeDeg: Double,
        isRising: Boolean,
    ): Long? {
        if (endMillis <= startMillis) return null

        var beforeMillis = startMillis
        var beforeAltitude = computeSunPosition(lat, lon, 0.0, beforeMillis).altitude
        while (beforeMillis < endMillis) {
            val afterMillis = (beforeMillis + SEARCH_STEP_MILLIS).coerceAtMost(endMillis)
            val afterAltitude = computeSunPosition(lat, lon, 0.0, afterMillis).altitude
            val crossed = if (isRising) {
                beforeAltitude < targetAltitudeDeg && afterAltitude >= targetAltitudeDeg
            } else {
                beforeAltitude > targetAltitudeDeg && afterAltitude <= targetAltitudeDeg
            }
            if (crossed) {
                var low = beforeMillis
                var high = afterMillis
                repeat(24) {
                    val middle = (low + high) / 2
                    val altitude = computeSunPosition(lat, lon, 0.0, middle).altitude
                    if ((isRising && altitude < targetAltitudeDeg) ||
                        (!isRising && altitude > targetAltitudeDeg)
                    ) {
                        low = middle
                    } else {
                        high = middle
                    }
                }
                return (low + high) / 2
            }
            beforeMillis = afterMillis
            beforeAltitude = afterAltitude
        }
        return null
    }

    private const val SEARCH_STEP_MILLIS = 10 * 60 * 1000L

    private fun zoneOffsetMinutesAt(epochMillis: Long, timeZone: TimeZone): Double {
        val localDateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val localAsUtc = localDateTime.toInstant(TimeZone.UTC)
        return (localAsUtc.epochSeconds - epochMillis / 1000L) / 60.0
    }
}

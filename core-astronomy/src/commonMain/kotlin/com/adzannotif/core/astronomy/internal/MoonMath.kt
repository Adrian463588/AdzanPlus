package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.CelestialPosition
import com.adzannotif.core.astronomy.MoonPhase
import com.adzannotif.core.astronomy.internal.MathUtils.toDegrees
import com.adzannotif.core.astronomy.internal.MathUtils.toRadians
import com.adzannotif.core.astronomy.internal.MathUtils.unwindAngle
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.datetime.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
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
        val d = computeMeanElongation(epochMillis)

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

    /**
     * Finds a true phase-angle crossing in the requested local civil day.
     * The underlying elongation is the same astronomical term used by the
     * phase and illumination calculations; no calendar date is hardcoded.
     */
    fun computePhaseEvent(
        dateMillis: Long,
        targetPhase: MoonPhase,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        val localDate = Instant.fromEpochMilliseconds(dateMillis)
            .toLocalDateTime(timeZone)
            .date
        val startMillis = localDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        val endMillis = localDate.plus(DatePeriod(days = 1))
            .atTime(0, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()
        val targetAngle = if (targetPhase == MoonPhase.FULL_MOON) 180.0 else 0.0
        var previousMillis = startMillis
        var previousAngle = computeMeanElongation(previousMillis)

        for (step in 1..24) {
            val currentMillis = startMillis + (endMillis - startMillis) * step / 24
            val currentAngle = computeMeanElongation(currentMillis)
            val advance = forwardAngle(previousAngle, currentAngle)
            val targetOffset = forwardAngle(previousAngle, targetAngle)

            if (targetOffset > 0.0 && targetOffset <= advance) {
                var low = previousMillis
                var high = currentMillis
                repeat(40) {
                    val middle = (low + high) / 2
                    val middleOffset = forwardAngle(previousAngle, computeMeanElongation(middle))
                    if (middleOffset >= targetOffset) high = middle else low = middle
                }
                return high
            }

            previousMillis = currentMillis
            previousAngle = currentAngle
        }
        return null
    }

    fun computeMoonIllumination(epochMillis: Long): Double {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)
        val d = (297.8501921 + 445267.1114034 * t).unwindAngle()
        return (1.0 - cos(d.toRadians())) / 2.0
    }

    fun computeMoonAgeInDays(epochMillis: Long): Double {
        val d = computeMeanElongation(epochMillis)
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
        val startOfDay = startOfDay(dateMillis, timeZone)
        val endOfDay = nextLocalDay(startOfDay, timeZone)
        return findMoonEvent(
            lat = lat,
            lon = lon,
            startMillis = startOfDay,
            endMillis = endOfDay,
            isRising = true,
        )
    }

    /**
     * Returns the first rise strictly after [fromMillis], searching the
     * selected local day and the following local day. This is the contract
     * used by live widgets, where today's rise may already be in the past.
     */
    fun computeNextMoonRise(
        lat: Double,
        lon: Double,
        fromMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        val localDate = Instant.fromEpochMilliseconds(fromMillis)
            .toLocalDateTime(timeZone)
            .date
        val startOfDay = localDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        val endOfFollowingDay = localDate
            .plus(DatePeriod(days = 2))
            .atTime(0, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()
        val searchStart = if (fromMillis == Long.MAX_VALUE) return null else fromMillis + 1L
        return findMoonEvent(
            lat = lat,
            lon = lon,
            startMillis = maxOf(startOfDay, searchStart),
            endMillis = endOfFollowingDay,
            isRising = true,
        )?.takeIf { it > fromMillis }
    }

    fun computeMoonSet(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        val startOfDay = startOfDay(dateMillis, timeZone)
        return findMoonEvent(
            lat = lat,
            lon = lon,
            startMillis = startOfDay,
            endMillis = nextLocalDay(startOfDay, timeZone),
            isRising = false,
        )
    }

    fun computeMoonTransit(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): Long? {
        // Approximate transit time within the selected civil day. A fixed
        // 24-hour interval leaks into the next local date on DST changes.
        val startOfDay = startOfDay(dateMillis, timeZone)
        val endOfDay = nextLocalDay(startOfDay, timeZone)
        val sampleCount = ceil((endOfDay - startOfDay) / 3600000.0).toInt()
        var maxAlt = -90.0
        var transitTime = startOfDay
        for (i in 0..sampleCount) {
            val ms = (startOfDay + i * 3600000L).coerceAtMost(endOfDay)
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
        startMillis: Long,
        endMillis: Long,
        isRising: Boolean,
    ): Long? {
        if (endMillis <= startMillis) return null
        val targetAltitude = 0.0
        var previousMillis = startMillis
        var previousAltitude = computeMoonPosition(lat, lon, 0.0, previousMillis).altitude
        while (previousMillis < endMillis) {
            val currentMillis = (previousMillis + SEARCH_STEP_MILLIS).coerceAtMost(endMillis)
            val currentAltitude = computeMoonPosition(lat, lon, 0.0, currentMillis).altitude
            val crossed = if (isRising) {
                previousAltitude < targetAltitude && currentAltitude >= targetAltitude
            } else {
                previousAltitude > targetAltitude && currentAltitude <= targetAltitude
            }
            if (crossed) {
                var low = previousMillis
                var high = currentMillis
                repeat(24) {
                    val middle = (low + high) / 2
                    val altitude = computeMoonPosition(lat, lon, 0.0, middle).altitude
                    if ((isRising && altitude < targetAltitude) ||
                        (!isRising && altitude > targetAltitude)
                    ) {
                        low = middle
                    } else {
                        high = middle
                    }
                }
                return (low + high) / 2
            }
            previousMillis = currentMillis
            previousAltitude = currentAltitude
        }
        return null
    }

    private fun startOfDay(millis: Long, timeZone: TimeZone): Long {
        val localDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
        return localDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
    }

    private fun nextLocalDay(startOfDayMillis: Long, timeZone: TimeZone): Long {
        val localDate = Instant.fromEpochMilliseconds(startOfDayMillis)
            .toLocalDateTime(timeZone)
            .date
        return localDate.plus(DatePeriod(days = 1))
            .atTime(0, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()
    }

    private fun computeMeanElongation(epochMillis: Long): Double {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)
        return (297.8501921 + 445267.1114034 * t - 0.0018819 * t * t).unwindAngle()
    }

    private fun forwardAngle(from: Double, to: Double): Double =
        ((to - from) % 360.0 + 360.0) % 360.0

    private const val SEARCH_STEP_MILLIS = 10 * 60 * 1000L
}

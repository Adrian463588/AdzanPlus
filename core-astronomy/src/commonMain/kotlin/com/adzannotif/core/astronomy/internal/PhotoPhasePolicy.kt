package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.GoldenBlueHour
import com.adzannotif.core.astronomy.GoldenBlueHourWindow
import com.adzannotif.core.astronomy.SolarPhase
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

public object PhotoPhasePolicy {
    private const val SEARCH_STEP_MILLIS = 10 * 60 * 1000L

    const val GOLDEN_HOUR_LOW_DEG = -4.0
    const val GOLDEN_HOUR_HIGH_DEG = 6.0
    const val BLUE_HOUR_LOW_DEG = -6.0
    const val BLUE_HOUR_HIGH_DEG = -4.0

    fun classifySolarPhase(altitudeDeg: Double): SolarPhase {
        return when {
            altitudeDeg > GOLDEN_HOUR_HIGH_DEG -> SolarPhase.DAY
            // Civil twilight wins over the overlapping photography interval.
            // Golden Hour is therefore the above-horizon part of [-4°, +6°].
            altitudeDeg >= 0.0 -> SolarPhase.GOLDEN_HOUR
            altitudeDeg >= GOLDEN_HOUR_LOW_DEG -> SolarPhase.CIVIL_TWILIGHT
            altitudeDeg >= BLUE_HOUR_LOW_DEG -> SolarPhase.BLUE_HOUR
            altitudeDeg > -12.0 -> SolarPhase.NAUTICAL_TWILIGHT
            altitudeDeg > -18.0 -> SolarPhase.ASTRONOMICAL_TWILIGHT
            else -> SolarPhase.NIGHT
        }
    }

    fun computeGoldenBlueHour(
        lat: Double,
        lon: Double,
        dateMillis: Long,
        timeZone: TimeZone = TimeZone.UTC
    ): GoldenBlueHour {
        val localDate = Instant
            .fromEpochMilliseconds(dateMillis)
            .toLocalDateTime(timeZone)
            .date
        val dayStart = localDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        val nextDayStart = localDate
            .plus(DatePeriod(days = 1))
            .atTime(0, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()
        val noon = SunMath.computeSolarNoon(lat, lon, dateMillis, timeZone)
            .coerceIn(dayStart, nextDayStart)

        // Search only inside the selected location's civil day. This keeps
        // DST transitions and locations whose solar noon is not near 12:00
        // from leaking an event into the adjacent local date.
        val morningBlueStart = findAltitudeCrossing(
            lat, lon, dayStart, noon, BLUE_HOUR_LOW_DEG, isRising = true,
        )
        val morningBlueEnd = findAltitudeCrossing(
            lat, lon, dayStart, noon, BLUE_HOUR_HIGH_DEG, isRising = true,
        )
        val morningGoldenStart = findAltitudeCrossing(
            lat, lon, dayStart, noon, GOLDEN_HOUR_LOW_DEG, isRising = true,
        )
        val morningGoldenEnd = findAltitudeCrossing(
            lat, lon, dayStart, noon, GOLDEN_HOUR_HIGH_DEG, isRising = true,
        )
        val eveningGoldenStart = findAltitudeCrossing(
            lat, lon, noon, nextDayStart, GOLDEN_HOUR_HIGH_DEG, isRising = false,
        )
        val eveningGoldenEnd = findAltitudeCrossing(
            lat, lon, noon, nextDayStart, GOLDEN_HOUR_LOW_DEG, isRising = false,
        )
        val eveningBlueStart = findAltitudeCrossing(
            lat, lon, noon, nextDayStart, BLUE_HOUR_HIGH_DEG, isRising = false,
        )
        val eveningBlueEnd = findAltitudeCrossing(
            lat, lon, noon, nextDayStart, BLUE_HOUR_LOW_DEG, isRising = false,
        )

        val morningBlue = windowOrNull(morningBlueStart, morningBlueEnd)
        val morningGolden = windowOrNull(morningGoldenStart, morningGoldenEnd)
        val eveningGolden = windowOrNull(eveningGoldenStart, eveningGoldenEnd)
        val eveningBlue = windowOrNull(eveningBlueStart, eveningBlueEnd)

        return GoldenBlueHour(morningBlue, morningGolden, eveningGolden, eveningBlue)
    }

    private fun windowOrNull(start: Long?, end: Long?): GoldenBlueHourWindow? {
        return if (start != null && end != null && start < end) {
            GoldenBlueHourWindow(start, end)
        } else {
            null
        }
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

        var previousMillis = startMillis
        var previousAltitude = SunMath.computeSunPosition(lat, lon, 0.0, previousMillis).altitude
        var currentMillis = (previousMillis + SEARCH_STEP_MILLIS).coerceAtMost(endMillis)

        while (true) {
            val currentAltitude = SunMath.computeSunPosition(lat, lon, 0.0, currentMillis).altitude
            val crossed = if (isRising) {
                previousAltitude < targetAltitudeDeg && currentAltitude >= targetAltitudeDeg
            } else {
                previousAltitude > targetAltitudeDeg && currentAltitude <= targetAltitudeDeg
            }
            if (crossed) {
                return refineCrossing(
                    lat = lat,
                    lon = lon,
                    beforeMillis = previousMillis,
                    afterMillis = currentMillis,
                    targetAltitudeDeg = targetAltitudeDeg,
                    isRising = isRising,
                )
            }
            if (currentMillis == endMillis) return null
            previousMillis = currentMillis
            previousAltitude = currentAltitude
            currentMillis = (currentMillis + SEARCH_STEP_MILLIS).coerceAtMost(endMillis)
        }
    }

    private fun refineCrossing(
        lat: Double,
        lon: Double,
        beforeMillis: Long,
        afterMillis: Long,
        targetAltitudeDeg: Double,
        isRising: Boolean,
    ): Long {
        var low = beforeMillis
        var high = afterMillis
        repeat(22) {
            val middle = (low + high) / 2
            val altitude = SunMath.computeSunPosition(lat, lon, 0.0, middle).altitude
            if ((isRising && altitude < targetAltitudeDeg) || (!isRising && altitude > targetAltitudeDeg)) {
                low = middle
            } else {
                high = middle
            }
        }
        return (low + high) / 2
    }
}

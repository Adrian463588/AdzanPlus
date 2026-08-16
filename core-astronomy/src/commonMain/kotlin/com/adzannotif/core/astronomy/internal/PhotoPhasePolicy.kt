package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.GoldenBlueHour
import com.adzannotif.core.astronomy.GoldenBlueHourWindow
import com.adzannotif.core.astronomy.SolarPhase
import kotlinx.datetime.TimeZone

public object PhotoPhasePolicy {
    const val GOLDEN_HOUR_LOW_DEG = -4.0
    const val GOLDEN_HOUR_HIGH_DEG = 6.0
    const val BLUE_HOUR_LOW_DEG = -6.0
    const val BLUE_HOUR_HIGH_DEG = -4.0

    fun classifySolarPhase(altitudeDeg: Double): SolarPhase {
        return when {
            altitudeDeg > GOLDEN_HOUR_HIGH_DEG -> SolarPhase.DAY
            altitudeDeg > GOLDEN_HOUR_LOW_DEG -> SolarPhase.GOLDEN_HOUR
            altitudeDeg > BLUE_HOUR_LOW_DEG -> SolarPhase.BLUE_HOUR
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
        val noon = SunMath.computeSolarNoon(lat, lon, dateMillis, timeZone)
        val dayStart = noon - 43200000L // 12 hours before solar noon (local solar midnight)

        var mBHS: Long? = null
        var mBHE: Long? = null
        var mGHS: Long? = null
        var mGHE: Long? = null
        var eGHS: Long? = null
        var eGHE: Long? = null
        var eBHS: Long? = null
        var eBHE: Long? = null

        var prevAlt = SunMath.computeSunPosition(lat, lon, 0.0, dayStart).altitude
        for (i in 1..1440) { // 1-minute step across 24 hours around solar noon
            val ms = dayStart + i * 60000L
            val alt = SunMath.computeSunPosition(lat, lon, 0.0, ms).altitude

            if (ms < noon) {
                // Morning rising
                if (prevAlt < BLUE_HOUR_LOW_DEG && alt >= BLUE_HOUR_LOW_DEG && mBHS == null) mBHS = ms
                if (prevAlt < BLUE_HOUR_HIGH_DEG && alt >= BLUE_HOUR_HIGH_DEG && mBHE == null) mBHE = ms

                if (prevAlt < GOLDEN_HOUR_LOW_DEG && alt >= GOLDEN_HOUR_LOW_DEG && mGHS == null) mGHS = ms
                if (prevAlt < GOLDEN_HOUR_HIGH_DEG && alt >= GOLDEN_HOUR_HIGH_DEG && mGHE == null) mGHE = ms
            } else {
                // Evening setting
                if (prevAlt > GOLDEN_HOUR_HIGH_DEG && alt <= GOLDEN_HOUR_HIGH_DEG && eGHS == null) eGHS = ms
                if (prevAlt > GOLDEN_HOUR_LOW_DEG && alt <= GOLDEN_HOUR_LOW_DEG && eGHE == null) eGHE = ms

                if (prevAlt > BLUE_HOUR_HIGH_DEG && alt <= BLUE_HOUR_HIGH_DEG && eBHS == null) eBHS = ms
                if (prevAlt > BLUE_HOUR_LOW_DEG && alt <= BLUE_HOUR_LOW_DEG && eBHE == null) eBHE = ms
            }
            prevAlt = alt
        }

        val morningBlue = if (mBHS != null && mBHE != null) GoldenBlueHourWindow(mBHS, mBHE) else null
        val morningGolden = if (mGHS != null && mGHE != null) GoldenBlueHourWindow(mGHS, mGHE) else null
        val eveningGolden = if (eGHS != null && eGHE != null) GoldenBlueHourWindow(eGHS, eGHE) else null
        val eveningBlue = if (eBHS != null && eBHE != null) GoldenBlueHourWindow(eBHS, eBHE) else null

        return GoldenBlueHour(morningBlue, morningGolden, eveningGolden, eveningBlue)
    }
}

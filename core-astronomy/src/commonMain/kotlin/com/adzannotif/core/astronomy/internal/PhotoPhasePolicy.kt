package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.GoldenBlueHour
import com.adzannotif.core.astronomy.GoldenBlueHourWindow
import com.adzannotif.core.astronomy.SolarPhase

internal object PhotoPhasePolicy {
    const val GOLDEN_HOUR_LOW_DEG = -4.0
    const val GOLDEN_HOUR_HIGH_DEG = 6.0
    const val BLUE_HOUR_LOW_DEG = -6.0
    const val BLUE_HOUR_HIGH_DEG = -4.0

    fun classifySolarPhase(altitudeDeg: Double): SolarPhase {
        return when {
            altitudeDeg > GOLDEN_HOUR_HIGH_DEG -> SolarPhase.DAY
            altitudeDeg > GOLDEN_HOUR_LOW_DEG -> SolarPhase.GOLDEN_HOUR
            altitudeDeg > BLUE_HOUR_LOW_DEG -> SolarPhase.BLUE_HOUR
            altitudeDeg > -12.0 -> SolarPhase.CIVIL_TWILIGHT
            altitudeDeg > -18.0 -> SolarPhase.NAUTICAL_TWILIGHT
            else -> SolarPhase.NIGHT // Includes ASTRONOMICAL_TWILIGHT for simplicity below -18
        }
    }

    fun computeGoldenBlueHour(lat: Double, lon: Double, dateMillis: Long): GoldenBlueHour {
        val startOfDay = (dateMillis / 86400000L) * 86400000L
        
        var mBHS: Long? = null
        var mBHE: Long? = null
        var mGHS: Long? = null
        var mGHE: Long? = null
        var eGHS: Long? = null
        var eGHE: Long? = null
        var eBHS: Long? = null
        var eBHE: Long? = null

        val noon = SunMath.computeSolarNoon(lat, lon, dateMillis)

        var prevAlt = SunMath.computeSunPosition(lat, lon, 0.0, startOfDay).altitude
        for (i in 1..1440) { // minute intervals
            val ms = startOfDay + i * 60000L
            val alt = SunMath.computeSunPosition(lat, lon, 0.0, ms).altitude
            
            if (ms < noon) {
                // Morning
                if (prevAlt < BLUE_HOUR_LOW_DEG && alt >= BLUE_HOUR_LOW_DEG) mBHS = ms
                if (prevAlt < BLUE_HOUR_HIGH_DEG && alt >= BLUE_HOUR_HIGH_DEG) mBHE = ms
                
                if (prevAlt < GOLDEN_HOUR_LOW_DEG && alt >= GOLDEN_HOUR_LOW_DEG) mGHS = ms
                if (prevAlt < GOLDEN_HOUR_HIGH_DEG && alt >= GOLDEN_HOUR_HIGH_DEG) mGHE = ms
            } else {
                // Evening
                if (prevAlt > GOLDEN_HOUR_HIGH_DEG && alt <= GOLDEN_HOUR_HIGH_DEG) eGHS = ms
                if (prevAlt > GOLDEN_HOUR_LOW_DEG && alt <= GOLDEN_HOUR_LOW_DEG) eGHE = ms
                
                if (prevAlt > BLUE_HOUR_HIGH_DEG && alt <= BLUE_HOUR_HIGH_DEG) eBHS = ms
                if (prevAlt > BLUE_HOUR_LOW_DEG && alt <= BLUE_HOUR_LOW_DEG) eBHE = ms
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
